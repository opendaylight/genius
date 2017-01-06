/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import static org.mockito.Mockito.mock;

import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.listeners.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.itm.impl.ITMManager;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.genius.itm.listeners.cache.DpnTepsInfoListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringIntervalListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringListener;
import org.opendaylight.genius.itm.listeners.cache.StateTunnelListListener;
import org.opendaylight.genius.itm.monitoring.ItmTunnelEventListener;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.lockmanager.LockManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;

/**
 * Dependency Injection Wiring for {@link ItmTepAutoConfigTest}.
 *
 * @author Michael Vorburger
 * @author Tarun Thakur
 */
public class ItmTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() throws Exception {
        // Bindings for services from this project
        bind(ItmRpcService.class).to(ItmManagerRpcService.class);
        bind(ItmConfig.class).toInstance(mock(ItmConfig.class));
        bind(ITMManager.class);
        bind(ItmProvider.class);


        bind(ItmMonitoringIntervalListener.class);
        bind(DpnTepsInfoListener.class);
        bind(StateTunnelListListener.class);
        bind(ItmMonitoringListener.class);

        bind(TunnelMonitorIntervalListener.class);
        bind(TransportZoneListener.class);
        bind(OvsdbNodeListener.class);
        bind(InterfaceStateListener.class);
        bind(VtepConfigSchemaListener.class);
        bind(TunnelMonitorChangeListener.class);
        bind(ItmTunnelEventListener.class);


        // Bindings for external services to "real" implementations
        bind(IdManagerService.class).to(IdManager.class);
        bind(LockManagerService.class).to(LockManager.class);
        DataBroker dataBroker = DataBrokerTestModule.dataBroker();
        bind(DataBroker.class).toInstance(dataBroker);

        // Bindings to test infra (fakes & mocks)
        TestIMdsalApiManager mdsalManager = TestIMdsalApiManager.newInstance();
        bind(IMdsalApiManager.class).toInstance(mdsalManager);
        bind(TestIMdsalApiManager.class).toInstance(mdsalManager);
    }

}
