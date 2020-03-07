/*
 * Copyright (c) 2016, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.BatchingUtils.EntityType.DEFAULT_OPERATIONAL;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.DPN_ID_1;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.DPN_ID_2;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.INTERFACE_NAME;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.INTERFACE_NAME_1;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.INTERFACE_NAME_2;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.PARENT_INTERFACE;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.PARENT_INTERFACE_1;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.PARENT_INTERFACE_2;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.PORT_NO_1;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.TRUNK_INTERFACE_NAME;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.TUNNEL_INTERFACE_NAME;
import static org.opendaylight.genius.interfacemanager.test.InterfaceManagerTestUtil.waitTillOperationCompletes;
import static org.opendaylight.genius.mdsalutil.NwConstants.DEFAULT_EGRESS_SERVICE_INDEX;
import static org.opendaylight.genius.mdsalutil.NwConstants.VLAN_INTERFACE_INGRESS_TABLE;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorCountedEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.BatchingUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.test.xtend.DpnFromInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.EgressActionsForInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.EgressInstructionsForInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.EndPointIpFromDpn;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedBoundServiceState;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedFlowEntries;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceChildEntry;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceConfig;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceInfo;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceListFromDpn;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceState;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedOvsdbBridge;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedServicesInfo;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedTerminationPoint;
import org.opendaylight.genius.interfacemanager.test.xtend.InterfaceMeta;
import org.opendaylight.genius.interfacemanager.test.xtend.InterfaceTypeOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.NodeconnectorIdFromInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.PortFromInterfaceOutput;
import org.opendaylight.genius.interfacemanager.test.xtend.TunnelTypeOutput;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.FlowAssertTestUtils;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.infrautils.testutils.concurrent.TestableQueues;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.DpnToInterfaceList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntryKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.get.dpn._interface.list.output.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
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
    // public static @ClassRule RunUntilFailureClassRule classRepeater = new RunUntilFailureClassRule();
    // public @Rule RunUntilFailureRule repeater = new RunUntilFailureRule(classRepeater);

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    public @Rule MethodRule guice = new GuiceRule(InterfaceManagerTestModule.class,
        TestableDataTreeChangeListenerModule.class, JobCoordinatorTestModule.class);

    @Inject DataBroker dataBroker;
    @Inject OdlInterfaceRpcService odlInterfaceRpcService;
    @Inject IInterfaceManager interfaceManager;
    @Inject JobCoordinatorCountedEventsWaiter coordinatorEventsWaiter;
    @Inject AsyncEventsWaiter asyncEventsWaiter;
    @Inject InterfaceMetaUtils interfaceMetaUtils;
    @Inject BatchingUtils batchingUtils;
    @Inject FlowAssertTestUtils flowAssertTestUtils;

    SingleTransactionDataBroker db;

    @Before
    public void start() throws InterruptedException, TransactionCommitFailedException {
        db = new SingleTransactionDataBroker(dataBroker);

        // Create the bridge and make sure it is ready
        setupAndAssertBridgeCreation();
    }

    @After
    public void stop() throws InterruptedException, TransactionCommitFailedException {
        setupAndAssertBridgeDeletion();
    }

    private void setupAndAssertBridgeDeletion() throws InterruptedException, TransactionCommitFailedException {
        OvsdbSouthboundTestUtil.deleteBridge(dataBroker);
        InterfaceManagerTestUtil.waitTillOperationCompletes("bridge deletion",
                coordinatorEventsWaiter,2, asyncEventsWaiter);
        assertEqualBeans(interfaceMetaUtils.getBridgeRefEntryFromOperationalDS(DPN_ID_1), null);
    }

    private void setupAndAssertBridgeCreation() throws InterruptedException, TransactionCommitFailedException {
        OvsdbSouthboundTestUtil.createBridge(dataBroker);
        // a) Check bridgeRefEntry in cache and OperDS are same and use the
        // right DPN_ID
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(DPN_ID_1);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        InterfaceManagerTestUtil.waitTillOperationCompletes("bridge creation",
                coordinatorEventsWaiter,3, asyncEventsWaiter);
        BridgeRefEntry bridgeRefEntry = IfmUtil
                .read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker)
                .orNull();
        assertEqualBeans(bridgeRefEntry.getDpid(), DPN_ID_1);
        // FIXME AsyncEventsWaiter does not help in this case, need to enhance -- TODO
        //assertEqualBeans(interfaceMetaUtils.getBridgeRefEntryFromCache(DPN_ID_1), bridgeRefEntry);

    }

    @Test
    public void testBinding() {

    }

    @Test
    @Ignore // TODO re-enable when stable, see https://jira.opendaylight.org/browse/GENIUS-120
    public void newl2vlanInterfaceTests() throws Exception {
        // 1. When
        // i) parent-interface specified in above vlan configuration comes in operational/ietf-interfaces-state
        OvsdbSouthboundTestUtil.createTerminationPoint(dataBroker, PARENT_INTERFACE, null, INTERFACE_NAME);
        InterfaceManagerTestUtil.createFlowCapableNodeConnector(dataBroker, PARENT_INTERFACE, null);

        // ii) Vlan interface written to config/ietf-interfaces DS and
        // corresponding parent-interface is not present
        // in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(PARENT_INTERFACE).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, INTERFACE_NAME, parentRefs, L2vlan.class);

        InterfaceManagerTestUtil.waitTillOperationCompletes("create interface configuration",
                coordinatorEventsWaiter,11, asyncEventsWaiter);

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
            INTERFACE_NAME, Interface.OperStatus.Up, L2vlan.class, DPN_ID_1.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);


        // FIXME can assert this only once ResourceBatchingManager becomes testable
        // b) check if lport-tag to interface mapping is created
        /*(InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = InstanceIdentifier
                .builder(IfIndexesInterfaceMap.class)
                .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
        Assert.assertEquals(INTERFACE_NAME, dataBroker.newReadOnlyTransaction()
                .read(OPERATIONAL, ifIndexInterfaceInstanceIdentifier).checkedGet().get().getInterfaceName());*/

        // c) check expected flow entries were created in Interface Ingress
        // Table
        BigInteger dpnId = BigInteger.valueOf(1);
        String ingressFlowRef = FlowBasedServicesUtils.getFlowRef(VLAN_INTERFACE_INGRESS_TABLE, dpnId, INTERFACE_NAME);
        FlowKey ingressFlowKey = new FlowKey(new FlowId(ingressFlowRef));
        Node nodeDpn = InterfaceManagerTestUtil.buildInventoryDpnNode(dpnId);
        InstanceIdentifier<Flow> ingressFlowInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(VLAN_INTERFACE_INGRESS_TABLE)).child(Flow.class, ingressFlowKey)
                .build();

        flowAssertTestUtils.assertFlowsInAnyOrder(ExpectedFlowEntries.newIngressFlow(),
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

        // Test all APIs exposed by interface-manager
        checkVlanApis();

        //Update config test
        // i) vlan interface admin-state updated
        InterfaceManagerTestUtil.updateInterfaceAdminState(dataBroker, INTERFACE_NAME, false);

        InterfaceManagerTestUtil.waitTillOperationCompletes("disable interface admin state",
                coordinatorEventsWaiter, 1, asyncEventsWaiter);
        // Then
        // a) check if operational/ietf-interfaces-state is updated for vlan interface
        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(INTERFACE_NAME)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(), INTERFACE_NAME, Interface
            .OperStatus.Down, L2vlan.class, DPN_ID_1.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);

        // Restore the opState back to UP for proceeding with further tests
        InterfaceManagerTestUtil.updateInterfaceAdminState(dataBroker, INTERFACE_NAME, true);
        InterfaceManagerTestUtil.waitTillOperationCompletes("enable interface admin state",
                coordinatorEventsWaiter, 1, asyncEventsWaiter);


        //state modification tests
        // 1. Make the operational state of port as DOWN
        InterfaceManagerTestUtil.updateFlowCapableNodeConnectorState(dataBroker, PARENT_INTERFACE, L2vlan.class, false);
        waitTillOperationCompletes("disable interface op state", coordinatorEventsWaiter, 2, asyncEventsWaiter);

        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(INTERFACE_NAME)).checkedGet().get();
        // Verify if operational/ietf-interface-state is marked down
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(),
            INTERFACE_NAME, Interface.OperStatus.Down, L2vlan.class, DPN_ID_1.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);

        // 4. Delete the southbound OF port
        InterfaceManagerTestUtil.removeFlowCapableNodeConnectorState(dataBroker, L2vlan.class);
        waitTillOperationCompletes("remove flow capable node connector",
                coordinatorEventsWaiter, 5, asyncEventsWaiter);

        // Verify if interfaces are deleted from oper/ietf-interfaces-state
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(PARENT_INTERFACE)).get());
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(INTERFACE_NAME)).get());

        // 3. Re-create the OF port to proceeed with vlan-member tests
        InterfaceManagerTestUtil.createFlowCapableNodeConnector(dataBroker, PARENT_INTERFACE, null);
        waitTillOperationCompletes("remove flow capable node connector",
                coordinatorEventsWaiter, 7, asyncEventsWaiter);

        testVlanMemberInterface();

        //Delete test
        // iii) vlan interface is deleted from config/ietf-interfaces
        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, INTERFACE_NAME);
        InterfaceManagerTestUtil.waitTillOperationCompletes("delete interface configuration",
                coordinatorEventsWaiter, 6, asyncEventsWaiter);
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
        /*Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(OPERATIONAL, ifIndexInterfaceInstanceIdentifier).get());*/
    }

    @Ignore
    @Test
    public void newTunnelInterface() throws Exception {
        // 3. Update DPN-ID of the bridge
        OvsdbSouthboundTestUtil.updateBridge(dataBroker, "00:00:00:00:00:00:00:02");
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(DPN_ID_2);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
            .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        // Verify if DPN-ID is updated in corresponding DS and cache
        BridgeRefEntry bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker)
            .orNull();
        assertEqualBeans(interfaceMetaUtils.getBridgeRefEntryFromCache(DPN_ID_2), bridgeRefEntry);

        // 1. Given
        // 2. When
        // i) dpn-id specified above configuration comes in
        // operational/network-topology
        // ii) Vlan interface written to config/ietf-interfaces DS and
        // corresponding parent-interface is not present
        // in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(DPN_ID_2).build();
        InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, TUNNEL_INTERFACE_NAME, parentRefs, Tunnel.class);
        Thread.sleep(5000);

        // 3. Then
        // a) check expected bridge-interface mapping in
        // odl-interface-meta/config/bridge-interface-info was created
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(DPN_ID_2);
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
        assertEqualTerminationPoints(ExpectedTerminationPoint.newTerminationPoint(), db.syncRead(CONFIGURATION, tpIid));

        // When termination end point is populated in network-topology
        OvsdbSouthboundTestUtil.createTerminationPoint(dataBroker, TUNNEL_INTERFACE_NAME, InterfaceTypeVxlan.class,
            null);
        InterfaceManagerTestUtil.createFlowCapableNodeConnector(dataBroker, TUNNEL_INTERFACE_NAME, Tunnel.class);
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        Thread.sleep(3000);
        TestableQueues.awaitEmpty(batchingUtils.getQueue(DEFAULT_OPERATIONAL), 1, MINUTES);

        // Then
        // a) check if operational/ietf-interfaces-state is populated for the tunnel interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface ifaceState =
            dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(TUNNEL_INTERFACE_NAME)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(),
            TUNNEL_INTERFACE_NAME, Interface.OperStatus.Up, Tunnel.class, DPN_ID_2.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);

        // Test all RPCs related to tunnel interfaces
        checkTunnelRpcs();

        checkTunnelApis();

        // Update test
        // i) Enable Tunnel Monitoring
        InterfaceManagerTestUtil.updateTunnelMonitoringAttributes(dataBroker, TUNNEL_INTERFACE_NAME);
        InterfaceManagerTestUtil.waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        // Then verify if bfd attributes are updated in topology config DS
        assertEqualTerminationPoints(ExpectedTerminationPoint.newBfdEnabledTerminationPoint(),
                db.syncRead(CONFIGURATION, tpIid));

        //state modification tests
        // 1. Make the operational state of port as DOWN
        InterfaceManagerTestUtil.updateFlowCapableNodeConnectorState(dataBroker, TUNNEL_INTERFACE_NAME, Tunnel
            .class, false);
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);

        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TUNNEL_INTERFACE_NAME)).checkedGet().get();
        // Verify if operational/ietf-interface-state is still up
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(),
            TUNNEL_INTERFACE_NAME, Interface.OperStatus.Up, Tunnel.class, DPN_ID_2.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);

        // 2. Make BFD staus of tunnel port as down
        OvsdbSouthboundTestUtil.updateTerminationPoint(dataBroker, TUNNEL_INTERFACE_NAME, InterfaceTypeVxlan.class);
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);

        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TUNNEL_INTERFACE_NAME)).checkedGet().get();
        // Verify if operational/ietf-interface-state is marked down
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(),
            TUNNEL_INTERFACE_NAME, Interface.OperStatus.Down, Tunnel.class, DPN_ID_2.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);


        // 2. Delete the Node
        InterfaceManagerTestUtil.removeNode(dataBroker);
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TUNNEL_INTERFACE_NAME)).checkedGet().get();
        // Verify if operational/ietf-interface-state is marked unknown
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(),
            TUNNEL_INTERFACE_NAME, Interface.OperStatus.Unknown, Tunnel.class, DPN_ID_2.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);

        // Re-create port to proceed with further tests
        InterfaceManagerTestUtil.createFlowCapableNodeConnector(dataBroker, TUNNEL_INTERFACE_NAME, Tunnel.class);
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);


        // 2. Delete the OF port
        InterfaceManagerTestUtil.removeFlowCapableNodeConnectorState(dataBroker, Tunnel.class);
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);

        // Verify if operational-states are deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TUNNEL_INTERFACE_NAME)).get());
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TUNNEL_INTERFACE_NAME)).get());

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
        waitTillOperationCompletes(coordinatorEventsWaiter, asyncEventsWaiter);
    }

    private void assertEqualTerminationPoints(TerminationPoint expected, TerminationPoint actual) {
        // Re-create the termination points to avoid re-deserialising the augmentations
        assertEqualBeans(rebuildTerminationPoint(expected), rebuildTerminationPoint(actual));
    }

    private TerminationPoint rebuildTerminationPoint(TerminationPoint tp) {
        // The problem we're fixing here is that, in MD-SAL binding v1, YANG lists are represented
        // as Java lists but they don't preserve order (unless they specify “ordered-by user”).
        // YANG keyed lists in particular are backed by maps, so you can store such a list in the
        // MD-SAL and get it back in a different order.
        // When comparing beans involving such lists, we need to sort the lists before comparing
        // them. Retrieving the augmentation gives a modifiable list, so it's tempting to just
        // sort that — but the list is re-created every time the augmentation is retrieved, so
        // the sort is lost.
        // To avoid all this, we rebuild instances of TerminationPoint, and sort the affected lists
        // in the augmentations, with full augmentation rebuilds too (since the lists in a built
        // augmentation might be unmodifiable).
        TerminationPointBuilder newTpBuilder = new TerminationPointBuilder(tp);
        OvsdbTerminationPointAugmentation ovsdbTpAugmentation =
                tp.augmentation(OvsdbTerminationPointAugmentation.class);
        if (ovsdbTpAugmentation != null) {
            OvsdbTerminationPointAugmentationBuilder newOvsdbTpAugmentationBuilder =
                    new OvsdbTerminationPointAugmentationBuilder(ovsdbTpAugmentation);
            if (ovsdbTpAugmentation.getOptions() != null) {
                List<Options> options = new ArrayList<>(ovsdbTpAugmentation.getOptions());
                options.sort(Comparator.comparing(o -> o.key().toString()));
                newOvsdbTpAugmentationBuilder.setOptions(options);
            }
            if (ovsdbTpAugmentation.getInterfaceBfd() != null) {
                List<InterfaceBfd> interfaceBfd = new ArrayList<>(ovsdbTpAugmentation.getInterfaceBfd());
                interfaceBfd.sort(Comparator.comparing(o -> o.key().toString()));
                newOvsdbTpAugmentationBuilder.setInterfaceBfd(interfaceBfd);
            }
            newTpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class,
                    newOvsdbTpAugmentationBuilder.build());
        }
        return newTpBuilder.build();
    }

    private void checkVlanApis() throws Exception {
        // 1. Test port-no corresponding to interface
        long portNo = interfaceManager.getPortForInterface(INTERFACE_NAME);
        Assert.assertEquals(PORT_NO_1, portNo);

        // 2. fetch interface config from datastore API
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
            interfaceInfo = interfaceManager.getInterfaceInfoFromConfigDataStore(INTERFACE_NAME);
        // FIXME change this once augmentation sorting fix lands
        assertEqualBeans(INTERFACE_NAME, interfaceInfo.getName());
        assertEqualBeans(PARENT_INTERFACE, interfaceInfo.augmentation(ParentRefs.class).getParentInterface());

        // 3. fetch dpn-id corresponding to an interface
        BigInteger dpnId = interfaceManager.getDpnForInterface(INTERFACE_NAME);
        Assert.assertEquals(DPN_ID_1, dpnId);

        // 4. fetch parent-interface corresponding to an interface
        Assert.assertEquals(PARENT_INTERFACE, interfaceManager.getParentRefNameForInterface(INTERFACE_NAME));

        //5. get interface information
        assertEqualBeans(ExpectedInterfaceInfo.newVlanInterfaceInfo(), interfaceManager
            .getInterfaceInfo(INTERFACE_NAME));

        Assert.assertEquals(org.opendaylight.genius.interfacemanager.globals.IfmConstants.VXLAN_GROUPID_MIN + 1,
            interfaceManager.getLogicalTunnelSelectGroupId(1));

        // 6. Test bind ingress service
        BoundServices serviceInfo = InterfaceManagerTestUtil.buildServicesInfo("ELAN", NwConstants.ELAN_SERVICE_INDEX);
        interfaceManager.bindService(INTERFACE_NAME, ServiceModeIngress.class, serviceInfo);

        waitTillOperationCompletes("test bind ingress service api",
                coordinatorEventsWaiter, 1, asyncEventsWaiter);

        String lportDispatcherFlowRef = String.valueOf(dpnId) + NwConstants.FLOWID_SEPARATOR
                + NwConstants.LPORT_DISPATCHER_TABLE + NwConstants.FLOWID_SEPARATOR + INTERFACE_NAME
                + NwConstants.FLOWID_SEPARATOR + NwConstants.DEFAULT_SERVICE_INDEX;
        FlowKey lportDispatcherFlowKey = new FlowKey(new FlowId(lportDispatcherFlowRef));
        Node nodeDpn = InterfaceManagerTestUtil.buildInventoryDpnNode(dpnId);
        InstanceIdentifier<Flow> lportDispatcherFlowId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(NwConstants.LPORT_DISPATCHER_TABLE)).child(Flow.class,
                lportDispatcherFlowKey).build();
        flowAssertTestUtils.assertFlowsInAnyOrder(ExpectedFlowEntries.newLportDispatcherFlow(),
                dataBroker.newReadOnlyTransaction().read(CONFIGURATION, lportDispatcherFlowId).checkedGet().get());

        // check whether service-binding state cache is populated
        assertEqualBeans(ExpectedBoundServiceState.newBoundServiceState(), FlowBasedServicesUtils
            .getBoundServicesState(dataBroker.newReadOnlyTransaction(), INTERFACE_NAME, ServiceModeIngress.class));

        //7. test check whether service is bound on ingress
        Assert.assertTrue(interfaceManager.isServiceBoundOnInterfaceForIngress(NwConstants.ELAN_SERVICE_INDEX,
            INTERFACE_NAME));

        //8. test unbind ingress service
        interfaceManager.unbindService(INTERFACE_NAME, ServiceModeIngress.class, serviceInfo);
        waitTillOperationCompletes("test unbind ingress service api",
                coordinatorEventsWaiter, 2, asyncEventsWaiter);

        Assert.assertEquals(Optional.absent(),
            dataBroker.newReadOnlyTransaction().read(CONFIGURATION, lportDispatcherFlowId).get());

        // check service-state cache is cleaned up
        // 9. Test bind egress service
        short egressACLIndex = ServiceIndex.getIndex(NwConstants.EGRESS_ACL_SERVICE_NAME,
            NwConstants.EGRESS_ACL_SERVICE_INDEX);
        serviceInfo = InterfaceManagerTestUtil.buildServicesInfo("EGRESS_ACL", egressACLIndex);
        interfaceManager.bindService(INTERFACE_NAME, ServiceModeEgress.class, serviceInfo);
        waitTillOperationCompletes("test bind egress service api",
                coordinatorEventsWaiter, 1, asyncEventsWaiter);

        String egressDispatcherFlowRef = String.valueOf(dpnId) + NwConstants.FLOWID_SEPARATOR
                + NwConstants.EGRESS_LPORT_DISPATCHER_TABLE + NwConstants.FLOWID_SEPARATOR
                + INTERFACE_NAME + NwConstants.FLOWID_SEPARATOR + NwConstants.DEFAULT_EGRESS_SERVICE_INDEX;

        FlowKey egressDispatcherFlowKey = new FlowKey(new FlowId(egressDispatcherFlowRef));
        InstanceIdentifier<Flow> egressDispatcherFlowId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE)).child(Flow.class,
                egressDispatcherFlowKey).build();

        // FIXME the extend file getting generated had some import issues, will revist  later
        //assertEqualBeans(null,
        //    dataBroker.newReadOnlyTransaction().read(CONFIGURATION, egressDispatcherFlowId).checkedGet().get());

        Assert.assertNotNull(dataBroker.newReadOnlyTransaction().read(CONFIGURATION,
            egressDispatcherFlowId).checkedGet().get());

        //10. test check whether service is bound on egress
        Assert.assertTrue(interfaceManager.isServiceBoundOnInterfaceForEgress(NwConstants.EGRESS_ACL_SERVICE_INDEX,
            INTERFACE_NAME));

        // 11. Test unbinding of egress service
        interfaceManager.unbindService(INTERFACE_NAME, ServiceModeEgress.class, serviceInfo);
        waitTillOperationCompletes("test unbind egress service api",
                coordinatorEventsWaiter, 2, asyncEventsWaiter);
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION,
            egressDispatcherFlowId).get());

        // 12. Test fetching child interfaces of an interface
        // FIXME change the below assert once sorted augmentation fix lands
        assertEqualBeans(INTERFACE_NAME, interfaceManager.getChildInterfaces(PARENT_INTERFACE).get(0).getName());

        // 13. Test fetching interface-info from operational DS
        assertEqualBeans(ExpectedInterfaceInfo.newInterfaceInfo(1, INTERFACE_NAME, PARENT_INTERFACE, null),
            interfaceManager.getInterfaceInfoFromOperationalDataStore(INTERFACE_NAME));

        // 14. Test fetching of interface-info from oper DS, given interface-type
        assertEqualBeans(ExpectedInterfaceInfo.newInterfaceInfo(1, INTERFACE_NAME, INTERFACE_NAME, InterfaceInfo
            .InterfaceType.VLAN_INTERFACE), interfaceManager.getInterfaceInfoFromOperationalDataStore(
                INTERFACE_NAME, InterfaceInfo.InterfaceType.VLAN_INTERFACE));

        // 15.Test fetching of interface-info from cache
        assertEqualBeans(ExpectedInterfaceInfo.newInterfaceInfo(1, INTERFACE_NAME, PARENT_INTERFACE, null),
            interfaceManager.getInterfaceInfoFromOperationalDSCache(INTERFACE_NAME));

        // 16. Test creation of VLAN interface
        // FIXME Make IInterfaceManager truly async
        interfaceManager.createVLANInterface(INTERFACE_NAME_1, PARENT_INTERFACE_1, null, INTERFACE_NAME_1,
            IfL2vlan.L2vlanMode.Trunk);
        //waitTillOperationCompletes(coordinatorEventsWaiter, 1, asyncEventsWaiter);

        //assertEqualBeans(ExpectedInterfaceConfig.newVlanInterfaceConfig(INTERFACE_NAME_1, null),
        //    dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, IfmUtil.buildId(
        //        INTERFACE_NAME_1)).checkedGet().get());

        // 17. Update Parent Refs for VLAN interface
        // FIXME Make IInterfaceManager truly async
        //interfaceManager.updateInterfaceParentRef(INTERFACE_NAME_1, PARENT_INTERFACE_1);
        waitTillOperationCompletes("create vlan interface api",
                coordinatorEventsWaiter, 4, asyncEventsWaiter);

        assertEqualBeans(ExpectedInterfaceConfig.newVlanInterfaceConfig(INTERFACE_NAME_1, PARENT_INTERFACE_1),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, IfmUtil
                .buildId(INTERFACE_NAME_1)).checkedGet().get());

        // 18. Test creation of external l2vlan interfaces
        // FIXME Make IInterfaceManager truly async
        interfaceManager.createVLANInterface(INTERFACE_NAME_2, PARENT_INTERFACE_2, null, INTERFACE_NAME_2,
            IfL2vlan.L2vlanMode.Trunk, true);
        //waitTillOperationCompletes(coordinatorEventsWaiter, 1, asyncEventsWaiter);

        // FIXME need to wait for https://git.opendaylight.org/gerrit/#/c/54811/ this to land
        // to do proper assertion
        //Assert.assertNotNull(dataBroker
        //    .newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, IfmUtil
        //    .buildId(INTERFACE_NAME_2)).checkedGet().get().augmentation(IfExternal.class));

        // 19. update parent-refs
        //interfaceManager.updateInterfaceParentRef(INTERFACE_NAME_2, PARENT_INTERFACE_2, true);
        waitTillOperationCompletes("create external vlan interface api",
                coordinatorEventsWaiter, 4, asyncEventsWaiter);
        Assert.assertEquals(PARENT_INTERFACE_2, dataBroker
            .newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, IfmUtil
                .buildId(INTERFACE_NAME_2)).checkedGet().get().augmentation(ParentRefs.class).getParentInterface());

        // 20. get list of vlan interfaces
        // FIXME need to wait for https://git.opendaylight.org/gerrit/#/c/54811/ this to land
        // to do proper assertion
        assertEqualBeans(3, interfaceManager.getVlanInterfaces().size());

        // 21. check if an interface is external interface
        Assert.assertTrue(interfaceManager.isExternalInterface(INTERFACE_NAME_2));

        // 22. check port name for an interface, given dpn-id and interface-name
        assertEqualBeans(PARENT_INTERFACE, interfaceManager.getPortNameForInterface(DPN_ID_1.toString(),
            PARENT_INTERFACE));

        // 23. check port name for an interface, given nodeconnectorid
        assertEqualBeans(PARENT_INTERFACE, interfaceManager.getPortNameForInterface(new NodeConnectorId("openflow:1:2"),
            PARENT_INTERFACE));

        // 24. get termination-points from cache
        Assert.assertNotNull(interfaceManager.getTerminationPointCache().get(INTERFACE_NAME));

        // 25. fetch termination point for interface
        assertEqualBeans(ExpectedTerminationPoint.newOvsdbTerminationPointAugmentation(), interfaceManager
            .getTerminationPointForInterface(INTERFACE_NAME));

        // 26. fetch ovsdb bridge corresponding to an interface
        assertEqualBeans(ExpectedOvsdbBridge.newOvsdbBridge(),
            interfaceManager.getOvsdbBridgeForInterface(INTERFACE_NAME));

        // 27. fetch ovsdb bridge corresponding to nodeIid
        assertEqualBeans(ExpectedOvsdbBridge.newOvsdbBridge(), interfaceManager.getOvsdbBridgeForNodeIid(
            OvsdbSouthboundTestUtil.createInstanceIdentifier("192.168.56.101", 6640, "s2")));
    }

    private void checkVlanRpcs() throws Exception {
        //1. Test dpn-id fetching from interface
        GetDpidFromInterfaceInput dpidFromInterfaceInput = new GetDpidFromInterfaceInputBuilder()
                .setIntfName(INTERFACE_NAME).build();
        Future<RpcResult<GetDpidFromInterfaceOutput>> dpidFromInterfaceOutput =
            odlInterfaceRpcService.getDpidFromInterface(dpidFromInterfaceInput);
        Assert.assertEquals(DpnFromInterfaceOutput.newDpnFromInterfaceOutput(),
                dpidFromInterfaceOutput.get().getResult());

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
        /* FIXME can be tested only once ResourceBatchingManager becomes testable
        GetInterfaceFromIfIndexInput interfaceFromIfIndexInput = new GetInterfaceFromIfIndexInputBuilder()
                .setIfIndex(1).build();
        Future<RpcResult<GetInterfaceFromIfIndexOutput>> interfaceFromIfIndexOutput = odlInterfaceRpcService
            .getInterfaceFromIfIndex(interfaceFromIfIndexInput);
        assertEqualBeans(InterfaceFromIfIndexOutput.newInterfaceFromIfIndexOutput(),
                interfaceFromIfIndexOutput.get().getResult());*/

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
        GetEndpointIpForDpnInput endpointIpForDpnInput = new GetEndpointIpForDpnInputBuilder().setDpid(DPN_ID_2)
            .build();
        Future<RpcResult<GetEndpointIpForDpnOutput>> endpointIpForDpnOutput = odlInterfaceRpcService
                .getEndpointIpForDpn(endpointIpForDpnInput);
        assertEqualBeans(EndPointIpFromDpn.newEndPointIpFromDpn(), endpointIpForDpnOutput.get().getResult());


        //2. fetch tunnel type from interface-name
        GetTunnelTypeInput tunnelTypeInput = new GetTunnelTypeInputBuilder().setIntfName(TUNNEL_INTERFACE_NAME).build();
        Future<RpcResult<GetTunnelTypeOutput>> tunnelTypeOutput = odlInterfaceRpcService.getTunnelType(tunnelTypeInput);
        assertEqualBeans(TunnelTypeOutput.newTunnelTypeOutput(), tunnelTypeOutput.get().getResult());
    }

    private void checkTunnelApis() throws  Exception {

        // 1. fetch get all ports on bridge
        assertEqualBeans(ExpectedTerminationPoint.newTerminationPointList(),
            interfaceManager.getPortsOnBridge(DPN_ID_2));

        // 2. fetch get all tunnel ports on bridge
        assertEqualBeans(ExpectedTerminationPoint.newTerminationPointList(),
            interfaceManager.getTunnelPortsOnBridge(DPN_ID_2));

        // 3. fetch tunnel end point ip for DPN
        assertEqualBeans("2.2.2.2", interfaceManager.getEndpointIpForDpn(DPN_ID_2));

        // 4. get list of vxlan interfaces
        assertEqualBeans(1, interfaceManager.getVxlanInterfaces().size());
    }

    public void testVlanMemberInterface() throws Exception {
        // Test VlanMember interface creation
        InterfaceManagerTestUtil.putVlanInterfaceConfig(dataBroker, TRUNK_INTERFACE_NAME, INTERFACE_NAME,
                IfL2vlan.L2vlanMode.TrunkMember);
        InterfaceManagerTestUtil.waitTillOperationCompletes("create vlan member interface",
                coordinatorEventsWaiter, 7, asyncEventsWaiter);

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
                Interface.OperStatus.Up, L2vlan.class, DPN_ID_1.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);

        // FIXME can assert this only once ResourceBatchingManager becomes testable
        // b) check if lport-tag to interface mapping is created
        /*InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = InstanceIdentifier.builder(
            IfIndexesInterfaceMap.class).child(
            IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
        Assert.assertEquals(TRUNK_INTERFACE_NAME, dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            ifIndexInterfaceInstanceIdentifier).checkedGet().get().getInterfaceName());*/

        //Update test
        // i) vlan member interface admin-state updated
        InterfaceManagerTestUtil.updateInterfaceAdminState(dataBroker, TRUNK_INTERFACE_NAME, false);

        InterfaceManagerTestUtil.waitTillOperationCompletes("update vlan member interface admin state",
                coordinatorEventsWaiter, 2, asyncEventsWaiter);

        //Then
        // a) check if operational/ietf-interfaces-state is updated for vlan interface
        ifaceState = dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TRUNK_INTERFACE_NAME)).checkedGet().get();
        assertEqualBeans(ExpectedInterfaceState.newInterfaceState(ifaceState.getIfIndex(), TRUNK_INTERFACE_NAME,
            Interface.OperStatus.Down, L2vlan.class, DPN_ID_1.toString(),
                ifaceState.getStatistics().getDiscontinuityTime()), ifaceState);

        InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, TRUNK_INTERFACE_NAME);
        InterfaceManagerTestUtil.waitTillOperationCompletes("delete vlan member interface",
                coordinatorEventsWaiter, 7, asyncEventsWaiter);
        // 1. Then
        // a)
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).get());

        // b) check if operational/ietf-interfaces-state is deleted for the vlan interface
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            IfmUtil.buildStateInterfaceId(TRUNK_INTERFACE_NAME)).get());

        // FIXME can assert this only once ResourceBatchingManager becomes testable
        // c) check if lport-tag to interface mapping is deleted
        /*Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
            ifIndexInterfaceInstanceIdentifier).get());*/
    }

    @Ignore
    @Test
    public void testDpnToInterfaceList() throws  Exception {
        //Write into DpnToInterfaceList
        createDpnToInterface(DPN_ID_1, "23701c04-7e58-4c65-9425-78a80d49a218", L2vlan.class);

        //Test interface list fetching from dpnId
        GetDpnInterfaceListInput dpnInterfaceListInput = new GetDpnInterfaceListInputBuilder()
                .setDpid(DPN_ID_1).build();
        Future<RpcResult<GetDpnInterfaceListOutput>> dpnInterfaceListOutput = odlInterfaceRpcService
                .getDpnInterfaceList(dpnInterfaceListInput);

        List<Interfaces> actualDpnInterfaceList = dpnInterfaceListOutput.get().getResult().getInterfaces();
        assertEqualBeans(ExpectedInterfaceListFromDpn.checkDpnToInterfaceList(), actualDpnInterfaceList.get(0));
    }

    private void createDpnToInterface(BigInteger dpId, String infName,
                                      Class<? extends InterfaceType> interfaceType) throws  Exception {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        DpnToInterfaceKey dpnToInterfaceKey = new DpnToInterfaceKey(dpId);
        InterfaceNameEntryKey interfaceNameEntryKey = new InterfaceNameEntryKey(infName);
        InstanceIdentifier<InterfaceNameEntry> intfid = InstanceIdentifier.builder(DpnToInterfaceList.class)
                .child(DpnToInterface.class, dpnToInterfaceKey)
                .child(InterfaceNameEntry.class, interfaceNameEntryKey)
                .build();
        InterfaceNameEntryBuilder entryBuilder =
                new InterfaceNameEntryBuilder().withKey(interfaceNameEntryKey).setInterfaceName(infName);
        if (interfaceType != null) {
            entryBuilder.setInterfaceType(interfaceType);
        }
        tx.put(LogicalDatastoreType.OPERATIONAL, intfid, entryBuilder.build(), true);
        tx.submit().checkedGet();
    }
}
