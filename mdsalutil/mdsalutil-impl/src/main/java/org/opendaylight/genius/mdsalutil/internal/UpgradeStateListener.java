/*
 * Copyright (c) 2017, 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.Config;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Tree Change Listener which sets the initial value of the UpgradeConfig
 * model (in serviceutils) to the value from the (genius!) configuration file (see
 * blueprint), and keeps the (serviceutils) UpgradeConfig model up-to-date in
 * case of changes to the (genius) Config model in the datastore via RESTCONF.
 */
@Singleton
// GENIUS-190: This is NOT @Deprecated (unlike UpgradeStateImpl) and stays until external (not internal) users migrate
public class UpgradeStateListener extends AbstractSyncDataTreeChangeListener<Config> {
    // GENIUS-207: intentionally not using a clustered DTCL here, due to OptimisticLockFailedException

    private static final Logger LOG = LoggerFactory.getLogger(UpgradeStateListener.class);

    private static final InstanceIdentifier<Config> CONFIG_IID = InstanceIdentifier.create(Config.class);

    private final UpgradeUtils upgradeUtils;

    @Inject
    public UpgradeStateListener(@Reference final DataBroker dataBroker, final Config config,
                                final UpgradeUtils upgradeStateUtils) {
        super(dataBroker, new DataTreeIdentifier<>(CONFIGURATION, CONFIG_IID));
        this.upgradeUtils = upgradeStateUtils;

        // When this config value is set from a file it is not accessible via the yang tree...
        // so we just write it once here just in case.
        try {
            upgradeStateUtils.setUpgradeConfig(config.isUpgradeInProgress());
            SingleTransactionDataBroker.syncWrite(dataBroker, CONFIGURATION, CONFIG_IID, config);
        } catch (TransactionCommitFailedException e) {
            LOG.error("Failed to write mdsalutil config", e);
        }
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Config> instanceIdentifier, @Nonnull Config config) {
        upgradeUtils.setUpgradeConfig(config.isUpgradeInProgress());
        LOG.info("UpgradeStateListener.add: isUpgradeInProgress = {}", config.isUpgradeInProgress());
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Config> instanceIdentifier, @Nonnull Config config) {
        upgradeUtils.setUpgradeConfig(false);
        LOG.info("UpgradeStateListener.remove: isUpgradeInProgress = {}", false);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Config> instanceIdentifier,
                       @Nonnull Config originalConfig, @Nonnull Config updatedConfig) {
        upgradeUtils.setUpgradeConfig(updatedConfig.isUpgradeInProgress());
        LOG.info("UpgradeStateListener.update: isUpgradeInProgress = {}", updatedConfig.isUpgradeInProgress());
    }
}
