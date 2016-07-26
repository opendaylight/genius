/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedIngressServicesStateBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedIngressServicesStateUnbindHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class FlowBasedServicesStateConfigurationTest {

    Interface interfaceEnabled = null;
    long portNum = 2;
    int instructionKeyval = 2;
    InstanceIdentifier<Interface> interfaceInstanceIdentifier = null;
    InstanceIdentifier<Flow> flowInstanceId = null;
    Flow ingressFlow = null;
    BigInteger dpId = BigInteger.valueOf(1);
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface stateInterface;
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceStateIdentifier = null;
    ServicesInfo servicesInfo = null;
    NodeConnectorId nodeConnectorId = null;
    BoundServices boundService = null;
    StypeOpenflow stypeOpenflow = null;
    Instruction instruction = null;
    InstructionKey instructionKey = null;
    List<Instruction>instructions = new ArrayList<>();
    short key =0;
    int ifIndexval = 100;
    int flowpriority = 2;
    String serviceName = "VPN";

    DataBroker dataBroker;
    ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    InterfacemgrProvider interfacemgrProvider;
    ReadOnlyTransaction mockReadTx;
    WriteTransaction mockWriteTx;

    FlowBasedServicesStateAddable flowBasedServicesStateAddable;
    FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

    @Before
    public void setUp() throws Exception {
        interfacemgrProvider = mock(InterfacemgrProvider.class);
        dataBroker = mock(DataBroker.class);
        mockReadTx = mock(ReadOnlyTransaction.class);
        mockWriteTx = mock(WriteTransaction.class);
        dataChangeListenerRegistration = mock(DataChangeListenerRegistration.class);

        when(dataBroker.registerDataChangeListener(
                any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(DataChangeListener.class),
                any(DataChangeScope.class)))
                .thenReturn(dataChangeListenerRegistration);
        when(interfacemgrProvider.getDataBroker()).thenReturn(dataBroker);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(mockReadTx);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTx);
        FlowBasedIngressServicesStateBindHelper.intitializeFlowBasedIngressServicesStateAddHelper(interfacemgrProvider);
        FlowBasedIngressServicesStateUnbindHelper.intitializeFlowBasedIngressServicesStateRemoveHelper(interfacemgrProvider);
        flowBasedServicesStateAddable = FlowBasedIngressServicesStateBindHelper.getFlowBasedIngressServicesStateAddHelper();
        flowBasedServicesStateRemovable = FlowBasedIngressServicesStateUnbindHelper.getFlowBasedIngressServicesStateRemoveHelper();
        setupMocks();
    }

    @After
    public void tearDown() throws Exception {
        FlowBasedIngressServicesStateBindHelper.clearFlowBasedIngressServicesStateAddHelper();
        FlowBasedIngressServicesStateUnbindHelper.clearFlowBasedIngressServicesStateUnbindHelper();
    }
    private void setupMocks(){


        interfaceEnabled = InterfaceManagerTestUtil.buildInterface(InterfaceManagerTestUtil.interfaceName, "Test Vlan Interface1", true, L2vlan.class, dpId);
        interfaceInstanceIdentifier = IfmUtil.buildId(InterfaceManagerTestUtil.interfaceName);
        nodeConnectorId = InterfaceManagerTestUtil.buildNodeConnectorId(BigInteger.valueOf(1), portNum);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
        List<String> lowerLayerIfList = new ArrayList<>();
        lowerLayerIfList.add(nodeConnectorId.getValue());
        ifaceBuilder.setOperStatus(OperStatus.Up).setAdminStatus(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up)
                .setPhysAddress(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress.getDefaultInstance("AA:AA:AA:AA:AA:AA"))
                .setIfIndex(ifIndexval)
                .setLowerLayerIf(lowerLayerIfList)
                .setKey(IfmUtil.getStateInterfaceKeyFromName(InterfaceManagerTestUtil.interfaceName))
                .setName(InterfaceManagerTestUtil.interfaceName).setType(interfaceEnabled.getType());
        stypeOpenflow = InterfaceManagerTestUtil.buildStypeOpenflow(dpId,flowpriority, NwConstants.LPORT_DISPATCHER_TABLE, instructions);
        instructionKey = new InstructionKey(instructionKeyval);
        BigInteger[] metadataValues = IfmUtil.mergeOpenflowMetadataWriteInstructions(instructions);
        boundService = InterfaceManagerTestUtil.buildBoundServices(serviceName,key,new BoundServicesKey(key),stypeOpenflow);
        short sIndex = boundService.getServicePriority();
        BigInteger metadata = MetaDataUtil.getMetaDataForLPortDispatcher(ifaceBuilder.getIfIndex(),
                ++sIndex, metadataValues[0]);
        BigInteger mask = MetaDataUtil.getWriteMetaDataMaskForDispatcherTable();
        instruction = InterfaceManagerTestUtil.buildInstruction(InterfaceManagerTestUtil.buildWriteMetaDataCase(InterfaceManagerTestUtil.buildWriteMetaData(metadata, mask)),
                new InstructionKey(instructionKey));
        instructions.add(instruction);
        stateInterface = ifaceBuilder.build();
        ServicesInfoKey servicesInfoKey = new ServicesInfoKey(InterfaceManagerTestUtil.interfaceName, ServiceModeIngress.class);
        List<BoundServices> lowerlayerIfList = new ArrayList<>();
        lowerlayerIfList.add(boundService);
        interfaceStateIdentifier = IfmUtil.buildStateInterfaceId(interfaceEnabled.getName());
        servicesInfo = InterfaceManagerTestUtil.buildServicesInfo(InterfaceManagerTestUtil.interfaceName,servicesInfoKey,lowerlayerIfList);
        String flowRef = InterfaceManagerTestUtil.buildFlowRef(dpId, NwConstants.LPORT_DISPATCHER_TABLE, InterfaceManagerTestUtil.interfaceName, boundService.getServicePriority());
        List<Instruction> instructionList = boundService.getAugmentation(StypeOpenflow.class).getInstruction();
        String serviceRef = boundService.getServiceName();
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInfo(MatchFieldType.metadata, new BigInteger[] {
                MetaDataUtil.getMetaDataForLPortDispatcher(ifaceBuilder.getIfIndex(), boundService.getServicePriority()),
                MetaDataUtil.getMetaDataMaskForLPortDispatcher() }));
        ingressFlow = MDSALUtil.buildFlowNew(NwConstants.LPORT_DISPATCHER_TABLE, flowRef, boundService.getServicePriority(), serviceRef, 0, 0,
                stypeOpenflow.getFlowCookie(), matches, instructionList);
        FlowKey flowKey = new FlowKey(new FlowId(ingressFlow.getId()));
        flowInstanceId = InterfaceManagerTestUtil.getFlowInstanceIdentifier(dpId,ingressFlow.getTableId(),flowKey);

        when(dataBroker.newReadOnlyTransaction()).thenReturn(mockReadTx);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTx);
    }

    @Test
    public void testStateBindSingleService(){

        Optional<ServicesInfo>expectedservicesInfo = Optional.of(servicesInfo);
        Optional<Interface> expectedInterface = Optional.of(interfaceEnabled);
        Optional<NodeConnectorId>expectednodeconnectorId = Optional.of(nodeConnectorId);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateInterface = Optional.of(stateInterface);

        ServicesInfoKey servicesInfoKey = new ServicesInfoKey(InterfaceManagerTestUtil.interfaceName, ServiceModeIngress.class);
        InstanceIdentifier.InstanceIdentifierBuilder<ServicesInfo> servicesInfoIdentifierBuilder =
                InstanceIdentifier.builder(ServiceBindings.class).child(ServicesInfo.class, servicesInfoKey);

        doReturn(Futures.immediateCheckedFuture(expectedservicesInfo)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION,servicesInfoIdentifierBuilder.build());
        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectednodeconnectorId)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL,interfaceStateIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL,interfaceStateIdentifier);

        flowBasedServicesStateAddable.bindServicesOnInterface(stateInterface);

        verify(mockWriteTx).put(LogicalDatastoreType.CONFIGURATION,flowInstanceId,ingressFlow, true);

    }

    @Test
    public void testStateUnbindSingleService(){

        Optional<ServicesInfo>expectedservicesInfo = Optional.of(servicesInfo);
        Optional<Interface> expectedInterface = Optional.of(interfaceEnabled);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> expectedStateInterface = Optional.of(stateInterface);

        ServicesInfoKey servicesInfoKey = new ServicesInfoKey(InterfaceManagerTestUtil.interfaceName, ServiceModeIngress.class);
        InstanceIdentifier.InstanceIdentifierBuilder<ServicesInfo> servicesInfoIdentifierBuilder =
                InstanceIdentifier.builder(ServiceBindings.class).child(ServicesInfo.class, servicesInfoKey);

        doReturn(Futures.immediateCheckedFuture(expectedservicesInfo)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION,servicesInfoIdentifierBuilder.build());
        doReturn(Futures.immediateCheckedFuture(expectedInterface)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(expectedStateInterface)).when(mockReadTx).read(
                LogicalDatastoreType.OPERATIONAL,interfaceStateIdentifier);

        flowBasedServicesStateRemovable.unbindServicesFromInterface(stateInterface);

        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,flowInstanceId);
    }
}
