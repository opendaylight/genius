/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.impl;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
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
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ItmExternalTunnelAddTest {

    BigInteger dpId1 = BigInteger.valueOf(1);
    int vlanId = 100 ;
    int interval = 1000;
    String portName1 = "phy0";
    String tepIp1 = "192.168.56.30";
    String tepIp2 = "192.168.56.40";
    String tepIp3 = "192.168.56.101";
    String gwyIp1 = "0.0.0.0";
    String gwyIp2 = "0.0.0.1";
    String subnetIp = "10.1.1.24";
    String parentInterfaceName = "1:phy0:100" ;
    String transportZone1 = "TZA" ;
    String source = "hwvtep://192.168.101.30:6640/physicalswitch/s3" ;
    String destination = "hwvtep://192.168.101.40:6640/physicalswitch/s4" ;
    String trunkInterfaceName = null;
    TunnelEndPoints tunnelEndPointsVxlan = null;
    IpAddress ipAddress1 = null;
    IpAddress ipAddress2 = null;
    IpAddress ipAddress3 = null;
    IpAddress gtwyIp1 = null;
    IpAddress gtwyIp2 = null;
    IpPrefix ipPrefixTest = null;
    DPNTEPsInfo dpntePsInfoVxlan = null;
    ExternalTunnel externalTunnel = null;
    DpnEndpoints dpnEndpointsVxlan = null;
    AllocateIdInput getIdInput1 = null;
    AllocateIdInput getIdInput2 = null;
    AllocateIdInput getIdInput3 = null;
    AllocateIdInput getIdInput4 = null;
    AllocateIdInput getIdInput5 = null;
    AllocateIdInput getIdInput6 = null;
    Subnets subnets = null;
    HwVtep hwVtep1  = null;
    Vteps vtepsTest = null;
    DeviceVteps deviceVteps1 = null;
    DeviceVteps deviceVteps2 = null;
    TransportZone transportZone = null;
    TunnelMonitorParams tunnelMonitorParams = null;
    TunnelMonitorInterval tunnelMonitorInterval = null;
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
    Interface iface = null;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>() ;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<BigInteger> bigIntegerList = new ArrayList<>();
    List<HwVtep> cfgdHwVtepsList = new ArrayList<>();
    List<Subnets> subnetsList = new ArrayList<>();
    List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    List<Vteps> vtepsList = new ArrayList<>();
    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;

    InstanceIdentifier<Interface> interfaceIdentifier = null;
    InstanceIdentifier<TunnelMonitorParams> tunnelMonitorParamsInstanceIdentifier =
            InstanceIdentifier.create(TunnelMonitorParams.class);
    InstanceIdentifier<TunnelMonitorInterval> tunnelMonitorIntervalIdentifier =
            InstanceIdentifier.create(TunnelMonitorInterval.class);
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier = null;
    InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier = InstanceIdentifier.builder(DpnEndpoints.class).build();
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
    Optional<DpnEndpoints> optionalDpnEndPoints ;
    Optional<TunnelMonitorParams> tunnelMonitorParamsOptional;
    Optional<TunnelMonitorInterval> tunnelMonitorIntervalOptional ;

    @Mock DataBroker dataBroker;
    @Mock JobCoordinator jobCoordinator;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock IdManagerService idManagerService;
    @Mock ItmConfig itmConfig;
    @Mock EntityOwnershipUtils entityOwnershipUtils;

    private ItmExternalTunnelAddWorker externalTunnelAddWorker;
    DirectTunnelUtils directTunnelUtils;
    UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;

    @Before
    public void setUp() {
        setupMocks();

        optionalDpnEndPoints = Optional.of(dpnEndpointsVxlan);
        tunnelMonitorParamsOptional = Optional.of(tunnelMonitorParams);
        tunnelMonitorIntervalOptional = Optional.of(tunnelMonitorInterval);

        doReturn(Futures.immediateCheckedFuture(optionalDpnEndPoints)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION,dpnEndpointsIdentifier);
        doReturn(Futures.immediateCheckedFuture(tunnelMonitorParamsOptional))
                .when(mockReadTx).read(LogicalDatastoreType.CONFIGURATION, tunnelMonitorParamsInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(tunnelMonitorIntervalOptional))
                .when(mockReadTx).read(LogicalDatastoreType.CONFIGURATION, tunnelMonitorIntervalIdentifier);

        externalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig,
            new DPNTEPsInfoCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl()),
                directTunnelUtils, jobCoordinator, unprocessedNodeConnectorEndPointCache));

    }

    @After
    public void cleanUp() {
    }

    private void setupMocks() {

        ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
        ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
        ipAddress3 = IpAddressBuilder.getDefaultInstance(tepIp3);
        gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
        gtwyIp2 = IpAddressBuilder.getDefaultInstance(gwyIp2);
        ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName1)
                .setIpAddress(ipAddress3).setGwIpAddress(gtwyIp1).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1))
                .setTunnelType(tunnelType1).setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId1).setUp(true).withKey(new DPNTEPsInfoKey(dpId1,
                ItmUtils.getTZonesFromTunnelEndPointList(tunnelEndPointsListVxlan)))
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        deviceVteps1 = new DeviceVtepsBuilder().setIpAddress(ipAddress1).withKey(new DeviceVtepsKey(ipAddress1,
                source)).setNodeId(source).setTopologyId("hwvtep:1").build();
        deviceVteps2 = new DeviceVtepsBuilder().setIpAddress(ipAddress2).withKey(new DeviceVtepsKey(ipAddress2,
                destination)).setNodeId(destination).setTopologyId("hwvtep:1").build();
        hwVtep1 = new HwVtep();
        hwVtep1.setTransportZone(transportZone1);
        hwVtep1.setGatewayIP(gtwyIp1);
        hwVtep1.setHwIp(ipAddress1);
        hwVtep1.setTunnelType(tunnelType1);
        hwVtep1.setVlanID(vlanId);
        hwVtep1.setTopoId("hwvtep:1");
        hwVtep1.setNodeId(source);
        hwVtep1.setIpPrefix(ipPrefixTest);
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        cfgdHwVtepsList.add(hwVtep1);
        bigIntegerList.add(dpId1);
        deviceVtepsList.add(deviceVteps1);
        deviceVtepsList.add(deviceVteps2);
        vtepsTest = new VtepsBuilder().setDpnId(dpId1).setIpAddress(ipAddress3).setPortname(portName1).withKey(new
                VtepsKey(dpId1,portName1)).build();
        vtepsList.add(vtepsTest);
        dpnEndpointsVxlan = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();
        transportZone = new TransportZoneBuilder().setTunnelType(tunnelType1).setZoneName(transportZone1).withKey(new
                TransportZoneKey(transportZone1)).setSubnets(subnetsList).build();
        idOutputOptional1 = RpcResultBuilder.success(expectedId1).buildFuture();
        getIdInput1 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.40:VXLAN").build();
        doReturn(idOutputOptional1).when(idManagerService).allocateId(getIdInput1);
        idOutputOptional2 = RpcResultBuilder.success(expectedId2).buildFuture();
        getIdInput2 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.30:VXLAN").build();
        doReturn(idOutputOptional2).when(idManagerService).allocateId(getIdInput2);
        idOutputOptional3 = RpcResultBuilder.success(expectedId3).buildFuture();
        getIdInput3 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep://192.168.101.30:6640/physicalswitch/s3:192.168.56.30:192.168.56.101:VXLAN")
                .build();
        doReturn(idOutputOptional3).when(idManagerService).allocateId(getIdInput3);
        idOutputOptional4 = RpcResultBuilder.success(expectedId4).buildFuture();
        getIdInput4 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep://192.168.101.40:6640/physicalswitch/s4:192.168.56.40:192.168.56.101:VXLAN")
                .build();
        doReturn(idOutputOptional4).when(idManagerService).allocateId(getIdInput4);
        idOutputOptional5 = RpcResultBuilder.success(expectedId5).buildFuture();
        getIdInput5 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep://192.168.101.30:6640/physicalswitch/s3:192.168.56.30:192.168.56.40:VXLAN")
                .build();
        doReturn(idOutputOptional5).when(idManagerService).allocateId(getIdInput5);
        idOutputOptional6 = RpcResultBuilder.success(expectedId6).buildFuture();
        getIdInput6 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("hwvtep:1:hwvtep://192.168.101.40:6640/physicalswitch/s4:192.168.56.40:192.168.56.30:VXLAN")
                .build();
        doReturn(idOutputOptional6).when(idManagerService).allocateId(getIdInput6);
        tunnelMonitorParams = new TunnelMonitorParamsBuilder().setEnabled(true).build();
        tunnelMonitorInterval = new TunnelMonitorIntervalBuilder().setInterval(interval).build();

        trunkInterfaceName = ItmUtils.getTrunkInterfaceName(parentInterfaceName,
                tunnelEndPointsVxlan.getIpAddress().stringValue(), ipAddress2.stringValue(),tunnelType1.getName());
        interfaceIdentifier = ItmUtils.buildId(trunkInterfaceName);
        externalTunnelIdentifier = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(ipAddress2.stringValue(),
                        dpId1.toString(), tunnelType1));
        iface = ItmUtils.buildTunnelInterface(dpId1,trunkInterfaceName, String.format("%s %s",
                ItmUtils.convertTunnelTypetoString(tunnelType1), "Trunk Interface"), true, tunnelType1, ipAddress3,
                ipAddress2, gtwyIp1, tunnelEndPointsVxlan.getVLANID(), false, false, monitorProtocol, null,  false,
                null);
        externalTunnel = ItmUtils.buildExternalTunnel(dpId1.toString(), ipAddress2.stringValue(),
                tunnelType1, trunkInterfaceName);
        subnets = new SubnetsBuilder().setGatewayIp(gtwyIp1).setVlanId(vlanId).withKey(new SubnetsKey(ipPrefixTest))
                .setVteps(vtepsList).setDeviceVteps(deviceVtepsList).build();
        subnetsList.add(subnets);

        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockWriteTx).when(dataBroker).newWriteOnlyTransaction();
        doReturn(Futures.immediateCheckedFuture(null)).when(mockWriteTx).submit();

    }

    @Test
    public void testBuildTunnelsToExternalEndPoint() {

        externalTunnelAddWorker.buildTunnelsToExternalEndPoint(cfgdDpnListVxlan, ipAddress2, tunnelType1);

        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,interfaceIdentifier,iface,true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier,externalTunnel,true);

    }

    @Ignore
    @Test
    public void testBuildTunnelsFromDpnToExternalEndPoint() {

        externalTunnelAddWorker.buildTunnelsFromDpnToExternalEndPoint(bigIntegerList, ipAddress2, tunnelType1);

        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,interfaceIdentifier,iface,true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier,externalTunnel,true);

    }

    @Test
    public void testBuildHwVtepsTunnels() {

        final Interface extTunnelIf1 = ItmUtils.buildTunnelInterface(dpId1, "tun030025bd04f",
                String.format("%s %s", tunnelType1.getName(), "Trunk Interface"), true, tunnelType1,
                tunnelEndPointsVxlan.getIpAddress(), ipAddress1,
                gtwyIp1, vlanId, false,false, monitorProtocol, ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL, false,
                null);
        final Interface hwTunnelIf2 = ItmUtils.buildHwTunnelInterface("tun9a55a9c38f2",
                String.format("%s %s", tunnelType1.getName(), "Trunk Interface"), true, hwVtep1.getTopoId(),
                hwVtep1.getNodeId(), tunnelType1, ipAddress1, ipAddress3, gtwyIp1, false, monitorProtocol,
                ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);
        final Interface extTunnelIf3 = ItmUtils.buildTunnelInterface(dpId1, "tun17c6e20c283",
                String.format("%s %s", tunnelType1.getName(), "Trunk Interface"), true, tunnelType1,
                tunnelEndPointsVxlan.getIpAddress(), ipAddress2, gtwyIp1, vlanId, false,false, monitorProtocol,
                ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL, false, null);
        final Interface hwTunnelIf4 = ItmUtils.buildHwTunnelInterface("tunaa109b6c8c5",
                String.format("%s %s", tunnelType1.getName(), "Trunk Interface"), true, hwVtep1.getTopoId(),
                destination, tunnelType1, ipAddress2, ipAddress3, gtwyIp1, false, monitorProtocol,
                ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);
        final Interface hwTunnelIf5 = ItmUtils.buildHwTunnelInterface("tund903ed434d5",
                String.format("%s %s", tunnelType1.getName(), "Trunk Interface"), true, hwVtep1.getTopoId(),
                hwVtep1.getNodeId(), tunnelType1, ipAddress1, ipAddress2, gtwyIp1, false, monitorProtocol,
                ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);
        final Interface hwTunnelIf6 = ItmUtils.buildHwTunnelInterface("tunc3315b110a6",
                String.format("%s %s", tunnelType1.getName(), "Trunk Interface"), true, hwVtep1.getTopoId(),
                destination, tunnelType1, ipAddress2, ipAddress1, gtwyIp1, false, monitorProtocol,
                ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);
        final ExternalTunnel externalTunnel1 = ItmUtils.buildExternalTunnel(getExternalTunnelKey(dpId1.toString()),
                getExternalTunnelKey(source), tunnelType1, "tun030025bd04f");
        final ExternalTunnel externalTunnel2 = ItmUtils.buildExternalTunnel(getExternalTunnelKey(source),
                getExternalTunnelKey(dpId1.toString()), tunnelType1, "tun9a55a9c38f2");
        final ExternalTunnel externalTunnel3 = ItmUtils.buildExternalTunnel(getExternalTunnelKey(dpId1.toString()),
                getExternalTunnelKey(destination), tunnelType1, "tun17c6e20c283");
        final ExternalTunnel externalTunnel4 = ItmUtils.buildExternalTunnel(getExternalTunnelKey(destination),
                getExternalTunnelKey(dpId1.toString()), tunnelType1, "tunaa109b6c8c5");
        final ExternalTunnel externalTunnel5 = ItmUtils.buildExternalTunnel(getExternalTunnelKey(source),
                getExternalTunnelKey(destination), tunnelType1, "tund903ed434d5");
        final ExternalTunnel externalTunnel6 = ItmUtils.buildExternalTunnel(getExternalTunnelKey(destination),
                getExternalTunnelKey(source), tunnelType1, "tunc3315b110a6");

        InstanceIdentifier<TransportZone> transportZoneIdentifier = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(transportZone1)).build();
        final InstanceIdentifier<Interface> ifIID1 = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey("tun030025bd04f")).build();
        final InstanceIdentifier<Interface> ifIID2 = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey("tun9a55a9c38f2")).build();
        final InstanceIdentifier<Interface> ifIID3 = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey("tun17c6e20c283")).build();
        final InstanceIdentifier<Interface> ifIID4 = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey("tunaa109b6c8c5")).build();
        final InstanceIdentifier<Interface> ifIID5 = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey("tund903ed434d5")).build();
        final InstanceIdentifier<Interface> ifIID6 = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey("tunc3315b110a6")).build();
        final InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier1 = InstanceIdentifier
                .create(ExternalTunnelList.class).child(ExternalTunnel.class,
                        new ExternalTunnelKey(getExternalTunnelKey(source),
                                getExternalTunnelKey(dpId1.toString()), tunnelType1));
        final InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier2 = InstanceIdentifier
                .create(ExternalTunnelList.class).child(ExternalTunnel.class,
                        new ExternalTunnelKey(getExternalTunnelKey(dpId1.toString()),
                                getExternalTunnelKey(source), tunnelType1));
        final InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier3 = InstanceIdentifier
                .create(ExternalTunnelList.class).child(ExternalTunnel.class,
                        new ExternalTunnelKey(getExternalTunnelKey(destination),
                                getExternalTunnelKey(dpId1.toString()), tunnelType1));
        final InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier4 = InstanceIdentifier
                .create(ExternalTunnelList.class).child(ExternalTunnel.class,
                        new ExternalTunnelKey(getExternalTunnelKey(dpId1.toString()),
                                getExternalTunnelKey(destination), tunnelType1));
        final InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier5 = InstanceIdentifier
                .create(ExternalTunnelList.class).child(ExternalTunnel.class,
                        new ExternalTunnelKey(getExternalTunnelKey(destination),
                                getExternalTunnelKey(source), tunnelType1));
        final InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier6 = InstanceIdentifier
                .create(ExternalTunnelList.class).child(ExternalTunnel.class,
                        new ExternalTunnelKey(getExternalTunnelKey(source),
                                getExternalTunnelKey(destination), tunnelType1));

        Optional<TransportZone> optionalTransportZone = Optional.of(transportZone);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZone)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);

        externalTunnelAddWorker.buildHwVtepsTunnels(cfgdDpnListVxlan, null);
        externalTunnelAddWorker.buildHwVtepsTunnels(null, cfgdHwVtepsList);

        verify(mockWriteTx, times(2)).merge(LogicalDatastoreType.CONFIGURATION,ifIID1,extTunnelIf1,true);
        verify(mockWriteTx, times(2)).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier1,
                externalTunnel1, true);
        verify(mockWriteTx, times(2)).merge(LogicalDatastoreType.CONFIGURATION,ifIID2,hwTunnelIf2,true);
        verify(mockWriteTx, times(2)).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier2,
                externalTunnel2, true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,ifIID3,extTunnelIf3,true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier3,
                externalTunnel3, true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,ifIID4,hwTunnelIf4,true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier4,
                externalTunnel4, true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,ifIID5,hwTunnelIf5,true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier5,
                externalTunnel5, true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,ifIID6,hwTunnelIf6,true);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,externalTunnelIdentifier6,
                externalTunnel6, true);

    }

    static String getExternalTunnelKey(String nodeid) {
        if (nodeid.indexOf("physicalswitch") > 0) {
            nodeid = nodeid.substring(0, nodeid.indexOf("physicalswitch") - 1);
        }
        return nodeid;
    }
}
