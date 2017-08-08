/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import static org.mockito.Mockito.mock;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.daexim.DataImportBootReady;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMManager;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.genius.itm.listeners.InterfaceStateListener;
import org.opendaylight.genius.itm.listeners.OvsdbNodeListener;
import org.opendaylight.genius.itm.listeners.TransportZoneListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorChangeListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorIntervalListener;
import org.opendaylight.genius.itm.listeners.VtepConfigSchemaListener;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;

/**
 * Dependency Injection Wiring for {@link ItmTest}.
 *
 * @author Michael Vorburger
 * @author Tarun Thakur
 */

public class ItmTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() {
        // Bindings for services from this project
        bind(ItmRpcService.class).to(ItmManagerRpcService.class);
        bind(ITMManager.class);
        bind(ItmProvider.class);
        ItmConfig itmConfigObj = new ItmConfigBuilder()
                .setDefTzEnabled(true)
                .setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_VXLAN)
                .setGpeExtensionEnabled(false)
                .build();
        bind(ItmConfig.class).toInstance(itmConfigObj);
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
        bind(EntityOwnershipService.class).toInstance(mock(EntityOwnershipService.class));
        bind(IdManagerService.class).to(IdManager.class);
        bind(LockManagerService.class).to(LockManager.class);
        bind(DataBroker.class).toInstance(DataBrokerTestModule.dataBroker());
        bind(IInterfaceManager.class).to(InterfacemgrProvider.class);

        // Bindings to test infra (fakes & mocks)
        TestIMdsalApiManager mdsalManager = TestIMdsalApiManager.newInstance();
        bind(IMdsalApiManager.class).toInstance(mdsalManager);
        bind(TestIMdsalApiManager.class).toInstance(mdsalManager);
        bind(DataImportBootReady.class).toInstance(new DataImportBootReady() {});
    }

}
