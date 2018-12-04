/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.cli;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cache.UnprocessedTunnelsStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class TepCommandHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(TepCommandHelper.class);

    private final int vlanId = 100 ;
    private final int interval = 1000;
    private final Boolean enabled = false ;
    private final Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
    private final String tepIp1 = "192.168.56.30";
    private final String tepIp2 = "192.168.56.102";
    private final String tepIp3 = "168.56.102";
    private final String tepIp4 = "150.168.56.102";
    private final String gwyIp1 = "192.168.56.105";
    private final String gwyIp2 = "192.168.56.106";
    private final String subnetMask = "192.168.56.100/24";
    private final String tunnelInterfaceName =  "1:phy0:100" ;
    private final String sourceDevice = "hwvtep://192.168.101.30:6640/physicalswitch/s3";
    private final String destinationDevice = "hwvtep:1";
    private final String portName1 = "phy0";
    private final String transportZone1 = "TZA" ;
    private final BigInteger dpId1 = BigInteger.valueOf(1);
    private final BigInteger dpId2 = BigInteger.valueOf(2);
    private final IpAddress gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
    private final IpAddress ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
    private final IpAddress ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
    private final IpPrefix ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetMask);
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private TransportZone transportZone = null;
    private TransportZone transportZoneNew = null;
    private TransportZones transportZones = null;
    private TransportZones transportZonesNew = null;
    private TunnelMonitorInterval tunnelMonitorInterval = null;
    private TunnelMonitorParams tunnelMonitorParams = null;
    private Vteps vteps = null;
    private Vteps vtepsTest = null;
    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
            .Interface interfaceTest = null;
    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
            interfaceTestNew = null;
    private final List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    private final List<Vteps> vtepsList = new ArrayList<>();
    private final List<TransportZone> transportZoneList = new ArrayList<>();
    private final List<TransportZone> transportZoneListNew = new ArrayList<>();
    private final List<InternalTunnel> internalTunnelList = new ArrayList<>();
    private final List<StateTunnelList> stateTunnelList = new ArrayList<>() ;
    private final List<String> lowerLayerIfList = new ArrayList<>();
    private final List<InstanceIdentifier> instanceIdentifierList = new ArrayList<>();
    private final java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;
    private final java.lang.Class<? extends TunnelTypeBase> tunnelType2 = TunnelTypeGre.class;

    private final InstanceIdentifier<TransportZone> transportZoneIdentifier = InstanceIdentifier
            .builder(TransportZones.class).child(TransportZone.class, new TransportZoneKey(transportZone1)).build();
    private final InstanceIdentifier<TransportZones> transportZonesIdentifier =
            InstanceIdentifier.builder(TransportZones.class).build();
    private final InstanceIdentifier<TunnelMonitorInterval> tunnelMonitorIntervalIdentifier =
            InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
    private final InstanceIdentifier<TunnelMonitorParams> tunnelMonitorParamsIdentifier =
            InstanceIdentifier.builder(TunnelMonitorParams.class).build();
    private final InstanceIdentifier<Vteps> vtepsIdentifier = InstanceIdentifier.builder(TransportZones.class)
                    .child(TransportZone.class, new TransportZoneKey(transportZone1))
                    .child(Vteps.class, new VtepsKey(dpId1)).build();
    private final InstanceIdentifier<Vteps> vtepsIdentifierNew = InstanceIdentifier.builder(TransportZones.class)
                    .child(TransportZone
                    .class, new TransportZoneKey(transportZone1))
                    .child(Vteps.class, new VtepsKey(dpId2)).build();
    private final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
            .rev140508.interfaces.state.Interface>
            interfaceIdentifier = ItmUtils.buildStateInterfaceId(tunnelInterfaceName);
    private final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
            .rev140508.interfaces.Interface>
            interfaceIdentifierNew = ItmUtils.buildId(tunnelInterfaceName);

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ItmConfig itmConfig;
    @Mock
    private ReadOnlyTransaction mockReadTx;
    @Mock
    private WriteTransaction mockWriteTx;

    private Optional<TransportZones> optionalTransportZones;

    private TepCommandHelper tepCommandHelper ;
    private UnprocessedTunnelsStateCache unprocessedTunnelsStateCache;

    @Before
    public void setUp() {
        setupMocks();

        Optional<TransportZone> optionalTransportZone = Optional.of(transportZone);
        optionalTransportZones = Optional.of(transportZones);
        Optional<TunnelMonitorInterval> optionalTunnelMonitorInterval = Optional.of(tunnelMonitorInterval);
        Optional<TunnelMonitorParams> optionalTunnelMonitorParams = Optional.of(tunnelMonitorParams);
        Optional<Vteps> optionalVteps = Optional.of(vteps);
        Optional<Interface> ifStateOptional = Optional.of(interfaceTest);
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
                .Interface>
                ifStateOptionalNew = Optional.of(interfaceTestNew);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZone)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalTransportZones)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalTunnelMonitorInterval)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorIntervalIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalTunnelMonitorParams)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorParamsIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalVteps)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION,vtepsIdentifier);
        doReturn(Futures.immediateCheckedFuture(ifStateOptional)).when(mockReadTx)
                .read(LogicalDatastoreType.OPERATIONAL,interfaceIdentifier);
        doReturn(Futures.immediateCheckedFuture(ifStateOptionalNew)).when(mockReadTx)
                .read(LogicalDatastoreType.CONFIGURATION,interfaceIdentifierNew);

        unprocessedTunnelsStateCache = new UnprocessedTunnelsStateCache();
        tepCommandHelper = new TepCommandHelper(dataBroker, itmConfig,
                unprocessedTunnelsStateCache);

    }

    @After
    public void cleanUp() {
    }

    private void setupMocks() {

        System.setOut(new PrintStream(outContent));
        instanceIdentifierList.add(transportZoneIdentifier);
        instanceIdentifierList.add(vtepsIdentifier);
        DeviceVteps deviceVteps = new DeviceVtepsBuilder().setIpAddress(ipAddress1)
                .withKey(new DeviceVtepsKey(ipAddress1, sourceDevice))
                .setNodeId(sourceDevice).setTopologyId(destinationDevice).build();
        vteps = new VtepsBuilder().setDpnId(dpId2)
                .setIpAddress(ipAddress1).withKey(new VtepsKey(dpId2)).build();
        vtepsTest = new VtepsBuilder().build();
        deviceVtepsList.add(deviceVteps);
        vtepsList.add(vteps);
        transportZone = new TransportZoneBuilder().setZoneName(transportZone1).setTunnelType(tunnelType1).withKey(new
                TransportZoneKey(transportZone1)).build();
        transportZoneNew = new TransportZoneBuilder().setZoneName(transportZone1).setTunnelType(tunnelType2).withKey(new
                TransportZoneKey(transportZone1)).build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
        transportZonesNew = new TransportZonesBuilder().setTransportZone(transportZoneListNew).build();
        tunnelMonitorInterval = new TunnelMonitorIntervalBuilder().setInterval(10000).build();
        tunnelMonitorParams = new TunnelMonitorParamsBuilder().setEnabled(true).build();
        InternalTunnel internalTunnelTest = new InternalTunnelBuilder().setSourceDPN(dpId1).setDestinationDPN(dpId2)
                .setTunnelInterfaceNames(Collections.singletonList(tunnelInterfaceName))
                .withKey(new InternalTunnelKey(dpId1, dpId2, tunnelType1)).setTransportType(tunnelType1).build();
        internalTunnelList.add(internalTunnelTest);
        StateTunnelList stateTunnelListTest = new StateTunnelListBuilder().setTunnelInterfaceName(tunnelInterfaceName)
                .setOperState(TunnelOperStatus.Up).build();
        stateTunnelList.add(stateTunnelListTest);
        lowerLayerIfList.add(dpId1.toString());
        interfaceTest = new InterfaceBuilder().setOperStatus(Interface.OperStatus.Up)
                .setAdminStatus(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                        .interfaces.state.Interface.AdminStatus.Up)
                .setPhysAddress(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715
                        .PhysAddress.getDefaultInstance("AA:AA:AA:AA:AA:AA"))
                .setIfIndex(100).setLowerLayerIf(lowerLayerIfList).setType(L2vlan.class).build();
        interfaceTestNew = ItmUtils.buildTunnelInterface(dpId1, tunnelInterfaceName, destinationDevice, enabled,
                TunnelTypeVxlan.class, ipAddress1, ipAddress2, gtwyIp1, vlanId, true, enabled,monitorProtocol,
                interval, false, null);
        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockWriteTx).when(dataBroker).newWriteOnlyTransaction();
        doReturn(Futures.immediateCheckedFuture(null)).when(mockWriteTx).submit();
        doReturn(FluentFuture.from(Futures.immediateCheckedFuture(CommitInfo.empty()))).when(mockWriteTx).commit();
    }

    @Test
    public void testCreateLocalCacheTzonesEmpty() {

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testCreateLocalCacheWithoutcheckExistingSubnet() {

        IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
        IpPrefix subnetMaskObj = ipPrefixTest;
        List<Vteps> vtepListTemp = new ArrayList<>();
        transportZoneNew = new TransportZoneBuilder().setZoneName(transportZone1).setTunnelType(tunnelType2).build();

        doReturn(Futures.immediateCheckedFuture(Optional.of(transportZoneNew))).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
            tepCommandHelper.createLocalCache(dpId2,portName1,vlanId, tepIp1,subnetMask,gwyIp1, transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testCreateLocalCacheWithcheckExistingSubnet() {

        transportZoneNew = new TransportZoneBuilder().setZoneName(transportZone1).setTunnelType(tunnelType2).build();

        Optional<TransportZone> optionalTransportZone = Optional.of(transportZoneNew);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZone)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);


        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
            tepCommandHelper.createLocalCache(dpId2,portName1,vlanId, tepIp1,subnetMask,gwyIp2, transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Test
    public void testCreateLocalCacheInvalidIp() {

        String output = null;
        try {
            tepCommandHelper.createLocalCache(dpId1, portName1, vlanId, tepIp3, subnetMask, gwyIp1,
                    transportZone1, null);
        } catch (Exception e) {
            output = e.getMessage();
        }
        assertEquals("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255", output);
    }

    @Test
    public void testCreateLocalCacheGtwyIpNull() {

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,null,transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        LOG.debug("gateway is null");
    }

    @Test
    public void testCreateLocalCacheInvalidSubnetMask() {

        String output = null;
        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,tepIp2,gwyIp1,transportZone1, null);
        } catch (TepException e) {
            output = e.getMessage();
        }

        assertEquals("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32", output);

    }

    @Test
    public void testCreateLocalCadcheMismatchIpwithSubnet() {

        String output = null;
        try {
            tepCommandHelper.createLocalCache(dpId1, portName1, vlanId, tepIp4, subnetMask, gwyIp1,
                    transportZone1, null);
        } catch (TepException e) {
            output = e.getMessage();
        }
        assertEquals("IpAddress and gateWayIp should belong to the subnet provided", output);
    }

    @Test
    public void testConfigureTunnelType() throws ExecutionException, InterruptedException {
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);

        tepCommandHelper.configureTunnelType(transportZone1,"VXLAN");

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).put(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZones,true);
    }

    @Test
    public void testConfigureTunnelMonitorInterval() {

        TunnelMonitorInterval tunnelMonitor = new TunnelMonitorIntervalBuilder().setInterval(interval).build();

        tepCommandHelper.configureTunnelMonitorInterval(interval);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorIntervalIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,tunnelMonitorIntervalIdentifier,
                tunnelMonitor,true);
    }

    @Test
    public void testConfigureTunnelMonitorParams() {

        TunnelMonitorParams tunnelMonitor = new TunnelMonitorParamsBuilder().setEnabled(enabled)
                .setMonitorProtocol(monitorProtocol).build();

        tepCommandHelper.configureTunnelMonitorParams(enabled, "BFD");

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorParamsIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,tunnelMonitorParamsIdentifier,tunnelMonitor,true);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Test
    public void testDeleteVtep() {

        try {
            tepCommandHelper.deleteVtep(dpId1, tepIp1, subnetMask, gwyIp1, transportZone1, null);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,vtepsIdentifier);
    }

    @Test
    public void testDeleteVtepInvalidIp() {

        String output = null;
        try {
            tepCommandHelper.deleteVtep(dpId1, tepIp3, subnetMask, gwyIp1, transportZone1, null);
        } catch (TepException e) {
            output = e.getMessage();
        }

        assertEquals("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255", output);

    }

    @Test
    public void testDeleteVtepInvalidSubnetMask() {

        String output = null;
        try {
            tepCommandHelper.deleteVtep(dpId1, tepIp1, tepIp1, gwyIp1, transportZone1, null);
        } catch (TepException e) {
            output = e.getMessage();
        }

        assertEquals("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32", output);

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Test
    public void testDeleteVtepGatewayIpNull() {

        try {
            tepCommandHelper.deleteVtep(dpId1, tepIp1, subnetMask, null, transportZone1, null);
        }  catch (Exception e) {
            LOG.error(e.getMessage());
        }

        LOG.debug("gateway is null in deleteVtep");

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Test
    public void testDeleteVtepIpSubnetMismatch() {

        String output = null;
        try {
            tepCommandHelper.deleteVtep(dpId1, tepIp4, subnetMask, gwyIp1, transportZone1, null);
        } catch (Exception e) {
            output = e.getMessage();
        }

        assertEquals("IpAddress and gateWayIp should belong to the subnet provided", output);

    }

    @Test
    public void testBuildTepsTunnelTypeVxlan() {

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.buildTeps();

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZonesNew,true);

    }

    @Test
    public void testBuildTepsTunnelTypeGre() {

        doReturn(Futures.immediateCheckedFuture(Optional.of(transportZoneNew))).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION, transportZoneIdentifier);

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.buildTeps();

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZonesNew,true);

    }


    @Test
    public void testBuildTepsTransportZoneAbsent() throws TepException {

        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);

        tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
        tepCommandHelper.buildTeps();

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZonesNew,true);

    }

    @Test
    public void testShowTepsWithTransportZone() {

        try {
            tepCommandHelper.showTeps(enabled, interval, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);

    }

    @Test
    public void testShowTepsWithoutTransportZone() {

        optionalTransportZones = Optional.of(transportZonesNew);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZones)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        String output = null;
        try {
            tepCommandHelper.showTeps(enabled, interval, null);
        } catch (TepException e) {
            output = e.getMessage();
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);
        assertEquals("No teps configured",output);

    }

    @Test
    public void testDeleteOnCommit() {

        transportZoneList.add(transportZone);
        transportZoneList.add(transportZoneNew);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
        optionalTransportZones = Optional.of(transportZones);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZones)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        try {
            tepCommandHelper.deleteVtep(dpId1, tepIp1, subnetMask, gwyIp1, transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.deleteOnCommit();

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);
        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,vtepsIdentifier);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,vtepsIdentifierNew);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testGetTransportZone() {

        tepCommandHelper.getTransportZone(transportZone1);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
    }

    @Test
    public void testIsInCache() {

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.isInCache(dpId1,tepIp1,subnetMask,gwyIp1,transportZone1, null);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testValidateForDuplicates() {

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);

    }

    @Test
    public void testCheckTepPerTzPerDpn() {

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.checkTepPerTzPerDpn(transportZone1,dpId2);

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

}
