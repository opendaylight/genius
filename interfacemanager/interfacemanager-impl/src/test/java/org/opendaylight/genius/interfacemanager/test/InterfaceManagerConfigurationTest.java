/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.interfaceName;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.parentInterface;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.tunnelInterfaceName;
import static org.opendaylight.genius.mdsalutil.NwConstants.DEFAULT_EGRESS_SERVICE_INDEX;
import static org.opendaylight.genius.mdsalutil.NwConstants.VLAN_INTERFACE_INGRESS_TABLE;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import com.google.common.base.Optional;
import java.math.BigInteger;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedFlowEntries;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceChildEntry;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceState;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedServicesInfo;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedTerminationPoint;
import org.opendaylight.genius.interfacemanager.test.xtend.InterfaceMeta;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
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

/**
 * Component tests for interface manager.
 *
 * @author Michael Vorburger
 * @author Faseela K
 */
@SuppressWarnings("deprecation")
public class InterfaceManagerConfigurationTest {

    // Uncomment this, temporarily (never commit!), to see concurrency issues:
    // public static @ClassRule RunUntilFailureRule repeater = new RunUntilFailureRule();

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(new InterfaceManagerTestModule());

    static final BigInteger dpnId = BigInteger.valueOf(1);

    @Inject DataBroker dataBroker;

    @Before
    public void start() throws InterruptedException {
        // TODO This is silly, because onSessionInitiated(), or later it's BP
        // equivalent, for clearer testability should just propagate the exception
        assertThat(InterfaceStatusMonitor.getInstance().acquireServiceStatus()).isEqualTo("OPERATIONAL");
        //Create the bridge and make sure it is ready
        setupAndAssertBridgeCreation();
    }

    @After
    public void end() throws InterruptedException {
        setupAndAssertBridgeDeletion();
    }

    private void setupAndAssertBridgeDeletion() throws InterruptedException {
        OvsdbSouthboundTestUtil.deleteBridge(dataBroker);
        Thread.sleep(2000);
        assertEqualBeans(InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpnId, dataBroker), null);
    }

    private void setupAndAssertBridgeCreation() throws InterruptedException {
        OvsdbSouthboundTestUtil.createBridge(dataBroker);
        Thread.sleep(2000);
        // a) Check bridgeRefEntry in cache and OperDS are same and use the right dpnId
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpnId);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntry bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker).orNull();
        assertEqualBeans(InterfaceMetaUtils.getBridgeRefEntryFromCache(dpnId), bridgeRefEntry);
        assertEqualBeans(bridgeRefEntry.getDpid(), dpnId);
    }

    @Test
    public void vlanInterfaceTests() throws Exception {
        // 1. When
        // i) parent-interface specified in above vlan configuration comes in operational/ietf-interfaces-state
        InterfaceManagerTestUtil.putInterfaceState(dataBroker, parentInterface, null);
        Thread.sleep(1000);
        // ii) Vlan interface written to config/ietf-interfaces DS and corresponding parent-interface is not present
        //     in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(parentInterface).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, interfaceName, parentRefs, L2vlan.class);
        // TODO Must think about proper solution for better synchronization here instead of silly wait()...
        // TODO use TestDataStoreJobCoordinator.waitForAllJobs() when https://git.opendaylight.org/gerrit/#/c/48061/ is merged
        Thread.sleep(2000);

        // 3. Then
        // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info was created
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey(parentInterface),
                        new InterfaceChildEntryKey(interfaceName));
        assertEqualBeans(ExpectedInterfaceChildEntry.interfaceChildEntry(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).checkedGet().get());

        // Then
        // a) check if operational/ietf-interfaces-state is populated for the vlan interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifaceState =
                dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(interfaceName)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(), ifaceState);

        // b) check if lport-tag to interface mapping is created
        InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = InstanceIdentifier.builder(
                IfIndexesInterfaceMap.class).child(
                        IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
        Assert.assertEquals(interfaceName, dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                ifIndexInterfaceInstanceIdentifier).checkedGet().get().getInterfaceName());

        // c) check expected flow entries were created in Interface Ingress Table
        BigInteger dpnId = BigInteger.valueOf(1);
        String ingressFlowRef = FlowBasedServicesUtils.getFlowRef(VLAN_INTERFACE_INGRESS_TABLE, dpnId, interfaceName);
        FlowKey ingressFlowKey = new FlowKey(new FlowId(ingressFlowRef));
        Node nodeDpn = InterfaceManagerTestUtil.buildInventoryDpnNode(dpnId);
        InstanceIdentifier<Flow> ingressFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(VLAN_INTERFACE_INGRESS_TABLE))
                .child(Flow.class,ingressFlowKey).build();
        assertEqualBeans(ExpectedFlowEntries.newIngressFlow(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, ingressFlowInstanceId).checkedGet().get());

        // d) check if default egress service is bound on the interface
        InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier =
            InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(interfaceName, ServiceModeEgress.class))
                .child(BoundServices.class, new BoundServicesKey(DEFAULT_EGRESS_SERVICE_INDEX)).build();
        assertEqualBeans(ExpectedServicesInfo.newboundService(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, boundServicesInstanceIdentifier).checkedGet().get());

        //Delete test
        // iii) vlan interface is deleted from config/ietf-interfaces
        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, interfaceName);
        Thread.sleep(4000);
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
        // 2. When
        // i) dpn-id specified above configuration comes in operational/network-topology
        // ii) Vlan interface written to config/ietf-interfaces DS and corresponding parent-interface is not present
        //     in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dpnId).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, tunnelInterfaceName, parentRefs, Tunnel.class);

        // TODO Must think about proper solution for better synchronization here instead of silly wait()...
        // TODO use TestDataStoreJobCoordinator.waitForAllJobs() when https://git.opendaylight.org/gerrit/#/c/48061/ is merged
        Thread.sleep(1000);

        // iii) tunnel interface comes up in operational/ietf-interfaces-state
        InterfaceManagerTestUtil.putInterfaceState(dataBroker, tunnelInterfaceName, Tunnel.class);
        Thread.sleep(1000);

        // 3. Then
        // a) check expected bridge-interface mapping in odl-interface-meta/config/bridge-interface-info was created
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpnId);
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey = new BridgeInterfaceEntryKey(tunnelInterfaceName);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey, bridgeInterfaceEntryKey);
        // TODO Later use nicer abstraction for DB access here.. see ElanServiceTest
        assertEqualBeans(InterfaceMeta.newBridgeInterface(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, bridgeInterfaceEntryIid).checkedGet().get());

        // Then
        // a) check if termination end point is created in config/network-topology
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
            .network.topology.topology.Node> bridgeIid =
                OvsdbSouthboundTestUtil.createInstanceIdentifier("192.168.56.101", 6640,  "s2");
        InstanceIdentifier<TerminationPoint> tpIid = InterfaceManagerTestUtil.getTerminationPointId(bridgeIid,
                tunnelInterfaceName);
        assertEqualBeans(ExpectedTerminationPoint.newTerminationPoint(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).checkedGet().get());

        // Delete test
        // iii) tunnel interface is deleted from config/ietf-interfaces
        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, tunnelInterfaceName);
        Thread.sleep(5000);

        // Then
        // a) check if tunnel is deleted from bridge-interface-info
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, bridgeInterfaceEntryIid).get());

        // b) check if termination end point is deleted in config/network-topology
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).get());
    }
}
