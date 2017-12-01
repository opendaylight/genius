/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.tests;

import java.net.UnknownHostException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.genius.lockmanager.impl.LockListener;
import org.opendaylight.genius.lockmanager.impl.LockManagerServiceImpl;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.ops4j.pax.cdi.api.OsgiService;

public class LockManagerTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() throws UnknownHostException {
        bind(LockManagerService.class).to(LockManagerServiceImpl.class);
        bind(LockListener.class);
        DataBroker dataBroker = DataBrokerTestModule.dataBroker();
        bind(DataBroker.class).annotatedWith(OsgiService.class).toInstance(dataBroker);
    }
}
