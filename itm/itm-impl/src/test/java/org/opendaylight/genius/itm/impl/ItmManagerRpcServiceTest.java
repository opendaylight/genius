/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInputBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ItmManagerRpcServiceTest {

    int vlanId = 100 ;
    String portName1 = "phy0";
    String sourceDevice = "abc";
    String destinationDevice = "xyz";
    String tepIp1 = "192.168.56.101";
    String gwyIp1 = "0.0.0.0";
    String subnetIp = "10.1.1.24";
    String tunnelInterfaceName =  "1:phy0:100" ;
    String transportZone1 = "TZA" ;
    String trunkInterfaceName = null;
    IpAddress ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
    IpAddress gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
    IpPrefix ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
    BigInteger dpId1 = BigInteger.valueOf(1);
    BigInteger dpId2 = BigInteger.valueOf(2);
    ExternalTunnel externalTunnel = null;
    ExternalTunnel externalTunnelNew = null;
    InternalTunnel internalTunnel = null;
    DpnEndpoints dpnEndpoints = null;
    DPNTEPsInfo dpntePsInfoVxlan = null;
    TunnelEndPoints tunnelEndPointsVxlan = null;
    AllocateIdInput getIdInput1 = null;
    Interface iface = null;
    Subnets subnetsTest = null;
    TransportZones transportZones = null;
    TransportZone transportZone = null;
    DeviceVteps deviceVteps = null;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>() ;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TransportZone> transportZoneList = new ArrayList<>() ;
    List<Subnets> subnetsList = new ArrayList<>() ;
    List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    List<String> stringList = new ArrayList<>();
    List<BigInteger> dpId1List = new ArrayList<>();
    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(ipAddress1, sourceDevice);
    AddExternalTunnelEndpointInput addExternalTunnelEndpointInput = null;
    GetInternalOrExternalInterfaceNameInput getInternalOrExternalInterfaceNameInput = null;
    BuildExternalTunnelFromDpnsInput buildExternalTunnelFromDpnsInput = null;
    RemoveExternalTunnelFromDpnsInput removeExternalTunnelFromDpnsInput = null;
    RemoveExternalTunnelEndpointInput removeExternalTunnelEndpointInput = null;
    RemoveTerminatingServiceActionsInput removeTerminatingServiceActionsInput = null;
    GetExternalTunnelInterfaceNameInput getExternalTunnelInterfaceNameInput = null;
    AddL2GwDeviceInput addL2GwDeviceInput = null;
    DeleteL2GwDeviceInput deleteL2GwDeviceInput = null;
    AddL2GwMlagDeviceInput addL2GwMlagDeviceInput = null;
    DeleteL2GwMlagDeviceInput deleteL2GwMlagDeviceInput = null;
    GetTunnelInterfaceNameInput getTunnelInterfaceNameInput = null;
    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;
    java.lang.Class<? extends TunnelTypeBase> tunnelType2 = TunnelTypeMplsOverGre.class;
    AllocateIdOutput expectedId1 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("100")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional1 ;
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;

    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(String.valueOf(ipAddress1), dpId1.toString(),
                    TunnelTypeMplsOverGre.class));
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifierNew = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(String.valueOf(ipAddress1), dpId1.toString(),
                    tunnelType1));
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier1 = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(destinationDevice, sourceDevice,
                    tunnelType1));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifier = InstanceIdentifier.create(TunnelList.class)
            .child(InternalTunnel.class, new InternalTunnelKey(dpId2, dpId1, tunnelType1));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifierNew = InstanceIdentifier.create(TunnelList.class)
            .child(InternalTunnel.class, new InternalTunnelKey(dpId1, dpId1, tunnelType1));
    InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier = InstanceIdentifier.builder(DpnEndpoints.class).build();
    InstanceIdentifier<Interface> interfaceIdentifier = null;
    InstanceIdentifier<TransportZones> transportZonesIdentifier = InstanceIdentifier.create(TransportZones.class);
    InstanceIdentifier<DeviceVteps> deviceVtepsIdentifier = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(transportZone1))
            .child(Subnets.class, new SubnetsKey(ipPrefixTest)).child(DeviceVteps.class, deviceVtepKey).build();


    @Mock DataBroker dataBroker;
    @Mock ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock IdManagerService idManagerService;
    @Mock IMdsalApiManager mdsalApiManager;
    @Mock ItmConfig itmConfig;

    ItmManagerRpcService itmManagerRpcService ;

    Optional<ExternalTunnel> externalTunnelOptional ;
    Optional<InternalTunnel> internalTunnelOptional;
    Optional<DpnEndpoints> dpnEndpointsOptional ;
    Optional<TransportZones> transportZonesOptional ;

    @Before
    public void setUp() {
        when(dataBroker.registerDataChangeListener(
                any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(DataChangeListener.class),
                any(AsyncDataBroker.DataChangeScope.class)))
                .thenReturn(dataChangeListenerRegistration);
        setupMocks();

        externalTunnelOptional = Optional.of(externalTunnel);
        internalTunnelOptional = Optional.of(internalTunnel);
        dpnEndpointsOptional = Optional.of(dpnEndpoints);
        transportZonesOptional = Optional.of(transportZones);

        doReturn(Futures.immediateCheckedFuture(externalTunnelOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,externalTunnelIdentifier);
        doReturn(Futures.immediateCheckedFuture(externalTunnelOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,externalTunnelIdentifier1);
        doReturn(Futures.immediateCheckedFuture(internalTunnelOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,internalTunnelIdentifier);
        doReturn(Futures.immediateCheckedFuture(internalTunnelOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,internalTunnelIdentifierNew);
        doReturn(Futures.immediateCheckedFuture(dpnEndpointsOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,dpnEndpointsIdentifier);
        doReturn(Futures.immediateCheckedFuture(transportZonesOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        itmManagerRpcService = new ItmManagerRpcService(dataBroker,idManagerService, mdsalApiManager, itmConfig);
    }

    @After
    public void cleanUp() {
    }

    private void setupMocks() {

        deviceVteps = new DeviceVtepsBuilder().setIpAddress(ipAddress1).setKey(new DeviceVtepsKey(ipAddress1,"abc"))
                .setNodeId(sourceDevice).setTopologyId(destinationDevice).build();
        deviceVtepsList.add(deviceVteps);
        stringList.add(sourceDevice);
        dpId1List.add(dpId1);
        stringList.add("def");
        idOutputOptional1 = RpcResultBuilder.success(expectedId1).buildFuture();
        getIdInput1 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.101:VXLAN").build();
        doReturn(idOutputOptional1).when(idManagerService).allocateId(getIdInput1);
        when(idManagerService.releaseId(any(ReleaseIdInput.class))).thenReturn(Futures.immediateFuture(RpcResultBuilder
                .<Void>success().build()));
        trunkInterfaceName = ItmUtils.getTrunkInterfaceName(idManagerService, tunnelInterfaceName, ipAddress1
                .getIpv4Address().getValue(), ipAddress1.getIpv4Address().getValue(), tunnelType1.getName());
        interfaceIdentifier = ItmUtils.buildId(trunkInterfaceName);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName1)
                .setIpAddress(ipAddress1).setGwIpAddress(gtwyIp1).setInterfaceName(tunnelInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType1)
                .setSubnetMask(ipPrefixTest)
                .setKey(new TunnelEndPointsKey(ipAddress1,portName1,tunnelType1,vlanId)).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId1).setKey(new DPNTEPsInfoKey(dpId1)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        dpnEndpoints = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();
        externalTunnel = new ExternalTunnelBuilder().setSourceDevice(sourceDevice)
                .setDestinationDevice(destinationDevice).setTransportType(tunnelType1)
                .setTunnelInterfaceName(tunnelInterfaceName)
                .setKey(new ExternalTunnelKey(destinationDevice,sourceDevice,tunnelType1)).build();
        internalTunnel = new InternalTunnelBuilder()
                .setTunnelInterfaceNames(Collections.singletonList(tunnelInterfaceName)).setDestinationDPN(dpId2)
                .setSourceDPN(dpId1).setTransportType(tunnelType1)
                .setKey(new InternalTunnelKey(dpId2, dpId1, tunnelType1)).build();
        getInternalOrExternalInterfaceNameInput = new GetInternalOrExternalInterfaceNameInputBuilder()
                .setDestinationIp(ipAddress1).setSourceDpid(dpId1).setTunnelType(tunnelType2).build();
        addExternalTunnelEndpointInput = new AddExternalTunnelEndpointInputBuilder().setTunnelType(tunnelType1)
                .setDestinationIp(ipAddress1).build();
        addL2GwDeviceInput = new AddL2GwDeviceInputBuilder().setIpAddress(ipAddress1).setNodeId(sourceDevice)
                .setTopologyId(destinationDevice).build();
        deleteL2GwDeviceInput = new DeleteL2GwDeviceInputBuilder().setIpAddress(ipAddress1).setNodeId(sourceDevice)
                .setTopologyId(destinationDevice).build();
        addL2GwMlagDeviceInput = new AddL2GwMlagDeviceInputBuilder().setIpAddress(ipAddress1).setNodeId(stringList)
                .setTopologyId(destinationDevice).build();
        deleteL2GwMlagDeviceInput = new DeleteL2GwMlagDeviceInputBuilder().setIpAddress(ipAddress1)
                .setNodeId(stringList).setTopologyId(destinationDevice).build();
        buildExternalTunnelFromDpnsInput = new BuildExternalTunnelFromDpnsInputBuilder().setTunnelType(tunnelType1)
                .setDestinationIp(ipAddress1).setDpnId(dpId1List).build();
        removeExternalTunnelFromDpnsInput = new RemoveExternalTunnelFromDpnsInputBuilder().setTunnelType(tunnelType1)
                .setDestinationIp(ipAddress1).setDpnId(dpId1List).build();
        removeExternalTunnelEndpointInput = new RemoveExternalTunnelEndpointInputBuilder().setTunnelType(tunnelType1)
                .setDestinationIp(ipAddress1).build();
        removeTerminatingServiceActionsInput = new RemoveTerminatingServiceActionsInputBuilder().setServiceId(vlanId)
                .setDpnId(dpId1).build();
        getTunnelInterfaceNameInput = new GetTunnelInterfaceNameInputBuilder().setTunnelType(tunnelType1)
                .setSourceDpid(dpId1).setDestinationDpid(dpId2).build();
        getExternalTunnelInterfaceNameInput = new GetExternalTunnelInterfaceNameInputBuilder()
                .setTunnelType(tunnelType1).setDestinationNode(destinationDevice).setSourceNode(sourceDevice).build();
        iface = ItmUtils.buildTunnelInterface(dpId1,trunkInterfaceName, String.format("%s %s",
                ItmUtils.convertTunnelTypetoString(tunnelType1), "Trunk Interface"),true,tunnelType1,
                tunnelEndPointsVxlan.getIpAddress(),ipAddress1,gtwyIp1,tunnelEndPointsVxlan.getVLANID(), false, false,
                monitorProtocol,null, false, null);
        subnetsTest = new SubnetsBuilder().setGatewayIp(gtwyIp1).setVlanId(vlanId).setKey(new SubnetsKey(ipPrefixTest))
                .setDeviceVteps(deviceVtepsList).build();
        subnetsList.add(subnetsTest);
        transportZone = new TransportZoneBuilder().setZoneName(transportZone1)
                .setTunnelType(tunnelType1).setKey(new TransportZoneKey(transportZone1))
                .setSubnets(subnetsList).build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockWriteTx).when(dataBroker).newWriteOnlyTransaction();
        doReturn(Futures.immediateCheckedFuture(null)).when(mockWriteTx).submit();
    }

    @Test
    public void testGetInternalOrExternalInterfaceNameExtTunnelPresent() {

        itmManagerRpcService.getInternalOrExternalInterfaceName(getInternalOrExternalInterfaceNameInput);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier);
    }

    @Ignore
    @Test
    public void testGetInternalOrExternalInterfaceNameExtTunnelAbsent() {

        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,externalTunnelIdentifier);

        itmManagerRpcService.getInternalOrExternalInterfaceName(getInternalOrExternalInterfaceNameInput);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier);
        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,internalTunnelIdentifierNew);

    }

    @Ignore
    @Test
    public void testAddExternalTunnelEndpoint() {

        externalTunnelNew = ItmUtils.buildExternalTunnel(dpId1.toString(), ipAddress1.toString(),
                tunnelType1, trunkInterfaceName);

        itmManagerRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput);

        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier, iface, true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifierNew,
                externalTunnelNew,true);
    }

    @Test
    public void testAddL2GwDevice() {

        itmManagerRpcService.addL2GwDevice(addL2GwDeviceInput);

        verify(mockWriteTx).put(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier, deviceVteps, true);
    }

    @Test
    public void testAddL2GwMlagDevice() {

        itmManagerRpcService.addL2GwMlagDevice(addL2GwMlagDeviceInput);

        verify(mockWriteTx).put(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier, deviceVteps, true);
    }

    @Test
    public void testDeleteL2GwDevice() {

        itmManagerRpcService.deleteL2GwDevice(deleteL2GwDeviceInput);

        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,deviceVtepsIdentifier);
    }

    @Test
    public void testDeleteL2GwMlagDevice() {

        itmManagerRpcService.deleteL2GwMlagDevice(deleteL2GwMlagDeviceInput);

        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,deviceVtepsIdentifier);
    }

    @Ignore
    @Test
    public void testBuildExternalTunnelFromDpns() {

        externalTunnelNew = ItmUtils.buildExternalTunnel(dpId1.toString(), ipAddress1.toString(),
                tunnelType1, trunkInterfaceName);

        itmManagerRpcService.buildExternalTunnelFromDpns(buildExternalTunnelFromDpnsInput);

        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,interfaceIdentifier,iface,true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifierNew,
                externalTunnelNew,true);
    }

    @Ignore
    @Test
    public void testRemoveExternalTunnelFromDpns() {

        itmManagerRpcService.removeExternalTunnelFromDpns(removeExternalTunnelFromDpnsInput);

        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,interfaceIdentifier);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifierNew);
    }

    @Ignore
    @Test
    public void testRemoveExternalTunnelEndpoint() {

        itmManagerRpcService.removeExternalTunnelEndpoint(removeExternalTunnelEndpointInput);

        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,interfaceIdentifier);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifierNew);
    }

    @Test
    public void testGetTunnelInterfaceName() {

        itmManagerRpcService.getTunnelInterfaceName(getTunnelInterfaceNameInput);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,internalTunnelIdentifier);
    }

    @Test
    public void testGetExternalTunnelInterfaceName() {

        itmManagerRpcService.getExternalTunnelInterfaceName(getExternalTunnelInterfaceNameInput);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier1);
    }
}
