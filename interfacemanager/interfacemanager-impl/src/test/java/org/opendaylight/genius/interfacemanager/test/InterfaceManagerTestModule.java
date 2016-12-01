/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.test.infra.TestEntityOwnershipService;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.infrautils.inject.ModuleSetupRuntimeException;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.lockmanager.LockManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.impl.rev160406.InterfacemgrImplModule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

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

    @Override
    protected void configureBindings() throws Exception {
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

        // Temporary bindings which normally would be on top, but cauz of CSS vs BP are here, for now:
        InterfacemgrProvider interfaceManager = interfaceManager(mdsalManager, entityOwnershipService, dataBroker,
                idManager);
        bind(IInterfaceManager.class).toInstance(interfaceManager);
        bind(Stopper.class).toInstance(new Stopper(interfaceManager));
    }

    @Singleton
    private static class Stopper {
        final InterfacemgrProvider interfaceManager;

        Stopper(InterfacemgrProvider interfaceManager) {
            this.interfaceManager = interfaceManager;
        }

        @PreDestroy
        void close() throws Exception {
            interfaceManager.close();
        }
    }

    /**
     * This method duplicates the logic in {@link InterfacemgrImplModule#createInstance()}.
     * This isn't ideal, but as interface-manager will hopefully soon be converted from CSS to BP,
     * at which point this can be simplified to be based on @Inject etc. just like e.g. the AclServiceModule
     * and AclServiceTestModule or ElanServiceTestModule, we do it like this, for now.
     */
    private InterfacemgrProvider interfaceManager(IMdsalApiManager mdsalManager,
            EntityOwnershipService entityOwnershipService, DataBroker dataBroker, IdManagerService idManager) {

        InterfacemgrProvider provider = new InterfacemgrProvider();

        RpcProviderRegistry rpcProviderRegistry = mock(RpcProviderRegistry.class /* TODO how-to? exception() */);
        when(rpcProviderRegistry.getRpcService(IdManagerService.class)).thenReturn(idManager);

        provider.setRpcProviderRegistry(rpcProviderRegistry);
        provider.setMdsalManager(mdsalManager);
        provider.setEntityOwnershipService(entityOwnershipService);
        provider.setNotificationService(mock(NotificationService.class));

        // TODO just use rpcProviderRegistry, which IS-A ProviderContext here?
        ProviderContext session = new TestProviderContext(dataBroker);
        provider.onSessionInitiated(session);

        return provider;
    }

    static class TestProviderContext implements ProviderContext {

        private final DataBroker dataBroker;

        TestProviderContext(DataBroker dataBroker) {
            this.dataBroker = dataBroker;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BindingAwareService> T getSALService(Class<T> service) {
            if (service.equals(DataBroker.class)) {
                return (T) dataBroker;
            } else {
                throw new UnsupportedOperationException(service.toGenericString());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
            if (serviceInterface.equals(OpendaylightPortStatisticsService.class)) {
                return (T) mock(MockOpendaylightPortStatisticsRpcService.class);
            } else if (serviceInterface.equals(OpendaylightFlowTableStatisticsService.class)) {
                return (T) mock(MockOpendaylightFlowTableStatisticsRpcService.class);
            } else {
                throw new UnsupportedOperationException(serviceInterface.toGenericString());
            }
        }

        @Override
        public <T extends RpcService> RpcRegistration<T> addRpcImplementation(Class<T> serviceInterface,
                T implementation) throws IllegalStateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> serviceInterface,
                T implementation) throws IllegalStateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L>
            registerRouteChangeListener(L listener) {
            throw new UnsupportedOperationException();
        }

    }

    interface MockOpendaylightPortStatisticsRpcService extends OpendaylightPortStatisticsService, RpcService {
    }

    interface MockOpendaylightFlowTableStatisticsRpcService extends OpendaylightFlowTableStatisticsService, RpcService {
    }

}
