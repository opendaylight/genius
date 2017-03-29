/*
 * Copyright (c) 2016, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.INTERFACE_NAME;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.PARENT_INTERFACE;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.TRUNK_INTERFACE_NAME;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.TUNNEL_INTERFACE_NAME;
import static org.opendaylight.genius.mdsalutil.NwConstants.DEFAULT_EGRESS_SERVICE_INDEX;
import static org.opendaylight.genius.mdsalutil.NwConstants.VLAN_INTERFACE_INGRESS_TABLE;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.test.xtend.DpnFromInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.DpnInterfaceListOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.EgressActionsForInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.EgressInstructionsForInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.EndPointIpFromDpn;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedFlowEntries;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceChildEntry;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceState;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedServicesInfo;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedTerminationPoint;
import org.opendaylight.genius.interfacemanager.test.xtend.InterfaceFromIfIndexOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.InterfaceMeta;
import org.opendaylight.genius.interfacemanager.test.xtend.InterfaceTypeOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.NodeconnectorIdFromInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.PortFromInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.TunnelTypeOutput;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;


/**
 * Component tests for interface manager.
 *
 * @author Michael Vorburger
 * @author Faseela K
 */
@SuppressWarnings("deprecation")
public class InterfaceManagerConfigurationTest {

    // Uncomment this, temporarily (never commit!), to see concurrency issues:
    // public static @ClassRule RunUntilFailureRule repeater = new
    // RunUntilFailureRule();

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(new InterfaceManagerTestModule(),
        new TestableDataTreeChangeListenerModule());

    private static final BigInteger DPN_ID = BigInteger.valueOf(1);

    @Inject DataBroker dataBroker;
    @Inject OdlInterfaceRpcService odlInterfaceRpcService;
    @Inject JobCoordinatorEventsWaiter coordinatorEventsWaiter;
    @Inject AsyncEventsWaiter asyncEventsWaiter;

    @Before
    public void start() throws InterruptedException {
        // Create the bridge and make sure it is ready
        setupAndAssertBridgeCreation();
    }

    @After
    public void end() throws InterruptedException {
        setupAndAssertBridgeDeletion();
    }

    private void setupAndAssertBridgeDeletion() throws InterruptedException {
        OvsdbSouthboundTestUtil.deleteBridge(dataBroker);
        Thread.sleep(2000);
        assertEqualBeans(InterfaceMetaUtils.getBridgeRefEntryFromOperDS(DPN_ID, dataBroker), null);
    }

    private void setupAndAssertBridgeCreation() throws InterruptedException {
        OvsdbSouthboundTestUtil.createBridge(dataBroker);
        Thread.sleep(2000);
        // a) Check bridgeRefEntry in cache and OperDS are same and use the
        // right DPN_ID
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(DPN_ID);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntry bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker)
                .orNull();
        assertEqualBeans(InterfaceMetaUtils.getBridgeRefEntryFromCache(DPN_ID), bridgeRefEntry);
        assertEqualBeans(bridgeRefEntry.getDpid(), DPN_ID);
    }

    @Test
    public void vlanInterfaceTests() throws Exception {
        // 1. When
        // i) parent-interface specified in above vlan configuration comes in operational/ietf-interfaces-state
        InterfaceManagerTestUtil.createFlowCapableNodeConnector(dataBroker, PARENT_INTERFACE, null);

        // ii) Vlan interface written to config/ietf-interfaces DS and
        // corresponding parent-interface is not present
        // in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(PARENT_INTERFACE).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, INTERFACE_NAME, parentRefs, L2vlan.class);
        Thread.sleep(3000);

        // 3. Then
        // a) check expected interface-child entry mapping in
        // odl-interface-meta/config/interface-child-info was created
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey(PARENT_INTERFACE),
                        new InterfaceChildEntryKey(INTERFACE_NAME));
        assertEqualBeans(ExpectedInterfaceChildEntry.interfaceChildEntry(INTERFACE_NAME),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION,
                        interfaceChildEntryInstanceIdentifier).checkedGet().get());

        // Then
        // a) check if operational/ietf-interfaces-state is populated for the vlan interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifaceState =
                dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(INTERFACE_NAME)).checkedGet().get();

        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(),
            INTERFACE_NAME, Interface.OperStatus.Up, L2vlan.class), ifaceState);


        // b) check if lport-tag to interface mapping is created
        InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = InstanceIdentifier
                .builder(IfIndexesInterfaceMap.class)
                .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
        Assert.assertEquals(INTERFACE_NAME, dataBroker.newReadOnlyTransaction()
                .read(OPERATIONAL, ifIndexInterfaceInstanceIdentifier).checkedGet().get().getInterfaceName());

        // c) check expected flow entries were created in Interface Ingress
        // Table
        BigInteger dpnId = BigInteger.valueOf(1);
        String ingressFlowRef = FlowBasedServicesUtils.getFlowRef(VLAN_INTERFACE_INGRESS_TABLE, dpnId, INTERFACE_NAME);
        FlowKey ingressFlowKey = new FlowKey(new FlowId(ingressFlowRef));
        Node nodeDpn = InterfaceManagerTestUtil.buildInventoryDpnNode(dpnId);
        InstanceIdentifier<Flow> ingressFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class, ingressFlowKey)
                .build();
        assertEqualBeans(ExpectedFlowEntries.newIngressFlow(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, ingressFlowInstanceId).checkedGet().get());

        // d) check if default egress service is bound on the interface
        InstanceIdentifier<BoundServices> boundServicesInstanceIdentifier = InstanceIdentifier
                .builder(ServiceBindings.class)
                .child(ServicesInfo.class, new ServicesInfoKey(INTERFACE_NAME, ServiceModeEgress.class))
                .child(BoundServices.class, new BoundServicesKey(DEFAULT_EGRESS_SERVICE_INDEX)).build();
        assertEqualBeans(ExpectedServicesInfo.newboundService(), dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, boundServicesInstanceIdentifier).checkedGet().get());


        // Test all RPCs related to vlan-interfaces
        checkVlanRpcs();

        //Update test
        // i) vlan interface admin-state updated
        InterfaceManagerTestUtil.updateInterfaceAdminState(dataBroker, INTERFACE_NAME, false);

        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        // Then
        // a) check if operational/ietf-interfaces-state is updated for vlan interface
        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(INTERFACE_NAME)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(), INTERFACE_NAME, Interface
            .OperStatus.Down, L2vlan.class), ifaceState);


        // Restore the opState back to UP for proceeding with further tests
        InterfaceManagerTestUtil.updateInterfaceAdminState(dataBroker, INTERFACE_NAME, true);

        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        testVlanMemberInterface();

        //Delete test
        // iii) vlan interface is deleted from config/ietf-interfaces
        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, INTERFACE_NAME);
        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        // 3. Then
        // a) check expected interface-child entry mapping in
        // odl-interface-meta/config/interface-child-info is deleted

        // TODO Later use nicer abstraction for DB access here.. see
        // ElanServiceTest
        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).get());

        // Then
        // a) check if operational/ietf-interfaces-state is deleted for the vlan
        // interface
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(OPERATIONAL, IfmUtil.buildStateInterfaceId(INTERFACE_NAME)).get());

        // b) check if lport-tag to interface mapping is deleted
        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(OPERATIONAL, ifIndexInterfaceInstanceIdentifier).get());
    }

    @Test
    public void newTunnelInterface() throws Exception {
        // 1. Given
        // 2. When
        // i) dpn-id specified above configuration comes in
        // operational/network-topology
        // ii) Vlan interface written to config/ietf-interfaces DS and
        // corresponding parent-interface is not present
        // in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(DPN_ID).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, TUNNEL_INTERFACE_NAME, parentRefs, Tunnel.class);
        Thread.sleep(5000);

        // 3. Then
        // a) check expected bridge-interface mapping in
        // odl-interface-meta/config/bridge-interface-info was created
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(DPN_ID);
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey = new BridgeInterfaceEntryKey(TUNNEL_INTERFACE_NAME);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid = InterfaceMetaUtils
                .getBridgeInterfaceEntryIdentifier(bridgeEntryKey, bridgeInterfaceEntryKey);
        // TODO Later use nicer abstraction for DB access here.. see
        // ElanServiceTest
        assertEqualBeans(InterfaceMeta.newBridgeInterface(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, bridgeInterfaceEntryIid).checkedGet().get());

        // Then
        // a) check if termination end point is created in
        // config/network-topology
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params
            .xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> bridgeIid = OvsdbSouthboundTestUtil
                .createInstanceIdentifier("192.168.56.101", 6640, "s2");
        InstanceIdentifier<TerminationPoint> tpIid = InterfaceManagerTestUtil.getTerminationPointId(bridgeIid,
                TUNNEL_INTERFACE_NAME);
        assertEqualBeans(ExpectedTerminationPoint.newTerminationPoint(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).checkedGet().get());

        // When termination end point is populated in network-topology
        OvsdbSouthboundTestUtil.createTerminationPoint(dataBroker, TUNNEL_INTERFACE_NAME, InterfaceTypeVxlan.class);
        InterfaceManagerTestUtil.createFlowCapableNodeConnector(dataBroker, TUNNEL_INTERFACE_NAME, Tunnel.class);
        Thread.sleep(5000);

        // Then
        // a) check if operational/ietf-interfaces-state is populated for the tunnel interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifaceState =
            dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(TUNNEL_INTERFACE_NAME)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(),
            TUNNEL_INTERFACE_NAME, Interface.OperStatus.Up, Tunnel.class), ifaceState);

        // Test all RPCs related to tunnel interfaces
        checkTunnelRpcs();

        // Update test
        // i) Enable Tunnel Monitoring
        InterfaceManagerTestUtil.updateTunnelMonitoringAttributes(dataBroker, TUNNEL_INTERFACE_NAME);
        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        // Then verify if bfd attributes are updated in topology config DS
        assertEqualBeans(ExpectedTerminationPoint.newBfdEnabledTerminationPoint(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).checkedGet().get());

        // Delete test
        // iii) tunnel interface is deleted from config/ietf-interfaces
        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, TUNNEL_INTERFACE_NAME);
        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);

        // Then
        // a) check if tunnel is deleted from bridge-interface-info
        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, bridgeInterfaceEntryIid).get());

        // b) check if termination end point is deleted in
        // config/network-topology
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).get());
    }

    private void checkVlanRpcs() throws Exception {
        //1. Test dpn-id fetching from interface
        GetDpidFromInterfaceInput dpidFromInterfaceInput = new GetDpidFromInterfaceInputBuilder()
                .setIntfName(INTERFACE_NAME).build();
        Future<RpcResult<GetDpidFromInterfaceOutput>> dpidFromInterfaceOutput =
            odlInterfaceRpcService.getDpidFromInterface(dpidFromInterfaceInput);
        Assert.assertEquals(DpnFromInterfaceOutput.newDpnFromInterfaceOutput(),
                dpidFromInterfaceOutput.get().getResult());

        //2. Test interface list fetching from dpnId
        GetDpnInterfaceListInput dpnInterfaceListInput = new GetDpnInterfaceListInputBuilder()
                .setDpid(DPN_ID).build();
        Future<RpcResult<GetDpnInterfaceListOutput>> dpnInterfaceListOutput = odlInterfaceRpcService
            .getDpnInterfaceList(dpnInterfaceListInput);
        List<String> expectedDpnInterfaceList = new ArrayList(DpnInterfaceListOutput.newDpnInterfaceListOutput()
            .getInterfacesList());
        List<String> actualDpnInterfaceList = dpnInterfaceListOutput.get().getResult().getInterfacesList();
        Collections.sort(expectedDpnInterfaceList);
        Collections.sort(actualDpnInterfaceList);
        assertEqualBeans(expectedDpnInterfaceList, actualDpnInterfaceList);

        //3. Test egress actions fetching for interface
        GetEgressActionsForInterfaceInput egressActionsForInterfaceInput = new
            GetEgressActionsForInterfaceInputBuilder().setIntfName(INTERFACE_NAME).build();
        Future<RpcResult<GetEgressActionsForInterfaceOutput>> egressActionsForInterfaceOutput =
            odlInterfaceRpcService.getEgressActionsForInterface(egressActionsForInterfaceInput);
        assertEqualBeans(EgressActionsForInterfaceOutput.newEgressActionsForInterfaceOutput(),
            egressActionsForInterfaceOutput.get().getResult());

        //4. Test egress instructions fetching for interface
        GetEgressInstructionsForInterfaceInput egressInstructionsForInterfaceInput = new
            GetEgressInstructionsForInterfaceInputBuilder().setIntfName(INTERFACE_NAME).build();
        Future<RpcResult<GetEgressInstructionsForInterfaceOutput>> egressInstructionsForInterfaceOutput =
            odlInterfaceRpcService.getEgressInstructionsForInterface(egressInstructionsForInterfaceInput);
        assertEqualBeans(EgressInstructionsForInterfaceOutput.newEgressInstructionsForInterfaceOutput(),
            egressInstructionsForInterfaceOutput.get().getResult());


        //5. Test interface fetching from if-index
        GetInterfaceFromIfIndexInput interfaceFromIfIndexInput = new GetInterfaceFromIfIndexInputBuilder()
                .setIfIndex(1).build();
        Future<RpcResult<GetInterfaceFromIfIndexOutput>> interfaceFromIfIndexOutput = odlInterfaceRpcService
            .getInterfaceFromIfIndex(interfaceFromIfIndexInput);
        assertEqualBeans(InterfaceFromIfIndexOutput.newInterfaceFromIfIndexOutput(),
                interfaceFromIfIndexOutput.get().getResult());

        //6. Test interface type fetching from interface-name
        GetInterfaceTypeInput interfaceTypeInput = new GetInterfaceTypeInputBuilder().setIntfName(INTERFACE_NAME)
            .build();
        Future<RpcResult<GetInterfaceTypeOutput>> interfaceTypeOutput =
                odlInterfaceRpcService.getInterfaceType(interfaceTypeInput);
        assertEqualBeans(InterfaceTypeOutput.newInterfaceTypeOutput(), interfaceTypeOutput.get().getResult());

        //7. Test get nodeconnector-id from interface-name
        GetNodeconnectorIdFromInterfaceInput nodeconnectorIdFromInterfaceInput = new
            GetNodeconnectorIdFromInterfaceInputBuilder().setIntfName(INTERFACE_NAME).build();
        Future<RpcResult<GetNodeconnectorIdFromInterfaceOutput>> nodeconnectorIdFromInterfaceOutput =
            odlInterfaceRpcService.getNodeconnectorIdFromInterface(nodeconnectorIdFromInterfaceInput);
        assertEqualBeans(NodeconnectorIdFromInterfaceOutput.newNodeconnectorIdFromInterfaceOutput(),
            nodeconnectorIdFromInterfaceOutput.get().getResult());

        //8. Test get port details from interface-name
        GetPortFromInterfaceInput portFromInterfaceInput =
                new GetPortFromInterfaceInputBuilder().setIntfName(INTERFACE_NAME).build();
        Future<RpcResult<GetPortFromInterfaceOutput>> portFromInterfaceOutput = odlInterfaceRpcService
            .getPortFromInterface(portFromInterfaceInput);
        assertEqualBeans(PortFromInterfaceOutput.newPortFromInterfaceOutput(),
                portFromInterfaceOutput.get().getResult());
    }

    private void checkTunnelRpcs() throws Exception {
        //1. Test endpoint ip fetching for dpn-id
        GetEndpointIpForDpnInput endpointIpForDpnInput = new GetEndpointIpForDpnInputBuilder().setDpid(DPN_ID).build();
        Future<RpcResult<GetEndpointIpForDpnOutput>> endpointIpForDpnOutput = odlInterfaceRpcService
                .getEndpointIpForDpn(endpointIpForDpnInput);
        assertEqualBeans(EndPointIpFromDpn.newEndPointIpFromDpn(), endpointIpForDpnOutput.get().getResult());


        //2. fetch tunnel type from interface-name
        GetTunnelTypeInput tunnelTypeInput = new GetTunnelTypeInputBuilder().setIntfName(TUNNEL_INTERFACE_NAME).build();
        Future<RpcResult<GetTunnelTypeOutput>> tunnelTypeOutput = odlInterfaceRpcService.getTunnelType(tunnelTypeInput);
        assertEqualBeans(TunnelTypeOutput.newTunnelTypeOutput(), tunnelTypeOutput.get().getResult());
    }

    public void testVlanMemberInterface() throws Exception {
        // Test VlanMember interface creation
        InterfaceManagerTestUtil.putVlanInterfaceConfig(dataBroker, TRUNK_INTERFACE_NAME, INTERFACE_NAME,
                IfL2vlan.L2vlanMode.TrunkMember);
        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);

        // 3. Then
        // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info was created
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils
            .getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey(INTERFACE_NAME),
                new InterfaceChildEntryKey(TRUNK_INTERFACE_NAME));
        assertEqualBeans(ExpectedInterfaceChildEntry.interfaceChildEntry(TRUNK_INTERFACE_NAME),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, interfaceChildEntryInstanceIdentifier)
                        .checkedGet().get());

        // Then
        // a) check if operational/ietf-interfaces-state is populated for the vlan interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifaceState =
            dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(TRUNK_INTERFACE_NAME)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(), TRUNK_INTERFACE_NAME,
                Interface.OperStatus.Up, L2vlan.class), ifaceState);

        // b) check if lport-tag to interface mapping is created
        InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = InstanceIdentifier.builder(
            IfIndexesInterfaceMap.class).child(
            IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
        Assert.assertEquals(TRUNK_INTERFACE_NAME, dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            ifIndexInterfaceInstanceIdentifier).checkedGet().get().getInterfaceName());

        //Update test
        // i) vlan member interface admin-state updated
        InterfaceManagerTestUtil.updateInterfaceAdminState(dataBroker, TRUNK_INTERFACE_NAME, false);

        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);

        //Then
        // a) check if operational/ietf-interfaces-state is updated for vlan interface
        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TRUNK_INTERFACE_NAME)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(), TRUNK_INTERFACE_NAME,
            Interface.OperStatus.Down, L2vlan.class), ifaceState);

        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, TRUNK_INTERFACE_NAME);
        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        // 1. Then
        // a)
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).get());

        // b) check if operational/ietf-interfaces-state is deleted for the vlan interface
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TRUNK_INTERFACE_NAME)).get());

        // c) check if lport-tag to interface mapping is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            ifIndexInterfaceInstanceIdentifier).get());
    }
}
