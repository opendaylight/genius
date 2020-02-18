/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListenerModule;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.tests.xtend.ExpectedDefTransportZoneObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedTepNotHostedTransportZoneObjects;
import org.opendaylight.genius.itm.tests.xtend.ExpectedTransportZoneObjects;
import org.opendaylight.infrautils.caches.testutils.CacheModule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Component tests for ITM TEP Auto Config feature.
 */
public class ItmTepAutoConfigTest {

    public @Rule LogRule logRule = new LogRule();
    // TODO public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class, TestableDataTreeChangeListenerModule.class,
            JobCoordinatorTestModule.class, CacheModule.class);

    TransportZone transportZone;
    RetryingManagedNewTransactionRunner txRunner;

    private @Inject DataBroker dataBroker;
    private @Inject JobCoordinatorEventsWaiter coordinatorEventsWaiter;
    private @Inject ItmProvider itmProvider;

    @Before
    public void start() throws InterruptedException {
        transportZone = new TransportZoneBuilder().setZoneName(ItmTestConstants.TZ_NAME)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .withKey(new TransportZoneKey(ItmTestConstants.TZ_NAME))
                .build();
        this.txRunner = new RetryingManagedNewTransactionRunner(dataBroker);
    }

    // Common method created for code-reuse
    private InstanceIdentifier<TransportZone> processDefTzOnItmConfig(boolean defTzEnabledFlag,
                                                                      String defTzTunnelType) throws Exception {

        ItmConfig itmConfigObj = null;
        if (defTzTunnelType != null) {
            // set def-tz-enabled flag and def-tz-tunnel-type
            itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(defTzEnabledFlag)
                    .setDefTzTunnelType(defTzTunnelType).build();
        } else {
            // set def-tz-enabled flag only
            itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(defTzEnabledFlag).build();
        }
        // creates/deletes default-TZ based on def-tz-enabled flag
        itmProvider.createDefaultTransportZone(itmConfigObj);
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ITMConstants.DEFAULT_TRANSPORT_ZONE);

        return tzonePath;
    }

    @Test
    public void defTzEnabledFalseConfigTest() throws Exception {
        InstanceIdentifier<ItmConfig> iid = InstanceIdentifier.create(ItmConfig.class);

        // set def-tz-enabled flag to false
        ItmConfig itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(false).build();

        // write into config DS
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> tx.put(iid, itmConfigObj, true)).get();

        // read from config DS
        boolean defTzEnabled = SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
                dataBroker, LogicalDatastoreType.CONFIGURATION, iid).get().isDefTzEnabled();
        Assert.assertEquals(defTzEnabled, false);
    }

    @Test
    public void defTzEnabledTrueConfigTest() throws Exception {
        InstanceIdentifier<ItmConfig> iid = InstanceIdentifier.create(ItmConfig.class);
        // set def-tz-enabled flag to true
        ItmConfig itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(true).build();

        // write into config DS
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> tx.put(iid, itmConfigObj, true)).get();
        // read from config DS
        boolean defTzEnabled = SingleTransactionDataBroker.syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
                dataBroker, LogicalDatastoreType.CONFIGURATION, iid).get().isDefTzEnabled();
        Assert.assertEquals(defTzEnabled, true);
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
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());
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
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());
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
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzonePath)
                        .get().getTunnelType());

        // now, change def-tz-tunnel-type to GRE
        defTzTunnelType = ITMConstants.TUNNEL_TYPE_GRE;

        tzonePath = processDefTzOnItmConfig(defTzEnabledFlag, defTzTunnelType);
        Assert.assertNotNull(tzonePath);

        // check default-TZ is re-created with GRE tunnel type
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithGreTunnelType().getTunnelType(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzonePath)
                        .get().getTunnelType());
    }

    @Test
    public void defTzDeletionTest() throws Exception {
        // wait for start-up default-TZ creation task to get over
        coordinatorEventsWaiter.awaitEventsConsumption();

        // create default-TZ first by setting def-tz-enabled flag to true
        ItmConfig itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(true)
                .setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_GRE).build();

        // creates/deletes default-TZ based on def-tz-enabled flag
        itmProvider.createDefaultTransportZone(itmConfigObj);
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(tzonePath);

        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithGreTunnelType(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());

        // now delete default-TZ first by setting def-tz-enabled flag to false
        itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(false).build();

        // creates/deletes default-TZ based on def-tz-enabled flag
        itmProvider.createDefaultTransportZone(itmConfigObj);
        coordinatorEventsWaiter.awaitEventsConsumption();

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());
    }

    @Test
    public void testAddDeleteTepForDefTz() throws Exception {
        // wait for start-up default-TZ creation task to get over
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(tzonePath);

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        // add TEP into default-TZ
        ListenableFuture<Void> futures =
                ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.DEF_TZ_TEP_IP,
                        ItmTestConstants.DEF_BR_DPID,
                        ITMConstants.DEFAULT_TRANSPORT_ZONE, false, dataBroker, txRunner);
        futures.get();

        InstanceIdentifier<Vteps> vtepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ITMConstants.DEFAULT_TRANSPORT_ZONE, ItmTestConstants.INT_DEF_BR_DPID,
                ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vtepPath);

        // check TEP is added into default-TZ
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithTep(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());

        // remove tep from default-TZ
        futures = ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.DEF_TZ_TEP_IP, ItmTestConstants.DEF_BR_DPID,
                ITMConstants.DEFAULT_TRANSPORT_ZONE, dataBroker, txRunner);
        futures.get();

        // check TEP is deleted from default-TZ
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, vtepPath).get());
    }

    @Test
    public void testAddDeleteTepForTz() throws Exception {
        // wait for start-up default-TZ creation task to get over
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // create TZA
        InstanceIdentifier<TransportZone> tzPath = ItmUtils.getTZInstanceIdentifier(ItmTestConstants.TZ_NAME);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();

        // check TZ is created with correct TZ name
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get().getZoneName());

        // add tep
        ListenableFuture<Void> futures = ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NB_TZ_TEP_IP,
                ItmTestConstants.DEF_BR_DPID, ItmTestConstants.TZ_NAME, false, dataBroker, txRunner);
        futures.get();

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vtepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vtepPath);

        // check TEP is added into TZ that is already created.
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());

        // remove tep
        futures = ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.NB_TZ_TEP_IP, ItmTestConstants.DEF_BR_DPID,
                ItmTestConstants.TZ_NAME, dataBroker, txRunner);
        futures.get();

        // check TEP is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, vtepPath).get());

        // for safe side, check TZ is present
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get().getZoneName());
    }

    @Test
    public void tepAddDeleteFromDefTzViaSouthboundTest() throws Exception  {
        // wait for start-up default-TZ creation task to get over
        coordinatorEventsWaiter.awaitEventsConsumption();

        String tepIp = ItmTestConstants.DEF_TZ_TEP_IP;
        // create Network topology node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        ConnectionInfo connInfo = OvsdbTestUtil.getConnectionInfo(ItmTestConstants.OVSDB_CONN_PORT,
                ItmTestConstants.LOCALHOST_IP);
        FluentFuture<Void> future =
                OvsdbTestUtil.createNode(connInfo,tepIp,ITMConstants.DEFAULT_TRANSPORT_ZONE,dataBroker);
        future.get();

        // add bridge into node
        future = OvsdbTestUtil.addBridgeIntoNode(connInfo, ItmTestConstants.DEF_BR_NAME,
                ItmTestConstants.DEF_BR_DPID, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(tzonePath);

        // check TEP is added into default-TZ
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithTep(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzonePath)
                        .get());

        // test TEP delete now,
        // pass tep-ip with NULL value, tep-ip paramtere in external_ids will not be set.
        tepIp = null;
        future = OvsdbTestUtil.updateNode(connInfo, tepIp, ITMConstants.DEFAULT_TRANSPORT_ZONE,
                null, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vtepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ITMConstants.DEFAULT_TRANSPORT_ZONE, ItmTestConstants.INT_DEF_BR_DPID,
                ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vtepPath);

        // check TEP is deleted from default-TZ when TEP-Ip is removed from southbound
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, vtepPath).get());
    }

    @Test
    public void tepAddDeleteFromNbTzViaSouthboundTest() throws Exception  {
        String tepIp = ItmTestConstants.NB_TZ_TEP_IP;
        // create Network topology node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        ConnectionInfo connInfo = OvsdbTestUtil.getConnectionInfo(ItmTestConstants.OVSDB_CONN_PORT,
                ItmTestConstants.LOCALHOST_IP);
        FluentFuture<Void> future = OvsdbTestUtil.createNode(
                connInfo, tepIp, ItmTestConstants.TZ_NAME, dataBroker);
        future.get();

        // create Transport-zone in advance
        InstanceIdentifier<TransportZone> tzPath = ItmUtils.getTZInstanceIdentifier(ItmTestConstants.TZ_NAME);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();

        // add bridge into node
        future = OvsdbTestUtil.addBridgeIntoNode(connInfo, ItmTestConstants.DEF_BR_NAME,
                ItmTestConstants.DEF_BR_DPID, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TEP is added into NB configured TZ
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzonePath)
                        .get());

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vtepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vtepPath);

        // test TEP delete now,
        // pass tep-ip with NULL value, tep-ip paramtere in external_ids will not be set.
        tepIp = null;
        future = OvsdbTestUtil.updateNode(connInfo, tepIp, ItmTestConstants.TZ_NAME, null, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // check TEP is deleted from default-TZ when TEP-Ip is removed from southbound
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, vtepPath).get());
    }

    @Test
    public void tzAddDeleteToNotHostedViaSouthboundTest() throws Exception  {
        // create Network topology node
        ConnectionInfo connInfo = OvsdbTestUtil.getConnectionInfo(ItmTestConstants.OVSDB_CONN_PORT,
                ItmTestConstants.LOCALHOST_IP);
        FluentFuture<Void> future = OvsdbTestUtil.createNode(
                connInfo, ItmTestConstants.NOT_HOSTED_TZ_TEP_IP, ItmTestConstants.NOT_HOSTED_TZ_NAME,
                dataBroker);
        future.get();

        // add bridge into node
        future = OvsdbTestUtil.addBridgeIntoNode(connInfo, ItmTestConstants.DEF_BR_NAME,
                ItmTestConstants.NOT_HOSTED_DEF_BR_DPID, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedtzPath = ItmTepAutoConfigTestUtil
                .getTepNotHostedInTZIid(ItmTestConstants.NOT_HOSTED_TZ_NAME);
        Assert.assertNotNull(notHostedtzPath);

        // check not hosted
        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                        notHostedtzPath).get());

        future = OvsdbTestUtil.updateNode(connInfo, ItmTestConstants.NOT_HOSTED_TZ_TEP_IP, null,
                ItmTestConstants.DEF_BR_NAME, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        Assert.assertEquals(Optional.absent(),dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedtzPath).get());
    }

    @Test
    public void tepUpdateForTepIpTest() throws Exception {
        // wait for start-up default-TZ creation task to get over
        coordinatorEventsWaiter.awaitEventsConsumption();

        String tepIp = ItmTestConstants.DEF_TZ_TEP_IP;
        // create Network topology node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        ConnectionInfo connInfo = OvsdbTestUtil.getConnectionInfo(ItmTestConstants.OVSDB_CONN_PORT,
                ItmTestConstants.LOCALHOST_IP);
        FluentFuture<Void> future = OvsdbTestUtil.createNode(
                connInfo, tepIp, ITMConstants.DEFAULT_TRANSPORT_ZONE, dataBroker);
        future.get();

        // add bridge into node
        future = OvsdbTestUtil.addBridgeIntoNode(connInfo, ItmTestConstants.DEF_BR_NAME,
                ItmTestConstants.DEF_BR_DPID, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // iid for default-TZ
        InstanceIdentifier<TransportZone> defTzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(defTzonePath);

        // check TEP is added into default-TZ
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithTep(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, defTzonePath)
                        .get());

        // update OVSDB node with tep-ip in local_ip list
        tepIp = ItmTestConstants.NB_TZ_TEP_IP;
        future = OvsdbTestUtil.updateNode(connInfo, tepIp, ITMConstants.DEFAULT_TRANSPORT_ZONE, null, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // check TEP is updated and now it is added with updated tep-ip
        // when local_ip is updated from southbound
        assertEqualBeans(ExpectedDefTransportZoneObjects.defTzWithUpdatedTepIp(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,defTzonePath)
                        .get());
    }

    @Ignore
    public void tepUpdateForTzTest() throws Exception {
        // wait for start-up default-TZ creation task to get over
        coordinatorEventsWaiter.awaitEventsConsumption();

        String tepIp = ItmTestConstants.DEF_TZ_TEP_IP;
        // create Network topology node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        ConnectionInfo connInfo = OvsdbTestUtil.getConnectionInfo(ItmTestConstants.OVSDB_CONN_PORT,
                ItmTestConstants.LOCALHOST_IP);
        FluentFuture<Void> future = OvsdbTestUtil.createNode(
                connInfo, tepIp, ITMConstants.DEFAULT_TRANSPORT_ZONE, dataBroker);
        future.get();

        // add bridge into node
        future = OvsdbTestUtil.addBridgeIntoNode(connInfo, ItmTestConstants.DEF_BR_NAME,
                ItmTestConstants.DEF_BR_DPID, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // iid for default-TZ
        InstanceIdentifier<TransportZone> defTzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ITMConstants.DEFAULT_TRANSPORT_ZONE);
        Assert.assertNotNull(defTzonePath);

        // check TEP is added into default-TZ
        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithTep(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, defTzonePath)
                        .get());

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();
        InstanceIdentifier<Vteps> oldVTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ITMConstants.DEFAULT_TRANSPORT_ZONE, ItmTestConstants.INT_DEF_BR_DPID,
                ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(oldVTepPath);

        // create Transport-zone TZA
        InstanceIdentifier<TransportZone> tzPath = ItmUtils.getTZInstanceIdentifier(ItmTestConstants.TZ_NAME);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();
        // iid for TZA configured from NB
        InstanceIdentifier<TransportZone> tzaTzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzaTzonePath);

        // update OVSDB node with tzname=TZA in ExternalIds list
        String tzName = ItmTestConstants.TZ_NAME;
        future = OvsdbTestUtil.updateNode(connInfo, tepIp, tzName, null, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // check old TEP which was in default-TZ is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, oldVTepPath).get());

        // check TEP is updated and now it is added into TZA transport-zone when tzname is updated
        // to TZA from southbound
        assertEqualBeans(ExpectedTransportZoneObjects.updatedTransportZone(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,tzaTzonePath)
                        .get());
    }

    @Test
    public void tepUpdateForBrNameTest() throws Exception {
        String tepIp = ItmTestConstants.NB_TZ_TEP_IP;
        // prepare OVSDB node with tep-ip set into ExternalIds list
        // OvsdbNodeListener would be automatically listen on Node to add TEP
        ConnectionInfo connInfo = OvsdbTestUtil.getConnectionInfo(ItmTestConstants.OVSDB_CONN_PORT,
                ItmTestConstants.LOCALHOST_IP);
        FluentFuture<Void> future = OvsdbTestUtil.createNode(
                connInfo, tepIp, ItmTestConstants.TZ_NAME, dataBroker);
        future.get();

        // create Transport-zone in advance
        InstanceIdentifier<TransportZone> tzPath = ItmUtils.getTZInstanceIdentifier(ItmTestConstants.TZ_NAME);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();


        // add bridge into node
        future = OvsdbTestUtil.addBridgeIntoNode(connInfo, ItmTestConstants.DEF_BR_NAME,
                ItmTestConstants.DEF_BR_DPID, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TEP is added into TZ
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> oldVTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(oldVTepPath);

        // add new bridge br2
        future = OvsdbTestUtil.addBridgeIntoNode(connInfo, ItmTestConstants.BR2_NAME,
                ItmTestConstants.BR2_DPID, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // update OVSDB node with br-name=br2 in ExternalIds column
        String brName = ItmTestConstants.BR2_NAME;
        future = OvsdbTestUtil.updateNode(connInfo, ItmTestConstants.NB_TZ_TEP_IP,
                ItmTestConstants.TZ_NAME, brName, dataBroker);
        future.get();
        // wait for OvsdbNodeListener to perform config DS update through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<Vteps> newVTepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ItmTestConstants.TZ_NAME, ItmTestConstants.INT_BR2_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(newVTepPath);

        // check old TEP having default-bridge-DPID is deleted
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, oldVTepPath).get());

        // check TEP is updated with dpnId of br2 when br-name is updated to br2 from southbound
        Assert.assertEquals(ItmTestConstants.INT_BR2_DPID, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, newVTepPath).get().getDpnId());
    }

    @Test
    public void tepAddIntoTepsNotHostedListTest() throws Exception {
        // add into not hosted list
        ListenableFuture<Void> future = ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP,
                ItmTestConstants.NOT_HOSTED_TZ_TEPDPN_ID, ItmTestConstants.NOT_HOSTED_TZ_NAME,
                false, dataBroker, txRunner);
        future.get();
        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedPath =
                ItmTepAutoConfigTestUtil.getTepNotHostedInTZIid(ItmTestConstants.NOT_HOSTED_TZ_NAME);
        Assert.assertNotNull(notHostedPath);

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL, notHostedPath)
                        .get());
    }

    @Test
    public void tepDeleteFromTepsNotHostedListTest() throws Exception {
        // add into not hosted list
        ListenableFuture<Void> future = ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP,
                ItmTestConstants.NOT_HOSTED_TZ_TEPDPN_ID, ItmTestConstants.NOT_HOSTED_TZ_NAME,
                false, dataBroker, txRunner);
        future.get();
        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedPath =
                ItmTepAutoConfigTestUtil.getTepNotHostedInTZIid(ItmTestConstants.NOT_HOSTED_TZ_NAME);
        Assert.assertNotNull(notHostedPath);

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get());

        //delete from not hosted list
        future = ItmTepAutoConfigTestUtil.deleteTep(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP,
                ItmTestConstants.NOT_HOSTED_TZ_TEPDPN_ID, ItmTestConstants.NOT_HOSTED_TZ_NAME, dataBroker, txRunner);
        future.get();

        Assert.assertEquals(Optional.absent(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get());
    }

    @Test
    public void tepMoveFromTepsNotHostedListToTzTest() throws Exception {
        // add into not hosted list
        ListenableFuture<Void> future = ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP,
                ItmTestConstants.NOT_HOSTED_TZ_TEPDPN_ID, ItmTestConstants.NOT_HOSTED_TZ_NAME,
                false, dataBroker, txRunner);
        future.get();
        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedPath =
                ItmTepAutoConfigTestUtil.getTepNotHostedInTZIid(ItmTestConstants.NOT_HOSTED_TZ_NAME);
        Assert.assertNotNull(notHostedPath);

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get());

        // create the same TZ
        TransportZone transportZoneNorth = new TransportZoneBuilder().setZoneName(ItmTestConstants.NOT_HOSTED_TZ_NAME)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN)
                .withKey(new TransportZoneKey(ItmTestConstants.NOT_HOSTED_TZ_NAME)).build();
        Assert.assertNotNull(transportZoneNorth);

        txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> tx.put(ItmTepAutoConfigTestUtil
                .getTzIid(ItmTestConstants.NOT_HOSTED_TZ_NAME), transportZoneNorth, true)).get();

        // wait for TransportZoneListener to perform config DS update
        // for TEP movement through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzPath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.NOT_HOSTED_TZ_NAME);
        Assert.assertNotNull(tzPath);

        // check TZ is Moved
        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects
                        .newTepNotHostedTransportZone().getUnknownVteps().get(0).getIpAddress().stringValue(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzPath)
                        .get().getVteps().get(0).getIpAddress().stringValue());

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone().getUnknownVteps()
                .get(0).getDpnId(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzPath).get()
                .getVteps().get(0).getDpnId());

        assertEqualBeans(ExpectedTepNotHostedTransportZoneObjects.newTepNotHostedTransportZone().getZoneName(),
                dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, tzPath)
                        .get().getZoneName());

        // check TZ is removed
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get());
    }

    @Test
    public void tzDeletedAndReaddedTest() throws Exception {
        // create TZ
        InstanceIdentifier<TransportZone> tzPath = ItmUtils.getTZInstanceIdentifier(ItmTestConstants.TZ_NAME);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();


        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TZ is created
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get().getZoneName());

        // add tep
        ListenableFuture<Void> futures =
                ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NB_TZ_TEP_IP,
                        ItmTestConstants.DEF_BR_DPID, ItmTestConstants.TZ_NAME, false, dataBroker, txRunner);
        futures.get();

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vtepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vtepPath);

        // check TEP is added into TZ that is already created.
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());

        // remove Transport Zone
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.delete(LogicalDatastoreType.CONFIGURATION,
                tzPath));

        // wait for TransportZoneListener to perform config DS update
        // for TEP movement through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();
        //verify delete
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet());

        //check deleted tz moved to notHosted
        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedPath =
                ItmTepAutoConfigTestUtil.getTepNotHostedInTZIid(ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(notHostedPath);
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get().getZoneName());

        //readd the same tz
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();
        // wait for TransportZoneListener to perform config DS update
        // for TEP movement through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).checkedGet());
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());
    }

    @Test
    @Ignore
    public void tzDeletedAndReaddedWithSameVtepsTest() throws Exception {
        // create TZ
        InstanceIdentifier<TransportZone> tzPath = ItmUtils.getTZInstanceIdentifier(ItmTestConstants.TZ_NAME);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();

        coordinatorEventsWaiter.awaitEventsConsumption();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TZ is created
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get().getZoneName());

        // add tep
        ListenableFuture<Void> futures =
                ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NB_TZ_TEP_IP,
                        ItmTestConstants.DEF_BR_DPID, ItmTestConstants.TZ_NAME, false, dataBroker, txRunner);
        futures.get();
        coordinatorEventsWaiter.awaitEventsConsumption();

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vtepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vtepPath);

        // check TEP is added into TZ that is already created.
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());

        // remove Transport Zone
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.delete(LogicalDatastoreType.CONFIGURATION,
                tzPath));

        // wait for TransportZoneListener to perform config DS update
        // for TEP movement through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();
        //verify delete
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet());

        //check deleted tz moved to notHosted
        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedPath =
                ItmTepAutoConfigTestUtil.getTepNotHostedInTZIid(ItmTestConstants.TZ_NAME);
        coordinatorEventsWaiter.awaitEventsConsumption();
        Assert.assertNotNull(notHostedPath);
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get().getZoneName());

        //create vtepList form unknownVtepList
        List<Vteps> vtepsList = new ArrayList<>();
        List<UnknownVteps> unknownVtepsList = dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get().getUnknownVteps();

        for (UnknownVteps unknownVtep:unknownVtepsList) {
            Vteps vteps = new VtepsBuilder().setDpnId(unknownVtep.getDpnId())
                    .setIpAddress(unknownVtep.getIpAddress())
                    .withKey(new VtepsKey(unknownVtep.getDpnId())).build();
            vtepsList.add(vteps);
        }

        TransportZone recreatedTransportZone = new TransportZoneBuilder().setZoneName(ItmTestConstants.TZ_NAME)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN).setVteps(vtepsList)
                .withKey(new TransportZoneKey(ItmTestConstants.TZ_NAME)).build();

        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, recreatedTransportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();

        // wait for TransportZoneListener to perform config DS update
        // for TEP movement through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // verify TZ is moved from notHosted to transport zone and their should be only one vtep in it.
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).checkedGet());
        assertEqualBeans(1, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get()
                .getVteps().size());
    }

    @Test
    public void tzReaddWithSameVtepsAndDiffDpnIDTest() throws Exception {
        // create TZ
        InstanceIdentifier<TransportZone> tzPath = ItmUtils.getTZInstanceIdentifier(ItmTestConstants.TZ_NAME);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, transportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();


        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getTzIid(
                ItmTestConstants.TZ_NAME);
        Assert.assertNotNull(tzonePath);

        // check TZ is created
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get().getZoneName());

        // add tep
        ListenableFuture<Void> futures =
                ItmTepAutoConfigTestUtil.addTep(ItmTestConstants.NB_TZ_TEP_IP,
                        ItmTestConstants.DEF_BR_DPID, ItmTestConstants.TZ_NAME, false, dataBroker, txRunner);
        futures.get();

        IpPrefix subnetMaskObj = ItmUtils.getDummySubnet();

        InstanceIdentifier<Vteps> vtepPath = ItmTepAutoConfigTestUtil.getTepIid(subnetMaskObj,
                ItmTestConstants.TZ_NAME, ItmTestConstants.INT_DEF_BR_DPID, ITMConstants.DUMMY_PORT);
        Assert.assertNotNull(vtepPath);

        // check TEP is added into TZ that is already created.
        assertEqualBeans(ExpectedTransportZoneObjects.newTransportZone(),
                dataBroker.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());

        // remove Transport Zone
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.delete(LogicalDatastoreType.CONFIGURATION,
                tzPath));

        // wait for TransportZoneListener to perform config DS update
        // for TEP movement through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();
        //verify delete
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet());

        //check deleted tz moved to notHosted
        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedPath =
                ItmTepAutoConfigTestUtil.getTepNotHostedInTZIid(ItmTestConstants.TZ_NAME);

        Assert.assertNotNull(notHostedPath);
        Assert.assertEquals(ItmTestConstants.TZ_NAME, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get().getZoneName());

        //create vtepList form unknownVtepList
        List<Vteps> vtepsList = new ArrayList<>();
        List<UnknownVteps> unknownVtepsList = dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).get().getUnknownVteps();
        //modifing the dpnid and keeping the ip same.
        for (UnknownVteps unknownVtep:unknownVtepsList) {
            Vteps vteps = new VtepsBuilder().setDpnId(Uint64.valueOf(10))
                    .setIpAddress(unknownVtep.getIpAddress())
                    .withKey(new VtepsKey(Uint64.valueOf(10))).build();
            vtepsList.add(vteps);
        }

        TransportZone recreatedTransportZone = new TransportZoneBuilder().setZoneName(ItmTestConstants.TZ_NAME)
                .setTunnelType(ItmTestConstants.TUNNEL_TYPE_VXLAN).setVteps(vtepsList)
                .withKey(new TransportZoneKey(ItmTestConstants.TZ_NAME)).build();

        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION,
                tzPath, recreatedTransportZone, WriteTransaction.CREATE_MISSING_PARENTS)).get();

        // wait for TransportZoneListener to perform config DS update
        // for TEP movement through transaction
        coordinatorEventsWaiter.awaitEventsConsumption();

        // verify TZ is moved from notHosted to transport zone and their should be only one vtep in it.
        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL, notHostedPath).checkedGet());
        assertEqualBeans(1, dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get()
                .getVteps().size());
    }
}

