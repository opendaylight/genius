/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.networkutils.test;

import static org.mockito.Mockito.mock;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.daexim.DataImportBootReady;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.lockmanager.impl.LockManagerServiceImpl;
import org.opendaylight.genius.networkutils.VniUtils;
import org.opendaylight.genius.networkutils.impl.VniUtilsImpl;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.networkutils.config.rev181129.NetworkConfig;

public class NetworkUtilTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() {
        DataBrokerTestModule dataBrokerTestModule = new DataBrokerTestModule(false);
        DataBroker dataBroker = dataBrokerTestModule.getDataBroker();
        bind(DataBroker.class).toInstance(dataBroker);
        bind(NetworkConfig.class).toInstance(mock(NetworkConfig.class));
        bind(IdManagerService.class).to(IdManager.class);
        bind(DataImportBootReady.class).toInstance(new DataImportBootReady() {});
        bind(LockManagerService.class).to(LockManagerServiceImpl.class);
        bind(VniUtils.class).to(VniUtilsImpl.class);
    }
}
