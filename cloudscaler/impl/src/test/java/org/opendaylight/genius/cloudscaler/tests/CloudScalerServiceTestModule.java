/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.cloudscaler.tests;

import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.caches.baseimpl.CacheManagersRegistry;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.testutils.DataBrokerTestModule;

public class CloudScalerServiceTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() {
        DataBroker dataBroker = DataBrokerTestModule.dataBroker();
        bind(DataBroker.class).toInstance(dataBroker);
        bind(CacheManagersRegistry.class).to(CacheManagersRegistryImpl.class);
        bind(CacheProvider.class).to(GuavaCacheProvider.class);
        SingleTransactionDataBroker singleTransactionDataBroker = new SingleTransactionDataBroker(dataBroker);
        bind(SingleTransactionDataBroker.class).toInstance(singleTransactionDataBroker);
    }
}
