/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.genius.itm.tests.xtend.ExpectedTransportZoneObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedDefTransportZoneObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedTepNotHostedTransportZoneObjects;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TepsNotHostedInTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Optional;

/**
 * Component tests for ITM TEP Auto Config feature.
 */
public class ItmTepAutoConfigTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class);

    TransportZone transportZone;
    TransportZones transportZones;

    // list objects
    List<TransportZone> transportZoneList = new ArrayList<>();

    // InstanceIdentifier objects building

    // TransportZones Iid
    InstanceIdentifier<TransportZones> tZonesPath = InstanceIdentifier.builder(TransportZones.class).build();

    @Inject DataBroker dataBroker;

    @Before
    public void start() throws InterruptedException {
        transportZone = new TransportZoneBuilder().setZoneName(ItmTestConstants.TZ_NAME).
            setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN).setKey(new TransportZoneKey(ItmTestConstants.TZ_NAME)).build();
        transportZoneList.add(transportZone);
        transportZones = new TransportZonesBuilder().setTransportZone(transportZoneList).build();
    }

    @After
    public void end() throws InterruptedException {
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
    public void defTzEnabledFalseConfigTest() throws Exception {
        InstanceIdentifier<ItmConfig> iid = InstanceIdentifier.create(ItmConfig.class);

        // set def-tz-enabled flag to false
        ItmConfig itmConfigObj = new ItmConfigBuilder().
            setDefTzEnabled(false).build();

        // write into config DS
        ItmTepAutoConfigTestUtil.writeItmConfig(iid, itmConfigObj, dataBroker);

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

        // read from config DS
        ItmConfig itmConfig = ItmTepAutoConfigTestUtil.readItmConfig(iid, dataBroker);
        if (itmConfig != null) {
            boolean defTzEnabled = itmConfig.isDefTzEnabled();
            Assert.assertEquals(defTzEnabled, true);
        }
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

        Thread.sleep(1000);

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

        // check default-TZ is created with VXLAN tunnel type
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithVxlanTunnelType().getTunnelType(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get().getTunnelType());

        // now, change def-tz-tunnel-type to GRE
        defTzTunnelType = ITMConstants.TUNNEL_TYPE_GRE;

        tzonePath = processDefTzOnItmConfig(defTzEnabledFlag, defTzTunnelType);
        Assert.assertNotNull(tzonePath);

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

        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithGreTunnelType(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());

        // now delete default-TZ first by setting def-tz-enabled flag to false
        itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(false).build();

        // Create TepCommandHelper object which creates/deletes default-TZ based on def-tz-enabled flag
        tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        Thread.sleep(1000);

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

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(tzonePath);

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        // add TEP into default-TZ
        ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.DEF_TZ_TEP_IP,
            ItmTestConstants.DEF_BR_DPID,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, false, dataBroker);
        Thread.sleep(1000);

        InstanceIdentifier<Vteps> vTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, ItmTestConstants.INT_DEF_BR_DPID,
            ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vTepPath);

        // check TEP is added into default-TZ
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithTep(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());

        // remove tep from default-TZ
        ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.DEF_TZ_TEP_IP, ItmTestConstants.DEF_BR_DPID,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, dataBroker);
        Thread.sleep(1000);

        // check TEP is deleted from default-TZ
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).get());
    }

    @Test
    public void testAddDeleteTepForTz() throws Exception {
        // create TZ
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, tZonesPath, transportZones, dataBroker);

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TZ is created
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get().getZoneName());

        // add tep
        ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NB_TZ_TEP_IP, ItmTestConstants.DEF_BR_DPID,
            ItmTestConstants.TZ_NAME, false, dataBroker);
        Thread.sleep(1000);

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vTepPath);

        // check TEP is added into TZ that is already created.
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
            dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());

        // remove tep
        ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.NB_TZ_TEP_IP, ItmTestConstants.DEF_BR_DPID,
            ItmTestConstants.TZ_NAME, dataBroker);

        Thread.sleep(1000);

        // check TEP is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).get());

        // for safe side, check TZ is present
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get().getZoneName());
    }

    @Test
    public void tepAddDeleteFromDefTzViaSouthboundTest() throws Exception  {
        String tepIp = ItmTestConstants.DEF_TZ_TEP_IP;
        // create Network topology node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        Node ovsdbNode = OvsdbTestUtil.createNode(tepIp, ITMConstants.DEFAULT_TRANSPORT_ZONE,
            dataBroker);

        // add bridge into node
        OvsdbTestUtil.addBridgeIntoNode(ovsdbNode, ItmTestConstants.DEF_BR_NAME,
            ItmTestConstants.DEF_BR_DPID, dataBroker);
        Thread.sleep(1000);

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(tzonePath);

        // check TEP is added into default-TZ
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithTep(),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzonePath)
                .checkedGet().get());

        // test TEP delete now,
        // pass tep-ip with NULL value, tep-ip paramtere in external_ids will not be set.
        tepIp = null;
        OvsdbTestUtil.updateNode(tepIp, ITMConstants.DEFAULT_TRANSPORT_ZONE, null, dataBroker);
        Thread.sleep(1000);

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, ItmTestConstants.INT_DEF_BR_DPID,
            ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vTepPath);

        // check TEP is deleted from default-TZ when TEP-Ip is removed from southbound
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).get());
    }

    @Test
    public void tepAddDeleteFromNbTzViaSouthboundTest() throws Exception  {
        // create Transport-zone in advance
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, tZonesPath, transportZones,
            dataBroker);

        String tepIp = ItmTestConstants.NB_TZ_TEP_IP;
        // create Network topology node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        Node ovsdbNode = OvsdbTestUtil.createNode(tepIp, ItmTestConstants.TZ_NAME, dataBroker);

        // add bridge into node
        OvsdbTestUtil.addBridgeIntoNode(ovsdbNode, ItmTestConstants.DEF_BR_NAME,
            ItmTestConstants.DEF_BR_DPID, dataBroker);
        Thread.sleep(1000);

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TEP is added into NB configured TZ
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzonePath)
                .checkedGet().get());

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vTepPath);

        // test TEP delete now,
        // pass tep-ip with NULL value, tep-ip paramtere in external_ids will not be set.
        tepIp = null;
        OvsdbTestUtil.updateNode(tepIp, ItmTestConstants.TZ_NAME, null, dataBroker);
        Thread.sleep(1000);

        // check TEP is deleted from default-TZ when TEP-Ip is removed from southbound
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, vTepPath).get());
    }

    @Test
    public void tzAddDeleteToNotHostedViaSouthboundTest() throws Exception  {
        // create Network topology node
        Node ovsdbNode = OvsdbTestUtil.createNode(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP,
            ItmTestConstants.NOT_HOSTED_TZ_NAME, dataBroker);

        // add bridge into node
        OvsdbTestUtil.addBridgeIntoNode(ovsdbNode, ItmTestConstants.DEF_BR_NAME,
            ItmTestConstants.NOT_HOSTED_DEF_BR_DPID, dataBroker);
        Thread.sleep(1000);

        InstanceIdentifier<TepsNotHostedInTransportZone> notHostedtzPath = ItmTepAutoConfigTestUtil.
            getTepNotHostedInTZIid(ItmTestConstants.NOT_HOSTED_TZ_NAME);
        Assert.assertNotNull(notHostedtzPath);

        // check not hosted
        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                notHostedtzPath).checkedGet().get());

        OvsdbTestUtil.updateNode(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP, null,
            ItmTestConstants.DEF_BR_NAME, dataBroker);
        Thread.sleep(1000);

        Assert.assertEquals(Optional.absent(),dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, notHostedtzPath).get());
    }

    @Test
    public void tepUpdateForTzTest() throws Exception {
        String tepIp = ItmTestConstants.DEF_TZ_TEP_IP;
        // create Network topology node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        Node ovsdbNode = OvsdbTestUtil.createNode(tepIp, ITMConstants.DEFAULT_TRANSPORT_ZONE,
            dataBroker);

        // add bridge into node
        OvsdbTestUtil.addBridgeIntoNode(ovsdbNode, ItmTestConstants.DEF_BR_NAME,
            ItmTestConstants.DEF_BR_DPID, dataBroker);
        Thread.sleep(1000);

        // iid for default-TZ
        InstanceIdentifier<TransportZone> defTzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(defTzonePath);

        // check TEP is added into default-TZ
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithTep(),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, defTzonePath)
                .checkedGet().get());

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();
        InstanceIdentifier<Vteps> oldVTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ITMConstants.DEFAULT_TRANSPORT_ZONE, ItmTestConstants.INT_DEF_BR_DPID,
            ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(oldVTepPath);

        // create Transport-zone TZA
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, tZonesPath, transportZones,
            dataBroker);

        // iid for TZA configured from NB
        InstanceIdentifier<TransportZone> tzaTzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzaTzonePath);

        // update OVSDB node with tzname=TZA in ExternalIds list
        String tzName = ItmTestConstants.TZ_NAME;
        OvsdbTestUtil.updateNode(tepIp, tzName, null, dataBroker);
        Thread.sleep(1000);

        // check old TEP which was in default-TZ is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, oldVTepPath).get());

        // check TEP is updated and now it is added into TZA transport-zone when tzname is updated
        // to TZA from southbound
        assertEqualBeans(ExpectedTransportZoneObjects.updatedTransportZone(),
            dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,tzaTzonePath)
                .checkedGet().get());
    }

    @Test
    public void tepUpdateForDpnBrNameTest() throws Exception {
        // create Transport-zone in advance
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, tZonesPath, transportZones, dataBroker);

        String tepIp = ItmTestConstants.NB_TZ_TEP_IP;
        // prepare OVSDB node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        Node ovsdbNode = OvsdbTestUtil.createNode(tepIp, ItmTestConstants.TZ_NAME, dataBroker);

        // add bridge into node
        OvsdbTestUtil.addBridgeIntoNode(ovsdbNode, ItmTestConstants.DEF_BR_NAME,
            ItmTestConstants.DEF_BR_DPID, dataBroker);
        Thread.sleep(1000);

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
            ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TEP is added into TZ
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> oldVTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(oldVTepPath);

        // add new bridge br2
        OvsdbTestUtil.addBridgeIntoNode(ovsdbNode, ItmTestConstants.BR2_NAME,
            ItmTestConstants.BR2_DPID, dataBroker);

        // update OVSDB node with dpn-br-name=br2 in ExternalIds column
        String dpnBrName = ItmTestConstants.BR2_NAME;
        OvsdbTestUtil.updateNode(ItmTestConstants.NB_TZ_TEP_IP, ItmTestConstants.TZ_NAME,
            dpnBrName, dataBroker);
        Thread.sleep(1000);

        InstanceIdentifier<Vteps> newVTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
            ItmTestConstants.TZ_NAME, ItmTestConstants.INT_BR2_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(newVTepPath);

        // check old TEP having default-bridge-DPID is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, oldVTepPath).get());

        // check TEP is updated with dpnId of br2 when dpn-br-name is updated to br2 from southbound
        Assert.assertEquals(ItmTestConstants.INT_BR2_DPID, dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, newVTepPath).checkedGet().get().getDpnId());
    }

    @Test
    public void tepAddIntoTepsNotHostedListTest() throws Exception {
        // add into not hosted list
        InstanceIdentifier<TepsNotHostedInTransportZone> notHostedPath = ItmTepAutoConfigTestUtil
            .addAndGetPathForTepNotHosted(dataBroker);
        Assert.assertNotNull(notHostedPath);

        Thread.sleep(1000);

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, notHostedPath).checkedGet().get());
    }

    @Test
    public void tepDeleteFromTepsNotHostedListTest() throws Exception {
        // add into not hosted list
        InstanceIdentifier<TepsNotHostedInTransportZone> notHostedPath = ItmTepAutoConfigTestUtil
            .addAndGetPathForTepNotHosted(dataBroker);
        Assert.assertNotNull(notHostedPath);
        
        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, notHostedPath).checkedGet().get());

        //delete from not hosted list
        ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP,
            ItmTestConstants.NOT_HOSTED_TZ_TEPDPN_ID,
            ItmTestConstants.NOT_HOSTED_TZ_NAME, dataBroker);

        Thread.sleep(1000);
        Assert.assertEquals(Optional.absent(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, notHostedPath).get());
    }

    /*
     * Commenting below method unless https://git.opendaylight.org/gerrit/#/c/45797/
     * code gets merged because this test method will perform TEP movement operation
     * which internally calls method of code for given #45797 review.
     *
    @Test
    public void tepMoveFromTepsNotHostedListToTzTest() throws Exception {
        InstanceIdentifier<TepsNotHostedInTransportZone> notHostedPath = ItmTepAutoConfigTestUtil
            .addAndGetPathForTepNotHosted(dataBroker);
        Assert.assertNotNull(notHostedPath);

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, notHostedPath).checkedGet().get());

        // create the same TZ
        TransportZone transportZoneNorth = new TransportZoneBuilder().setZoneName(ItmTestConstants.NOT_HOSTED_TZ_NAME)
            .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN).setKey(new TransportZoneKey(ItmTestConstants.NOT_HOSTED_TZ_NAME)).build();
        Assert.assertNotNull(transportZoneNorth);

        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, ItmTepAutoConfigTestUtil
            .getTzIid(ItmTestConstants.NOT_HOSTED_TZ_NAME), transportZoneNorth, dataBroker);

        Thread.sleep(2000);

        InstanceIdentifier<TransportZone> tzPath = ItmTepAutoConfigTestUtil.getTzIid(ItmTestConstants.NOT_HOSTED_TZ_NAME);
        Assert.assertNotNull(tzPath);

        // check TZ is Moved
        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone().getUnknownVteps().
                get(0).getIpAddress().getIpv4Address().getValue(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzPath).checkedGet().get().getSubnets().get(0)
                .getVteps().get(0).getIpAddress().getIpv4Address().getValue());

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone().getUnknownVteps().
                get(0).getDpnId(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzPath).checkedGet().get().getSubnets().get(0)
                .getVteps().get(0).getDpnId());

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone().getZoneName(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzPath).checkedGet().get().getZoneName());

        // check TZ is removed
        Assert.assertEquals(Optional.absent(),
            dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, notHostedPath).get());
    }
    */
}
