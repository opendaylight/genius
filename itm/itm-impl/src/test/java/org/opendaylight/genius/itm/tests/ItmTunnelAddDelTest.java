/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.*;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.confighelpers.*;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.genius.itm.tests.xtend.ExpectedExternalTunnelObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedInternalTunnelIdentifierObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedDpnEndPointObjects;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

/**
 * Tests for ITM Tunnel Add Delete.
 */
public class ItmTunnelAddDelTest {

    public
    @Rule
    MethodRule guice = new GuiceRule(ItmTestModule.class);

    @Inject
    DataBroker dataBroker;
    @Inject
    IdManagerService idManagerService;
    @Inject
    IMdsalApiManager mdsalApiManager;

    private ListenableFuture<Void> futureObj;
    private TransportZone transportZone;
    private HwVtep hwVtep1;
    private DeviceVteps deviceVteps1, deviceVteps2;
    private Subnets subnets;
    private Vteps vteps;
    private DPNTEPsInfo dpntePsInfoGre, dpntePsInfoVxlan;
    private Interface ifaceForDpnToExtrEndPoint, extTunnelIf1, hwTunnelIf2, extTunnelIf3, hwTunnelIf4, hwTunnelIf5, hwTunnelIf6;
    private TunnelEndPoints tunnelEndPointsGre, tunnelEndPointsVxlan, tunnelEndPointForDpnToExtrEndPoint, tunnelEndPointHwVteps;
    private ExternalTunnel externalTunnelForDpnToExtrEndPoint, externalTunnel1, externalTunnel2, externalTunnel3, externalTunnel4, externalTunnel5, externalTunnel6;

    private InstanceIdentifier<TunnelEndPoints> tunnelEndPointsIdentifierGre, tunnelEndPointsIdentifierVxlan;
    private InstanceIdentifier<Interface> interfaceIdentifierForDpnToExtrEndPoint, ifIID1, ifIID2, ifIID3, ifIID4, ifIID5, ifIID6;
    private InstanceIdentifier<InternalTunnel> internalTunnelIdentifierGre1, internalTunnelIdentifierGre2, internalTunnelIdentifierVxlan1, internalTunnelIdentifierVxlan2;
    private InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier, externalTunnelIdentifier1, externalTunnelIdentifier2, externalTunnelIdentifier3, externalTunnelIdentifier4,
            externalTunnelIdentifier5, externalTunnelIdentifier6;


    private List<DPNTEPsInfo> dpnTepsInfoList, cfgdDpnListHwVtep;
    private List<HwVtep> cfgdHwVtepsList;

    private List<Vteps> vtepsList = new ArrayList<>();
    private List<Subnets> subnetsList = new ArrayList<>();
    private List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    private List<DPNTEPsInfo> cfgdDpnListGre = new ArrayList<>();
    private List<DPNTEPsInfo> meshDpnListGre = new ArrayList<>();
    private List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>();
    private List<DPNTEPsInfo> meshDpnListVxlan = new ArrayList<>();
    private List<BigInteger> bigIntegerList = new ArrayList<>();

    private ItmExternalTunnelAddWorker itmExternalTunnelAddWorker = new ItmExternalTunnelAddWorker();
    private ItmExternalTunnelDeleteWorker itmExternalTunnelDeleteWorker = new ItmExternalTunnelDeleteWorker();
    private ItmInternalTunnelAddWorker itmInternalTunnelAddWorker = new ItmInternalTunnelAddWorker();
    private ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker();
    private InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier = InstanceIdentifier.builder(DpnEndpoints.class).build();

    private String trunkInterfaceNameForDpnToExtrEndPoint = ItmTestConstants.tunnelInterfaceName3;

    @Before
    public void start() throws InterruptedException {
        //Common for External tunnel test setup
        dpnTepsInfoList = ItmTestUtil.getDpnList(ItmTestConstants.dpId1, ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress3,
                ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName, ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipPrefixTest);
        externalTunnelIdentifier = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(ItmTestConstants.ipAddress2.toString(), ItmTestConstants.dpId1.toString(), ItmTestConstants.TUNNEL_TYPE_VXLAN));
        bigIntegerList.add(ItmTestConstants.dpId1);
        tunnelEndPointForDpnToExtrEndPoint = ItmTestUtil.getTunnelEndPoint(ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress3,
                ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName, ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipPrefixTest);
        ifaceForDpnToExtrEndPoint = ItmUtils.buildTunnelInterface(ItmTestConstants.dpId1, trunkInterfaceNameForDpnToExtrEndPoint, ItmTestUtil.getFormattedString(ItmTestConstants.TUNNEL_TYPE_VXLAN),
                true, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipAddress3
                , ItmTestConstants.ipAddress2, ItmTestConstants.gtwyIp1, tunnelEndPointForDpnToExtrEndPoint.getVLANID(), false, false, ItmTestConstants.monitorProtocol, null, false);
        interfaceIdentifierForDpnToExtrEndPoint = ItmUtils.buildId(trunkInterfaceNameForDpnToExtrEndPoint);
        externalTunnelForDpnToExtrEndPoint = ItmUtils.buildExternalTunnel(ItmTestConstants.dpId1.toString(), ItmTestConstants.ipAddress2.toString(), ItmTestConstants.TUNNEL_TYPE_VXLAN,
                trunkInterfaceNameForDpnToExtrEndPoint);

        //hwvtep tunnel test setup
        tunnelEndPointHwVteps = ItmTestUtil.getTunnelEndPoint(ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress3,
                ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName, ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipPrefixTest);
        externalTunnel1 = ItmUtils.buildExternalTunnel(ItmTestUtil.getExternalTunnelKey(ItmTestConstants.dpId1.toString()),
                ItmTestUtil.getExternalTunnelKey(ItmTestConstants.source), ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.tunnelInterfaceName1);
        externalTunnel2 = ItmUtils.buildExternalTunnel(ItmTestUtil.getExternalTunnelKey(ItmTestConstants.source),
                ItmTestUtil.getExternalTunnelKey(ItmTestConstants.dpId1.toString()), ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.tunnelInterfaceName2);
        externalTunnel3 = ItmUtils.buildExternalTunnel(ItmTestUtil.getExternalTunnelKey(ItmTestConstants.dpId1.toString()),
                ItmTestUtil.getExternalTunnelKey(ItmTestConstants.destination), ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.tunnelInterfaceName3);
        externalTunnel4 = ItmUtils.buildExternalTunnel(ItmTestUtil.getExternalTunnelKey(ItmTestConstants.destination),
                ItmTestUtil.getExternalTunnelKey(ItmTestConstants.dpId1.toString()), ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.tunnelInterfaceName4);
        externalTunnel5 = ItmUtils.buildExternalTunnel(ItmTestUtil.getExternalTunnelKey(ItmTestConstants.source),
                ItmTestUtil.getExternalTunnelKey(ItmTestConstants.destination), ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.tunnelInterfaceName5);
        externalTunnel6 = ItmUtils.buildExternalTunnel(ItmTestUtil.getExternalTunnelKey(ItmTestConstants.destination),
                ItmTestUtil.getExternalTunnelKey(ItmTestConstants.source), ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.tunnelInterfaceName6);
        cfgdDpnListHwVtep = ItmTestUtil.getDpnList(ItmTestConstants.dpId1, ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress3,
                ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName, ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipPrefixTest);
        cfgdHwVtepsList = ItmTestUtil.getHwVtepsList(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress1, ItmTestConstants.TUNNEL_TYPE_VXLAN,
                ItmTestConstants.vlanId, ItmTestConstants.source, ItmTestConstants.ipPrefixTest);
        ifIID1 = ItmTestUtil.getInterfaceByKey(ItmTestConstants.tunnelInterfaceName1);
        ifIID2 = ItmTestUtil.getInterfaceByKey(ItmTestConstants.tunnelInterfaceName2);
        ifIID3 = ItmTestUtil.getInterfaceByKey(ItmTestConstants.tunnelInterfaceName3);
        ifIID4 = ItmTestUtil.getInterfaceByKey(ItmTestConstants.tunnelInterfaceName4);
        ifIID5 = ItmTestUtil.getInterfaceByKey(ItmTestConstants.tunnelInterfaceName5);
        ifIID6 = ItmTestUtil.getInterfaceByKey(ItmTestConstants.tunnelInterfaceName6);

        externalTunnelIdentifier1 = ItmTestUtil.getExternalTunnel(ItmTestConstants.source, ItmTestConstants.dpId1.toString(), ItmTestConstants.TUNNEL_TYPE_VXLAN);
        externalTunnelIdentifier2 = ItmTestUtil.getExternalTunnel(ItmTestConstants.dpId1.toString(), ItmTestConstants.source, ItmTestConstants.TUNNEL_TYPE_VXLAN);
        externalTunnelIdentifier3 = ItmTestUtil.getExternalTunnel(ItmTestConstants.destination, ItmTestConstants.dpId1.toString(), ItmTestConstants.TUNNEL_TYPE_VXLAN);
        externalTunnelIdentifier4 = ItmTestUtil.getExternalTunnel(ItmTestConstants.dpId1.toString(), ItmTestConstants.destination, ItmTestConstants.TUNNEL_TYPE_VXLAN);
        externalTunnelIdentifier5 = ItmTestUtil.getExternalTunnel(ItmTestConstants.destination, ItmTestConstants.source, ItmTestConstants.TUNNEL_TYPE_VXLAN);
        externalTunnelIdentifier6 = ItmTestUtil.getExternalTunnel(ItmTestConstants.source, ItmTestConstants.destination, ItmTestConstants.TUNNEL_TYPE_VXLAN);

        extTunnelIf1 = ItmUtils.buildTunnelInterface(ItmTestConstants.dpId1, ItmTestConstants.tunnelInterfaceName1, ItmTestUtil.getFormattedString(ItmTestConstants.TUNNEL_TYPE_VXLAN),
                true, ItmTestConstants.TUNNEL_TYPE_VXLAN, tunnelEndPointHwVteps.getIpAddress(), ItmTestConstants.ipAddress1,
                ItmTestConstants.gtwyIp1, ItmTestConstants.vlanId, false, false, ItmTestConstants.monitorProtocol, ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL, false);

        hwTunnelIf2 = ItmUtils.buildHwTunnelInterface(ItmTestConstants.tunnelInterfaceName2, ItmTestUtil.getFormattedString(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                , true, ItmTestUtil.getHwVtep(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress1, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.vlanId,
                        ItmTestConstants.source, ItmTestConstants.ipPrefixTest).getTopo_id(), ItmTestUtil.getHwVtep(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress1,
                        ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.vlanId, ItmTestConstants.source, ItmTestConstants.ipPrefixTest).getNode_id(), ItmTestConstants.TUNNEL_TYPE_VXLAN,
                ItmTestConstants.ipAddress1, ItmTestConstants.ipAddress3, ItmTestConstants.gtwyIp1, false, ItmTestConstants.monitorProtocol, ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);

        extTunnelIf3 = ItmUtils.buildTunnelInterface(ItmTestConstants.dpId1, ItmTestConstants.tunnelInterfaceName3, ItmTestUtil.getFormattedString(ItmTestConstants.TUNNEL_TYPE_VXLAN),
                true, ItmTestConstants.TUNNEL_TYPE_VXLAN, tunnelEndPointHwVteps.getIpAddress(), ItmTestConstants.ipAddress2,
                ItmTestConstants.gtwyIp1, ItmTestConstants.vlanId, false, false, ItmTestConstants.monitorProtocol, ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL, false);

        hwTunnelIf4 = ItmUtils.buildHwTunnelInterface(ItmTestConstants.tunnelInterfaceName4, ItmTestUtil.getFormattedString(ItmTestConstants.TUNNEL_TYPE_VXLAN),
                true, ItmTestUtil.getHwVtep(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress1, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.vlanId,
                        ItmTestConstants.source, ItmTestConstants.ipPrefixTest).getTopo_id(), ItmTestConstants.destination, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipAddress2,
                ItmTestConstants.ipAddress3, ItmTestConstants.gtwyIp1, false, ItmTestConstants.monitorProtocol, ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);

        hwTunnelIf5 = ItmUtils.buildHwTunnelInterface(ItmTestConstants.tunnelInterfaceName5, ItmTestUtil.getFormattedString(ItmTestConstants.TUNNEL_TYPE_VXLAN),
                true, ItmTestUtil.getHwVtep(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress1, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.vlanId,
                        ItmTestConstants.source, ItmTestConstants.ipPrefixTest).getTopo_id(), ItmTestUtil.getHwVtep(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress1,
                        ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.vlanId, ItmTestConstants.source, ItmTestConstants.ipPrefixTest).getNode_id(), ItmTestConstants.TUNNEL_TYPE_VXLAN,
                ItmTestConstants.ipAddress1, ItmTestConstants.ipAddress2, ItmTestConstants.gtwyIp1, false, ItmTestConstants.monitorProtocol, ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);

        hwTunnelIf6 = ItmUtils.buildHwTunnelInterface(ItmTestConstants.tunnelInterfaceName6, ItmTestUtil.getFormattedString(ItmTestConstants.TUNNEL_TYPE_VXLAN),
                true, ItmTestUtil.getHwVtep(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress1, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.vlanId,
                        ItmTestConstants.source, ItmTestConstants.ipPrefixTest).getTopo_id(), ItmTestConstants.destination, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipAddress2,
                ItmTestConstants.ipAddress1, ItmTestConstants.gtwyIp1, false, ItmTestConstants.monitorProtocol, ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL);

        deviceVteps1 = ItmTestUtil.getDeviceVtepList(ItmTestConstants.ipAddress1, ItmTestConstants.TOPO_ID, ItmTestConstants.source);
        deviceVteps2 = ItmTestUtil.getDeviceVtepList(ItmTestConstants.ipAddress2, ItmTestConstants.TOPO_ID, ItmTestConstants.source);
        deviceVtepsList.add(deviceVteps1);
        deviceVtepsList.add(deviceVteps2);
        vteps = ItmTestUtil.getVtep(ItmTestConstants.dpId1, ItmTestConstants.ipAddress1, false, ItmTestConstants.portName1);
        vtepsList.add(vteps);
        subnets = ItmTestUtil.getSubnet(vtepsList, ItmTestConstants.gtwyIp1, ItmTestConstants.ipPrefixTest, ItmTestConstants.vlanId, deviceVtepsList);
        subnetsList.add(subnets);
        transportZone = ItmTestUtil.getTransportZone(ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.TZ_NAME, subnetsList);


        //internal tunnel gre setup
        dpntePsInfoGre = ItmTestUtil.getdpntePsInfo(ItmTestConstants.dpId1, ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress1, ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName,
                ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_GRE, ItmTestConstants.ipPrefixTest);
        cfgdDpnListGre.add(dpntePsInfoGre);
        meshDpnListGre = ItmTestUtil.getDpnList(ItmTestConstants.dpId2, ItmTestConstants.vlanId, ItmTestConstants.portName2, ItmTestConstants.ipAddress2,
                ItmTestConstants.gtwyIp2, ItmTestConstants.parentInterfaceName, ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_GRE, ItmTestConstants.ipPrefixTest);
        internalTunnelIdentifierGre1 = InstanceIdentifier.create(
                TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(ItmTestConstants.dpId2, ItmTestConstants.dpId1, ItmTestConstants.TUNNEL_TYPE_GRE));
        internalTunnelIdentifierGre2 = InstanceIdentifier.create(
                TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(ItmTestConstants.dpId1, ItmTestConstants.dpId2, ItmTestConstants.TUNNEL_TYPE_GRE));
        tunnelEndPointsGre = ItmTestUtil.getTunnelEndPoint(ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress1, ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName,
                ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_GRE, ItmTestConstants.ipPrefixTest);
        tunnelEndPointsIdentifierGre =
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, dpntePsInfoGre.getKey())
                        .child(TunnelEndPoints.class, tunnelEndPointsGre.getKey()).build();

        //Internal tunnel vxlan setup
        dpntePsInfoVxlan = ItmTestUtil.getdpntePsInfo(ItmTestConstants.dpId1, ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress1, ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName,
                ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipPrefixTest);
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        meshDpnListVxlan = ItmTestUtil.getDpnList(ItmTestConstants.dpId2, ItmTestConstants.vlanId, ItmTestConstants.portName2, ItmTestConstants.ipAddress2,
                ItmTestConstants.gtwyIp2, ItmTestConstants.parentInterfaceName, ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipPrefixTest);
        internalTunnelIdentifierVxlan1 = InstanceIdentifier.create(
                TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(ItmTestConstants.dpId2, ItmTestConstants.dpId1, ItmTestConstants.TUNNEL_TYPE_VXLAN));
        internalTunnelIdentifierVxlan2 = InstanceIdentifier.create(
                TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(ItmTestConstants.dpId1, ItmTestConstants.dpId2, ItmTestConstants.TUNNEL_TYPE_VXLAN));
        tunnelEndPointsVxlan = ItmTestUtil.getTunnelEndPoint(ItmTestConstants.vlanId, ItmTestConstants.portName1, ItmTestConstants.ipAddress1, ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName,
                ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.ipPrefixTest);
        tunnelEndPointsIdentifierVxlan =
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, dpntePsInfoVxlan.getKey())
                        .child(TunnelEndPoints.class, tunnelEndPointsVxlan.getKey()).build();

    }

    @Test
    public void testBuildDeleteTunnelsToExternalEndPoint() throws Exception {
        //build tunnel
        List<ListenableFuture<Void>> futures = itmExternalTunnelAddWorker.buildTunnelsToExternalEndPoint(dataBroker, idManagerService, dpnTepsInfoList,
                ItmTestConstants.ipAddress2, ItmTestConstants.TUNNEL_TYPE_VXLAN);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }

        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier).checkedGet().get());

        //delete tunnel
        futures = itmExternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService, dpnTepsInfoList, ItmTestConstants.ipAddress2, ItmTestConstants.TUNNEL_TYPE_VXLAN);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier).get());

    }

    @Test
    public void testBuildTunnelsFromDpnToExternalEndPoint() throws Exception {
        //build tunnel
        List<ListenableFuture<Void>> futures = itmExternalTunnelAddWorker.buildTunnelsFromDpnToExternalEndPoint(dataBroker, idManagerService, bigIntegerList,
                ItmTestConstants.ipAddress2, ItmTestConstants.TUNNEL_TYPE_VXLAN);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }
        futureObj = ItmTestUtil.writeInterfaceConfig(interfaceIdentifierForDpnToExtrEndPoint, ifaceForDpnToExtrEndPoint, dataBroker);
        futureObj.get();

        Assert.assertEquals(ItmTestConstants.tunnelInterfaceName3,
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        interfaceIdentifierForDpnToExtrEndPoint).checkedGet().get().getName());


        futureObj = ItmTestUtil.writeExternalTunnelConfig(externalTunnelIdentifier, externalTunnelForDpnToExtrEndPoint, dataBroker);
        futureObj.get();

        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier).checkedGet().get());

    }

    @Test
    public void testBuildDeleteHwVtepsTunnels() throws Exception {
        //build tunnels
        List<ListenableFuture<Void>> futures = itmExternalTunnelAddWorker.buildHwVtepsTunnels(dataBroker, idManagerService, cfgdDpnListHwVtep, null);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }

        futures = itmExternalTunnelAddWorker.buildHwVtepsTunnels(dataBroker, idManagerService, null, cfgdHwVtepsList);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }


        futureObj = ItmTestUtil.writeInterfaceConfig(ifIID1, extTunnelIf1, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeInterfaceConfig(ifIID2, hwTunnelIf2, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeInterfaceConfig(ifIID3, extTunnelIf3, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeInterfaceConfig(ifIID4, hwTunnelIf4, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeInterfaceConfig(ifIID5, hwTunnelIf5, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeInterfaceConfig(ifIID6, hwTunnelIf6, dataBroker);
        futureObj.get();

        futureObj = ItmTestUtil.writeExternalTunnelConfig(externalTunnelIdentifier1, externalTunnel1, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeExternalTunnelConfig(externalTunnelIdentifier2, externalTunnel2, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeExternalTunnelConfig(externalTunnelIdentifier3, externalTunnel3, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeExternalTunnelConfig(externalTunnelIdentifier4, externalTunnel4, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeExternalTunnelConfig(externalTunnelIdentifier5, externalTunnel5, dataBroker);
        futureObj.get();
        futureObj = ItmTestUtil.writeExternalTunnelConfig(externalTunnelIdentifier6, externalTunnel6, dataBroker);
        futureObj.get();

        Assert.assertEquals(ItmTestConstants.tunnelInterfaceName1, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, ifIID1).checkedGet().get().getName());
        Assert.assertEquals(ItmTestConstants.tunnelInterfaceName2, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, ifIID2).checkedGet().get().getName());
        Assert.assertEquals(ItmTestConstants.tunnelInterfaceName3, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, ifIID3).checkedGet().get().getName());
        Assert.assertEquals(ItmTestConstants.tunnelInterfaceName4, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, ifIID4).checkedGet().get().getName());
        Assert.assertEquals(ItmTestConstants.tunnelInterfaceName5, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, ifIID5).checkedGet().get().getName());
        Assert.assertEquals(ItmTestConstants.tunnelInterfaceName6, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, ifIID6).checkedGet().get().getName());

        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel1(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier1).checkedGet().get());
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel2(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier2).checkedGet().get());
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel3(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier3).checkedGet().get());
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel4(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier4).checkedGet().get());
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel5(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier5).checkedGet().get());
        assertEqualBeans(ExpectedExternalTunnelObjects.newExternalTunnel6(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier6).checkedGet().get());


        hwVtep1 = ItmTestUtil.getHwVtep(ItmTestConstants.TZ_NAME, ItmTestConstants.gtwyIp1, ItmTestConstants.ipAddress2, ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.vlanId,
                ItmTestConstants.destination, ItmTestConstants.ipPrefixTest);
        cfgdHwVtepsList.add(hwVtep1);

        //Delete
        futures = itmExternalTunnelDeleteWorker.deleteHwVtepsTunnels(dataBroker, idManagerService, ItmTestUtil.getDpnList(ItmTestConstants.dpId1, ItmTestConstants.vlanId,
                ItmTestConstants.portName1, ItmTestConstants.ipAddress3, ItmTestConstants.gtwyIp1, ItmTestConstants.parentInterfaceName, ItmTestConstants.TZ_NAME, ItmTestConstants.TUNNEL_TYPE_VXLAN,
                ItmTestConstants.ipPrefixTest), cfgdHwVtepsList, transportZone);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier1).get());
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier2).get());
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier3).get());
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier4).get());
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier5).get());
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier6).get());

    }

    @Test
    public void testBuildDeleteInternalTunnels_GREType() throws Exception {

        //Build tunnel
        List<ListenableFuture<Void>> futures = itmInternalTunnelAddWorker.build_all_tunnels(dataBroker, idManagerService, mdsalApiManager, cfgdDpnListGre, meshDpnListGre);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }
        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelIdentifierGreOneToTwo(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierGre1).checkedGet().get());

        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelIdentifierGreTwoToOne(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierGre2).checkedGet().get());

        assertEqualBeans(ExpectedDpnEndPointObjects.newDpnEndPointGreType(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        dpnEndpointsIdentifier).checkedGet().get());

        //Delete Tunnel
        futures = itmInternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService, mdsalApiManager, cfgdDpnListGre,
                meshDpnListGre);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }

        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        tunnelEndPointsIdentifierGre).get());
        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierGre1).get());
        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierGre2).get());
    }

    @Test
    public void testBuildDeleteInternalTunnels_VxLanType() throws Exception {
        //Build
        List<ListenableFuture<Void>> futures = itmInternalTunnelAddWorker.build_all_tunnels(dataBroker, idManagerService, mdsalApiManager, cfgdDpnListVxlan, meshDpnListVxlan);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }

        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelIdentifierVxLanOneToTwo(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierVxlan1).checkedGet().get());
        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelIdentifierVxLanTwoToOne(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierVxlan2).checkedGet().get());
        assertEqualBeans(ExpectedDpnEndPointObjects.newDpnEndPointVxLanType(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        dpnEndpointsIdentifier).checkedGet().get());

        //Delete
        futures = itmInternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService, mdsalApiManager, cfgdDpnListVxlan,
                meshDpnListVxlan);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }

        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        tunnelEndPointsIdentifierVxlan).get());
        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierVxlan1).get());
        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierVxlan2).get());
    }

    @Test
    public void testBuildAllInternalTunnels_BothType() throws Exception {

        List<ListenableFuture<Void>> futures = itmInternalTunnelAddWorker.build_all_tunnels(dataBroker, idManagerService, mdsalApiManager, cfgdDpnListVxlan, meshDpnListGre);
        for (ListenableFuture<Void> future : futures) {
            future.get();
        }

        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelIdentifierBothOneToTwo(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierVxlan1).checkedGet().get());
        assertEqualBeans(ExpectedInternalTunnelIdentifierObjects.newInternalTunnelIdentifierBothTwoToOne(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalTunnelIdentifierGre2).checkedGet().get());
        assertEqualBeans(ExpectedDpnEndPointObjects.newDpnEndPointVxLanType(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        dpnEndpointsIdentifier).checkedGet().get());


    }

}
