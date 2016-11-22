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
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.childInterface;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.interfaceName;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import java.math.BigInteger;
import javax.inject.Inject;

import org.junit.*;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Component tests for interface manager.
 *
 * @author Michael Vorburger
 */
public class InterfaceManagerTest {

    public @Rule MethodRule guice = new GuiceRule(new InterfaceManagerTestModule());

    @Inject DataBroker dataBroker;

    @Before
    public void start(){
        // TODO This is silly, because onSessionInitiated(), or later it's BP
        // equivalent, for clearer testability should just propagate the exception
        assertThat(InterfaceStatusMonitor.getInstance().acquireServiceStatus()).isEqualTo("OPERATIONAL");
    }

    @Test public void newVlanInterface() throws Exception {
        // 1. Given
        BigInteger dpnId = BigInteger.valueOf(1);
        // 2. When
        // i) parent-interface specified in above vlan configuration comes in operational/ietf-interfaces-state
        putInterfaceState(interfaceName);
        Thread.sleep(500);
        // ii) Vlan interface written to config/ietf-interfaces DS and corresponding parent-interface is not present
        //     in operational/ietf-interface-state
        putInterfaceConfig(childInterface, interfaceName);
        // TODO Must think about proper solution for better synchronization here instead of silly wait()...
        // TODO use TestDataStoreJobCoordinator.waitForAllJobs() when https://git.opendaylight.org/gerrit/#/c/48061/ is merged
        Thread.sleep(1000);

        // 3. Then
        // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info was created
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey(interfaceName),
                        new InterfaceChildEntryKey(childInterface));
        InterfaceChildEntry expectedInterfaceChildEntry = new InterfaceChildEntryBuilder()
                .setKey(new InterfaceChildEntryKey(childInterface))
                .setChildInterface(childInterface).build();
        // TODO Later use nicer abstraction for DB access here.. see ElanServiceTest
        assertEqualBeans(expectedInterfaceChildEntry, dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).checkedGet().get());

        // Then
        // a) check if operational/ietf-interfaces-state is populated for the vlan interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifaceState =
                dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(childInterface)).checkedGet().get();
        Assert.assertNotNull(ifaceState);

        // b) check if lport-tag to interface mapping is created
        InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = InstanceIdentifier.builder(IfIndexesInterfaceMap.class).child(
                IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
        Assert.assertEquals(childInterface, dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                ifIndexInterfaceInstanceIdentifier).checkedGet().get().getInterfaceName());

        // c) check expected flow entries were created in Interface Ingress Table
        String ingressFlowRef = FlowBasedServicesUtils.getFlowRef(NwConstants.VLAN_INTERFACE_INGRESS_TABLE, dpnId, childInterface);
        FlowKey ingressFlowKey = new FlowKey(new FlowId(ingressFlowRef));
        Node nodeDpn = InterfaceManagerTestUtil.buildInventoryDpnNode(dpnId);
        InstanceIdentifier<Flow> ingressFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class,ingressFlowKey).build();
        Assert.assertNotNull(dataBroker.newReadOnlyTransaction().read(CONFIGURATION, ingressFlowInstanceId).checkedGet().get());

        // d) check if default egress service is bound on the interface
        InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier = InstanceIdentifier.builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(childInterface, ServiceModeEgress.class))
                .child(BoundServices.class, new BoundServicesKey(NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)).build();
        Assert.assertNotNull(dataBroker.newReadOnlyTransaction().read(CONFIGURATION, boundServicesInstanceIdentifier).checkedGet().get());

        Thread.sleep(500);
        // d) check expected flow entries are created in Egress Dispatcher Table
        String egressFlowRef = getFlowRef(dpnId, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE,
                childInterface, NwConstants.DEFAULT_SERVICE_INDEX);
        FlowKey egressFlowKey = new FlowKey(new FlowId(egressFlowRef));
        InstanceIdentifier<Flow> egressFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE)).child(Flow.class,egressFlowKey).build();
        Assert.assertNotNull(dataBroker.newReadOnlyTransaction().read(CONFIGURATION, egressFlowInstanceId).checkedGet().get());
    }

    public void putInterfaceConfig(String ifaceName, String parentInterface){
        Interface vlanInterfaceEnabled = InterfaceManagerTestUtil.buildInterface(
                ifaceName, "Test VLAN if1", true, L2vlan.class, parentInterface);
        InstanceIdentifier<Interface> vlanInterfaceEnabledInterfaceInstanceIdentifier = IfmUtil.buildId(
                InterfaceManagerTestUtil.childInterface);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, vlanInterfaceEnabledInterfaceInstanceIdentifier, vlanInterfaceEnabled, true);
        tx.submit();
    }

    public void putInterfaceState(String interfaceName){
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifaceState =
                InterfaceManagerTestUtil.buildStateInterface(interfaceName, "1", "2", "AA:AA:AA:AA:AA:AA");
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(OPERATIONAL, IfmUtil.buildStateInterfaceId(interfaceName), ifaceState, true);
        tx.submit();
    }

    public static String getFlowRef(BigInteger dpnId, short tableId, String iface, short currentServiceIndex) {
        return new StringBuffer().append(dpnId).append(tableId).append(NwConstants.FLOWID_SEPARATOR)
                .append(iface).append(NwConstants.FLOWID_SEPARATOR).append(currentServiceIndex).toString();
    }
}
