/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.networkutils.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorTestModule;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.networkutils.RDUtils;
import org.opendaylight.genius.networkutils.VniUtils;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link VniUtils}.
 *
 * @author Faseela K
 */
public class NetworkUtilTest extends AbstractConcurrentDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtilTest.class);

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    public @Rule MethodRule guice = new GuiceRule(NetworkUtilTestModule.class, JobCoordinatorTestModule.class);

    private @Inject VniUtils vniUtils;
    private @Inject RDUtils  rdUtils;

    @Test
    public void testDefaultVniPoolCreated() throws ReadFailedException {
        IdPool idPool = vniUtils.getVxlanVniPool().get();
        assertThat(idPool.getPoolName()).isEqualTo(NwConstants.ODL_VNI_POOL_NAME);
    }

    @Test
    public void testGetVNI() throws ExecutionException, InterruptedException {
        assertThat(vniUtils.getVNI("test").longValue()).isEqualTo(NwConstants.VNI_DEFAULT_LOW_VALUE);
    }

    @Test
    public void testDefaultRDPoolCreated() throws ReadFailedException {
        IdPool idPool = rdUtils.getRDPool().get();
        assertThat(idPool.getPoolName()).isEqualTo(NwConstants.ODL_RD_POOL_NAME);
    }

    @Test
    public void testGetRD() throws ExecutionException, InterruptedException {
        assertThat(rdUtils.getRD("testRD").equals(NwConstants.RD_DEFAULT_LOW_VALUE));
    }


}
