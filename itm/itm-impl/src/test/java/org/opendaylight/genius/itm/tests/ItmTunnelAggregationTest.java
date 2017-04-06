/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.testutils.AsyncEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
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



public class ItmTunnelAggregationTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(new ItmTunnelAggregationTestModule(),
            new TestableDataTreeChangeListenerModule());

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
    TunnelEndPoints tunnelEndPointsVxlan = null;
    TunnelEndPoints tunnelEndPointsVxlanNew = null;
    TunnelMonitorParams tunnelMonitorParams = null;
    TunnelMonitorInterval tunnelMonitorInterval = null;
    InternalTunnel expectedInternalTunnel1 = null;
    InternalTunnel expectedInternalTunnel2 = null;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListVxlanNew = new ArrayList<>();
    List<DPNTEPsInfo> meshDpnListVxlan = new ArrayList<>() ;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<>() ;
    java.lang.Class<? extends TunnelTypeBase> tunnelType = TunnelTypeVxlan.class;

    InstanceIdentifier<InternalTunnel> internalTunnelIdentifierVxlan1 = InstanceIdentifier.create(
            TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId2, dpId1, tunnelType));
    InstanceIdentifier<InternalTunnel> internalTunnelIdentifierVxlan2 = InstanceIdentifier.create(
            TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId1, dpId2, tunnelType));

    AllocateIdOutput expectedId1 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("100")).build();
    AllocateIdOutput expectedId2 = new AllocateIdOutputBuilder().setIdValue(Long.valueOf("200")).build();

    Future<RpcResult<AllocateIdOutput>> idOutputOptional1;
    Future<RpcResult<AllocateIdOutput>> idOutputOptional2;

    @Inject DataBroker dataBroker;
    @Inject AsyncEventsWaiter asyncEventsWaiter;
    @Inject JobCoordinatorEventsWaiter coordinatorEventsWaiter;
    @Inject IMdsalApiManager mdsalApiManager;
    @Inject IdManagerService idManagerService;



    @Before
    public void setUp() throws Exception {

        setupMocks();

        idOutputOptional1 = RpcResultBuilder.success(expectedId1).buildFuture();
        idOutputOptional2 = RpcResultBuilder.success(expectedId2).buildFuture();
    }

    private void setupMocks() {

        ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
        ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
        ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
        gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
        gtwyIp2 = IpAddressBuilder.getDefaultInstance(gwyIp2);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName1)
                .setIpAddress(ipAddress1).setGwIpAddress(gtwyIp1).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType)
                .setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsVxlanNew = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName2)
                .setIpAddress(ipAddress2).setGwIpAddress(gtwyIp2).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone1)).setTunnelType(tunnelType)
                .setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        tunnelEndPointsListVxlanNew.add(tunnelEndPointsVxlanNew);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId1).setUp(true).setKey(new DPNTEPsInfoKey(dpId1))
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        dpntePsInfoVxlanNew = new DPNTEPsInfoBuilder().setDPNID(dpId2).setKey(new DPNTEPsInfoKey(dpId2)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlanNew).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        meshDpnListVxlan.add(dpntePsInfoVxlanNew);
    }

    @Test
    public void testBuild_all_tunnels_multipleVXLANtype() throws Exception {

        trunkInterfaceName1 = ItmUtils.getTrunkInterfaceName(idManagerService, parentInterfaceName, tepIp1, tepIp2,
                tunnelType.getName());
        trunkInterfaceName2 = ItmUtils.getTrunkInterfaceName(idManagerService, parentInterfaceName, tepIp2, tepIp1,
                tunnelType.getName());

        expectedInternalTunnel1 = ItmUtils.buildInternalTunnel(dpId1, dpId2, tunnelType, trunkInterfaceName1);
        expectedInternalTunnel2 = ItmUtils.buildInternalTunnel(dpId2, dpId1, tunnelType, trunkInterfaceName2);

        // check the tunnel aggregation configuration
        boolean isTunnelAggregationEnabled = ItmTunnelAggregationHelper.isTunnelAggregationEnabled();
        assertTrue(isTunnelAggregationEnabled);

        // build tunnels
        ItmInternalTunnelAddWorker.build_all_tunnels(dataBroker, idManagerService, mdsalApiManager,
                cfgdDpnListVxlan, meshDpnListVxlan);

        waitTillOperationCompletes();

        //check the regular tunnels created
        InternalTunnel internalTunnel1 =
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                                                         internalTunnelIdentifierVxlan1).checkedGet().get();
        assertEquals(expectedInternalTunnel1, internalTunnel1);

        InternalTunnel internalTunnel2 =
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                                                         internalTunnelIdentifierVxlan2).checkedGet().get();
        assertEquals(expectedInternalTunnel2, internalTunnel2);

        // check the logical tunnel groups are created
        java.lang.Class<? extends TunnelTypeBase> logicType = TunnelTypeLogicalGroup.class;

        InstanceIdentifier<InternalTunnel> internalLogicTunnelId1 = InstanceIdentifier.create(
                TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId2, dpId1, logicType));
        String logicTunnelName1 = ItmUtils.getLogicalTunnelGroupName(dpId1, dpId2);
        InternalTunnel expectedLogicTunnel1 = ItmUtils.buildInternalTunnel(dpId1, dpId2, logicType, logicTunnelName1);
        InternalTunnel logicInternalTunnel1 =
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalLogicTunnelId1).checkedGet().get();
        assertEquals(expectedLogicTunnel1, logicInternalTunnel1);

        InstanceIdentifier<InternalTunnel> internalLogicTunnelId2 = InstanceIdentifier.create(
                TunnelList.class).child(InternalTunnel.class, new InternalTunnelKey(dpId1, dpId2, logicType));
        String logicTunnelName2 = ItmUtils.getLogicalTunnelGroupName(dpId2, dpId1);
        InternalTunnel expectedLogicTunnel2 = ItmUtils.buildInternalTunnel(dpId2, dpId1, logicType, logicTunnelName2);
        InternalTunnel logicInternalTunnel2 =
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                        internalLogicTunnelId2).checkedGet().get();
        assertEquals(expectedLogicTunnel2, logicInternalTunnel2);

        // delete tunnels
        ItmInternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService, mdsalApiManager,
                cfgdDpnListVxlan, meshDpnListVxlan);
        waitTillOperationCompletes();

        //check the logical tunnel groups deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, internalLogicTunnelId1).get());

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, internalLogicTunnelId2).get());
    }

    public void waitTillOperationCompletes() {
        coordinatorEventsWaiter.awaitEventsConsumption();
        asyncEventsWaiter.awaitEventsConsumption();
    }
}
