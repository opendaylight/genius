/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.cli;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
        .InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone
        .SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class TepCommandHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(TepCommandHelper.class);

    int vlanId = 100 ;
    int interval = 1000;
    Boolean enabled = false ;
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
    String tepIp1 = "192.168.56.30";
    String tepIp2 = "192.168.56.102";
    String tepIp3 = "168.56.102";
    String tepIp4 = "150.168.56.102";
    String gwyIp1 = "192.168.56.105";
    String gwyIp2 = "192.168.56.106";
    String subnetMask = "192.168.56.100/24";
    String tunnelInterfaceName =  "1:phy0:100" ;
    String sourceDevice = "hwvtep://192.168.101.30:6640/physicalswitch/s3";
    String newline = System.getProperty("line.separator");
    String destinationDevice = "hwvtep:1";
    String portName1 = "phy0";
    String transportZone1 = "TZA" ;
    BigInteger dpId1 = BigInteger.valueOf(1);
    BigInteger dpId2 = BigInteger.valueOf(2);
    IpAddress gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
    IpAddress ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
    IpAddress ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
    IpPrefix ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetMask);
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    TransportZone transportZone = null;
    TransportZone transportZoneNew = null;
    TransportZones transportZones = null;
    TransportZones transportZonesNew = null;
    TunnelMonitorInterval tunnelMonitorInterval = null;
    TunnelMonitorParams tunnelMonitorParams = null;
    Subnets subnetsTest = null;
    DeviceVteps deviceVteps = null;
    Vteps vteps = null;
    Vteps vtepsNew = null;
    Vteps vtepsTest = null;
    InternalTunnel internalTunnelTest = null;
    TunnelList tunnelList = null;
    TunnelList tunnelListTest = null;
    StateTunnelList stateTunnelListTest = null ;
    StateTunnelList stateTunnelTest = null ;
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface
            interfaceTest = null;
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
            interfaceTestNew = null;
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface
            interfaceTestNewCase = null;
    List<Subnets> subnetsList = new ArrayList<>() ;
    List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    List<Vteps> vtepsList = new ArrayList<>();
    List<TransportZone> transportZoneList = new ArrayList<>();
    List<TransportZone> transportZoneListNew = new ArrayList<>();
    List<InternalTunnel> internalTunnelList = new ArrayList<>();
    List<StateTunnelList> stateTunnelList = new ArrayList<>() ;
    List<String> lowerLayerIfList = new ArrayList<>();
    List<InstanceIdentifier> instanceIdentifierList = new ArrayList<>();
    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;
    java.lang.Class<? extends TunnelTypeBase> tunnelType2 = TunnelTypeGre.class;

    InstanceIdentifier<TransportZone> transportZoneIdentifier = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(transportZone1)).build();
    InstanceIdentifier<TransportZones> transportZonesIdentifier = InstanceIdentifier.builder(TransportZones.class).build();
    InstanceIdentifier<TunnelMonitorInterval> tunnelMonitorIntervalIdentifier = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
    InstanceIdentifier<TunnelMonitorParams> tunnelMonitorParamsIdentifier = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
    InstanceIdentifier<Vteps> vtepsIdentifier = InstanceIdentifier.builder(TransportZones.class).child(TransportZone.class, new
            TransportZoneKey(transportZone1))
            .child(Subnets.class, new SubnetsKey(ipPrefixTest)).child(Vteps.class, new VtepsKey(dpId1,portName1)).build();
    InstanceIdentifier<Vteps> vtepsIdentifierNew = InstanceIdentifier.builder(TransportZones.class).child(TransportZone
            .class, new TransportZoneKey(transportZone1))
            .child(Subnets.class, new SubnetsKey(ipPrefixTest)).child(Vteps.class, new VtepsKey(dpId2,portName1)).build();
    InstanceIdentifier<Subnets> subnetsIdentifier = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(transportZone1)).child(Subnets.class,
                    new SubnetsKey(ipPrefixTest)).build();
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
            .state.Interface> interfaceIdentifier = ItmUtils.buildStateInterfaceId(tunnelInterfaceName);
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
            .Interface> interfaceIdentifierNew = ItmUtils.buildId(tunnelInterfaceName);

    @Mock DataBroker dataBroker;
    @Mock ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock Map<String, Map<SubnetObject, List<Vteps>>> tZones;

    Optional<TransportZone> optionalTransportZone;
    Optional<TransportZones> optionalTransportZones;
    Optional<TunnelMonitorInterval> optionalTunnelMonitorInterval;
    Optional<TunnelMonitorParams> optionalTunnelMonitorParams;
    Optional<Vteps> optionalVteps;
    Optional<Subnets> optionalSubnets;
    Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
            .Interface> ifStateOptional;
    Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface>
            ifStateOptionalNew;

    TepCommandHelper tepCommandHelper ;

    @Before
    public void setUp() throws Exception {
        when(dataBroker.registerDataChangeListener(
                any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(DataChangeListener.class),
                any(AsyncDataBroker.DataChangeScope.class)))
                .thenReturn(dataChangeListenerRegistration);
        setupMocks();

        optionalTransportZone = Optional.of(transportZone);
        optionalTransportZones = Optional.of(transportZones);
        optionalTunnelMonitorInterval = Optional.of(tunnelMonitorInterval);
        optionalTunnelMonitorParams = Optional.of(tunnelMonitorParams);
        optionalVteps = Optional.of(vteps);
        optionalSubnets = Optional.of(subnetsTest);
        ifStateOptional = Optional.of(interfaceTest);
        ifStateOptionalNew = Optional.of(interfaceTestNew);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZone)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalTransportZones)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalTunnelMonitorInterval)).when(mockReadTx).read
                (LogicalDatastoreType.CONFIGURATION,tunnelMonitorIntervalIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalTunnelMonitorParams)).when(mockReadTx).read
                (LogicalDatastoreType.CONFIGURATION,tunnelMonitorParamsIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalVteps)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,vtepsIdentifier);
        doReturn(Futures.immediateCheckedFuture(optionalSubnets)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,subnetsIdentifier);
        doReturn(Futures.immediateCheckedFuture(ifStateOptional)).when(mockReadTx).read(LogicalDatastoreType
                .OPERATIONAL,interfaceIdentifier);
        doReturn(Futures.immediateCheckedFuture(ifStateOptionalNew)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,interfaceIdentifierNew);

        tepCommandHelper = new TepCommandHelper(dataBroker);

    }

    @After
    public void cleanUp() {
    }

    private void setupMocks() {

        System.setOut(new PrintStream(outContent));
        instanceIdentifierList.add(transportZoneIdentifier);
        instanceIdentifierList.add(vtepsIdentifier);
        instanceIdentifierList.add(subnetsIdentifier);
        deviceVteps = new DeviceVtepsBuilder().setIpAddress(ipAddress1).setKey(new DeviceVtepsKey(ipAddress1,sourceDevice))
                .setNodeId(sourceDevice).setTopologyId(destinationDevice).build();
        vteps = new VtepsBuilder().setPortname(portName1).setDpnId(dpId2).setIpAddress(ipAddress1).setKey(new
                VtepsKey(dpId2,portName1)).build();
        vtepsNew = new VtepsBuilder().setPortname(portName1).setDpnId(dpId1).setIpAddress(ipAddress1).setKey(new
                VtepsKey(dpId1,portName1)).build();
        vtepsTest = new VtepsBuilder().build();
        deviceVtepsList.add(deviceVteps);
        vtepsList.add(vteps);
        subnetsTest = new SubnetsBuilder().setGatewayIp(gtwyIp1).setVlanId(vlanId).setKey(new SubnetsKey(ipPrefixTest))
                .setDeviceVteps(deviceVtepsList).setVteps(vtepsList).build();
        subnetsList.add(subnetsTest);
        transportZone = new TransportZoneBuilder().setZoneName(transportZone1).setTunnelType(tunnelType1).setKey(new
                TransportZoneKey(transportZone1)).setSubnets(subnetsList).build();
        transportZoneNew = new TransportZoneBuilder().setZoneName(transportZone1).setTunnelType(tunnelType2).setKey(new
                TransportZoneKey(transportZone1)).setSubnets(subnetsList).build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
        transportZonesNew = new TransportZonesBuilder().setTransportZone(transportZoneListNew).build();
        tunnelMonitorInterval = new TunnelMonitorIntervalBuilder().setInterval(10000).build();
        tunnelMonitorParams = new TunnelMonitorParamsBuilder().setEnabled(true).build();
        internalTunnelTest = new InternalTunnelBuilder().setSourceDPN(dpId1).setDestinationDPN(dpId2)
                .setTunnelInterfaceName(tunnelInterfaceName).setKey(new InternalTunnelKey(dpId1,dpId2,tunnelType1))
                .setTransportType(tunnelType1).build();
        internalTunnelList.add(internalTunnelTest);
        stateTunnelListTest = new StateTunnelListBuilder().setTunnelInterfaceName(tunnelInterfaceName).setOperState(TunnelOperStatus.Up).build();
        stateTunnelList.add(stateTunnelListTest);
        tunnelList = new TunnelListBuilder().setInternalTunnel(internalTunnelList).build();
        tunnelListTest = new TunnelListBuilder().build();
        lowerLayerIfList.add(dpId1.toString());
        interfaceTest = new InterfaceBuilder().setOperStatus(Interface.OperStatus.Up).setAdminStatus
                (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up)
                .setPhysAddress(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress.getDefaultInstance("AA:AA:AA:AA:AA:AA"))
                .setIfIndex(100).setLowerLayerIf(lowerLayerIfList).setType(L2vlan.class).build();
        interfaceTestNew = ItmUtils.buildTunnelInterface(dpId1, tunnelInterfaceName, destinationDevice, enabled,
                TunnelTypeVxlan.class, ipAddress1, ipAddress2, gtwyIp1, vlanId, true, enabled,monitorProtocol, interval, false);
        interfaceTestNewCase = ItmUtils.buildTunnelInterface(dpId1, tunnelInterfaceName, destinationDevice, enabled,
                TunnelTypeGre.class, ipAddress1, ipAddress2, gtwyIp1, vlanId, true, enabled, monitorProtocol, interval, false);
        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockWriteTx).when(dataBroker).newWriteOnlyTransaction();
        doReturn(Futures.immediateCheckedFuture(null)).when(mockWriteTx).submit();
    }

    @Test
    public void testCreateLocalCacheTzonesEmpty(){

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testCreateLocalCacheWithoutcheckExistingSubnet(){

        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        IpPrefix subnetMaskObj = new IpPrefix(subnetMask.toCharArray());
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        SubnetObject subObCli = new SubnetObject(gatewayIpObj, subnetsKey, subnetMaskObj, vlanId);
        Map<SubnetObject, List<Vteps>> subVtepMapTemp = new HashMap<>();
        subVtepMapTemp.put(subObCli, vtepsList);
        transportZoneNew = new TransportZoneBuilder().setZoneName(transportZone1).setTunnelType(tunnelType2).build();

        Optional<TransportZone> optionalTransportZone = Optional.of(transportZoneNew);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZone)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
            tepCommandHelper.createLocalCache(dpId2,portName1,vlanId, tepIp1,subnetMask,gwyIp1, transportZone1,
                    false, null);
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
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
            tepCommandHelper.createLocalCache(dpId2,portName1,vlanId, tepIp1,subnetMask,gwyIp2, transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testCreateLocalCacheInvalidIp() {

        String output = null;
        try {
            tepCommandHelper.createLocalCache(dpId1, portName1, vlanId, tepIp3, subnetMask, gwyIp1, transportZone1,
                    false, null);
        } catch (Exception e) {
            output = e.getMessage() + newline;
        }

        assertEquals("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255" + newline,output);

    }

    @Test
    public void testCreateLocalCacheGtwyIpNull(){

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,null,transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }

        LOG.debug("gateway is null");

    }

    @Test
    public void testCreateLocalCacheInvalidSubnetMask(){

        String output = null;
        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,tepIp2,gwyIp1,transportZone1,
                    false, null);
        } catch (TepException e) {
            output = e.getMessage()+newline;
        }

        String newline = System.getProperty("line.separator");
        assertEquals("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32" + newline,output);

    }

    @Test
    public void testCreateLocalCadcheMismatchIpwithSubnet() {

        String output = null;
        try {
            tepCommandHelper.createLocalCache(dpId1, portName1, vlanId, tepIp4, subnetMask, gwyIp1, transportZone1,
                    false, null);
        } catch (TepException e) {
            output = e.getMessage() + newline;
        }

        assertEquals("IpAddress and gateWayIp should belong to the subnet provided" + newline,output);

    }

    @Test
    public void testConfigureTunnelType(){

        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);

        tepCommandHelper.configureTunnelType(transportZone1,"VXLAN");

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).put(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZones,true);

    }

    @Test
    public void testConfigureTunnelMonitorInterval(){

        TunnelMonitorInterval tunnelMonitor = new TunnelMonitorIntervalBuilder().setInterval(interval).build();

        tepCommandHelper.configureTunnelMonitorInterval(interval);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorIntervalIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,tunnelMonitorIntervalIdentifier,tunnelMonitor,true);

    }

    @Test
    public void testConfigureTunnelMonitorParams(){

        TunnelMonitorParams tunnelMonitor = new TunnelMonitorParamsBuilder().setEnabled(enabled).setMonitorProtocol(monitorProtocol).build();

        tepCommandHelper.configureTunnelMonitorParams(enabled, "BFD");

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorParamsIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,tunnelMonitorParamsIdentifier,tunnelMonitor,true);

    }

    @Test
    public void testDeleteVtep(){

        try {
            tepCommandHelper.deleteVtep(dpId1, portName1, vlanId, tepIp1, subnetMask, gwyIp1, transportZone1, null);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,vtepsIdentifier);
        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,subnetsIdentifier);

    }

    @Test
    public void testDeleteVtepInvalidIp(){

        String output = null;
        try {
            tepCommandHelper.deleteVtep(dpId1, portName1, vlanId, tepIp3, subnetMask, gwyIp1, transportZone1, null);
        }catch (TepException e) {
            output = e.getMessage() +newline;
        }

        String newline = System.getProperty("line.separator");
        assertEquals("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255" + newline,output);

    }

    @Test
    public void testDeleteVtepInvalidSubnetMask(){

        String output = null;
        try {
            tepCommandHelper.deleteVtep(dpId1, portName1, vlanId, tepIp1, tepIp1, gwyIp1, transportZone1, null);
        } catch (TepException e) {
            output = e.getMessage() + newline;
        }

        assertEquals("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32" + newline,output);

    }

    @Test
    public void testDeleteVtepGatewayIpNull(){

        try {
            tepCommandHelper.deleteVtep(dpId1, portName1, vlanId, tepIp1, subnetMask, null, transportZone1, null);
        }  catch (Exception e) {
            LOG.error(e.getMessage());
        }

        LOG.debug("gateway is null in deleteVtep");

    }

    @Test
    public void testDeleteVtepIpSubnetMismatch(){

        String output = null;
        try {
            tepCommandHelper.deleteVtep(dpId1, portName1, vlanId, tepIp4, subnetMask, gwyIp1, transportZone1, null);
        } catch (Exception e) {
            output = e.getMessage()+ newline;
        }

        assertEquals("IpAddress and gateWayIp should belong to the subnet provided" + newline,output);

    }

    @Test
    public void testBuildTepsTunnelTypeVxlan(){

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.buildTeps();

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZonesNew,true);

    }

    @Test
    public void testBuildTepsTunnelTypeGre(){

        optionalTransportZone = Optional.of(transportZoneNew);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZone)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.buildTeps();

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZonesNew,true);

    }


    @Test
    public void testBuildTepsTransportZoneAbsent() throws TepException {

        optionalTransportZone = Optional.absent();

        doReturn(Futures.immediateCheckedFuture(optionalTransportZone)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZoneIdentifier);

        tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                false, null);
        tepCommandHelper.buildTeps();

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
        verify(mockWriteTx).merge(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier,transportZonesNew,true);

    }

    @Test
    public void testShowTepsWithTransportZone(){

        try {
            tepCommandHelper.showTeps(enabled, interval, null);
        } catch (TepException e){
            LOG.error(e.getMessage());
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);

    }

    @Test
    public void testShowTepsWithoutTransportZone(){

        optionalTransportZones = Optional.of(transportZonesNew);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZones)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        String output = null;
        try {
            tepCommandHelper.showTeps(enabled, interval, null);
        } catch (TepException e) {
            output = e.getMessage() + newline;
        }

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);
        assertEquals("No teps configured" + newline,output);

    }

    @Test
    public void testDeleteOnCommit(){

        transportZoneList.add(transportZone);
        transportZoneList.add(transportZoneNew);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
        optionalTransportZones = Optional.of(transportZones);

        doReturn(Futures.immediateCheckedFuture(optionalTransportZones)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        try {
            tepCommandHelper.deleteVtep(dpId1, portName1, vlanId, tepIp1, subnetMask, gwyIp1, transportZone1, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.deleteOnCommit();

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);
        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,vtepsIdentifier);
        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,subnetsIdentifier);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,vtepsIdentifierNew);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,subnetsIdentifier);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testGetTransportZone(){

        tepCommandHelper.getTransportZone(transportZone1);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);
    }

    @Test
    public void testIsInCache(){

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.isInCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1, null);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

    @Test
    public void testValidateForDuplicates(){

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.validateForDuplicates(vtepsTest,transportZone1);

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZonesIdentifier);

    }

    @Test
    public void testCheckTepPerTzPerDpn(){

        try {
            tepCommandHelper.createLocalCache(dpId1,portName1,vlanId,tepIp1,subnetMask,gwyIp1,transportZone1,
                    false, null);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
        tepCommandHelper.checkTepPerTzPerDpn(transportZone1,dpId2);

        verify(mockReadTx, times(2)).read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier);

    }

}
