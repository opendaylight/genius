/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.networkutils.test;

import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import org.opendaylight.daexim.DataImportBootReady;
import org.opendaylight.genius.idmanager.IdManager;
import org.opendaylight.genius.lockmanager.impl.LockManagerServiceImpl;
import org.opendaylight.genius.networkutils.RDUtils;
import org.opendaylight.genius.networkutils.VniUtils;
import org.opendaylight.genius.networkutils.impl.RDUtilsImpl;
import org.opendaylight.genius.networkutils.impl.VniUtilsImpl;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractBaseDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.networkutils.config.rev181129.NetworkConfig;

public class NetworkUtilTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() throws Exception {
        AbstractBaseDataBrokerTest test = new AbstractBaseDataBrokerTest() {
            @Override
            protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
                return new AbstractDataBrokerTestCustomizer() {
                    @Override
                    public ListeningExecutorService getCommitCoordinatorExecutor() {
                        return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
                    }
                };
            }
        };
        test.setup();
        DataBroker dataBroker = test.getDataBroker();
        bind(DataBroker.class).toInstance(dataBroker);
        bind(NetworkConfig.class).toInstance(mock(NetworkConfig.class));
        bind(IdManagerService.class).to(IdManager.class);
        bind(DataImportBootReady.class).toInstance(new DataImportBootReady() {});
        bind(LockManagerService.class).to(LockManagerServiceImpl.class);
        bind(VniUtils.class).to(VniUtilsImpl.class);
        bind(RDUtils.class).to(RDUtilsImpl.class);
    }
}
