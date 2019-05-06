/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.impl;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelAddWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

//@RunWith(MockitoJUnitRunner.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest(ITMBatchingUtils.class)
public class ItmInternalTunnelAddTest {

    BigInteger dpId1 = BigInteger.valueOf(1);
    BigInteger dpId2 = BigInteger.valueOf(2);
    String portName1 = "phy0";
    String portName2 = "phy1" ;
    int vlanId = 100 ;
    int interval = 1000;
    String tepIp1 = "192.168.56.101";
    String tepIp2 = "192.168.56.102";
    String gwyIp1 = "0.0.0.0";
    String gwyIp2 = "0.0.0.1";
    String subnetIp = "10.1.1.24";
    String transportZone1 = "TZA" ;
    String parentInterfaceName = "1:phy0:100" ;
    String trunkInterfaceName1 = null;
    String trunkInterfaceName2 = null;
    IpAddress ipAddress1 = null;
    IpAddress ipAddress2 = null;
    IpAddress gtwyIp1 = null;
    IpAddress gtwyIp2 = null;
    IpPrefix ipPrefixTest = null;
    DPNTEPsInfo dpntePsInfoVxlan = null;
    DPNTEPsInfo dpntePsInfoVxlanNew = null;
    DPNTEPsInfo dpntePsInfoGre = null;
    DPNTEPsInfo dpntePsInfoGreNew = null;
    TunnelEndPoints tunnelEndPointsVxlan = null;
    TunnelEndPoints tunnelEndPointsVxlanNew = null;
    TunnelEndPoints tunnelEndPointsGre = null;
    TunnelEndPoints tunnelEndPointsGreNew = null;
    TunnelMonitorParams tunnelMonitorParams = null;
    TunnelMonitorInterval tunnelMonitorInterval = null;
    InternalTunnel internalTunnel1 = null;
    InternalTunnel internalTunnel2 = null;
    DpnEndpoints dpnEndpointsVxlan = null;
    DpnEndpoints dpnEndpointsGre = null;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListVxlanNew = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListGre = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListGreNew = new ArrayList<>();
    List<DPNTEPsInfo> meshDpnListVxlan = new ArrayList<>() ;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>() ;
    List<DPNTEPsInfo> meshDpnListGre = new ArrayList<>() ;
    List<DPNTEPsInfo> cfgdDpnListGre = new ArrayList<>() ;
    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;
    java.lang.Class<? extends TunnelTypeBase> tunnelType2 = TunnelTypeGre.class;
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = TunnelMonitoringTypeBfd.class;

    InstanceIdentifier<TunnelMonitorParams> tunnelMonitorParamsInstanceIdentifier =
            InstanceIdentifier.create(TunnelMonitorParams.class);
    InstanceIdentifier<TunnelMonitorInterval> tunnelMonitorIntervalIdentifier =
            InstanceIdentifier.create(TunnelMonitorInterval.class);
    InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier = InstanceIdentifier.builder(DpnEndpoints.class).build() ;
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifierVxlan1 = InstanceIdentifier.create(
            TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId2, dpId1, tunnelType1));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifierVxlan2 = InstanceIdentifier.create(
            TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId1, dpId2, tunnelType1));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifierGre1 = InstanceIdentifier.create(
            TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId2, dpId1, tunnelType2));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifierGre2 = InstanceIdentifier.create(
            TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId1, dpId2, tunnelType2));

    AllocateIdOutput expectedId1 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("100")).build();
    AllocateIdOutput expectedId2 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("200")).build();

    Future<RpcResult<AllocateIdOutput>> idOutputOptional1;
    Future<RpcResult<AllocateIdOutput>> idOutputOptional2;

    @Mock DataBroker dataBroker;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock ReadWriteTransaction mockReadWriteTx;
    @Mock IMdsalApiManager mdsalApiManager;
    @Mock IdManagerService idManagerService;
    @Mock ITMBatchingUtils batchingUtils;
    @Mock ItmConfig itmConfig;
    @Mock JobCoordinator jobCoordinator;
    @Mock IInterfaceManager interfaceManager;
    ItmInternalTunnelAddWorker itmInternalTunnelAddWorker;
    DirectTunnelUtils directTunnelUtils;
    DataTreeEventCallbackRegistrar eventCallbacks;

    Optional<TunnelMonitorParams> tunnelMonitorParamsOptional;
    Optional<TunnelMonitorInterval> tunnelMonitorIntervalOptional;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ITMBatchingUtils.class);
        setupMocks();

        tunnelMonitorParamsOptional = Optional.of(tunnelMonitorParams);
        tunnelMonitorIntervalOptional = Optional.of(tunnelMonitorInterval);

        idOutputOptional1 = RpcResultBuilder.success(expectedId1).buildFuture();
        idOutputOptional2 = RpcResultBuilder.success(expectedId2).buildFuture();

        doReturn(Futures.immediateCheckedFuture(tunnelMonitorParamsOptional)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION, tunnelMonitorParamsInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(tunnelMonitorIntervalOptional)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION, tunnelMonitorIntervalIdentifier);
        doReturn(Futures.immediateCheckedFuture(tunnelMonitorParamsOptional)).when(mockReadWriteTx)
                .read(LogicalDatastoreType.CONFIGURATION, tunnelMonitorParamsInstanceIdentifier);
        doReturn(Futures.immediateCheckedFuture(tunnelMonitorIntervalOptional)).when(mockReadWriteTx)
                .read(LogicalDatastoreType.CONFIGURATION, tunnelMonitorIntervalIdentifier);

        itmInternalTunnelAddWorker = new ItmInternalTunnelAddWorker(dataBroker, jobCoordinator,
                new TunnelMonitoringConfig(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl())),
                itmConfig, directTunnelUtils, interfaceManager, new OvsBridgeRefEntryCache(dataBroker,
                new GuavaCacheProvider(new CacheManagersRegistryImpl())), eventCallbacks);
    }

    @After
    public void cleanUp() {
    }

    private void setupMocks() {

        ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
        ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
        ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
        gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
        gtwyIp2 = IpAddressBuilder.getDefaultInstance(gwyIp2);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName1)
                .setIpAddress(ipAddress1).setGwIpAddress(gtwyIp1).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType1)
                .setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsVxlanNew = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName2)
                .setIpAddress(ipAddress2).setGwIpAddress(gtwyIp2).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType1)
                .setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsGre = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName1)
                .setIpAddress(ipAddress1).setGwIpAddress(gtwyIp1).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType2)
                .setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsGreNew = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName2)
                .setIpAddress(ipAddress2).setGwIpAddress(gtwyIp2).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType2)
                .setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        tunnelEndPointsListVxlanNew.add(tunnelEndPointsVxlanNew);
        tunnelEndPointsListGre.add(tunnelEndPointsGre);
        tunnelEndPointsListGreNew.add(tunnelEndPointsGreNew);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId1).setUp(true).withKey(new DPNTEPsInfoKey(dpId1))
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        dpntePsInfoVxlanNew = new DPNTEPsInfoBuilder().setDPNID(dpId2).withKey(new DPNTEPsInfoKey(dpId2)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlanNew).build();
        dpntePsInfoGre = new DPNTEPsInfoBuilder().setDPNID(dpId1).setUp(true).withKey(new DPNTEPsInfoKey(dpId1))
                .setTunnelEndPoints(tunnelEndPointsListGre).build();
        dpntePsInfoGreNew = new DPNTEPsInfoBuilder().setDPNID(dpId2).withKey(new DPNTEPsInfoKey(dpId2)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListGreNew).build();
        tunnelMonitorParams = new TunnelMonitorParamsBuilder().setEnabled(true)
                .setMonitorProtocol(monitorProtocol).build();
        tunnelMonitorInterval = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        meshDpnListVxlan.add(dpntePsInfoVxlanNew);
        cfgdDpnListGre.add(dpntePsInfoGre);
        meshDpnListGre.add(dpntePsInfoGreNew);
        dpnEndpointsVxlan = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();
        dpnEndpointsGre = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListGre).build();

        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockReadWriteTx).when(dataBroker).newReadWriteTransaction();
        doReturn(Futures.immediateCheckedFuture(null)).when(mockReadWriteTx).submit();
        doReturn(true).when(mockReadWriteTx).cancel();
    }

    @Test
    public void testBuild_all_tunnels_VXLANtype() {

        AllocateIdInput getIdInput1 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.102:VXLAN").build();
        AllocateIdInput getIdInput2 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.102:192.168.56.101:VXLAN").build();

        doReturn(idOutputOptional1).when(idManagerService).allocateId(getIdInput1);
        doReturn(idOutputOptional2).when(idManagerService).allocateId(getIdInput2);

        trunkInterfaceName1 = ItmUtils.getTrunkInterfaceName(parentInterfaceName,tepIp1,tepIp2,
                tunnelType1.getName());
        trunkInterfaceName2 = ItmUtils.getTrunkInterfaceName(parentInterfaceName,tepIp2,tepIp1,
                tunnelType1.getName());
        internalTunnel1 = ItmUtils.buildInternalTunnel(dpId1,dpId2,tunnelType1,trunkInterfaceName1);
        internalTunnel2 = ItmUtils.buildInternalTunnel(dpId2,dpId1,tunnelType1,trunkInterfaceName2);

        itmInternalTunnelAddWorker.buildAllTunnels(mdsalApiManager, cfgdDpnListVxlan, meshDpnListVxlan);

        //Add some verifications
        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifierVxlan1,
                internalTunnel1,true);
        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifierVxlan2,
                internalTunnel2,true);
        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, dpnEndpointsIdentifier,
                dpnEndpointsVxlan);
    }

    @Test
    public void testBuild_all_tunnels_GREtype() {

        AllocateIdInput getIdInput1 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.102:GRE").build();
        AllocateIdInput getIdInput2 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.102:192.168.56.101:GRE").build();

        doReturn(idOutputOptional1).when(idManagerService).allocateId(getIdInput1);
        doReturn(idOutputOptional2).when(idManagerService).allocateId(getIdInput2);

        trunkInterfaceName1 = ItmUtils.getTrunkInterfaceName(parentInterfaceName,tepIp1,tepIp2,
                tunnelType2.getName());
        trunkInterfaceName2 = ItmUtils.getTrunkInterfaceName(parentInterfaceName,tepIp2,tepIp1,
                tunnelType2.getName());
        internalTunnel1 = ItmUtils.buildInternalTunnel(dpId1,dpId2,tunnelType2,trunkInterfaceName1);
        internalTunnel2 = ItmUtils.buildInternalTunnel(dpId2,dpId1,tunnelType2,trunkInterfaceName2);

        itmInternalTunnelAddWorker.buildAllTunnels(mdsalApiManager, cfgdDpnListGre, meshDpnListGre);

        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifierGre1,
                internalTunnel1,true);
        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifierGre2,
                internalTunnel2,true);
        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, dpnEndpointsIdentifier,
                dpnEndpointsGre);
    }

    @Test
    public void testBuild_all_tunnels_Boyhtype() {

        AllocateIdInput getIdInput1 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.101:192.168.56.102:VXLAN").build();
        AllocateIdInput getIdInput2 = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey("1:phy0:100:192.168.56.102:192.168.56.101:GRE").build();

        doReturn(idOutputOptional1).when(idManagerService).allocateId(getIdInput1);
        doReturn(idOutputOptional2).when(idManagerService).allocateId(getIdInput2);

        trunkInterfaceName1 = ItmUtils.getTrunkInterfaceName(parentInterfaceName,tepIp1,tepIp2,
                tunnelType1.getName());
        trunkInterfaceName2 = ItmUtils.getTrunkInterfaceName(parentInterfaceName,tepIp2,tepIp1,
                tunnelType2.getName());
        internalTunnel1 = ItmUtils.buildInternalTunnel(dpId1,dpId2,tunnelType1,trunkInterfaceName1);
        internalTunnel2 = ItmUtils.buildInternalTunnel(dpId2,dpId1,tunnelType2,trunkInterfaceName2);

        itmInternalTunnelAddWorker.buildAllTunnels(mdsalApiManager, cfgdDpnListVxlan, meshDpnListGre);

        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifierVxlan1,
                internalTunnel1,true);
        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifierGre2,
                internalTunnel2,true);
        verify(mockReadWriteTx).merge(LogicalDatastoreType.CONFIGURATION, dpnEndpointsIdentifier,
                dpnEndpointsVxlan);
    }
}
