/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.genius.itm.snd.ITMStatusMonitor;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;

/**
 * Unit Test for ITMStatusMonitor.
 *
 * @author Michael Vorburger.ch
 */
public class ITMStatusMonitorTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    /**
     * Make sure that we can register and un-register the MBean, without any ERROR logs.
     */
    @Test
    public void test2xRegisterUnregister() {
        ITMStatusMonitor.getInstance().registerMbean();
        ITMStatusMonitor.getInstance().unregisterMbean();

        ITMStatusMonitor.getInstance().registerMbean();
        ITMStatusMonitor.getInstance().unregisterMbean();
    }

}
