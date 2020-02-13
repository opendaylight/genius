/*
 * Copyright © 2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.test.AbstractBaseDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.controller.sal.core.compat.LegacyDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;

/**
 * Legacy (Controller) DataBroker test wiring which wraps an MD-SAL DataBroker.
 */
public class WrappingDataBrokerTestWiring {
    private final AbstractBaseDataBrokerTest test;

    public WrappingDataBrokerTestWiring(org.opendaylight.mdsal.dom.api.DOMDataBroker domDataBroker) throws Exception {
        test = new AbstractBaseDataBrokerTest() {
            @Override
            protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
                return new AbstractDataBrokerTestCustomizer() {
                    @Override
                    public ListeningExecutorService getCommitCoordinatorExecutor() {
                        return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
                    }

                    @Override
                    public DOMDataBroker createDOMDataBroker() {
                        return new LegacyDOMDataBrokerAdapter(domDataBroker);
                    }
                };
            }
        };
        test.setup();
    }

    public DataBroker getDataBroker() {
        return test.getDataBroker();
    }
}
