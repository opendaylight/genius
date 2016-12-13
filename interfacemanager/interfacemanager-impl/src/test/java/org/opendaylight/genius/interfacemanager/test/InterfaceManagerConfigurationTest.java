/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.TestableJobCoordinatorDataTreeChangeListenerModule;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.interfacemanager.test.xtend.*;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.IfIndexesInterfaceMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.inject.Inject;
import java.math.BigInteger;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.*;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

/**
 * Component tests for interface manager.
 *
 * @author Michael Vorburger
 * @author Faseela K
 */
public class InterfaceManagerConfigurationTest {

    public @Rule MethodRule guice = new GuiceRule(
            InterfaceManagerTestModule.class,
            TestableJobCoordinatorDataTreeChangeListenerModule.class);

    @Inject DataBroker dataBroker;
    @Inject AsyncEventsWaiter asyncEventsWaiter;

    @Before
    public void start() {
        // TODO This is silly, because onSessionInitiated(), or later it's BP
        // equivalent, for clearer testability should just propagate the exception
        assertThat(InterfaceStatusMonitor.getInstance().acquireServiceStatus()).isEqualTo("OPERATIONAL");
    }

    @Test
    public void vlanInterfaceTests() throws Exception {
        // 1. Given

        // 2. When
        // i) parent-interface specified in above vlan configuration comes in operational/ietf-interfaces-state
        InterfaceManagerTestUtil.putInterfaceState(dataBroker, parentInterface, null);
        asyncEventsWaiter.awaitEventsConsumption();

        // ii) Vlan interface written to config/ietf-interfaces DS and corresponding parent-interface is not present
        //     in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(parentInterface).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, interfaceName, parentRefs, L2vlan.class);
        asyncEventsWaiter.awaitEventsConsumption();

        // 3. Then
        // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info was created
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey(parentInterface),
                        new InterfaceChildEntryKey(interfaceName));
        assertEqualBeans(ExpectedInterfaceChildEntry.interfaceChildEntry(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).checkedGet().get());

        // Then
        // a) check if operational/ietf-interfaces-state is populated for the vlan interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifaceState =
                dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(interfaceName)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(), ifaceState);

        // b) check if lport-tag to interface mapping is created
        InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = InstanceIdentifier.builder(IfIndexesInterfaceMap.class).child(
                IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
        Assert.assertEquals(interfaceName, dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                ifIndexInterfaceInstanceIdentifier).checkedGet().get().getInterfaceName());

        // c) check expected flow entries were created in Interface Ingress Table
        BigInteger dpnId = BigInteger.valueOf(1);
        String ingressFlowRef = FlowBasedServicesUtils.getFlowRef(NwConstants.VLAN_INTERFACE_INGRESS_TABLE, dpnId, interfaceName);
        FlowKey ingressFlowKey = new FlowKey(new FlowId(ingressFlowRef));
        Node nodeDpn = InterfaceManagerTestUtil.buildInventoryDpnNode(dpnId);
        InstanceIdentifier<Flow> ingressFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class,ingressFlowKey).build();
        assertEqualBeans(ExpectedFlowEntries.newIngressFlow(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION, ingressFlowInstanceId).checkedGet().get());

        // d) check if default egress service is bound on the interface
        InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier = InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, ServiceModeEgress.class))
                .child(BoundServices.class, new BoundServicesKey(NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)).build();
        assertEqualBeans(ExpectedServicesInfo.newboundService(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION, boundServicesInstanceIdentifier).checkedGet().get());

        //Delete test
        // iii) vlan interface is deleted from config/ietf-interfaces
        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, interfaceName);
        asyncEventsWaiter.awaitEventsConsumption();

        // 3. Then
        // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info is deleted

        // TODO Later use nicer abstraction for DB access here.. see ElanServiceTest
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).get());

        // Then
        // a) check if operational/ietf-interfaces-state is deleted for the vlan interface
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(interfaceName)).get());

        // b) check if lport-tag to interface mapping is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                ifIndexInterfaceInstanceIdentifier).get());
    }

    @Test public void newTunnelInterface() throws Exception {
        // 1. Given
        BigInteger dpnId = BigInteger.valueOf(1);
        // 2. When
        // i) dpn-id specified above configuration comes in operational/network-topology
        OvsdbSoutbboundTestUtil.createBridge(dataBroker);
        asyncEventsWaiter.awaitEventsConsumption();

        // ii) Vlan interface written to config/ietf-interfaces DS and corresponding parent-interface is not present
        //     in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dpnId).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, tunnelInterfaceName, parentRefs, Tunnel.class);
        asyncEventsWaiter.awaitEventsConsumption();

        // iii) tunnel interface comes up in operational/ietf-interfaces-state
        InterfaceManagerTestUtil.putInterfaceState(dataBroker, tunnelInterfaceName, Tunnel.class);
        asyncEventsWaiter.awaitEventsConsumption();

        // 3. Then
        // a) check expected bridge-interface mapping in odl-interface-meta/config/bridge-interface-info was created
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpnId);
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey = new BridgeInterfaceEntryKey(tunnelInterfaceName);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey, bridgeInterfaceEntryKey);
        BridgeInterfaceEntryBuilder entryBuilder = new BridgeInterfaceEntryBuilder().setKey(bridgeInterfaceEntryKey)
                .setInterfaceName(tunnelInterfaceName);
        // TODO Later use nicer abstraction for DB access here.. see ElanServiceTest
        assertEqualBeans(InterfaceMeta.newBridgeInterface(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, bridgeInterfaceEntryIid).checkedGet().get());

        // Then
        // a) check if termination end point is created in config/network-topology
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> bridgeIid =
                OvsdbSoutbboundTestUtil.createInstanceIdentifier("192.168.56.101", 6640,  "s2");
        InstanceIdentifier<TerminationPoint> tpIid = InterfaceManagerTestUtil.getTerminationPointId(bridgeIid,
                tunnelInterfaceName);
        assertEqualBeans(ExpectedTerminationPoint.newTerminationPoint(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).checkedGet().get());


        //Delete test
        // iii) tunnel interface is deleted from config/ietf-interfaces
        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, tunnelInterfaceName);
        asyncEventsWaiter.awaitEventsConsumption();

        // Then
        // a) check if tunnel is deleted from bridge-interface-info
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, bridgeInterfaceEntryIid).get());

        // b) check if termination end point is deleted in config/network-topology
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).get());

    }
}
