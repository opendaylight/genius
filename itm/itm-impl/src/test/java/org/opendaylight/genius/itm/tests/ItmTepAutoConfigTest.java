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
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.genius.itm.tests.xtend.ExpectedDefTransportZoneObjects;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import javax.inject.Inject;

/**
 * Component tests for ITM TEP Auto Config feature.
 */
public class ItmTepAutoConfigTest {

    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class);

    @Inject DataBroker dataBroker;

    @Before
    public void start() throws InterruptedException {
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
        // Test for default-TZ creation with DefTzEnabled flag set to True and Vxlan TunnelType

        // set def-tz-enabled flag to true
        ItmConfig itmConfigObj = new ItmConfigBuilder().
            setDefTzEnabled(true).setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_VXLAN).build();

        // Create TepCommandHelper object which creates default-TZ based on def-tz-enabled flag
        TepCommandHelper tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getDefTzIid();
        Assert.assertNotNull(tzonePath);

        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithVxlanTunnelType(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());
    }

    @Test
    public void defTzCreationTestWithDefTzEnabledTrueAndGreTunnelType() throws Exception {
        // set def-tz-enabled flag to true
        ItmConfig itmConfigObj = new ItmConfigBuilder().
            setDefTzEnabled(true).setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_GRE).build();

        TepCommandHelper tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        // Create TepCommandHelper object which creates default-TZ based on def-tz-enabled flag
        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getDefTzIid();
        Assert.assertNotNull(tzonePath);

        assertEqualBeans(ExpectedDefTransportZoneObjects.newDefTzWithGreTunnelType(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).checkedGet().get());
    }

    @Test
    public void defTzCreationFailedTestWithDefTzEnabledFalse() throws Exception {
        // set def-tz-enabled flag to false
        ItmConfig itmConfigObj = new ItmConfigBuilder().setDefTzEnabled(false).build();

        // Create TepCommandHelper object which creates default-TZ based on def-tz-enabled flag
        TepCommandHelper tepCmdHelper = new TepCommandHelper(dataBroker, itmConfigObj);
        tepCmdHelper.start();

        InstanceIdentifier<TransportZone> tzonePath = ItmTepAutoConfigTestUtil.getDefTzIid();
        Assert.assertNotNull(tzonePath);

        Assert.assertEquals(Optional.absent(), dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tzonePath).get());
    }

    @Test
    public void defTzTunnelTypeConfigTest() throws Exception {

    }

    @Test
    public void defTzDeletionTest() throws Exception {

    }

    @Test
    public void tepAddIntoDefTzTest() throws Exception {

    }

    @Test
    public void tepAddIntoTzTest() throws Exception {

    }

    @Test
    public void tepAddIntoTepsNotHostedListTest() throws Exception {

    }

    @Test
    public void tepDeleteFromDefTzTest() throws Exception {

    }

    @Test
    public void tepDeleteFromTzTest() throws Exception {

    }

    @Test
    public void tepDeleteFromTepsNotHostedListTest() throws Exception {

    }

    @Test
    public void tepMoveFromTepsNotHostedListToTzTest() throws Exception {

    }

    @Test
    public void tepUpdateForTzTest() throws Exception {

    }

    @Test
    public void tepUpdateForDpnBrNameTest() throws Exception {

    }
}
