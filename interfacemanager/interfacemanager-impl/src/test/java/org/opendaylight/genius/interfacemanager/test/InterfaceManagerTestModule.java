/*
 * Copyright (c) 2016, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static org.mockito.Mockito.mock;

import java.net.UnknownHostException;

import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.daexim.DataImportBootReady;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.datastoreutils.testutils.AbstractTestableListener;
import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorCountedEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataTreeChangeListener;
import org.opendaylight.genius.datastoreutils.testutils.TestableJobCoordinatorCountedEventsWaiter;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.diagstatus.IfmDiagStatusProvider;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.interfaces.InterfaceManagerService;
import org.opendaylight.genius.interfacemanager.listeners.CacheBridgeEntryConfigListener;
import org.opendaylight.genius.interfacemanager.listeners.CacheBridgeRefEntryListener;
import org.opendaylight.genius.interfacemanager.listeners.HwVTEPConfigListener;
import org.opendaylight.genius.interfacemanager.listeners.HwVTEPTunnelsStateListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceConfigListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceStateListener;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceTopologyStateListener;
import org.opendaylight.genius.interfacemanager.listeners.TerminationPointStateListener;
import org.opendaylight.genius.interfacemanager.listeners.VlanMemberConfigListener;
import org.opendaylight.genius.interfacemanager.rpcservice.InterfaceManagerRpcService;
import org.opendaylight.genius.interfacemanager.rpcservice.InterfaceManagerServiceImpl;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners.FlowBasedServicesConfigListener;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners.FlowBasedServicesInterfaceStateListener;
import org.opendaylight.genius.lockmanager.impl.LockListener;
import org.opendaylight.genius.lockmanager.impl.LockManagerServiceImpl;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.genius.utils.hwvtep.HwvtepNodeHACache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.binding.dom.adapter.BindingDOMEntityOwnershipServiceAdapter;
import org.opendaylight.mdsal.eos.dom.simple.SimpleDOMEntityOwnershipService;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.config.rev160406.IfmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;

/**
 * Dependency Injection Wiring for {@link InterfaceManagerConfigurationTest}.
 *
 * <p>
 * For other *Module examples, please see the AclServiceModule and
 * AclServiceTestModule or ElanServiceTestModule instead.
 *
 * @author Michael Vorburger
 */
public class InterfaceManagerTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() throws UnknownHostException {
        // Bindings for services from this project
        // Bindings for external services to "real" implementations
        // Bindings to test infra (fakes & mocks)

        DataBroker dataBroker = new AbstractDataBrokerTest().getDataBroker();
        bind(DataBroker.class).toInstance(dataBroker);
        bind(DataTreeEventCallbackRegistrar.class).toInstance(mock(DataTreeEventCallbackRegistrar.class));
        bind(ManagedNewTransactionRunner.class).toInstance(mock(ManagedNewTransactionRunner.class));
        bind(DataImportBootReady.class).toInstance(new DataImportBootReady() {});

        bind(LockManagerService.class).to(LockManagerServiceImpl.class);
        bind(LockListener.class);
        bind(IdManagerService.class).to(IdManager.class);
        bind(IInterfaceManager.class).to(InterfacemgrProvider.class);

        TestIMdsalApiManager mdsalManager = TestIMdsalApiManager.newInstance();
        bind(IMdsalApiManager.class).toInstance(mdsalManager);
        bind(TestIMdsalApiManager.class).toInstance(mdsalManager);
        bind(AbstractTestableListener.class).to(TestableDataTreeChangeListener.class);
        bind(JobCoordinatorCountedEventsWaiter.class).to(TestableJobCoordinatorCountedEventsWaiter.class);
        bind(InterfaceManagerService.class).to(InterfaceManagerServiceImpl.class);
        bind(ServiceRecoveryRegistry.class).toInstance(mock(ServiceRecoveryRegistry.class));
        EntityOwnershipService entityOwnershipService = new BindingDOMEntityOwnershipServiceAdapter(
                new SimpleDOMEntityOwnershipService(),
                new DataBrokerTestModule(false).getBindingToNormalizedNodeCodec());
        bind(EntityOwnershipService.class).toInstance(entityOwnershipService);
        bind(EntityOwnershipUtils.class);
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
        bind(InterfaceStateListener.class);
        bind(HwvtepNodeHACache.class).toInstance(mock(HwvtepNodeHACache.class));
        bind(IfmConfig.class).toInstance(mock(IfmConfig.class));
        bind(CacheProvider.class).toInstance(mock(CacheProvider.class));
        bind(IfmDiagStatusProvider.class).toInstance(mock(IfmDiagStatusProvider.class));
    }
}
