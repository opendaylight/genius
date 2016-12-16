/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;

/**
 * Component tests for ITM.
 */
public class ItmTest {

    public @Rule MethodRule guice = new GuiceRule(ItmTestModule.class);

    @Test
    public void test() throws Exception {

    }
}
