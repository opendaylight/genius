/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.genius.itm.tests.xtend.ExpectedDefTransportZoneObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedVtepsObjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Component tests for ITM TEP Auto Config feature.
 */
public class ItmTepAutoConfigTest {

    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class);
    public static final Logger LOG = LoggerFactory.getLogger(ItmTepAutoConfigTest.class);

    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;

    TransportZone transportZone = null;
    TransportZones transportZones = null;

    // list objects
    List<TransportZone> transportZoneList = new ArrayList<>();

    // InstanceIdentifier objects building

    // TransportZones Iid
    InstanceIdentifier<TransportZones> tZonesPath = InstanceIdentifier.builder(TransportZones.class).build();

    @Inject DataBroker dataBroker;

    @Before
    public void start() throws InterruptedException {
        transportZone = new TransportZoneBuilder().setZoneName(ItmTestConstants.TZ_NAME).setTunnelType(tunnelType1).setKey(new TransportZoneKey(ItmTestConstants.TZ_NAME)).build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
    }

    @After
    public void end() throws InterruptedException {
    }

    @Test
    public void defTzEnabledFalseConfigTest() throws Exception {
        InstanceIdentifier<ItmConfig> iid = InstanceIdentifier.create(ItmConfig.class);

        // set def-tz-enabled flag to false
        ItmConfig itmConfigObj = new ItmConfigBuilder().
            setDefTzEnabled(false).build();

        // write into config DS
        ItmTepAutoConfigTestUtil.writeItmConfig(iid, itmConfigObj, dataBroker);

        Thread.sleep(2000);

        // read from config DS
        ItmConfig itmConfig = ItmTepAutoConfigTestUtil.readItmConfig(iid, dataBroker);
        if (itmConfig != null) {
            boolean defTzEnabled = itmConfig.isDefTzEnabled();
            Assert.assertEquals(defTzEnabled, false);
        }
    }

    @Test
    public void defTzEnabledTrueConfigTest() throws Exception {
        InstanceIdentifier<ItmConfig> iid = InstanceIdentifier.create(ItmConfig.class);

        // set def-tz-enabled flag to true
        ItmConfig itmConfigObj = new ItmConfigBuilder().
            setDefTzEnabled(true).build();

        // write into config DS
        ItmTepAutoConfigTestUtil.writeItmConfig(iid, itmConfigObj, dataBroker);

        Thread.sleep(2000);

        // read from config DS
        ItmConfig itmConfig = ItmTepAutoConfigTestUtil.readItmConfig(iid, dataBroker);
        if (itmConfig != null) {
            boolean defTzEnabled = itmConfig.isDefTzEnabled();
            Assert.assertEquals(defTzEnabled, true);
        }
    }

    // Common method created for code-reuse
    private InstanceIdentifier<TransportZone> processDefTzOnItmConfig(boolean defTzEnabledFlag,
        String defTzTunnelType) throws Exception {
        ItmConfig itmConfigObj = null;
        if (defTzTunnelType != null) {
            // set def-tz-enabled flag and def-tz-tunnel-type
            itmConfigObj = new ItmConfigBuilder().
                setDefTzEnabled(defTzEnabledFlag).setDefTzTunnelType(defTzTunnelType).build();
        } else {
            // set def-tz-enabled flag only
            itmConfigObj = new ItmConfigBuilder().
                setDefTzEnabled(defTzEnabledFlag).build();
        }
        // Create TepCommandHelper object which creates/deletes default-TZ based
        // on def-tz-enabled flag
        TepCommandHelper tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ITMConstants.DEFAULT_TRANSPORT_ZONE);

        return tzonePath;
    }

    @Test
    public void defTzCreationTestWithDefTzEnabledTrueAndVxlanTunnelType() throws Exception {
        // set def-tz-enabled flag to true
        boolean defTzEnabledFlag = true;
        // set def-tz-tunnel-type to VXLAN
        String defTzTunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;

        InstanceIdentifier<TransportZone> tzonePath = processDefTzOnItmConfig(defTzEnabledFlag,
            defTzTunnelType);
        Assert.assertNotNull(tzonePath);

        Thread.sleep(2000);

        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithVxlanTunnelType(),
            dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());
    }

    @Test
    public void defTzCreationTestWithDefTzEnabledTrueAndGreTunnelType() throws Exception {
        // set def-tz-enabled flag to true
        boolean defTzEnabledFlag = true;
        // set def-tz-tunnel-type to GRE
        String defTzTunnelType = ITMConstants.TUNNEL_TYPE_GRE;

        InstanceIdentifier<TransportZone> tzonePath = processDefTzOnItmConfig(defTzEnabledFlag,
            defTzTunnelType);
        Assert.assertNotNull(tzonePath);

        Thread.sleep(2000);

        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithGreTunnelType(),
            dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());
    }

    @Test
    public void defTzCreationFailedTestWithDefTzEnabledFalse() throws Exception {
        // set def-tz-enabled flag to false
        boolean defTzEnabledFlag = false;
        // set def-tz-tunnel-type to GRE
        String defTzTunnelType = null;

        InstanceIdentifier<TransportZone> tzonePath = processDefTzOnItmConfig(defTzEnabledFlag,
            defTzTunnelType);
        Assert.assertNotNull(tzonePath);

        Thread.sleep(2000);

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());
    }

    @Test
    public void defTzRecreationTestOnTunnelTypeChange() throws Exception {
        // set def-tz-enabled flag to true
        boolean defTzEnabledFlag = true;
        // set def-tz-tunnel-type to VXLAN
        String defTzTunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;

        InstanceIdentifier<TransportZone> tzonePath = processDefTzOnItmConfig(defTzEnabledFlag,
            defTzTunnelType);
        Assert.assertNotNull(tzonePath);

        Thread.sleep(2000);

        // check default-TZ is created with VXLAN tunnel type
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithVxlanTunnelType().getTunnelType(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get().getTunnelType());

        // now, change def-tz-tunnel-type to GRE
        defTzTunnelType = ITMConstants.TUNNEL_TYPE_GRE;

        tzonePath = processDefTzOnItmConfig(defTzEnabledFlag, defTzTunnelType);
        Assert.assertNotNull(tzonePath);

        Thread.sleep(2000);

        // check default-TZ is re-created with GRE tunnel type
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithGreTunnelType().getTunnelType(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get().getTunnelType());

    }

    @Test
    public void defTzDeletionTest() throws Exception {
        // create default-TZ first by setting def-tz-enabled flag to true
        ItmConfig itmConfigObj = new ItmConfigBuilder().
            setDefTzEnabled(true).setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_GRE).build();

        TepCommandHelper tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        // Create TepCommandHelper object which creates/deletes default-TZ based on def-tz-enabled flag
        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(tzonePath);

        Thread.sleep(2000);

        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithGreTunnelType(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());

        // now delete default-TZ first by setting def-tz-enabled flag to false
        itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(false).build();

        // Create TepCommandHelper object which creates/deletes default-TZ based on def-tz-enabled flag
        tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        Thread.sleep(2000);

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());
    }

    @Test
    public void testAddDeleteTepForDefTz() throws Exception {
        // create default-TZ first by setting def-tz-enabled flag to true
        ItmConfig itmConfigObj = new ItmConfigBuilder().
            setDefTzEnabled(true).setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_VXLAN).build();

        TepCommandHelper tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        // add TEP into default-TZ
        ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.DEF_TZ_TEP_IP,
            ItmTestConstants.STR_DEF_TZ_TEP_DPN_ID,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, false, dataBroker);

        InstanceIdentifier<Vteps> vTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, ItmTestConstants.INT_DEF_TZ_TEP_DPN_ID,
            ItmTestConstants.DUMMY_PORT_NAME);
        Assert.assertNotNull(vTepPath);

        Thread.sleep(2000);

        Assert.assertEquals(ItmTestConstants.DEF_TZ_TEP_IP, dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).checkedGet().get().
                getIpAddress().getIpv4Address().getValue());

        // remove tep from default-TZ
        ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.DEF_TZ_TEP_IP, ItmTestConstants.STR_DEF_TZ_TEP_DPN_ID,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, dataBroker);

        Thread.sleep(2000);

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).get());
    }

    @Test
    public void testAddDeleteTepForTz() throws Exception {
        // create TZ
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, tZonesPath, transportZones, dataBroker);

        Thread.sleep(2000);

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TZ is created
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get().getZoneName());

        // add tep
        ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.TZ_TEP_IP, ItmTestConstants.STR_TZ_TEP_DPN_ID,
            ItmTestConstants.TZ_NAME, false, dataBroker);

        Thread.sleep(2000);

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ItmTestConstants.TZ_NAME, ItmTestConstants.INT_TZ_TEP_DPN_ID,
            ItmTestConstants.DUMMY_PORT_NAME);
        Assert.assertNotNull(vTepPath);

        assertEqualBeans(ExpectedVtepsObjects.newVtep().getIpAddress().getIpv4Address().getValue(),
            dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).checkedGet().get().getIpAddress().
                getIpv4Address().getValue());

        // remove tep
        ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.TZ_TEP_IP, ItmTestConstants.STR_TZ_TEP_DPN_ID,
            ItmTestConstants.TZ_NAME, dataBroker);

        Thread.sleep(2000);

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).get());
    }

    @Test
    public void tepUpdateForTzTest() throws Exception {

    }

    @Test
    public void tepUpdateForDpnBrNameTest() throws Exception {

    }
}
