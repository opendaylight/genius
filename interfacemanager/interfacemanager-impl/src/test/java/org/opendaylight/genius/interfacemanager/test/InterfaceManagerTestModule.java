/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static org.mockito.Mockito.mock;

import java.net.UnknownHostException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.interfacemanager.listeners.CacheBridgeEntryConfigListener;
import org.opendaylight.genius.interfacemanager.listeners.CacheBridgeRefEntryListener;
import org.opendaylight.genius.interfacemanager.listeners.HwVTEPConfigListener;
import org.opendaylight.genius.interfacemanager.listeners.HwVTEPTunnelsStateListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceConfigListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceTopologyStateListener;
import org.opendaylight.genius.interfacemanager.listeners.TerminationPointStateListener;
import org.opendaylight.genius.interfacemanager.listeners.VlanMemberConfigListener;
import org.opendaylight.genius.interfacemanager.rpcservice.InterfaceManagerRpcService;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners.FlowBasedServicesConfigListener;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners.FlowBasedServicesInterfaceStateListener;
import org.opendaylight.genius.interfacemanager.test.infra.TestEntityOwnershipService;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.infrautils.inject.ModuleSetupRuntimeException;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.lockmanager.LockManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
