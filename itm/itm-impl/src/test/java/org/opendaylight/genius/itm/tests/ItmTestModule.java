/*
 * Copyright (c) 2016, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import static org.mockito.Mockito.mock;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.daexim.DataImportBootReady;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.interfaces.InterfaceManagerService;
import org.opendaylight.genius.interfacemanager.rpcservice.InterfaceManagerServiceImpl;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.genius.itm.listeners.InterfaceStateListener;
import org.opendaylight.genius.itm.listeners.OvsdbNodeListener;
import org.opendaylight.genius.itm.listeners.TransportZoneListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorChangeListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorIntervalListener;
import org.opendaylight.genius.itm.listeners.VtepConfigSchemaListener;
import org.opendaylight.genius.itm.monitoring.ItmTunnelEventListener;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.genius.lockmanager.impl.LockManagerServiceImpl;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.ops4j.pax.cdi.api.OsgiService;

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
        bind(ItmProvider.class);
        ItmConfig itmConfigObj = new ItmConfigBuilder()
                .setDefTzEnabled(true)
                .setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_VXLAN)
                .setGpeExtensionEnabled(false)
                .build();
        bind(ItmConfig.class).toInstance(itmConfigObj);
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
        bind(LockManagerService.class).to(LockManagerServiceImpl.class);
        DataBroker dataBroker = DataBrokerTestModule.dataBroker();
        bind(DataBroker.class).toInstance(dataBroker);
        bind(DataBroker.class).annotatedWith(OsgiService.class).toInstance(dataBroker);
        bind(InterfaceManagerService.class).to(InterfaceManagerServiceImpl.class);
        bind(IInterfaceManager.class).to(InterfacemgrProvider.class);

        // Bindings to test infra (fakes & mocks)
        TestIMdsalApiManager mdsalManager = TestIMdsalApiManager.newInstance();
        bind(IMdsalApiManager.class).toInstance(mdsalManager);
        bind(TestIMdsalApiManager.class).toInstance(mdsalManager);
        bind(DataImportBootReady.class).annotatedWith(OsgiService.class).toInstance(new DataImportBootReady() {});
        bind(DiagStatusService.class).toInstance(mock(DiagStatusService.class));
    }

}
