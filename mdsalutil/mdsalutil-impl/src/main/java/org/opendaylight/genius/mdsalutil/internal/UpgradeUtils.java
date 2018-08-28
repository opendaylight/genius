/*
 * Copyright (c) 2018 Red Hat Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import static org.opendaylight.controller.md.sal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;
import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.upgrade.rev180702.UpgradeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.upgrade.rev180702.UpgradeConfigBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.cdi.api.OsgiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UpgradeUtils {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeUtils.class);

    private final DataBroker dataBroker;
    private final RetryingManagedNewTransactionRunner txRunner;

    @Inject
    public UpgradeUtils(@OsgiService final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.txRunner = new RetryingManagedNewTransactionRunner(dataBroker);
    }

    public void setUpgradeConfig(boolean state) {
        InstanceIdentifier<UpgradeConfig> iid = InstanceIdentifier.create(UpgradeConfig.class);
        UpgradeConfig upgradeConfig = new UpgradeConfigBuilder().setUpgradeInProgress(state).build();
        try {
            txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
                Optional<UpgradeConfig> optPrevUpgradeConfig = tx.read(iid).get();
                if (optPrevUpgradeConfig.isPresent()) {
                    if (!optPrevUpgradeConfig.get().equals(upgradeConfig)) {
                        tx.merge(iid, upgradeConfig, CREATE_MISSING_PARENTS);
                    }
                } else {
                    tx.put(iid, upgradeConfig, CREATE_MISSING_PARENTS);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to update UpgradeState", e);
        }
    }
}
