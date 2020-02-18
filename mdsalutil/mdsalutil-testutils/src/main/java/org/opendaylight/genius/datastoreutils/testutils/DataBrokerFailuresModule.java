/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractBaseDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;

/**
 * Guice Module which correctly binds the {@link DataBrokerFailures}.
 *
 * @author Michael Vorburger.ch
 */
public class DataBrokerFailuresModule extends AbstractModule {

    private final DataBroker realDataBroker;

    public DataBrokerFailuresModule(DataBroker realDataBroker) {
        this.realDataBroker = realDataBroker;
    }

    public DataBrokerFailuresModule() throws Exception{
        AbstractDataBrokerTest test = new AbstractDataBrokerTest();
        test.setup();
        this.realDataBroker = test.getDataBroker();
    }




    /*public DataBrokerFailuresModule(DataBroker realDataBroker) {
        this.realDataBroker = realDataBroker;
    }

    public DataBrokerFailuresModule() {
        //this(DataBrokerTestModule.dataBroker());
    }*/

    /*private AbstractBaseDataBrokerTest test;

    public DataBrokerFailuresModule() throws Exception {
         test = new AbstractBaseDataBrokerTest() {
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
    }*/


    @Override
    protected void configure() {
        DataBrokerFailuresImpl testableDataBroker = new DataBrokerFailuresImpl(realDataBroker);
        bind(DataBroker.class).toInstance(testableDataBroker);
        // bind(DataBroker.class).annotatedWith(Reference.class).toInstance(testableDataBroker);
        bind(DataBrokerFailures.class).toInstance(testableDataBroker);
    }
}
