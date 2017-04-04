/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;

public class ArpUtilTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(new ArpUtilTestModule());

    @Inject DataBroker dataBroker;

    @Test
    public void testWiring(){ }
}
