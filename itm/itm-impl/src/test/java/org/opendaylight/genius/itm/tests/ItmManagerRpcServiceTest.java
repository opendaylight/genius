/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests;

import com.google.common.base.Optional;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.MethodRule;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.tests.xtend.ExpectedExternalTunnelObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedDeviceVtepsObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedInternalTunnelIdentifierObjects;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs
        .rev160406.GetExternalTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs
        .rev160406.GetInternalOrExternalInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs
        .rev160406.GetInternalOrExternalInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;
import static com.google.common.truth.Truth.assertThat;

public class ItmManagerRpcServiceTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class);

    String trunkInterfaceName;

    ExternalTunnel externalTunnel;
    ExternalTunnel externalTunnel2;
    InternalTunnel internalTunnel;
    DpnEndpoints dpnEndpoints;
    DPNTEPsInfo dpntePsInfoVxlan;
    TunnelEndPoints tunnelEndPointsVxlan;
    Interface iface;
    Subnets subnetsTest;
    TransportZones transportZones;
    TransportZone transportZone;
    DeviceVteps deviceVteps;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TransportZone> transportZoneList = new ArrayList<>();
    List<Subnets> subnetsList = new ArrayList<>();
    List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    List<String> stringList = new ArrayList<>();
    List<BigInteger> dpId1List = new ArrayList<>();
    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(ItmTestConstants.ipAddress3, ItmTestConstants.sourceDevice);
    DeviceVtepsKey deviceVtep2Key = new DeviceVtepsKey(ItmTestConstants.ipAddress3, ItmTestConstants.sourceDevice2);
    AddExternalTunnelEndpointInput addExternalTunnelEndpointInput;
    GetInternalOrExternalInterfaceNameInput getExternalInterfaceNameInput;
    GetInternalOrExternalInterfaceNameInput getInternalInterfaceNameInput;
    GetInternalOrExternalInterfaceNameOutput getInternalOrExternalInterfaceNameOutput;
    BuildExternalTunnelFromDpnsInput buildExternalTunnelFromDpnsInput;
    RemoveExternalTunnelFromDpnsInput removeExternalTunnelFromDpnsInput;
    RemoveExternalTunnelEndpointInput removeExternalTunnelEndpointInput;
    GetExternalTunnelInterfaceNameInput getExternalTunnelInterfaceNameInput;
    AddL2GwDeviceInput addL2GwDeviceInput;
    DeleteL2GwDeviceInput deleteL2GwDeviceInput;
    AddL2GwMlagDeviceInput addL2GwMlagDeviceInput;
    DeleteL2GwMlagDeviceInput deleteL2GwMlagDeviceInput;
    GetTunnelInterfaceNameInput getTunnelInterfaceNameInput;

    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(String.valueOf(ItmTestConstants.ipAddress3)
                    , ItmTestConstants.dpId1.toString(), TunnelTypeMplsOverGre.class));
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifierNew = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(String.valueOf(ItmTestConstants.ipAddress3)
                    , ItmTestConstants.dpId1.toString(), ItmTestConstants.TUNNEL_TYPE_VXLAN));
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier2 = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(ItmTestConstants.destinationDevice
                    , ItmTestConstants.sourceDevice, ItmTestConstants.TUNNEL_TYPE_VXLAN));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifier = InstanceIdentifier.create(TunnelList.class)
            .child(InternalTunnel.class, new InternalTunnelKey(ItmTestConstants.dpId2, ItmTestConstants.dpId1
                    , ItmTestConstants.TUNNEL_TYPE_VXLAN));
    InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier = InstanceIdentifier.builder(DpnEndpoints.class).build();
    InstanceIdentifier<Interface> interfaceIdentifier;
    InstanceIdentifier<TransportZones> transportZonesIdentifier = InstanceIdentifier.create(TransportZones.class);
    InstanceIdentifier<DeviceVteps> deviceVtepsIdentifier = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(ItmTestConstants.TZ_NAME))
            .child(Subnets.class, new SubnetsKey(ItmTestConstants.ipPrefixTest))
            .child(DeviceVteps.class, deviceVtepKey).build();
    InstanceIdentifier<DeviceVteps> deviceVtepsIdentifier2 = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(ItmTestConstants.TZ_NAME))
            .child(Subnets.class, new SubnetsKey(ItmTestConstants.ipPrefixTest))
            .child(DeviceVteps.class, deviceVtep2Key).build();

    @Inject DataBroker dataBroker;
    @Inject IdManagerService idManagerService;
    @Inject ItmManagerRpcService itmManagerRpcService ;

    @Before
    public void setUp() throws Exception {
        deviceVteps = new DeviceVtepsBuilder().setIpAddress(ItmTestConstants.ipAddress3).setKey(new DeviceVtepsKey(
            ItmTestConstants.ipAddress3,ItmTestConstants.sourceDevice))
            .setNodeId(ItmTestConstants.sourceDevice).setTopologyId(ItmTestConstants.destinationDevice).build();
        deviceVtepsList.add(deviceVteps);
        stringList.add(ItmTestConstants.sourceDevice);
        dpId1List.add(ItmTestConstants.dpId1);
        stringList.add(ItmTestConstants.sourceDevice2);

        trunkInterfaceName = ItmUtils.getTrunkInterfaceName(idManagerService, ItmTestConstants.parentInterfaceName
                , ItmTestConstants.ipAddress3.getIpv4Address().getValue()
                , ItmTestConstants.ipAddress3.getIpv4Address().getValue()
                , ItmTestConstants.TUNNEL_TYPE_VXLAN.getName());
        interfaceIdentifier = ItmUtils.buildId(trunkInterfaceName);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder().setVLANID(ItmTestConstants.vlanId)
                .setPortname(ItmTestConstants.portName1).setIpAddress(ItmTestConstants.ipAddress3)
                .setGwIpAddress(ItmTestConstants.gtwyIp1).setInterfaceName(ItmTestConstants.parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(ItmTestConstants.TZ_NAME))
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN).setSubnetMask(ItmTestConstants.ipPrefixTest)
                .setKey(new TunnelEndPointsKey(ItmTestConstants.ipAddress3,ItmTestConstants.portName1
                        ,ItmTestConstants.TUNNEL_TYPE_VXLAN,ItmTestConstants.vlanId)).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(ItmTestConstants.dpId1)
                .setKey(new DPNTEPsInfoKey(ItmTestConstants.dpId1)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        dpnEndpoints = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();
        internalTunnel = new InternalTunnelBuilder().setTunnelInterfaceName(ItmTestConstants.parentInterfaceName)
                .setDestinationDPN(ItmTestConstants.dpId2).setSourceDPN(ItmTestConstants.dpId1)
                .setTransportType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setKey(new InternalTunnelKey(ItmTestConstants.dpId2,ItmTestConstants.dpId1
                        , ItmTestConstants.TUNNEL_TYPE_VXLAN)).build();
        getExternalInterfaceNameInput = new GetInternalOrExternalInterfaceNameInputBuilder()
                .setDestinationIp(ItmTestConstants.ipAddress3).setSourceDpid(ItmTestConstants.dpId1)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN).build();
        getInternalInterfaceNameInput = new GetInternalOrExternalInterfaceNameInputBuilder()
                .setDestinationIp(ItmTestConstants.ipAddress3).setSourceDpid(ItmTestConstants.dpId1)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN).build();
        addExternalTunnelEndpointInput = new AddExternalTunnelEndpointInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.ipAddress3).build();
        addL2GwDeviceInput = new AddL2GwDeviceInputBuilder().setIpAddress(ItmTestConstants.ipAddress3)
                .setNodeId(ItmTestConstants.sourceDevice)
                .setTopologyId(ItmTestConstants.destinationDevice).build();
        deleteL2GwDeviceInput = new DeleteL2GwDeviceInputBuilder().setIpAddress(ItmTestConstants.ipAddress3)
                .setNodeId(ItmTestConstants.sourceDevice)
                .setTopologyId(ItmTestConstants.destinationDevice).build();
        addL2GwMlagDeviceInput = new AddL2GwMlagDeviceInputBuilder().setIpAddress(ItmTestConstants.ipAddress3)
                .setNodeId(stringList)
                .setTopologyId(ItmTestConstants.destinationDevice).build();
        deleteL2GwMlagDeviceInput = new DeleteL2GwMlagDeviceInputBuilder().setIpAddress(ItmTestConstants.ipAddress3)
                .setNodeId(stringList).setTopologyId(ItmTestConstants.destinationDevice).build();
        buildExternalTunnelFromDpnsInput = new BuildExternalTunnelFromDpnsInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.ipAddress3).setDpnId(dpId1List).build();
        removeExternalTunnelFromDpnsInput = new RemoveExternalTunnelFromDpnsInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.ipAddress3).setDpnId(dpId1List).build();
        removeExternalTunnelEndpointInput = new RemoveExternalTunnelEndpointInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.ipAddress3).build();
        getTunnelInterfaceNameInput = new GetTunnelInterfaceNameInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setSourceDpid(ItmTestConstants.dpId1).setDestinationDpid(ItmTestConstants.dpId2).build();
        getExternalTunnelInterfaceNameInput = new GetExternalTunnelInterfaceNameInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationNode(ItmTestConstants.destinationDevice)
                .setSourceNode(ItmTestConstants.sourceDevice).build();
        iface = ItmUtils.buildTunnelInterface(ItmTestConstants.dpId1,trunkInterfaceName, String.format("%s %s"
                , ItmUtils.convertTunnelTypetoString(ItmTestConstants.TUNNEL_TYPE_VXLAN), "Trunk Interface")
                ,true,ItmTestConstants.TUNNEL_TYPE_VXLAN,tunnelEndPointsVxlan.getIpAddress()
                , ItmTestConstants.ipAddress3,ItmTestConstants.gtwyIp1
                , tunnelEndPointsVxlan.getVLANID(),false,false, ItmTestConstants.monitorProtocol,null, false);
        subnetsTest = new SubnetsBuilder().setGatewayIp(ItmTestConstants.gtwyIp1).setVlanId(ItmTestConstants.vlanId)
                .setKey(new SubnetsKey(ItmTestConstants.ipPrefixTest)).setDeviceVteps(deviceVtepsList).build();
        subnetsList.add(subnetsTest);
        transportZone = new TransportZoneBuilder().setZoneName(ItmTestConstants.TZ_NAME)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setKey(new TransportZoneKey(ItmTestConstants.TZ_NAME)).setSubnets(subnetsList).build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();

        // build external tunnel objects
        externalTunnel = new ExternalTunnelBuilder().setSourceDevice(ItmTestConstants.dpId1.toString())
                .setDestinationDevice(String.valueOf(ItmTestConstants.ipAddress3))
                .setTransportType(TunnelTypeMplsOverGre.class)
                .setTunnelInterfaceName(ItmTestConstants.parentInterfaceName)
                .setKey(new ExternalTunnelKey(String.valueOf(ItmTestConstants.ipAddress3)
                        ,ItmTestConstants.dpId1.toString(),TunnelTypeMplsOverGre.class)).build();

        externalTunnel2 = new ExternalTunnelBuilder().setSourceDevice(ItmTestConstants.sourceDevice)
                .setDestinationDevice(ItmTestConstants.destinationDevice)
                .setTransportType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setTunnelInterfaceName(ItmTestConstants.parentInterfaceName)
                .setKey(new ExternalTunnelKey(ItmTestConstants.destinationDevice
                        , ItmTestConstants.sourceDevice, ItmTestConstants.TUNNEL_TYPE_VXLAN)).build();

        getInternalOrExternalInterfaceNameOutput = new GetInternalOrExternalInterfaceNameOutputBuilder().
            setInterfaceName(ItmTestConstants.parentInterfaceName).build();

        // commit external tunnel into config DS
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier,
            externalTunnel, dataBroker);
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier2,
            externalTunnel2, dataBroker);

        // commit internal tunnel into config DS
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifier,
            internalTunnel, dataBroker);

        // commit dpnEndpoints into config DS
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, dpnEndpointsIdentifier,
            dpnEndpoints, dataBroker);

        // commit TZ into config DS
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, transportZonesIdentifier,
            transportZones, dataBroker);
    }

    @Test
    public void testGetExternalInterfaceNameExtTunnelPresent() throws Exception {
        Future<RpcResult<GetInternalOrExternalInterfaceNameOutput>> rpcRes  = itmManagerRpcService.
            getInternalOrExternalInterfaceName(getExternalInterfaceNameInput);
        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        assertThat(getInternalOrExternalInterfaceNameOutput).isEqualTo(rpcRes.get().getResult());
    }

    @Test
    public void testGetInternalInterfaceNameIntTunnelPresent() throws Exception {
        Future<RpcResult<GetInternalOrExternalInterfaceNameOutput>> rpcRes  = itmManagerRpcService.
            getInternalOrExternalInterfaceName(getInternalInterfaceNameInput);
        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        assertThat(getInternalOrExternalInterfaceNameOutput).isEqualTo(rpcRes.get().getResult());
    }

    @Test
    public void testAddExternalTunnelEndpoint() throws Exception {
        Future<RpcResult<Void>> rpcRes = itmManagerRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnelEndpoint is added in config DS
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnelForRpcTest()
                ,  dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifierNew).checkedGet().get());
    }

    @Test
    public void testAddL2GwDevice() throws Exception {
        Future<RpcResult<java.lang.Void>> rpcRes = itmManagerRpcService.addL2GwDevice(addL2GwDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwDevice is added in config DS
        assertEqualBeans(ExpectedDeviceVtepsObjects.newDeviceVtepsObject(),  dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).checkedGet().get());
    }

    @Test
    public void testAddL2GwMlagDevice() throws Exception {
        Future<RpcResult<java.lang.Void>> rpcRes = itmManagerRpcService.addL2GwMlagDevice(addL2GwMlagDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwMlagDevice is added in config DS
        assertEqualBeans(ExpectedDeviceVtepsObjects.newDeviceVtepsObject(),  dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).checkedGet().get());
        assertEqualBeans(ExpectedDeviceVtepsObjects.newDeviceVtepsObject2(),  dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier2).checkedGet().get());
    }

    @Test
    public void testDeleteL2GwDevice() throws Exception {
        Future<RpcResult<java.lang.Void>> rpcRes = itmManagerRpcService.deleteL2GwDevice(deleteL2GwDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwDevice is deleted from config DS
        assertThat(Optional.absent()).isEqualTo(dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).get());
    }

    @Test
    public void testDeleteL2GwMlagDevice() throws Exception {
        Future<RpcResult<java.lang.Void>> rpcRes =
                itmManagerRpcService.deleteL2GwMlagDevice(deleteL2GwMlagDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwMlagDevice is deleted from config DS
        assertThat(Optional.absent()).isEqualTo(dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).get());
        assertThat(Optional.absent()).isEqualTo(dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier2).get());
    }

    @Test
    public void testBuildExternalTunnelFromDpns() throws Exception {
        Future<RpcResult<Void>> rpcRes =
                itmManagerRpcService.buildExternalTunnelFromDpns(buildExternalTunnelFromDpnsInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnel From Dpns is added in config DS
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnelForRpcTest(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifierNew).checkedGet().get());
    }

    @Test
    public void testRemoveExternalTunnelFromDpns() throws Exception {
        Future<RpcResult<Void>> rpcRes =
                itmManagerRpcService.removeExternalTunnelFromDpns(removeExternalTunnelFromDpnsInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnel From Dpns is deleted from config DS
        assertThat(Optional.absent()).isEqualTo(dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifierNew).get());
        // check iface is removed
        assertThat(Optional.absent()).isEqualTo(dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier).get());
    }

    @Test
    public void testRemoveExternalTunnelEndpoint() throws Exception {
        // call RPC to add ExternalTunnelEndpoint as pre-requisite
        Future<RpcResult<Void>> rpcRes =
                itmManagerRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        rpcRes = itmManagerRpcService.removeExternalTunnelEndpoint(removeExternalTunnelEndpointInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnelEndpoint is deleted from config DS
        assertThat(Optional.absent()).isEqualTo(dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifierNew).get());
        // check iface is removed
        assertThat(Optional.absent()).isEqualTo(dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier).get());
    }

    @Test
    public void testGetTunnelInterfaceName() throws Exception {
        Future<RpcResult<GetTunnelInterfaceNameOutput>> rpcRes = itmManagerRpcService.
            getTunnelInterfaceName(getTunnelInterfaceNameInput);
        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnel From Dpns is added in config DS
        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelObjVxLanOneToTwo(),
            dataBroker.newReadOnlyTransaction().
            read(LogicalDatastoreType.CONFIGURATION,internalTunnelIdentifier).checkedGet().get());
    }

    @Test
    public void testGetExternalTunnelInterfaceName() throws Exception {
        Future<RpcResult<GetExternalTunnelInterfaceNameOutput>> rpcRes = itmManagerRpcService.
            getExternalTunnelInterfaceName(getExternalTunnelInterfaceNameInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnel From Dpns is added in config DS
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel2ForRpcTest(),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.
                CONFIGURATION,externalTunnelIdentifier2).checkedGet().get());

        // check for interfaceName
        assertThat(ItmTestConstants.parentInterfaceName).isEqualTo(rpcRes.get().getResult().getInterfaceName());
    }
}
