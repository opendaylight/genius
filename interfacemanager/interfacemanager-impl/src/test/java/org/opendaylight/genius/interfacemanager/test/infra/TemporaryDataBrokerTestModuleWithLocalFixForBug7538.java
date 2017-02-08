/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.infra;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.ConstantSchemaAbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;

/**
 * Alternative to official {@link DataBrokerTestModule},
 * which locally works around <a href="https://bugs.opendaylight.org/show_bug.cgi?id=7538">bug 7538</a>,
 * until the (2nd, new; 1st one had to be reverted) permanent fix for that bug in
 * https://git.opendaylight.org/gerrit/#/c/51486/ will be merged in controller.
 *
 * <p>This class should be DELETED again when https://git.opendaylight.org/gerrit/#/c/51486/ is merged,
 * and the normal DataBrokerTestModule should instead be used again in InterfaceManagerTestModule.
 *
 * <p>Please DO NOT just copy/paste this class into other projects!
 *
 * @author Michael Vorburger.ch
 */
public class TemporaryDataBrokerTestModuleWithLocalFixForBug7538 {

    @SuppressWarnings({ "checkstyle:IllegalCatch", "checkstyle:IllegalThrows" })
    public static DataBroker dataBroker() throws RuntimeException {
        try {
            ConstantSchemaAbstractDataBrokerTest dataBrokerTest = new ConstantSchemaAbstractDataBrokerTest() {
                @Override
                protected DataBrokerTestCustomizer createDataBrokerTestCustomizer() {
                    return new DataBrokerTestCustomizer() {
                        @Override
                        public ListeningExecutorService getCommitCoordinatorExecutor() {
                            return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
                        }
                    };
                }
            };
            dataBrokerTest.setup();
            return dataBrokerTest.getDataBroker();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
