/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.cloudscaler.tests;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;
import org.opendaylight.controller.md.sal.binding.test.SchemaContextSingleton;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.caches.baseimpl.CacheManagersRegistry;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractBaseDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class CloudScalerServiceTestModule extends AbstractGuiceJsr250Module {

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
        /*****************/
        /*AbstractConcurrentDataBrokerTest dataBrokerTest = new AbstractConcurrentDataBrokerTest(false) {};
        try {
            dataBrokerTest.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        DataBroker dataBroker = dataBrokerTest.getDataBroker();*/

        /*AbstractBaseDataBrokerTest test = new AbstractBaseDataBrokerTest() {
            protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
                return new ConcurrentDataBrokerTestCustomizer(false);
            }
            protected SchemaContext getSchemaContext() throws Exception {
               return SchemaContextSingleton.getSchemaContext(() -> {
                       return super.getSchemaContext();
              });
            }
        };
        test.setup();
        DataBroker dataBroker =test.getDataBroker();*/
        bind(DataBroker.class).toInstance(dataBroker);
        bind(CacheManagersRegistry.class).to(CacheManagersRegistryImpl.class);
        bind(CacheProvider.class).to(GuavaCacheProvider.class);
        SingleTransactionDataBroker singleTransactionDataBroker = new SingleTransactionDataBroker(dataBroker);
        bind(SingleTransactionDataBroker.class).toInstance(singleTransactionDataBroker);
    }
}
