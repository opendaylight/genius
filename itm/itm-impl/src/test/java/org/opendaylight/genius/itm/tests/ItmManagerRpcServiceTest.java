/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.genius.itm.tests.xtend.ExpectedDeviceVtepsObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedExternalTunnelObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedInternalTunnelIdentifierObjects;
import org.opendaylight.infrautils.caches.testutils.CacheModule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsOutput;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint64;

public class ItmManagerRpcServiceTest {

    public @Rule LogRule logRule = new LogRule();
    // TODO public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class,  TestableDataTreeChangeListenerModule.class,
            JobCoordinatorTestModule.class, CacheModule.class);

    String trunkInterfaceName;

    ExternalTunnel externalTunnel;
    ExternalTunnel externalTunnel2;
    InternalTunnel internalTunnel;
    DpnEndpoints dpnEndpoints;
    DPNTEPsInfo dpntePsInfoVxlan;
    TunnelEndPoints tunnelEndPointsVxlan;
    Interface iface;
    TransportZones transportZones;
    TransportZone transportZone;
    DeviceVteps deviceVteps;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TransportZone> transportZoneList = new ArrayList<>();
    List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    List<String> stringList = new ArrayList<>();
    List<Uint64> dpId1List = new ArrayList<>();
    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(ItmTestConstants.IP_ADDRESS_3, ItmTestConstants.SOURCE_DEVICE);
    DeviceVtepsKey deviceVtep2Key = new DeviceVtepsKey(ItmTestConstants.IP_ADDRESS_3, ItmTestConstants.SOURCE_DEVICE_2);
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
            .child(ExternalTunnel.class, new ExternalTunnelKey(ItmTestConstants.IP_ADDRESS_3.stringValue(),
                    ItmTestConstants.DP_ID_1.toString(), TunnelTypeMplsOverGre.class));
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifierNew = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(ItmTestConstants.IP_ADDRESS_3.stringValue(),
                    ItmTestConstants.DP_ID_1.toString(), ItmTestConstants.TUNNEL_TYPE_VXLAN));
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier2 = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(ItmTestConstants.DESTINATION_DEVICE,
                    ItmTestConstants.SOURCE_DEVICE, ItmTestConstants.TUNNEL_TYPE_VXLAN));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifier = InstanceIdentifier.create(TunnelList.class)
            .child(InternalTunnel.class, new InternalTunnelKey(ItmTestConstants.DP_ID_2, ItmTestConstants.DP_ID_1,
                    ItmTestConstants.TUNNEL_TYPE_VXLAN));
    InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier = InstanceIdentifier.builder(DpnEndpoints.class).build();
    InstanceIdentifier<Interface> interfaceIdentifier;
    InstanceIdentifier<TransportZones> transportZonesIdentifier = InstanceIdentifier.create(TransportZones.class);
    InstanceIdentifier<DeviceVteps> deviceVtepsIdentifier = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(ItmTestConstants.TZ_NAME))
            .child(DeviceVteps.class, deviceVtepKey).build();
    InstanceIdentifier<DeviceVteps> deviceVtepsIdentifier2 = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(ItmTestConstants.TZ_NAME))
            .child(DeviceVteps.class, deviceVtep2Key).build();

    @Inject DataBroker dataBroker;
    private @Inject JobCoordinatorEventsWaiter coordinatorEventsWaiter;
    @Inject ItmManagerRpcService itmManagerRpcService ;
    ManagedNewTransactionRunner txRunner;

    @Before
    public void setUp() throws Exception {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        deviceVteps = new DeviceVtepsBuilder().setIpAddress(ItmTestConstants.IP_ADDRESS_3).withKey(new DeviceVtepsKey(
            ItmTestConstants.IP_ADDRESS_3,ItmTestConstants.SOURCE_DEVICE))
            .setNodeId(ItmTestConstants.SOURCE_DEVICE).setTopologyId(ItmTestConstants.DESTINATION_DEVICE).build();
        deviceVtepsList.add(deviceVteps);
        stringList.add(ItmTestConstants.SOURCE_DEVICE);
        dpId1List.add(ItmTestConstants.DP_ID_1);
        stringList.add(ItmTestConstants.SOURCE_DEVICE_2);

        trunkInterfaceName = ItmUtils.getTrunkInterfaceName(ItmTestConstants.PARENT_INTERFACE_NAME,
            ItmTestConstants.IP_ADDRESS_3.stringValue(),
            ItmTestConstants.IP_ADDRESS_3.stringValue(),
                ItmTestConstants.TUNNEL_TYPE_VXLAN.getName());
        interfaceIdentifier = ItmUtils.buildId(trunkInterfaceName);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder().setIpAddress(ItmTestConstants.IP_ADDRESS_3)
                .setInterfaceName(ItmTestConstants.PARENT_INTERFACE_NAME)
                .setTzMembership(ItmUtils.createTransportZoneMembership(ItmTestConstants.TZ_NAME))
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .withKey(new TunnelEndPointsKey(ItmTestConstants.IP_ADDRESS_3,
                        ItmTestConstants.TUNNEL_TYPE_VXLAN)).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(ItmTestConstants.DP_ID_1)
                .withKey(new DPNTEPsInfoKey(ItmTestConstants.DP_ID_1)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        dpnEndpoints = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();
        internalTunnel = new InternalTunnelBuilder()
                .setTunnelInterfaceNames(Collections.singletonList(ItmTestConstants.PARENT_INTERFACE_NAME))
                .setDestinationDPN(ItmTestConstants.DP_ID_2).setSourceDPN(ItmTestConstants.DP_ID_1)
                .setTransportType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .withKey(new InternalTunnelKey(ItmTestConstants.DP_ID_2, ItmTestConstants.DP_ID_1,
                        ItmTestConstants.TUNNEL_TYPE_VXLAN))
                .build();
        getExternalInterfaceNameInput = new GetInternalOrExternalInterfaceNameInputBuilder()
                .setDestinationIp(ItmTestConstants.IP_ADDRESS_3).setSourceDpid(ItmTestConstants.DP_ID_1)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_MPLS_OVER_GRE).build();
        getInternalInterfaceNameInput = new GetInternalOrExternalInterfaceNameInputBuilder()
                .setDestinationIp(ItmTestConstants.IP_ADDRESS_3).setSourceDpid(ItmTestConstants.DP_ID_1)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_MPLS_OVER_GRE).build();
        addExternalTunnelEndpointInput = new AddExternalTunnelEndpointInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.IP_ADDRESS_3).build();
        addL2GwDeviceInput = new AddL2GwDeviceInputBuilder().setIpAddress(ItmTestConstants.IP_ADDRESS_3)
                .setNodeId(ItmTestConstants.SOURCE_DEVICE)
                .setTopologyId(ItmTestConstants.DESTINATION_DEVICE).build();
        deleteL2GwDeviceInput = new DeleteL2GwDeviceInputBuilder().setIpAddress(ItmTestConstants.IP_ADDRESS_3)
                .setNodeId(ItmTestConstants.SOURCE_DEVICE)
                .setTopologyId(ItmTestConstants.DESTINATION_DEVICE).build();
        addL2GwMlagDeviceInput = new AddL2GwMlagDeviceInputBuilder().setIpAddress(ItmTestConstants.IP_ADDRESS_3)
                .setNodeId(stringList)
                .setTopologyId(ItmTestConstants.DESTINATION_DEVICE).build();
        deleteL2GwMlagDeviceInput = new DeleteL2GwMlagDeviceInputBuilder().setIpAddress(ItmTestConstants.IP_ADDRESS_3)
                .setNodeId(stringList).setTopologyId(ItmTestConstants.DESTINATION_DEVICE).build();
        buildExternalTunnelFromDpnsInput = new BuildExternalTunnelFromDpnsInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.IP_ADDRESS_3).setDpnId(dpId1List).build();
        removeExternalTunnelFromDpnsInput = new RemoveExternalTunnelFromDpnsInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.IP_ADDRESS_3).setDpnId(dpId1List).build();
        removeExternalTunnelEndpointInput = new RemoveExternalTunnelEndpointInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationIp(ItmTestConstants.IP_ADDRESS_3).build();
        getTunnelInterfaceNameInput = new GetTunnelInterfaceNameInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setSourceDpid(ItmTestConstants.DP_ID_1).setDestinationDpid(ItmTestConstants.DP_ID_2).build();
        getExternalTunnelInterfaceNameInput = new GetExternalTunnelInterfaceNameInputBuilder()
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setDestinationNode(ItmTestConstants.DESTINATION_DEVICE)
                .setSourceNode(ItmTestConstants.SOURCE_DEVICE).build();
        iface = ItmUtils.buildTunnelInterface(ItmTestConstants.DP_ID_1,trunkInterfaceName, String.format("%s %s",
                ItmUtils.convertTunnelTypetoString(ItmTestConstants.TUNNEL_TYPE_VXLAN), "Trunk Interface"),
                true,ItmTestConstants.TUNNEL_TYPE_VXLAN,tunnelEndPointsVxlan.getIpAddress(),
                ItmTestConstants.IP_ADDRESS_3,false,false, ItmTestConstants.MONITOR_PROTOCOL,null, false, null);

        transportZone = new TransportZoneBuilder().setZoneName(ItmTestConstants.TZ_NAME)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .withKey(new TransportZoneKey(ItmTestConstants.TZ_NAME)).build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();

        // build external tunnel objects
        externalTunnel = new ExternalTunnelBuilder().setSourceDevice(ItmTestConstants.DP_ID_1.toString())
                .setDestinationDevice(ItmTestConstants.IP_ADDRESS_3.stringValue())
                .setTransportType(TunnelTypeMplsOverGre.class)
                .setTunnelInterfaceName(ItmTestConstants.PARENT_INTERFACE_NAME).build();

        externalTunnel2 = new ExternalTunnelBuilder().setSourceDevice(ItmTestConstants.SOURCE_DEVICE)
                .setDestinationDevice(ItmTestConstants.DESTINATION_DEVICE)
                .setTransportType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .setTunnelInterfaceName(ItmTestConstants.PARENT_INTERFACE_NAME).build();

        getInternalOrExternalInterfaceNameOutput = new GetInternalOrExternalInterfaceNameOutputBuilder()
                .setInterfaceName(ItmTestConstants.PARENT_INTERFACE_NAME).build();

        // commit external tunnel into config DS
        syncWrite(externalTunnelIdentifier, externalTunnel);
        syncWrite(externalTunnelIdentifier2, externalTunnel2);

        // commit internal tunnel into config DS
        syncWrite(internalTunnelIdentifier, internalTunnel);

        // commit dpnEndpoints into config DS
        syncWrite(dpnEndpointsIdentifier, dpnEndpoints);

        // wait for completion of ITM config DS default-TZ creation task of DJC
        coordinatorEventsWaiter.awaitEventsConsumption();
        // commit TZ into config DS
        syncWrite(transportZonesIdentifier, transportZones);
    }

    @Test
    public void testGetExternalInterfaceNameExtTunnelPresent() throws Exception {
        Future<RpcResult<GetInternalOrExternalInterfaceNameOutput>> rpcRes  = itmManagerRpcService
                .getInternalOrExternalInterfaceName(getExternalInterfaceNameInput);
        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        assertThat(getInternalOrExternalInterfaceNameOutput).isEqualTo(rpcRes.get().getResult());
    }

    @Test
    public void testGetInternalInterfaceNameIntTunnelPresent() throws Exception {
        Future<RpcResult<GetInternalOrExternalInterfaceNameOutput>> rpcRes  = itmManagerRpcService
                .getInternalOrExternalInterfaceName(getInternalInterfaceNameInput);
        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        assertThat(getInternalOrExternalInterfaceNameOutput).isEqualTo(rpcRes.get().getResult());
    }

    @Test
    public void testAddExternalTunnelEndpoint() throws Exception {
        ListenableFuture<RpcResult<AddExternalTunnelEndpointOutput>> rpcRes =
                itmManagerRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnelEndpoint is added in config DS
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnelForRpcTest(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifierNew).checkedGet().get());
    }

    @Test
    public void testAddL2GwDevice() throws Exception {
        ListenableFuture<RpcResult<AddL2GwDeviceOutput>> rpcRes =
                itmManagerRpcService.addL2GwDevice(addL2GwDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwDevice is added in config DS
        assertEqualBeans(ExpectedDeviceVtepsObjects.newDeviceVtepsObject(),  dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).checkedGet().get());
    }

    @Test
    public void testAddL2GwMlagDevice() throws Exception {
        ListenableFuture<RpcResult<AddL2GwMlagDeviceOutput>> rpcRes =
                itmManagerRpcService.addL2GwMlagDevice(addL2GwMlagDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwMlagDevice is added in config DS
        assertEqualBeans(ExpectedDeviceVtepsObjects.newDeviceVtepsObject(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).checkedGet().get());
        assertEqualBeans(ExpectedDeviceVtepsObjects.newDeviceVtepsObject2(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier2).checkedGet().get());
    }

    @Test
    public void testDeleteL2GwDevice() throws Exception {
        ListenableFuture<RpcResult<DeleteL2GwDeviceOutput>> rpcRes =
                itmManagerRpcService.deleteL2GwDevice(deleteL2GwDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwDevice is deleted from config DS
        assertThat(Optional.empty()).isEqualTo(dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).get());
    }

    @Test
    public void testDeleteL2GwMlagDevice() throws Exception {
        ListenableFuture<RpcResult<DeleteL2GwMlagDeviceOutput>> rpcRes =
                itmManagerRpcService.deleteL2GwMlagDevice(deleteL2GwMlagDeviceInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check L2GwMlagDevice is deleted from config DS
        assertThat(Optional.empty()).isEqualTo(dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier).get());
        assertThat(Optional.empty()).isEqualTo(dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, deviceVtepsIdentifier2).get());
    }

    @Test
    public void testBuildExternalTunnelFromDpns() throws Exception {
        ListenableFuture<RpcResult<BuildExternalTunnelFromDpnsOutput>> rpcRes =
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
        ListenableFuture<RpcResult<RemoveExternalTunnelFromDpnsOutput>> rpcRes =
                itmManagerRpcService.removeExternalTunnelFromDpns(removeExternalTunnelFromDpnsInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnel From Dpns is deleted from config DS
        assertThat(Optional.empty()).isEqualTo(dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifierNew).get());
        // check iface is removed
        assertThat(Optional.empty()).isEqualTo(dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier).get());
    }

    @Test
    public void testRemoveExternalTunnelEndpoint() throws Exception {
        // call RPC to add ExternalTunnelEndpoint as pre-requisite
        ListenableFuture<RpcResult<AddExternalTunnelEndpointOutput>> rpcRes =
                itmManagerRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        ListenableFuture<RpcResult<RemoveExternalTunnelEndpointOutput>> removeExternalTunnelEndpoint =
                itmManagerRpcService.removeExternalTunnelEndpoint(removeExternalTunnelEndpointInput);

        // check RPC response is SUCCESS
        assertThat(removeExternalTunnelEndpoint.get().isSuccessful()).isTrue();

        // check ExternalTunnelEndpoint is deleted from config DS
        assertThat(Optional.empty()).isEqualTo(dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifierNew).get());
        // check iface is removed
        assertThat(Optional.empty()).isEqualTo(dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, interfaceIdentifier).get());
    }

    @Test
    public void testGetTunnelInterfaceName() throws Exception {
        Future<RpcResult<GetTunnelInterfaceNameOutput>> rpcRes = itmManagerRpcService
                .getTunnelInterfaceName(getTunnelInterfaceNameInput);
        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnel From Dpns is added in config DS
        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelObjVxLanOneToTwo(),
            dataBroker.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.CONFIGURATION,internalTunnelIdentifier).checkedGet().get());
    }

    @Test
    public void testGetExternalTunnelInterfaceName() throws Exception {
        Future<RpcResult<GetExternalTunnelInterfaceNameOutput>> rpcRes = itmManagerRpcService
                .getExternalTunnelInterfaceName(getExternalTunnelInterfaceNameInput);

        // check RPC response is SUCCESS
        assertThat(rpcRes.get().isSuccessful()).isTrue();

        // check ExternalTunnel From Dpns is added in config DS
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel2ForRpcTest(),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType
                    .CONFIGURATION,externalTunnelIdentifier2).checkedGet().get());

        // check for interfaceName
        assertThat(ItmTestConstants.PARENT_INTERFACE_NAME).isEqualTo(rpcRes.get().getResult().getInterfaceName());
    }

    private <T extends DataObject> void syncWrite(InstanceIdentifier<T> path, T data) {
        try {
            txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                tx -> tx.put(path, data, true)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
