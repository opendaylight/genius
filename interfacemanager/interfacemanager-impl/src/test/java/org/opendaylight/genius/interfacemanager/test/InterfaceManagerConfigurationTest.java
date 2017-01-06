/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Rule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceChildEntry;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedFlowEntries;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedInterfaceState;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedServicesInfo;
import org.opendaylight.genius.interfacemanager.test.xtend.InterfaceMeta;
import org.opendaylight.genius.interfacemanager.test.xtend.ExpectedTerminationPoint;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
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
import static org.opendaylight.genius.mdsalutil.NwConstants.DEFAULT_EGRESS_SERVICE_INDEX;
import static org.opendaylight.genius.mdsalutil.NwConstants.VLAN_INTERFACE_INGRESS_TABLE;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

/**
 * Component tests for interface manager.
 *
 * @author Michael Vorburger
 * @author Faseela K
 * @author sathish kumar B T
 */
@SuppressWarnings("deprecation")
public class InterfaceManagerConfigurationTest {

    public @Rule MethodRule guice = new GuiceRule(new InterfaceManagerTestModule());
    private static final BigInteger dpnId = BigInteger.valueOf(1);

    private @Inject DataBroker dataBroker;

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

        CheckedFuture<Void,TransactionCommitFailedException> deleteBridgeFuture = OvsdbSouthboundTestUtil.deleteBridge(dataBroker);
        AsyncFunction<Object,Void> verifyOperDSBridge = input -> {
            assertEqualBeans(null,InterfaceMetaUtils.getBridgeRefEntryFromOperDS(dpnId, dataBroker));
            return null;
        };
        Futures.transform(deleteBridgeFuture,verifyOperDSBridge);
    }

    private void setupAndAssertBridgeCreation() throws InterruptedException {

        CheckedFuture<Void,TransactionCommitFailedException> createBridgeFuture = OvsdbSouthboundTestUtil.createBridge(dataBroker);
        AsyncFunction<Object,Void>  verifyBridgeCreationAsync = input -> {
            // a) Check bridgeRefEntry in cache and OperDS are same and use the right dpnId
            BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpnId);
            InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                    .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
            BridgeRefEntry bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker).orNull();
            assertEqualBeans(InterfaceMetaUtils.getBridgeRefEntryFromCache(dpnId), bridgeRefEntry);
            assertEqualBeans(bridgeRefEntry.getDpid(), dpnId);
            return null;
        };
        Futures.transform(createBridgeFuture,verifyBridgeCreationAsync);

    }

    @Test
    public void vlanInterfaceTests() throws Exception {

        // i) parent-interface specified in above vlan configuration comes in operational/ietf-interfaces-state
        CheckedFuture<Void,TransactionCommitFailedException> interfaceStateFuture = InterfaceManagerTestUtil.putInterfaceState(dataBroker, parentInterface, null);
        AsyncFunction<Object,Void> addVLANIntffunc = input -> {
            // ii) Vlan interface written to config/ietf-interfaces DS and corresponding parent-interface is not present
            //     in operational/ietf-interface-state
            ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(parentInterface).build();
            return InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, interfaceName, parentRefs, L2vlan.class);
        };
        ListenableFuture<Void> wroteVLANIntfFuture = Futures.transform(interfaceStateFuture,addVLANIntffunc);

        // TODO Must think about proper solution for better synchronization here instead of silly wait()...
        // TODO use TestDataStoreJobCoordinator.waitForAllJobs() when https://git.opendaylight.org/gerrit/#/c/48061/ is merged
        // 3. Then
        // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info was created
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey(parentInterface),
                        new InterfaceChildEntryKey(interfaceName));

        AsyncFunction<Object ,Void>  interfaceValidationFun = input-> {
            // a) check if operational/ietf-interfaces-state is populated for the vlan interface
            Interface ifaceState = getAnInterfaceState();
            // b) check if lport-tag to interface mapping is created
            InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = getIfIndexInterfaceInstanceIdentifier(ifaceState);

            assertEqualBeans(ExpectedInterfaceChildEntry.interfaceChildEntry(), dataBroker.newReadOnlyTransaction()
                    .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).checkedGet().get());

            // Then
            assertEqualBeans(ExpectedInterfaceState.newInterfaceState(), ifaceState);

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
            return InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, interfaceName);
        };

        ListenableFuture<Void> interfaceValidationFuture = Futures.transform(wroteVLANIntfFuture,interfaceValidationFun);

        AsyncFunction<Object,Void> verifyInterfaceChildMapFunc = input -> {
            // 3. Then
            // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info is deleted
            // b) check if lport-tag to interface mapping is created

            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                    .interfaces.state.Interface ifaceState = getAnInterfaceState();

            InstanceIdentifier<IfIndexInterface> ifIndexInterfaceInstanceIdentifier = getIfIndexInterfaceInstanceIdentifier(ifaceState);

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
            return null;
        };

        Futures.transform(interfaceValidationFuture,verifyInterfaceChildMapFunc);

    }

    //  check if lport-tag to interface mapping is created
    private InstanceIdentifier<IfIndexInterface> getIfIndexInterfaceInstanceIdentifier(Interface ifaceState) {
        return InstanceIdentifier.builder(
                IfIndexesInterfaceMap.class).child(
                IfIndexInterface.class, new IfIndexInterfaceKey(ifaceState.getIfIndex())).build();
    }

    @Test public void newTunnelInterface() throws Exception {
        // 1. Given
        // 2. When
        // i) dpn-id specified above configuration comes in operational/network-topology
        // ii) Vlan interface written to config/ietf-interfaces DS and corresponding parent-interface is not present
        //     in operational/ietf-interface-state
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dpnId).build();
        CheckedFuture<Void,TransactionCommitFailedException> tunnelInterfaceConfigFuture = InterfaceManagerTestUtil.putInterfaceConfig(dataBroker, tunnelInterfaceName, parentRefs, Tunnel.class);
        AsyncFunction<Object,Void> updateTunnelInterfaceStateFunc =
                input ->  {
                    // iii) tunnel interface comes up in operational/ietf-interfaces-state
                    return   InterfaceManagerTestUtil.putInterfaceState(dataBroker, tunnelInterfaceName, Tunnel.class);
                };
        ListenableFuture<Void> queryFuture = Futures.transform(tunnelInterfaceConfigFuture,updateTunnelInterfaceStateFunc);

        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpnId);
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey = new BridgeInterfaceEntryKey(tunnelInterfaceName);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey, bridgeInterfaceEntryKey);
        AsyncFunction<Object,Void> validateBridgeFunc = input ->  {
            assertEqualBeans(InterfaceMeta.newBridgeInterface(), dataBroker.newReadOnlyTransaction()
                    .read(CONFIGURATION, bridgeInterfaceEntryIid).checkedGet().get());
            return null;
        };
        ListenableFuture<Void> bridgeValidateFuture = Futures.transform(queryFuture,validateBridgeFunc);

        // TODO Must think about proper solution for better synchronization here instead of silly wait()...
        // TODO use TestDataStoreJobCoordinator.waitForAllJobs() when https://git.opendaylight.org/gerrit/#/c/48061/ is merged
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                .network.topology.topology.Node> bridgeIid =
                OvsdbSouthboundTestUtil.createInstanceIdentifier("192.168.56.101", 6640,  "s2");
        InstanceIdentifier<TerminationPoint> tpIid = InterfaceManagerTestUtil.getTerminationPointId(bridgeIid,
                tunnelInterfaceName);

        AsyncFunction<Object,Void> validateTerminationEndPointFunc = input ->  {
            //        // a) check if termination end point is created in config/network-topology

            assertEqualBeans(ExpectedTerminationPoint.newTerminationPoint(),
                    dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).checkedGet().get());

            // Delete test
            // iii) tunnel interface is deleted from config/ietf-interfaces
            return InterfaceManagerTestUtil.deleteInterfaceConfig(dataBroker, tunnelInterfaceName);
        };

        ListenableFuture<Void> endPointValidateFuture = Futures.transform(bridgeValidateFuture,validateTerminationEndPointFunc);

        AsyncFunction<Object,Void> verifyTunnelAndTEPdelFromDSFunc = input -> {
            Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                    .read(CONFIGURATION, bridgeInterfaceEntryIid).get());

            // b) check if termination end point is deleted in config/network-topology
            Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().read(CONFIGURATION, tpIid).get());
            return null;
        };
        Futures.transform(endPointValidateFuture,verifyTunnelAndTEPdelFromDSFunc);

    }

    //    check if operational/ietf-interfaces-state is populated for the vlan interface
    private Interface getAnInterfaceState() throws ReadFailedException {
        return dataBroker.newReadOnlyTransaction().read(OPERATIONAL,
                IfmUtil.buildStateInterfaceId(interfaceName)).checkedGet().get();
    }
}
