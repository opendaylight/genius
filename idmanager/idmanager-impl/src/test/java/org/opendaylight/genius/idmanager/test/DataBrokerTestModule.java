/*
 * Copyright © 2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.test;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

public class DataBrokerTestModule {
    public static DataBroker dataBroker() {
        return new DataBrokerTestModule().getDataBroker();
    }

    private AbstractDataBrokerTest dataBrokerTest;

    // Suppress IllegalCatch because of AbstractDataBrokerTest (change later)
    @SuppressWarnings({ "checkstyle:IllegalCatch", "checkstyle:IllegalThrows" })
    public DataBroker getDataBroker() throws RuntimeException {
        try {
            // This is a little bit "upside down" - in the future,
            // we should probably put what is in AbstractDataBrokerTest
            // into this DataBrokerTestModule, and make AbstractDataBrokerTest
            // use it, instead of the way around it currently is (the opposite);
            // this is just for historical reasons... and works for now.
            dataBrokerTest = new AbstractDataBrokerTest();
            dataBrokerTest.setup();
            return dataBrokerTest.getDataBroker();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DOMDataBroker getDOMDataBroker() {
        return dataBrokerTest.getDomBroker();
    }

    public BindingToNormalizedNodeCodec getBindingToNormalizedNodeCodec() {
        return dataBrokerTest.getDataBrokerTestCustomizer().getBindingToNormalized();
    }

    public DOMNotificationRouter getDOMNotificationRouter() {
        return dataBrokerTest.getDataBrokerTestCustomizer().getDomNotificationRouter();
    }

    public DOMSchemaService getSchemaService() {
        return dataBrokerTest.getDataBrokerTestCustomizer().getSchemaService();
    }

    public SchemaContextProvider getSchemaContextProvider() {
        return (SchemaContextProvider) dataBrokerTest.getDataBrokerTestCustomizer().getSchemaService();
    }
}
