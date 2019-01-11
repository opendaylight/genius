/*
 * Copyright © 2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsal.testutils;

import com.google.inject.AbstractModule;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.testutils.DataBrokerFailures;
import org.opendaylight.mdsal.binding.testutils.DataBrokerFailuresImpl;

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

    public DataBrokerFailuresModule() throws Exception {
        this(DataBrokerTestWiring.dataBroker());
    }

    @Override
    protected void configure() {
        DataBrokerFailuresImpl testableDataBroker = new DataBrokerFailuresImpl(realDataBroker);
        bind(DataBroker.class).toInstance(testableDataBroker);
        // bind(DataBroker.class).annotatedWith(Reference.class).toInstance(testableDataBroker);
        bind(DataBrokerFailures.class).toInstance(testableDataBroker);
    }
}
