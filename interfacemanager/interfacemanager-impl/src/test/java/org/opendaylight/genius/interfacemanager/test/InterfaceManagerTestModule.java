/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import java.net.*;
import static org.mockito.Mockito.*;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.binding.test.*;
import org.opendaylight.controller.md.sal.common.api.clustering.*;
import org.opendaylight.controller.md.sal.common.api.data.*;
import org.opendaylight.genius.idmanager.*;
import org.opendaylight.genius.interfacemanager.listeners.*;
import org.opendaylight.genius.interfacemanager.rpcservice.*;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners.*;
import org.opendaylight.genius.interfacemanager.test.infra.*;
import org.opendaylight.genius.mdsalutil.interfaces.*;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.*;
import org.opendaylight.infrautils.inject.*;
import org.opendaylight.infrautils.inject.guice.testutils.*;
import org.opendaylight.lockmanager.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.*;
import org.slf4j.*;

/**
 * Dependency Injection Wiring for {@link InterfaceManagerConfigurationTest}.
 *
 * <p>This class looks a little bit more complicated than it could and later will be
 * just because interfacemanager is still using CSS instead of BP with @Inject.
 *
 * <p>Please DO NOT copy/paste this class as-is into other projects; this is intended
 * to be temporary, until interfacemanager is switch from CSS to BP.
 *
 * <p>For "proper" *Module examples, please see the AclServiceModule and
 * AclServiceTestModule or ElanServiceTestModule instead.
 *
 * @author Michael Vorburger
 */
public class InterfaceManagerTestModule extends AbstractGuiceJsr250Module {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerTestModule.class);

    @Override
    protected void configureBindings() throws UnknownHostException {
        // TODO Ordering as below.. hard to do currently, because of interdeps. due to CSS
        // Bindings for services from this project
        // Bindings for external services to "real" implementations
        // Bindings to test infra (fakes & mocks)

        DataBroker dataBroker = DataBrokerTestModule.dataBroker();
        bind(DataBroker.class).toInstance(dataBroker);

        LockManagerService lockManager = new LockManager(dataBroker);
        bind(LockManagerService.class).toInstance(lockManager);

        IdUtils idUtils = new IdUtils();
        IdManagerService idManager;
        try {
            idManager = new IdManager(dataBroker, lockManager, idUtils);
        } catch (ReadFailedException e) {
            // TODO Support AbstractGuiceJsr250Module
            throw new ModuleSetupRuntimeException(e);
        }
        bind(IdManagerService.class).toInstance(idManager);

        TestIMdsalApiManager mdsalManager = TestIMdsalApiManager.newInstance();
        bind(IMdsalApiManager.class).toInstance(mdsalManager);
        bind(TestIMdsalApiManager.class).toInstance(mdsalManager);

        EntityOwnershipService entityOwnershipService = TestEntityOwnershipService.newInstance();
        bind(EntityOwnershipService.class).toInstance(entityOwnershipService);

        bind(AlivenessMonitorService.class).toInstance(mock(AlivenessMonitorService.class));
        bind(OdlInterfaceRpcService.class).to(InterfaceManagerRpcService.class);
        bind(CacheBridgeEntryConfigListener.class);
        bind(CacheBridgeRefEntryListener.class);
        bind(CacheInterfaceConfigListener.class);
        bind(CacheInterfaceStateListener.class);
        bind(FlowBasedServicesConfigListener.class);
        bind(FlowBasedServicesInterfaceStateListener.class);
        bind(HwVTEPConfigListener.class);
        bind(HwVTEPTunnelsStateListener.class);
        bind(InterfaceConfigListener.class);
        bind(InterfaceInventoryStateListener.class);
        bind(InterfaceTopologyStateListener.class);
        bind(TerminationPointStateListener.class);
        bind(VlanMemberConfigListener.class);
    }
}
