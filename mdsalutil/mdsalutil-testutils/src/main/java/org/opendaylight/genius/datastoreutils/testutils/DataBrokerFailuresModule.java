/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import java.util.Set;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.genius.mdsal.testutils.DataBrokerTestWiring;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractBaseDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelList;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Guice Module which correctly binds the {@link DataBrokerFailures}.
 *
 * @author Michael Vorburger.ch
 */
public class DataBrokerFailuresModule extends AbstractModule {

    private DataBroker realDataBroker;

    public DataBrokerFailuresModule(DataBroker realDataBroker) {
        this.realDataBroker = realDataBroker;
    }

    public DataBrokerFailuresModule() throws Exception {
        this(DataBrokerTestWiring.dataBroker());
    }

    /*public DataBrokerFailuresModule() throws Exception {
        AbstractBaseDataBrokerTest dataBrokerTest = new AbstractBaseDataBrokerTest() {
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
        dataBrokerTest.setup();
        this.realDataBroker = dataBrokerTest.getDataBroker();
    }*/


    /*public DataBrokerFailuresModule() throws Exception {
        AbstractDataBrokerTest dataBrokerTest = new AbstractDataBrokerTest() {
            @Override
            protected Set<YangModuleInfo> getModuleInfos() throws Exception {
                return ImmutableSet.of(BindingReflections.getModuleInfo(TwoLevelList.class),
                        BindingReflections.getModuleInfo(TreeComplexUsesAugment.class));
            }

            @Override
            protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
                return new ConcurrentDataBrokerTestCustomizer(true);
            }
        };
        dataBrokerTest.setup();
        this.realDataBroker = dataBrokerTest .getDataBroker();
    }*/




    /*public DataBrokerFailuresModule(DataBroker realDataBroker) {
        this.realDataBroker = realDataBroker;
    }

    public DataBrokerFailuresModule() {
        this(new AbstractDataBrokerTest().getDataBroker());
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
