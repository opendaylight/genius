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
import javax.inject.Named;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.UpgradeState;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.Config;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named("geniusUpgradeStateListener") // to distinguish the <bean id=".."> from serviceutils' UpgradeStateListener
@OsgiServiceProvider(classes = UpgradeState.class)
public class UpgradeStateListener extends AbstractSyncDataTreeChangeListener<Config> implements UpgradeState {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeStateListener.class);

    private static final InstanceIdentifier<Config> CONFIG_IID = InstanceIdentifier.create(Config.class);

    private final InstanceIdDataObjectCache<Config> configCache;
    private final UpgradeUtils upgradeUtils;

    @Inject
    public UpgradeStateListener(@OsgiService final DataBroker dataBroker, final Config config,
                                final UpgradeUtils upgradeStateUtils, @OsgiService CacheProvider caches) {
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

        configCache = new InstanceIdDataObjectCache<>(Config.class, dataBroker, CONFIGURATION, CONFIG_IID, caches);
    }

    @Override
    public boolean isUpgradeInProgress() {
        try {
            return configCache.get(null).toJavaUtil()
                    .orElseThrow(() -> new IllegalStateException("isUpgrade Config was deleted from data store?!"))
                    .isUpgradeInProgress();
        } catch (ReadFailedException e) {
            throw new IllegalStateException("ReadFailedException while reading isUpgrade Config from data store", e);
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
