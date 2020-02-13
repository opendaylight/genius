/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.class)
public class ItmExternalTunnelDeleteTest {

    int vlanId = 100 ;
    String portName1 = "phy0";
    String parentInterfaceName = "1:phy0:100" ;
    String transportZone1 = "TZA" ;
    String subnetIp = "10.1.1.24";
    String tepIp1 = "192.168.56.30";
    String tepIp2 = "192.168.56.40";
    String tepIp3 = "192.168.56.101";
    String gwyIp1 = "0.0.0.0";
    String gwyIp2 = "0.0.0.1";
    String trunkInterfaceName = null;
    IpAddress ipAddress1 = null;
    IpAddress ipAddress2 = null;
    IpAddress ipAddress3 = null;
    IpAddress gtwyIp1 = null;
    IpAddress gtwyIp2 = null;
    IpPrefix ipPrefixTest = null;
    Uint64 dpId2 = Uint64.ONE;
    DPNTEPsInfo dpntePsInfoVxlan = null;
    TunnelEndPoints tunnelEndPointsVxlan = null;
    HwVtep hwVtep1  = null;
    DeviceVteps deviceVteps1 = null;
    DeviceVteps deviceVteps2 = null;
    Vteps vteps = null;
    TransportZone transportZone = null;
    AllocateIdInput getIdInput1 = null;
    AllocateIdInput getIdInput2 = null;
    AllocateIdInput getIdInput3 = null;
    AllocateIdInput getIdInput4 = null;
    AllocateIdInput getIdInput5 = null;
    AllocateIdInput getIdInput6 = null;
    AllocateIdInput getIdInput7 = null;
    AllocateIdInput getIdInput8 = null;
    ExternalTunnel externalTunnel = null;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<DPNTEPsInfo> dpnTepsList = new ArrayList<>() ;
    List<HwVtep> cfgdHwVtepsList = new ArrayList<>();
    List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    List<Vteps> vtepsList = new ArrayList<>();
    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;

    AllocateIdOutput expectedId1 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("100")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional1 ;
    AllocateIdOutput expectedId2 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("101")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional2 ;
    AllocateIdOutput expectedId3 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("102")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional3 ;
    AllocateIdOutput expectedId4 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("103")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional4 ;
    AllocateIdOutput expectedId5 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("104")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional5 ;
    AllocateIdOutput expectedId6 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("105")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional6 ;
    AllocateIdOutput expectedId7 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("105")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional7 ;
    AllocateIdOutput expectedId8 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("106")).build();
    Future<RpcResult<AllocateIdOutput>> idOutputOptional8 ;
    InstanceIdentifier<Interface> trunkIdentifier ;
    InstanceIdentifier<ExternalTunnel> path ;

    @Mock DataBroker dataBroker;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock IdManagerService idManagerService;
    @Mock TypedWriteTransaction<Datastore.Configuration> typedWriteTransaction;
    @Mock TypedReadWriteTransaction<Datastore.Configuration> typedReadWriteTransaction;
    @Mock ItmConfig itmConfig;

    @Before
    public void setUp() {
        setupMocks();

        lenient().when(idManagerService.releaseId(any(ReleaseIdInput.class))).thenReturn(Futures.immediateFuture(
                RpcResultBuilder.<ReleaseIdOutput>success().build()));
    }

    @After
    public void cleanUp() {
    }

    private void setupMocks() {

        ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
        ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
        ipAddress3 = IpAddressBuilder.getDefaultInstance(tepIp3);
        ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
        gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
        gtwyIp2 = IpAddressBuilder.getDefaultInstance(gwyIp2);
        deviceVteps1 = new DeviceVtepsBuilder().setIpAddress(ipAddress1)
                .withKey(new DeviceVtepsKey(ipAddress1, "hwvtep:1"))
                .setNodeId("hwvtep://192.168.101.30:6640/physicalswitch/s3")
                .setTopologyId("hwvtep:1").build();
        deviceVteps2 = new DeviceVtepsBuilder().setIpAddress(ipAddress2)
                .withKey(new DeviceVtepsKey(ipAddress2, "hwvtep:1"))
                .setNodeId("hwvtep://192.168.101.30:6640/physicalswitch/s3")
                .setTopologyId("hwvtep:1").build();
        deviceVtepsList.add(deviceVteps1);
        deviceVtepsList.add(deviceVteps2);
        hwVtep1 = new HwVtep();
        hwVtep1.setTransportZone(transportZone1);
        hwVtep1.setGatewayIP(gtwyIp1);
        hwVtep1.setHwIp(ipAddress2);
        hwVtep1.setTunnelType(tunnelType1);
        hwVtep1.setVlanID(vlanId);
        hwVtep1.setTopoId("hwvtep:1");
        hwVtep1.setNodeId("hwvtep://192.168.101.30:6640/physicalswitch/s3");
        hwVtep1.setIpPrefix(ipPrefixTest);
        vteps = new VtepsBuilder().setDpnId(dpId2).setIpAddress(ipAddress1).withKey(new
                VtepsKey(dpId2)).build();
        vtepsList.add(vteps);
        idOutputOptional1 = RpcResultBuilder.success(expectedId1).buildFuture();
        getIdInput1 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.30:VXLAN").build();
        lenient().doReturn(idOutputOptional1).when(idManagerService).allocateId(getIdInput1);
        idOutputOptional2 = RpcResultBuilder.success(expectedId2).buildFuture();
        getIdInput2 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep:1:192.168.56.30:192.168.56.101:VXLAN").build();
        lenient().doReturn(idOutputOptional2).when(idManagerService).allocateId(getIdInput2);
        idOutputOptional3 = RpcResultBuilder.success(expectedId3).buildFuture();
        getIdInput3 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.40:VXLAN").build();
        lenient().doReturn(idOutputOptional3).when(idManagerService).allocateId(getIdInput3);
        idOutputOptional4 = RpcResultBuilder.success(expectedId4).buildFuture();
        getIdInput4 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep:1:192.168.56.40:192.168.56.101:VXLAN").build();
        lenient().doReturn(idOutputOptional4).when(idManagerService).allocateId(getIdInput4);
        idOutputOptional5 = RpcResultBuilder.success(expectedId5).buildFuture();
        getIdInput5 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep://192.168.101.30:6640/physicalswitch/"
                        + "s3:192.168.56.40:192.168.56.101:VXLAN").build();
        lenient().doReturn(idOutputOptional5).when(idManagerService).allocateId(getIdInput5);
        idOutputOptional6 = RpcResultBuilder.success(expectedId6).buildFuture();
        getIdInput6 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep://192.168.101.30:6640/physicalswitch/"
                        + "s3:192.168.56.40:192.168.56.30:VXLAN")
                .build();
        lenient().doReturn(idOutputOptional6).when(idManagerService).allocateId(getIdInput6);
        idOutputOptional7 = RpcResultBuilder.success(expectedId7).buildFuture();
        getIdInput7 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep:1:192.168.56.30:192.168.56.40:VXLAN").build();
        lenient().doReturn(idOutputOptional7).when(idManagerService).allocateId(getIdInput7);
        idOutputOptional8 = RpcResultBuilder.success(expectedId8).buildFuture();
        getIdInput8 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.30:192.168.56.40:VXLAN").build();
        lenient().doReturn(idOutputOptional8).when(idManagerService).allocateId(getIdInput8);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder()
                .setIpAddress(ipAddress3).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType1)
                .build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId2).setUp(true).withKey(new DPNTEPsInfoKey(dpId2))
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        dpnTepsList.add(dpntePsInfoVxlan);
        cfgdHwVtepsList.add(hwVtep1);

        transportZone = new TransportZoneBuilder().setTunnelType(tunnelType1).setZoneName(transportZone1).withKey(new
                TransportZoneKey(transportZone1)).setDeviceVteps(deviceVtepsList).setVteps(vtepsList).build();
        externalTunnel = new ExternalTunnelBuilder().setTunnelInterfaceName(parentInterfaceName)
                .setTransportType(tunnelType1).setDestinationDevice("hwvtep:1").setSourceDevice(dpId2.toString())
                .withKey(new ExternalTunnelKey(dpId2.toString() , hwVtep1.getNodeId() , tunnelType1)).build();
        trunkInterfaceName = ItmUtils.getTrunkInterfaceName(parentInterfaceName,
            tunnelEndPointsVxlan.getIpAddress().stringValue(), ipAddress1.stringValue(), tunnelType1.getName());
        trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
        path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(ipAddress1.stringValue(),
                    dpId2.toString(), tunnelType1));

        lenient().doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        lenient().doReturn(mockWriteTx).when(dataBroker).newWriteOnlyTransaction();
        lenient().doReturn(Futures.immediateCheckedFuture(null)).when(mockWriteTx).submit();
        doReturn("phy0").when(itmConfig).getPortname();
        doReturn(Uint16.valueOf(100)).when(itmConfig).getVlanId();
    }

    @Test
    public void testDeleteTunnels() {
        ItmExternalTunnelDeleteWorker.deleteTunnels(dpnTepsList, ipAddress1, tunnelType1, typedWriteTransaction);
        verify(typedWriteTransaction).delete(trunkIdentifier);
        verify(typedWriteTransaction).delete(path);
    }

    @Test
    public void testDeleteHwVtepsTunnels() {

        InstanceIdentifier<TransportZone> transportZoneIdentifier = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(transportZone1)).build();
        InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier1 = InstanceIdentifier.create(ExternalTunnelList
                .class).child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(hwVtep1.getTopoId(),
                dpId2.toString(), tunnelType1));
        InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier2 = InstanceIdentifier.create(ExternalTunnelList
                .class).child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(dpId2.toString(),
                hwVtep1.getTopoId(), tunnelType1));
        InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier3 = InstanceIdentifier.create(ExternalTunnelList
                .class).child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(hwVtep1.getNodeId(),
                dpId2.toString(), tunnelType1));
        InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier4 = InstanceIdentifier.create(ExternalTunnelList
                .class).child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(dpId2.toString(),
                hwVtep1.getNodeId(), tunnelType1));
        InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier5 = InstanceIdentifier.create(ExternalTunnelList
                .class).child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(hwVtep1.getTopoId(),
                hwVtep1.getNodeId(), tunnelType1));
        InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier6 = InstanceIdentifier.create(ExternalTunnelList
                .class).child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(hwVtep1.getNodeId(),
                hwVtep1.getTopoId(), tunnelType1));

        Optional<TransportZone> optionalTransportZone = Optional.of(transportZone);
        Optional<ExternalTunnel> exTunnels = Optional.of(externalTunnel);

        doReturn(FluentFutures.immediateFluentFuture(optionalTransportZone))
                .when(typedReadWriteTransaction).read(transportZoneIdentifier);
        doReturn(FluentFutures.immediateTrueFluentFuture())
                .when(typedReadWriteTransaction).exists(externalTunnelIdentifier1);
        doReturn(FluentFutures.immediateTrueFluentFuture())
                .when(typedReadWriteTransaction).exists(externalTunnelIdentifier2);
        doReturn(FluentFutures.immediateTrueFluentFuture()).when(typedReadWriteTransaction)
                .exists(externalTunnelIdentifier3);
        doReturn(FluentFutures.immediateTrueFluentFuture()).when(typedReadWriteTransaction)
                .exists(externalTunnelIdentifier4);
        doReturn(FluentFutures.immediateTrueFluentFuture()).when(typedReadWriteTransaction)
                .exists(externalTunnelIdentifier5);
        doReturn(FluentFutures.immediateTrueFluentFuture()).when(typedReadWriteTransaction)
                .exists(externalTunnelIdentifier6);

        try {
            ItmExternalTunnelDeleteWorker
                    .deleteHwVtepsTunnels(dpnTepsList, cfgdHwVtepsList, transportZone, typedReadWriteTransaction,
                            itmConfig);
            verify(typedReadWriteTransaction).delete(trunkIdentifier);
            verify(typedReadWriteTransaction, times(2)).delete(externalTunnelIdentifier1);
            verify(typedReadWriteTransaction, times(2)).delete(externalTunnelIdentifier2);
            verify(typedReadWriteTransaction, times(2)).delete(externalTunnelIdentifier3);
            verify(typedReadWriteTransaction, times(2)).delete(externalTunnelIdentifier4);
            verify(typedReadWriteTransaction).delete(externalTunnelIdentifier5);
            verify(typedReadWriteTransaction).delete(externalTunnelIdentifier6);
        } catch (ExecutionException | InterruptedException e) {
            TestCase.fail();// Fail in case of an exception
        }
    }
}
