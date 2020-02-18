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

import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.interfaces.InterfaceManagerService;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsKey;
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
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

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
    Uint64 dpId1 = Uint64.ONE;
    Uint64 dpId2 = Uint64.valueOf(2);
    ExternalTunnel externalTunnel = null;
    ExternalTunnel externalTunnelNew = null;
    InternalTunnel internalTunnel = null;
    DpnEndpoints dpnEndpoints = null;
    DPNTEPsInfo dpntePsInfoVxlan = null;
    TunnelEndPoints tunnelEndPointsVxlan = null;
    Interface iface = null;
    TransportZones transportZones = null;
    TransportZone transportZone = null;
    DeviceVteps deviceVteps = null;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>() ;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TransportZone> transportZoneList = new ArrayList<>() ;
    List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    List<String> stringList = new ArrayList<>();
    List<Uint64> dpId1List = new ArrayList<>();
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
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;

    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(ipAddress1.stringValue(), dpId1.toString(),
                    TunnelTypeMplsOverGre.class));
    InstanceIdentifier<ExternalTunnel> externalTunnelIdentifierNew = InstanceIdentifier.create(ExternalTunnelList.class)
            .child(ExternalTunnel.class, new ExternalTunnelKey(ipAddress1.stringValue(), dpId1.toString(),
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
            .child(DeviceVteps.class, deviceVtepKey).build();

    @Mock DataBroker dataBroker;
    @Mock JobCoordinator jobCoordinator;
    @Mock ReadTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock IMdsalApiManager mdsalApiManager;
    @Mock ItmConfig itmConfig;
    @Mock IInterfaceManager interfaceManager;
    @Mock InterfaceManagerService interfaceManagerService;
    @Mock EntityOwnershipUtils entityOwnershipUtils;
    @Mock TypedReadWriteTransaction mockTypedReadWriteTx;

    ItmManagerRpcService itmManagerRpcService ;
    DirectTunnelUtils directTunnelUtils;
    UnprocessedNodeConnectorCache unprocessedNodeConnectorCache;
    UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;

    Optional<ExternalTunnel> externalTunnelOptional ;
    Optional<InternalTunnel> internalTunnelOptional;
    Optional<DpnEndpoints> dpnEndpointsOptional ;
    Optional<TransportZones> transportZonesOptional ;

    @Before
    public void setUp() {
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
        lenient().doReturn(Futures.immediateCheckedFuture(internalTunnelOptional)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION,internalTunnelIdentifierNew);
        lenient().doReturn(Futures.immediateCheckedFuture(dpnEndpointsOptional)).when(mockReadTx).read(
                LogicalDatastoreType.CONFIGURATION,dpnEndpointsIdentifier);
        doReturn(Futures.immediateCheckedFuture(transportZonesOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,transportZonesIdentifier);

        DPNTEPsInfoCache dpntePsInfoCache =
            new DPNTEPsInfoCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl()),
            directTunnelUtils, jobCoordinator, unprocessedNodeConnectorEndPointCache);
        DpnTepStateCache dpnTepStateCache =
            new DpnTepStateCache(dataBroker, jobCoordinator, new GuavaCacheProvider(new CacheManagersRegistryImpl()),
            directTunnelUtils, dpntePsInfoCache, unprocessedNodeConnectorCache, unprocessedNodeConnectorEndPointCache);
        TunnelStateCache tunnelStateCache =
                new TunnelStateCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl()));
        OvsBridgeRefEntryCache ovsBridgeRefEntryCache =
            new OvsBridgeRefEntryCache(dataBroker, new GuavaCacheProvider(new CacheManagersRegistryImpl()));

        itmManagerRpcService = new ItmManagerRpcService(dataBroker, mdsalApiManager, itmConfig,
            dpntePsInfoCache, interfaceManager, dpnTepStateCache, tunnelStateCache, interfaceManagerService,
            ovsBridgeRefEntryCache, directTunnelUtils);
    }

    @After
    public void cleanUp() {
    }

    private void setupMocks() {
        deviceVteps = new DeviceVtepsBuilder().setIpAddress(ipAddress1).withKey(new DeviceVtepsKey(ipAddress1,"abc"))
                .setNodeId(sourceDevice).setTopologyId(destinationDevice).build();
        deviceVtepsList.add(deviceVteps);
        stringList.add(sourceDevice);
        dpId1List.add(dpId1);
        stringList.add("def");
        trunkInterfaceName = ItmUtils.getTrunkInterfaceName(tunnelInterfaceName, ipAddress1.stringValue(),
            ipAddress1.stringValue(), tunnelType1.getName());
        interfaceIdentifier = ItmUtils.buildId(trunkInterfaceName);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder()
                .setIpAddress(ipAddress1).setInterfaceName(tunnelInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType1)
                .withKey(new TunnelEndPointsKey(ipAddress1,tunnelType1)).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId1).withKey(new DPNTEPsInfoKey(dpId1)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        dpnEndpoints = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();
        externalTunnel = new ExternalTunnelBuilder().setSourceDevice(sourceDevice)
                .setDestinationDevice(destinationDevice).setTransportType(tunnelType1)
                .setTunnelInterfaceName(tunnelInterfaceName)
                .withKey(new ExternalTunnelKey(destinationDevice,sourceDevice,tunnelType1)).build();
        internalTunnel = new InternalTunnelBuilder()
                .setTunnelInterfaceNames(Collections.singletonList(tunnelInterfaceName)).setDestinationDPN(dpId2)
                .setSourceDPN(dpId1).setTransportType(tunnelType1)
                .withKey(new InternalTunnelKey(dpId2, dpId1, tunnelType1)).build();
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
                tunnelEndPointsVxlan.getIpAddress(),ipAddress1, false, false,
                monitorProtocol,null, false, null);

        transportZone = new TransportZoneBuilder().setZoneName(transportZone1)
                .setTunnelType(tunnelType1).withKey(new TransportZoneKey(transportZone1))
                .build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockWriteTx).when(dataBroker).newWriteOnlyTransaction();
        lenient().doReturn(FluentFutures.immediateFluentFuture(null)).when(mockWriteTx).commit();
    }

    @Test
    public void testGetInternalOrExternalInterfaceNameExtTunnelPresent() {
        itmManagerRpcService.getInternalOrExternalInterfaceName(getInternalOrExternalInterfaceNameInput);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier);
    }

    @Ignore
    @Test
    public void testGetInternalOrExternalInterfaceNameExtTunnelAbsent() {
        doReturn(Futures.immediateCheckedFuture(Optional.empty())).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,externalTunnelIdentifier);

        itmManagerRpcService.getInternalOrExternalInterfaceName(getInternalOrExternalInterfaceNameInput);

        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier);
        verify(mockReadTx).read(LogicalDatastoreType.CONFIGURATION, internalTunnelIdentifierNew);
    }

    @Ignore
    @Test
    public void testAddExternalTunnelEndpoint() {
        externalTunnelNew = ItmUtils.buildExternalTunnel(dpId1.toString(), ipAddress1.stringValue(),
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
        externalTunnelNew = ItmUtils.buildExternalTunnel(dpId1.toString(), ipAddress1.stringValue(),
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
