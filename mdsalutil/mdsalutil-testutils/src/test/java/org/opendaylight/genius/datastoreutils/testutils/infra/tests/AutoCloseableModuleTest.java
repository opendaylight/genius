/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.infra.tests;

import static org.junit.Assert.assertTrue;

import com.mycila.guice.ext.closeable.CloseableModule;
import javax.inject.Inject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.genius.datastoreutils.testutils.infra.AutoCloseableModule;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for AutoCloseableModule.
 *
 * @author Michael Vorburger
 */
public class AutoCloseableModuleTest {

    private static final Logger LOG = LoggerFactory.getLogger(AutoCloseableModuleTest.class);

    public @Rule GuiceRule guice = new GuiceRule(AutoCloseableModule.class, CloseableModule.class);

    @Inject SomeAutoCloseableSingleton someAutoCloseableClass;

    static SomeAutoCloseableSingleton staticSomeAutoCloseableClass;

    @After
    public void after() {
        // AutoCloseableModule's Closer has not run yet at this point
        staticSomeAutoCloseableClass = someAutoCloseableClass;
    }

    @AfterClass
    public static void afterClass() {
        assertTrue(staticSomeAutoCloseableClass.wasClosed);
    }

    @Test
    public void testAutoCloseableModule() {
    }

}
