/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.impl.rev160406.InterfacemgrImplModule;

/**
 * Dependency Injection Wiring for {@link InterfaceManagerTest}.
 *
 * @author Michael Vorburger
 */
@SuppressWarnings("deprecation")
public class InterfaceManagerTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() {
        // Bindings for services from this project

        // Bindings for external services to "real" implementations

        // Bindings to test infra (fakes & mocks)
        bind(DataBroker.class).toInstance(DataBrokerTestModule.dataBroker());

        TestIMdsalApiManager mdsalManager = TestIMdsalApiManager.newInstance();
        bind(IMdsalApiManager.class).toInstance(mdsalManager);
        bind(TestIMdsalApiManager.class).toInstance(mdsalManager);

        EntityOwnershipService entityOwnershipService = Mockito.mock(EntityOwnershipService.class);
        bind(EntityOwnershipService.class).toInstance(entityOwnershipService);

        // Temporary bindings which normally would be on top, but cauz of CSS vs BP are here, for now:
        bind(IInterfaceManager.class).toInstance(interfaceManager(mdsalManager, entityOwnershipService));
    }

    /**
     * This method duplicates the logic in {@link InterfacemgrImplModule#createInstance()}.
     * This isn't ideal, but as interface-manager will hopefully soon be converted from CSS to BP,
     * at which point this can be simplified to be based on @Inject etc. just like e.g. the AclServiceModule
     * and AclServiceTestModule or ElanServiceTestModule, we do it like this, for now.
     */
    private IInterfaceManager interfaceManager(IMdsalApiManager mdsalManager,
            EntityOwnershipService entityOwnershipService) {
        InterfacemgrProvider provider = new InterfacemgrProvider();

        provider.setMdsalManager(mdsalManager);
        provider.setEntityOwnershipService(entityOwnershipService);

        return provider;
    }

}
