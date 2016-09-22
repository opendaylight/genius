/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.test;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateUpdateHelper;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.DpnToInterfaceList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.IfIndexesInterfaceMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class StateInterfaceTest {
    BigInteger dpId = BigInteger.valueOf(1);
    NodeConnectorId nodeConnectorId = null;
    NodeConnector nodeConnector = null;
    FlowCapableNodeConnector fcNodeConnectorNew = null;
    Interface vlanInterfaceEnabled = null;
    Interface vlanInterfaceDisabled = null;
    Interface tunnelInterfaceEnabled;
    Interface childVlanInterfaceEnabled = null;
    InterfaceParentEntryKey interfaceParentEntryKey = null;
    InterfaceParentEntryKey higherLayerInterfaceParentEntryKey = null;
    IfIndexInterface IfindexInterface = null;
    InstanceIdentifier<Interface> interfaceInstanceIdentifier = null;
    InstanceIdentifier<Interface> childInterfaceInstanceIdentifier= null;
    InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryInstanceIdentifier = null;
    InstanceIdentifier<FlowCapableNodeConnector> fcNodeConnectorId = null;
    InstanceIdentifier<IfIndexInterface> ifIndexId =null;
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceStateIdentifier = null;
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> childInterfaceStateIdentifier = null;
    InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier = null;
    InstanceIdentifier<InterfaceParentEntry> higherLevelInterfaceParentEntryIdentifier = null;
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface stateInterface;
    InterfaceParentEntry interfaceParentEntry;
    InterfaceParentEntry higherLayerInterfaceParentEntry;
    DpnToInterface dpnToInterface;

    @Mock DataBroker dataBroker;
    @Mock
    IdManagerService idManager;
    @Mock IMdsalApiManager mdsalManager;
    @Mock ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock AlivenessMonitorService alivenessMonitorService;
    @Mock org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node mockNode;

    OvsInterfaceStateAddHelper addHelper;
    OvsInterfaceStateRemoveHelper removeHelper;
    OvsInterfaceStateUpdateHelper updateHelper;

    @Before
    public void setUp() throws Exception {
        when(dataBroker.registerDataChangeListener(
                any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(DataChangeListener.class),
                any(DataChangeScope.class)))
                .thenReturn(dataChangeListenerRegistration);
        setupMocks();
        InterfaceManagerTestUtil.clearInterfaceCaches();
    }

    private void setupMocks() {
        List<InterfaceNameEntry> interfaceNameEntries = new ArrayList<>();
        interfaceNameEntries.add(new InterfaceNameEntryBuilder().setInterfaceName(InterfaceManagerTestUtil.interfaceName).build());
        nodeConnectorId = InterfaceManagerTestUtil.buildNodeConnectorId(BigInteger.valueOf(1), 2);
        nodeConnector = InterfaceManagerTestUtil.buildFlowCapableNodeConnector(nodeConnectorId);
        fcNodeConnectorNew = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);
        fcNodeConnectorId = InterfaceManagerTestUtil.getFlowCapableNodeConnectorIdentifier("openflow:1", nodeConnectorId);
        IfindexInterface = InterfaceManagerTestUtil.buildIfIndexInterface(100, InterfaceManagerTestUtil.interfaceName);
        ifIndexId = InstanceIdentifier.builder(IfIndexesInterfaceMap.class).child(IfIndexInterface.class, new IfIndexInterfaceKey(100)).build();
        interfaceInstanceIdentifier = InterfaceManagerCommonUtils.getInterfaceIdentifier(new InterfaceKey(InterfaceManagerTestUtil.interfaceName));
        childInterfaceInstanceIdentifier = InterfaceManagerCommonUtils.getInterfaceIdentifier(new InterfaceKey(InterfaceManagerTestUtil.childInterface));
        interfaceStateIdentifier = IfmUtil.buildStateInterfaceId(InterfaceManagerTestUtil.interfaceName);
        childInterfaceStateIdentifier = IfmUtil.buildStateInterfaceId(InterfaceManagerTestUtil.childInterface);
        vlanInterfaceEnabled = InterfaceManagerTestUtil.buildInterface(InterfaceManagerTestUtil.interfaceName, "Test Vlan Interface1", true, L2vlan.class, BigInteger.valueOf(1));
        vlanInterfaceDisabled = InterfaceManagerTestUtil.buildInterface(InterfaceManagerTestUtil.interfaceName, "Test Vlan Interface1", false, L2vlan.class, BigInteger.valueOf(1));
        tunnelInterfaceEnabled = InterfaceManagerTestUtil.buildTunnelInterface(dpId, InterfaceManagerTestUtil.tunnelInterfaceName ,"Test Tunnel Interface", true, TunnelTypeGre.class, "192.168.56.101", "192.168.56.102");
        childVlanInterfaceEnabled = InterfaceManagerTestUtil.buildInterface(InterfaceManagerTestUtil.childInterface, "Test Vlan Interface2", true, L2vlan.class, BigInteger.valueOf(1));
        interfaceParentEntryKey = new InterfaceParentEntryKey(InterfaceManagerTestUtil.interfaceName);
        interfaceParentEntryIdentifier = InterfaceMetaUtils.getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        higherLayerInterfaceParentEntryKey = new InterfaceParentEntryKey(InterfaceManagerTestUtil.childInterface);
        higherLevelInterfaceParentEntryIdentifier= InterfaceMetaUtils.getInterfaceParentEntryIdentifier(higherLayerInterfaceParentEntryKey);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
        List<String> lowerLayerIfList = new ArrayList<>();
        lowerLayerIfList.add(nodeConnectorId.getValue());
        ifaceBuilder.setOperStatus(OperStatus.Up).setAdminStatus(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up)
                .setPhysAddress(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress.getDefaultInstance("AA:AA:AA:AA:AA:AA"))
                .setIfIndex(100)
                .setLowerLayerIf(lowerLayerIfList).setType(L2vlan.class);
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(InterfaceManagerTestUtil.interfaceName));

        stateInterface = ifaceBuilder.build();

        InterfaceParentEntryBuilder ifaceParentEntryBuilder = new InterfaceParentEntryBuilder();
        List<InterfaceChildEntry> ifaceChildEntryList= new ArrayList<>();
        ifaceChildEntryList.add(new InterfaceChildEntryBuilder().setChildInterface(InterfaceManagerTestUtil.childInterface).build());
        interfaceParentEntry = ifaceParentEntryBuilder.setInterfaceChildEntry(ifaceChildEntryList).build();

        InterfaceParentEntryBuilder higherLayerIfParentEntryBuilder = new InterfaceParentEntryBuilder();
        List<InterfaceChildEntry> ifaceChildEntryListForHigherParent= new ArrayList<>();
        higherLayerInterfaceParentEntry = higherLayerIfParentEntryBuilder.setInterfaceChildEntry(ifaceChildEntryListForHigherParent).build();
        dpnToInterface = new DpnToInterfaceBuilder().setDpid(dpId).setInterfaceNameEntry(interfaceNameEntries).build();

        when(dataBroker.newReadOnlyTransaction()).thenReturn(mockReadTx);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTx);
    }

    @Test
    public void testAddStateInterface() {
        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);
        AllocateIdOutput expectedId = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("100")).build();
        Optional<InterfaceParentEntry>expectedParentEntry = Optional.of(interfaceParentEntry);
        Optional<InterfaceParentEntry> higherLayerParentOptional = Optional.of(higherLayerInterfaceParentEntry);
        Optional<Interface> expectedChildInterface = Optional.of(childVlanInterfaceEnabled);

        Future<RpcResult<AllocateIdOutput>> idOutputOptional = RpcResultBuilder.success(expectedId).buildFuture();
        Future<RpcResult<AllocateIdOutput>> idOutputOptional2 = RpcResultBuilder.success(expectedId).buildFuture();

        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedParentEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(higherLayerParentOptional)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, higherLevelInterfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedChildInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, childInterfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, ifIndexId);

        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setIdKey(InterfaceManagerTestUtil.interfaceName).build();
        doReturn(idOutputOptional).when(idManager).allocateId(getIdInput);
        AllocateIdInput getIdInput2 = new AllocateIdInputBuilder()
                .setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setIdKey(InterfaceManagerTestUtil.childInterface).build();
        doReturn(idOutputOptional2).when(idManager).allocateId(getIdInput2);

        addHelper.addState(dataBroker, idManager, mdsalManager, alivenessMonitorService,
                nodeConnectorId, InterfaceManagerTestUtil.interfaceName, fcNodeConnectorNew);

        //Add some verifications
        verify(mockWriteTx).put(LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier,
                stateInterface,true);
    }

    @Test
    public void testAddStateTunnelInterface() {

        Optional<Interface> expectedInterface = Optional.of(tunnelInterfaceEnabled);
        AllocateIdOutput expectedId = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("100")).build();

        Future<RpcResult<AllocateIdOutput>> idOutputOptional = RpcResultBuilder.success(expectedId).buildFuture();

        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);

        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setIdKey(InterfaceManagerTestUtil.interfaceName).build();
        doReturn(idOutputOptional).when(idManager).allocateId(getIdInput);

        addHelper.addState(dataBroker, idManager, mdsalManager, alivenessMonitorService,
                nodeConnectorId, InterfaceManagerTestUtil.interfaceName, fcNodeConnectorNew);
    }

    @Test
    public void testDeleteStateInterface() {
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface>
                expectedStateInterface = Optional.of(stateInterface);
        Optional<IfIndexInterface> expectedIfindexInterface = Optional.of(IfindexInterface);
        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);
        Optional<InterfaceParentEntry>expectedParentEntry = Optional.of(interfaceParentEntry);
        Optional<InterfaceParentEntry> higherLayerParentOptional = Optional.of(higherLayerInterfaceParentEntry);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, InterfaceManagerTestUtil.nodeKey).build();
        Optional<Node> nodeOptional = Optional.of(mockNode);
        Optional<DpnToInterface> dpnToInterfaceOptional = Optional.of(dpnToInterface);
        InstanceIdentifier<DpnToInterface> dpnToInterfaceInstanceIdentifier =
                InstanceIdentifier.builder(DpnToInterfaceList.class).child(DpnToInterface.class, new DpnToInterfaceKey(BigInteger.ONE)).build();

        doReturn(Futures.immediateCheckedFuture(expectedIfindexInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, ifIndexId);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, childInterfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedParentEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(higherLayerParentOptional)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, higherLevelInterfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(nodeOptional)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(dpnToInterfaceOptional)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, dpnToInterfaceInstanceIdentifier);

        ReleaseIdInput getIdInput = new ReleaseIdInputBuilder()
                .setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setIdKey(InterfaceManagerTestUtil.interfaceName).build();

        doReturn(Futures.immediateFuture(RpcResultBuilder.<Void>success().build())).when(idManager).releaseId(getIdInput);
        boolean isNodePresent = InterfaceManagerCommonUtils.isNodePresent(dataBroker, nodeConnectorId);
        removeHelper.removeInterfaceStateConfiguration(idManager, mdsalManager, alivenessMonitorService, nodeConnectorId,
                nodeConnectorId, dataBroker, InterfaceManagerTestUtil.interfaceName, fcNodeConnectorNew, isNodePresent);

        verify(mockWriteTx).delete(LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);

    }

    @Test
    public void testDeleteStateTunnelInterface() {
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface>
                expectedStateInterface = Optional.of(stateInterface);
        Optional<Interface> expectedInterface = Optional.of(tunnelInterfaceEnabled);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, InterfaceManagerTestUtil.nodeKey).build();
        Optional<Node> nodeOptional = Optional.of(mockNode);
        AllocateIdOutput expectedId = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("100")).build();
        Future<RpcResult<AllocateIdOutput>> idOutputOptional = RpcResultBuilder.success(expectedId).buildFuture();
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setIdKey(InterfaceManagerTestUtil.interfaceName).build();
        doReturn(idOutputOptional).when(idManager).allocateId(getIdInput);
        ReleaseIdInput releaseIdInput = new ReleaseIdInputBuilder()
                .setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setIdKey(InterfaceManagerTestUtil.interfaceName).build();

        doReturn(Futures.immediateFuture(RpcResultBuilder.<Void>success().build())).when(idManager).releaseId(releaseIdInput);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(nodeOptional)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier);

        boolean isNodePresent = InterfaceManagerCommonUtils.isNodePresent(dataBroker, nodeConnectorId);
        removeHelper.removeInterfaceStateConfiguration(idManager, mdsalManager, alivenessMonitorService, nodeConnectorId,
                nodeConnectorId, dataBroker, InterfaceManagerTestUtil.interfaceName, fcNodeConnectorNew, isNodePresent);

    }

    @Test
    public void testUpdateStateInterface() {

        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface>
                expectedStateInterface = Optional.of(stateInterface);
        Optional<InterfaceParentEntry> expectedParentEntry = Optional.of(interfaceParentEntry);
        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);
        Optional<Interface> expectedChildInterface = Optional.of(childVlanInterfaceEnabled);
        Optional<InterfaceParentEntry> higherLayerParentOptional = Optional.of(higherLayerInterfaceParentEntry);

        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, childInterfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedParentEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(higherLayerParentOptional)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, higherLevelInterfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedChildInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, childInterfaceInstanceIdentifier);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface parentInterface = InterfaceManagerTestUtil.buildStateInterface(InterfaceManagerTestUtil.interfaceName, null, OperStatus.Down);
        InterfaceManagerTestUtil.buildStateInterface(InterfaceManagerTestUtil.childInterface, null, OperStatus.Down);

        FlowCapableNodeConnector fcNodeConnectorOld = InterfaceManagerTestUtil.buildFlowCapableNodeConnector(false, true, "AA:AA:AA:AA:AA:AA");
        FlowCapableNodeConnector fcNodeConnectorNew = InterfaceManagerTestUtil.buildFlowCapableNodeConnector(true, false, "AA:AA:AA:AA:AA:AA");

        updateHelper.updateState(fcNodeConnectorId, alivenessMonitorService, dataBroker, InterfaceManagerTestUtil.interfaceName, fcNodeConnectorNew, fcNodeConnectorOld);

        verify(mockWriteTx).merge(LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier, parentInterface, false);
    }
}
