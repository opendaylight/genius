/*
 * Copyright © 2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsal.testutils;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextProvider;

public class DataBrokerTestWiring {
    public static DataBroker dataBroker() throws Exception {
        return new DataBrokerTestWiring().getDataBroker();
    }

    private AbstractConcurrentDataBrokerTest dataBrokerTest;

    public DataBrokerTestWiring() throws Exception {
        dataBrokerTest = new AbstractConcurrentDataBrokerTest(true) {};
        dataBrokerTest.setup();
    }

    public DataBroker getDataBroker() {
        return dataBrokerTest.getDataBroker();
    }

    public DOMDataBroker getDOMDataBroker() {
        return dataBrokerTest.getDomBroker();
    }

    public DOMNotificationRouter getDOMNotificationRouter() {
        return dataBrokerTest.getDataBrokerTestCustomizer().getDomNotificationRouter();
    }

    public DOMSchemaService getSchemaService() {
        return dataBrokerTest.getDataBrokerTestCustomizer().getSchemaService();
    }

    public EffectiveModelContextProvider getEffectiveModelContextProvider() {
        DOMSchemaService schemaService = dataBrokerTest.getDataBrokerTestCustomizer().getSchemaService();
        if (schemaService instanceof EffectiveModelContextProvider) {
            return (EffectiveModelContextProvider) schemaService;
        }
        throw new IllegalStateException(
            "The schema service isn't a SchemaContextProvider, it's a " + schemaService.getClass());
    }
}
