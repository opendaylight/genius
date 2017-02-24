/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.cli.TepException;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.tests.xtend.ExpectedCliTestsObjects;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

/**
 * Tests for ITM CLI Cases.
 */
public class ItmCliTest {

    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class);

    @Inject DataBroker dataBroker;
    @Inject TepCommandHelper tepCommandHelper;

    private Vteps vteps;
    private Subnets subnets,subnetsGwNull;
    private TransportZone transportZone,transportZoneGwNull;

    private InstanceIdentifier<Vteps> vpath;
    private InstanceIdentifier<TransportZone> transportZoneIdentifier;
    private InstanceIdentifier<TunnelMonitorInterval> tunnelMonitorIntervalInstanceIdentifier;
    private InstanceIdentifier<TunnelMonitorParams> tunnelMonitorParamsInstanceIdentifier;

    private List<Subnets> subnetsList = new ArrayList<>();
    private List<Subnets> subnetsListGwNull = new ArrayList<>();
    private List<DeviceVteps> deviceVtepsList = new ArrayList<>();
    private List<Vteps> vtepsList = new ArrayList<>();

    @Before
    public void start() throws InterruptedException {
        tunnelMonitorIntervalInstanceIdentifier = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        tunnelMonitorParamsInstanceIdentifier = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        vteps = ItmTestUtil.getVtep(ItmTestConstants.dpId1, ItmTestConstants.ipAddress1,false, ItmTestConstants.portName1);
        vtepsList.add(vteps);
        subnets = ItmTestUtil.getSubnet(vtepsList, ItmTestConstants.gtwyIp1, ItmTestConstants.ipPrefixTest, ItmTestConstants.vlanId,deviceVtepsList);
        subnetsList.add(subnets);
        transportZone = ItmTestUtil.getTransportZone(ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.TZ_NAME,subnetsList);
        transportZoneIdentifier = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(ItmTestConstants.TZ_NAME)).build();
        vpath =(InstanceIdentifier<Vteps>) InstanceIdentifier.builder(TransportZones.class).child(TransportZone.class, transportZone.getKey())
                .child(Subnets.class, subnets.getKey()).child(Vteps.class, vteps.getKey()).build();
    }

    @Test
    public void testAddTzInLocalCache() throws Exception {
        tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);

        Assert.assertTrue(tepCommandHelper.isInCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null));
    }

    @Test
    public void testAddMultiTepInSameSubnetInLocalCache() throws Exception {
        tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
        tepCommandHelper.createLocalCache(ItmTestConstants.dpId2, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);

        Assert.assertTrue(tepCommandHelper.isInCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null));
        Assert.assertTrue(tepCommandHelper.isInCache(ItmTestConstants.dpId2, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null));

    }

    @Test
    public void testBuildLocalCacheWithInvalidIp() throws Exception{
        String errorOutput = null;
        try {
            tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp_invalid, ItmTestConstants.subnetMask,
                    ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
            fail("Expected an TepException to be thrown for invalid ip.");
        } catch (TepException e) {
            errorOutput = e.getMessage();
        }
        assertEquals(ITMConstants.INVALID_IP_ERROR_STR, errorOutput);

    }

    @Test
    public void testBuildLocalCacheWithNullGtwyIp() throws Exception{
        tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,null,
                ItmTestConstants.TZ_NAME, null);

        //When gw is null in creating cache, internally it
        // creates as 0.0.0.0, that's why asserting it with that.
        Assert.assertTrue(tepCommandHelper.isInCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwIpCliDef, ItmTestConstants.TZ_NAME, null));

    }

    @Test
    public void testBuildLocalCacheWithInvalidSubnetMask() throws Exception{
        String errorOutput = null;
        try {
            tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask_invalid,
                    ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
            fail("Expected an TepException to be thrown for invalid subnet mask.");
        } catch (TepException e) {
            errorOutput = e.getMessage();
        }
        assertEquals(ITMConstants.SUBNET_MASK_ERROR_STR, errorOutput);
    }

    @Test
    public void testBuildLocalCacheWithMismatchIpwithSubnet() throws Exception{
        String errorOutput = null;
        try {
            tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp4, ItmTestConstants.subnetMask,
                    ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
            fail("Expected an TepException to be thrown for mismatch of ip with subnet.");
        } catch (TepException e) {
            errorOutput = e.getMessage();
        }
        assertEquals(ITMConstants.MISMATCH_IPWITH_SUBNET_ERROR_STR, errorOutput);

    }

    @Test
    public void testConfigureTunnelType() throws Exception{
        tepCommandHelper.configureTunnelType(ItmTestConstants.TZ_NAME, ItmTestConstants.VXLAN_STR);

        InstanceIdentifier<TransportZone> transportZoneIdentifier = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(ItmTestConstants.TZ_NAME)).build();

        assertEqualBeans(ExpectedCliTestsObjects.newVxlanTransportZone(),  dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION,transportZoneIdentifier).checkedGet().get());

    }

    @Test
    public void testConfigureTunnelMonitorInterval() throws Exception{
        tepCommandHelper.configureTunnelMonitorInterval(ItmTestConstants.MON_INTERVAL);

        assertEqualBeans(ExpectedCliTestsObjects.newTunnelMonitorInterval(),dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorIntervalInstanceIdentifier).checkedGet().get());
    }


    @Test
    public void testConfigureTunnelMonitorParams() throws Exception{
        tepCommandHelper.configureTunnelMonitorParams(ItmTestConstants.ENABLE_TUN_MON, ItmTestConstants.TUNNEL_MON_TYPE_STR);

        TunnelMonitorParams tunnelMonitorParams=dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION,tunnelMonitorParamsInstanceIdentifier).checkedGet().get();
        assertEqualBeans(ExpectedCliTestsObjects.newTunnelMonitorParams(),tunnelMonitorParams);

    }

    @Test
    public void testDeleteVtepAndCommit() throws Exception{
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, transportZoneIdentifier, transportZone, dataBroker);

        assertEqualBeans(ExpectedCliTestsObjects.newVtep(),  dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION,vpath).checkedGet().get());

        tepCommandHelper.deleteVtep(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask, ItmTestConstants.gwyIp1,
                ItmTestConstants.TZ_NAME, null);
        tepCommandHelper.deleteOnCommit();

        Assert.assertEquals(Optional.absent(),  dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION,vpath).get());

    }

    @Test
    public void testDeleteVtepInvalidIp() throws Exception{
        String errorOutput = null;
        try {
            tepCommandHelper.deleteVtep(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp_invalid, ItmTestConstants.subnetMask,
                    ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
            fail("Expected an TepException to be thrown for invalid IP.");
        } catch (TepException e) {
            errorOutput = e.getMessage();
        }
        assertEquals(ITMConstants.INVALID_IP_ERROR_STR, errorOutput);

    }

    @Test
    public void testDeleteVtepInvalidSubnetMask() throws Exception{
        String errorOutput = null;
        try {
            tepCommandHelper.deleteVtep(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask_invalid,
                    ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
            fail("Expected an TepException to be thrown for delete vtep invalid subnet mask.");
        } catch (TepException e) {
            errorOutput = e.getMessage();
        }
        assertEquals(ITMConstants.SUBNET_MASK_ERROR_STR, errorOutput);

    }

    @Test
    public void testDeleteVtepGatewayIpNull() throws Exception{
        subnetsGwNull = ItmTestUtil.getSubnet(vtepsList, ItmTestConstants.gtwyIpDef, ItmTestConstants.ipPrefixTest, ItmTestConstants.vlanId,deviceVtepsList);
        subnetsListGwNull.add(subnetsGwNull);
        transportZoneGwNull = ItmTestUtil.getTransportZone(ItmTestConstants.TUNNEL_TYPE_VXLAN, ItmTestConstants.TZ_NAME,subnetsListGwNull);

        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, transportZoneIdentifier, transportZoneGwNull, dataBroker);

        assertEqualBeans(ExpectedCliTestsObjects.newVtep(),  dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION,vpath).checkedGet().get());

        tepCommandHelper.deleteVtep(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,null,
                ItmTestConstants.TZ_NAME, null);
        tepCommandHelper.deleteOnCommit();

        Assert.assertEquals(Optional.absent(),  dataBroker.newReadOnlyTransaction().
                read(LogicalDatastoreType.CONFIGURATION,vpath).get());

    }

    @Test
    public void testDeleteVtepWithMismatchIpwithSubnet() throws Exception{
        String errorOutput = null;
        try {
            tepCommandHelper.deleteVtep(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp4, ItmTestConstants.subnetMask,
                    ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
            fail("Expected an TepException to be thrown for delete vtep mismatch ip with subnet.");
        } catch (TepException e) {
            errorOutput = e.getMessage();
        }
        assertEquals(ITMConstants.MISMATCH_IPWITH_SUBNET_ERROR_STR, errorOutput);

    }


    @Test
    public void testGetTransportZone() throws Exception{
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, transportZoneIdentifier, transportZone, dataBroker);

        Assert.assertEquals(ItmTestConstants.TZ_NAME,tepCommandHelper.getTransportZone(ItmTestConstants.TZ_NAME).getZoneName());
    }


    @Test
    public void testIsInCache()throws Exception{
        tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);
        Assert.assertTrue(tepCommandHelper.isInCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null));
    }

    @Test
    public void testCheckTepPerTzPerDpn() throws Exception{
        tepCommandHelper.createLocalCache(ItmTestConstants.dpId1, ItmTestConstants.portName1, ItmTestConstants.vlanId, ItmTestConstants.tepIp1, ItmTestConstants.subnetMask,
                ItmTestConstants.gwyIp1, ItmTestConstants.TZ_NAME, null);

        Assert.assertFalse(tepCommandHelper.checkTepPerTzPerDpn(ItmTestConstants.TZ_NAME, ItmTestConstants.dpId2));
        Assert.assertTrue(tepCommandHelper.checkTepPerTzPerDpn(ItmTestConstants.TZ_NAME, ItmTestConstants.dpId1));
    }
}
