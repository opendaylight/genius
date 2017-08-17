/*
 * Copyright (c) 2017 Ericsson Spain, S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.resourcemanager.tests;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.lockmanager.LockListener;
import org.opendaylight.genius.lockmanager.LockManager;
import org.opendaylight.genius.resourcemanager.ResourceManager;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceManagerService;

/**
 * Dependency Injection Wiring for {@link ResourceManagerTest}.
 *
 * <p>
 * For other *Module examples, please see the AclServiceModule and
 * AclServiceTestModule or ElanServiceTestModule instead.
 *
 * @author David Suárez
 */
public class ResourceManagerTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() throws Exception {
        DataBroker dataBroker = DataBrokerTestModule.dataBroker();
        bind(DataBroker.class).toInstance(dataBroker);

        bind(LockManagerService.class).to(LockManager.class);
        bind(LockListener.class);

        bind(IdManagerService.class).to(IdManager.class);
        bind(ResourceManagerService.class).to(ResourceManager.class);
    }
}
