/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.test;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
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
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.*;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VlanInterfaceConfigurationTest {

    @Mock
    DataBroker dataBroker;
    @Mock
    AlivenessMonitorService alivenessMonitorService;
    @Mock IdManager idManager;
    @Mock ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock IMdsalApiManager mdsalApiManager;
    OvsInterfaceConfigAddHelper addHelper;
    OvsInterfaceConfigRemoveHelper removeHelper;
    OvsVlanMemberConfigAddHelper memberConfigAddHelper;
    OvsVlanMemberConfigRemoveHelper memberConfigRemoveHelper;
    OvsVlanMemberConfigUpdateHelper memberConfigUpdateHelper;

    NodeConnectorId nodeConnectorId;
    NodeConnector nodeConnector;
    Interface vlanInterfaceEnabled;
    Interface vlanInterfaceDisabled;
    InstanceIdentifier<Interface> interfaceInstanceIdentifier;
    InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier;
    InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier = null;
    InterfaceParentEntry interfaceParentEntry;
    InterfaceChildEntry interfaceChildEntry = null;
    InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier;
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceStateIdentifier;
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface stateInterface;

    @Before
    public void setUp() throws Exception {
        when(dataBroker.registerDataChangeListener(
                any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(DataChangeListener.class),
                any(DataChangeScope.class)))
                .thenReturn(dataChangeListenerRegistration);
        setupMocks();
    }

    @After
    public void cleanUp(){
    }

    private void setupMocks() {
        nodeConnectorId = InterfaceManagerTestUtil.buildNodeConnectorId(BigInteger.valueOf(1), 1);
        nodeConnector = InterfaceManagerTestUtil.buildNodeConnector(nodeConnectorId);
        vlanInterfaceEnabled = InterfaceManagerTestUtil.buildInterface(InterfaceManagerTestUtil.interfaceName, "Test Vlan Interface1", true, L2vlan.class, BigInteger.valueOf(1));
        vlanInterfaceDisabled = InterfaceManagerTestUtil.buildInterface(InterfaceManagerTestUtil.interfaceName, "Test Vlan Interface1", false, L2vlan.class, BigInteger.valueOf(1));
        interfaceInstanceIdentifier = IfmUtil.buildId(InterfaceManagerTestUtil.interfaceName);
        nodeConnectorInstanceIdentifier = InterfaceManagerTestUtil.getNcIdent("openflow:1", nodeConnectorId);
        interfaceStateIdentifier = IfmUtil.buildStateInterfaceId(vlanInterfaceEnabled.getName());
        stateInterface = InterfaceManagerTestUtil.buildStateInterface(InterfaceManagerTestUtil.interfaceName, nodeConnectorId);
        AllocateIdOutput output = new AllocateIdOutputBuilder().setIdValue((long)1).build();
        RpcResultBuilder<AllocateIdOutput> allocateIdRpcBuilder = RpcResultBuilder.success();
        allocateIdRpcBuilder.withResult(output);
        ListenableFuture<RpcResult<AllocateIdOutput>> future = Futures.immediateFuture(allocateIdRpcBuilder.build());
        interfaceParentEntryIdentifier = InterfaceMetaUtils.getInterfaceParentEntryIdentifier(
                new InterfaceParentEntryKey(InterfaceManagerTestUtil.interfaceName));
        interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils.getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey("s1-eth1"),
                new InterfaceChildEntryKey(vlanInterfaceEnabled.getName()));
        interfaceChildEntry = new InterfaceChildEntryBuilder().setKey(new InterfaceChildEntryKey(vlanInterfaceEnabled.getName())).
                setChildInterface(vlanInterfaceEnabled.getName()).build();

        InterfaceParentEntryBuilder ifaceParentEntryBuilder = new InterfaceParentEntryBuilder();
        List<InterfaceChildEntry> ifaceChildEntryList= new ArrayList<>();
        ifaceChildEntryList.add(interfaceChildEntry);
        interfaceParentEntry = ifaceParentEntryBuilder.setInterfaceChildEntry(ifaceChildEntryList).build();

        // Setup mocks
        when(dataBroker.newReadOnlyTransaction()).thenReturn(mockReadTx);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTx);

        when(idManager.allocateId(any(AllocateIdInput.class))).thenReturn(future);
    }

    @Test
    public void testAddVlanInterfaceWhenSwitchIsNotConnected() {
        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateInterface = Optional.of(stateInterface);

        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                        LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(
               LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);

        addHelper.addConfiguration(dataBroker, vlanInterfaceEnabled.getAugmentation(ParentRefs.class), vlanInterfaceEnabled, idManager,
                alivenessMonitorService, mdsalApiManager);

        //Nothing to verify, since when switch is not connected we don't do any datastore operation

    }

    @Test
    public void testAddVlanInterfaceWhenSwitchIsConnected() {
        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface>
                expectedStateInterface = Optional.of(stateInterface);

        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);

        addHelper.addConfiguration(dataBroker, vlanInterfaceEnabled.getAugmentation(ParentRefs.class), vlanInterfaceEnabled, idManager,
                alivenessMonitorService, mdsalApiManager);

        //Nothing to verify, since when adminstate is enabled and switch opstate is already up,
        //we don't do any datastore operation
    }

    @Test
    public void testAddVlanInterfaceWhenAdminStateDisabled() {
        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateInterface =
                Optional.of(stateInterface);

        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
        ifaceBuilder.setOperStatus(OperStatus.Down);
        ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(vlanInterfaceEnabled.getName()));
        ifaceBuilder.setType(L2vlan.class);
        stateInterface = ifaceBuilder.build();

        addHelper.addConfiguration(dataBroker, vlanInterfaceDisabled.getAugmentation(ParentRefs.class), vlanInterfaceDisabled, idManager,
                alivenessMonitorService, mdsalApiManager);

        //verification
        verify(mockWriteTx).put(LogicalDatastoreType.CONFIGURATION, interfaceChildEntryInstanceIdentifier, interfaceChildEntry, true);
    }

    @Test
    public void testDeleteVlanInterface() {
        Optional<Interface> expected = Optional.of(vlanInterfaceEnabled);
        Optional<NodeConnector> expectedNc = Optional.of(nodeConnector);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateIf = Optional.of(stateInterface);
        doReturn(Futures.immediateCheckedFuture(expected)).when(mockReadTx).read(
                        LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedNc)).when(mockReadTx).read(
                        LogicalDatastoreType.OPERATIONAL, nodeConnectorInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateIf)).when(mockReadTx).read(
                        LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);

        removeHelper.removeConfiguration(dataBroker,alivenessMonitorService, vlanInterfaceEnabled, idManager,
                mdsalApiManager, vlanInterfaceEnabled.getAugmentation(ParentRefs.class));

        //verification
        verify(mockWriteTx).delete(LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
    }

    @Test
    public void testAddMemberConfiguration(){

        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateInterface = Optional.of(stateInterface);

        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);

        memberConfigAddHelper.addConfiguration(dataBroker, vlanInterfaceEnabled.getAugmentation(ParentRefs.class), vlanInterfaceEnabled, null, idManager);
    }

    @Test
    public void testRemoveMemberConfiguration(){

        Optional<InterfaceParentEntry> expectedParentEntry = Optional.of(interfaceParentEntry);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateInterface = Optional.of(stateInterface);

        doReturn(Futures.immediateCheckedFuture(expectedParentEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);

        memberConfigRemoveHelper.removeConfiguration(dataBroker, vlanInterfaceEnabled.getAugmentation(ParentRefs.class), vlanInterfaceEnabled, null, idManager);
    }

    @Test
    public void testUpdateMemberConfiguration(){

        Optional<InterfaceChildEntry> expectedChildEntry = Optional.of(interfaceChildEntry);
        Optional<InterfaceParentEntry> expectedParentEntry = Optional.of(interfaceParentEntry);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateInterface = Optional.of(stateInterface);
        Optional<Interface> expectedInterface = Optional.of(vlanInterfaceEnabled);

        doReturn(Futures.immediateCheckedFuture(expectedChildEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceChildEntryInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedParentEntry)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL, interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);

        memberConfigUpdateHelper.updateConfiguration(dataBroker, alivenessMonitorService, vlanInterfaceEnabled.getAugmentation(ParentRefs.class), vlanInterfaceDisabled, vlanInterfaceEnabled.getAugmentation(IfL2vlan.class), vlanInterfaceEnabled, idManager, mdsalApiManager);
    }
}
