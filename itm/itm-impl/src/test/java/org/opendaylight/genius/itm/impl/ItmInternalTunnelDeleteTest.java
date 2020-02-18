/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.cloudscaler.api.TombstonedNodeManager;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OfEndPointCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.class)
public class ItmInternalTunnelDeleteTest {

    Uint64 dpId1 = Uint64.ONE;
    Uint64 dpId2 = Uint64.valueOf(2);
    int vlanId = 100 ;
    int interval = 1000;
    String portName1 = "phy0";
    String portName2 = "phy1" ;
    String parentInterfaceName = "1:phy0:100" ;
    String transportZone1 = "TZA" ;
    String subnetIp = "10.1.1.24";
    String tepIp1 = "192.168.56.101";
    String tepIp2 = "192.168.56.102";
    String gwyIp1 = "0.0.0.0";
    String gwyIp2 = "0.0.0.1";
    IpAddress ipAddress1 = null;
    IpAddress ipAddress2 = null;
    IpAddress gtwyIp1 = null;
    IpAddress gtwyIp2 = null;
    IpPrefix ipPrefixTest = null;
    DPNTEPsInfo dpntePsInfoVxlan = null;
    DPNTEPsInfo dpntePsInfoVxlanNew = null;
    TunnelEndPoints tunnelEndPointsVxlan = null;
    TunnelEndPoints tunnelEndPointsVxlanNew = null;
    DpnEndpoints dpnEndpoints = null;
    List<DPNTEPsInfo> meshDpnListVxlan = new ArrayList<>() ;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>() ;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListVxlanNew = new ArrayList<>();
    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;
    TunnelMonitorParams tunnelMonitorParams = null;
    TunnelMonitorInterval tunnelMonitorInterval = null;
    InternalTunnel internalTunnel = new InternalTunnelBuilder().build();
    InstanceIdentifier<TunnelMonitorParams> tunnelMonitorParamsInstanceIdentifier =
            InstanceIdentifier.create(TunnelMonitorParams.class);
    InstanceIdentifier<TunnelMonitorInterval> tunnelMonitorIntervalIdentifier =
            InstanceIdentifier.create(TunnelMonitorInterval.class);
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = TunnelMonitoringTypeBfd.class;
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifier = InstanceIdentifier.builder(TunnelList.class)
            .child(InternalTunnel.class, new InternalTunnelKey(Uint64.ONE, Uint64.valueOf(2), tunnelType1))
            .build();

    @Mock DataBroker dataBroker;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock ReadWriteTransaction mockReadWriteTx;
    @Mock IdManagerService idManagerService;
    @Mock IMdsalApiManager mdsalApiManager;
    @Mock JobCoordinator jobCoordinator;
    @Mock IInterfaceManager interfaceManager;
    @Mock ItmConfig itmConfig;
    @Mock TunnelMonitoringConfig tunnelMonitoringConfig;
    @Mock TombstonedNodeManager tombstonedNodeManager;
    DirectTunnelUtils directTunnelUtils;
    ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker;
    UnprocessedNodeConnectorCache unprocessedNodeConnectorCache;
    UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;
    OfEndPointCache ofEndPointCache;


    Optional<TunnelMonitorParams> tunnelMonitorParamsOptional;
    Optional<TunnelMonitorInterval> tunnelMonitorIntervalOptional ;
    Optional<InternalTunnel> internalTunnelOptional ;

    @Before
    public void setUp() {
        setupMocks();

        tunnelMonitorParamsOptional = Optional.of(tunnelMonitorParams);
        tunnelMonitorIntervalOptional = Optional.of(tunnelMonitorInterval);
        internalTunnelOptional = Optional.of(internalTunnel);

        doReturn(Futures.immediateCheckedFuture(tunnelMonitorParamsOptional)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION,
                        tunnelMonitorParamsInstanceIdentifier);
        lenient().doReturn(Futures.immediateCheckedFuture(tunnelMonitorParamsOptional)).when(mockReadWriteTx)
                .read(LogicalDatastoreType.CONFIGURATION,
                        tunnelMonitorParamsInstanceIdentifier);
        lenient().doReturn(Futures.immediateCheckedFuture(tunnelMonitorIntervalOptional)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION,
                        tunnelMonitorIntervalIdentifier);
        lenient().doReturn(Futures.immediateCheckedFuture(tunnelMonitorIntervalOptional)).when(mockReadWriteTx)
                .read(LogicalDatastoreType.CONFIGURATION,
                        tunnelMonitorIntervalIdentifier);
        doReturn(Futures.immediateCheckedFuture(internalTunnelOptional)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifier);
        lenient().doReturn(Futures.immediateCheckedFuture(internalTunnelOptional)).when(mockReadWriteTx)
                .read(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifier);

        DPNTEPsInfoCache dpntePsInfoCache =
                new DPNTEPsInfoCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl()),
                        directTunnelUtils, jobCoordinator, unprocessedNodeConnectorEndPointCache);

        itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker(dataBroker, jobCoordinator,
            new TunnelMonitoringConfig(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl())),
            interfaceManager, new DpnTepStateCache(dataBroker, jobCoordinator,
            new GuavaCacheProvider(new CacheManagersRegistryImpl()), directTunnelUtils, dpntePsInfoCache,
                unprocessedNodeConnectorCache, unprocessedNodeConnectorEndPointCache),
            new OvsBridgeEntryCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl())),
            new OvsBridgeRefEntryCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl())),
            new TunnelStateCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl())),
            directTunnelUtils, ofEndPointCache, itmConfig, tombstonedNodeManager);
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
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder()
                .setIpAddress(ipAddress1).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1))
                .setTunnelType(tunnelType1)
                .withKey(new TunnelEndPointsKey(ipAddress1,tunnelType1)).build();
        tunnelEndPointsVxlanNew = new TunnelEndPointsBuilder()
                .setIpAddress(ipAddress2).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType1)
                .build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        tunnelEndPointsListVxlanNew.add(tunnelEndPointsVxlanNew);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId1).withKey(new DPNTEPsInfoKey(dpId1)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        dpntePsInfoVxlanNew = new DPNTEPsInfoBuilder().setDPNID(dpId2).withKey(new DPNTEPsInfoKey(dpId2)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlanNew).setTunnelEndPoints(tunnelEndPointsListVxlanNew)
                .build();
        tunnelMonitorParams = new TunnelMonitorParamsBuilder().setEnabled(true).setMonitorProtocol(monitorProtocol)
                .build();
        tunnelMonitorInterval = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        meshDpnListVxlan.add(dpntePsInfoVxlan);
        meshDpnListVxlan.add(dpntePsInfoVxlanNew);
        dpnEndpoints = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();

        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockReadWriteTx).when(dataBroker).newReadWriteTransaction();
        lenient().doReturn(FluentFutures.immediateNullFluentFuture()).when(mockReadWriteTx).commit();
        doReturn(true).when(mockReadWriteTx).cancel();
        lenient().doReturn(false).when(itmConfig).isUseOfTunnels();
    }

    // Since all the unit test cases will be written to the new way, this should be taken care then.
    @Test
    public void testDeleteTunnels() {

        InstanceIdentifier<TunnelEndPoints> tunnelEndPointsIdentifier =
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, dpntePsInfoVxlan.key())
                        .child(TunnelEndPoints.class,tunnelEndPointsVxlan.key()).build();
        InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier =
                InstanceIdentifier.builder(DpnEndpoints.class).build();
        final InstanceIdentifier<DPNTEPsInfo> dpntePsInfoIdentifier = InstanceIdentifier.builder(DpnEndpoints.class)
                .child(DPNTEPsInfo.class, dpntePsInfoVxlan.key()).build();

        Optional<DpnEndpoints> dpnEndpointsOptional = Optional.of(dpnEndpoints);

        lenient().doReturn(Futures.immediateCheckedFuture(dpnEndpointsOptional)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION,dpnEndpointsIdentifier);

        itmInternalTunnelDeleteWorker.deleteTunnels(mdsalApiManager, cfgdDpnListVxlan,meshDpnListVxlan);
        //FIXME: This verification is broken revisit this.
        //verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,tunnelEndPointsIdentifier);
        verify(mockReadWriteTx).delete(LogicalDatastoreType.CONFIGURATION,dpntePsInfoIdentifier);
    }
}
