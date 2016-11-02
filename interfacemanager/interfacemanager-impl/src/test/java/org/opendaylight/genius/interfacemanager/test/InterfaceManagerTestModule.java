/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;

/**
 * Dependency Injection Wiring for {@link InterfaceManagerTest}.
 *
 * @author Michael Vorburger
 */
public class InterfaceManagerTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() {
        // Bindings for services from this project

        // Bindings for external services to "real" implementations

        // Bindings to test infra (fakes & mocks)
        bind(DataBroker.class).toInstance(DataBrokerTestModule.dataBroker());

        TestIMdsalApiManager singleton = TestIMdsalApiManager.newInstance();
        bind(IMdsalApiManager.class).toInstance(singleton);
        bind(TestIMdsalApiManager.class).toInstance(singleton);
    }

}
