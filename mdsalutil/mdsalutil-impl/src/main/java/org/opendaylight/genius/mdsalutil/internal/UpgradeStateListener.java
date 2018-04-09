/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.internal;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.UpgradeState;
import org.opendaylight.genius.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.Config;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OsgiServiceProvider(classes = UpgradeState.class)
public class UpgradeStateListener extends AbstractClusteredSyncDataTreeChangeListener<Config> implements UpgradeState {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeStateListener.class);

    private final AtomicBoolean isUpgradeInProgress = new AtomicBoolean(false);

    @Inject
    public UpgradeStateListener(@OsgiService final DataBroker dataBroker, final Config config) {
        super(dataBroker, new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Config.class)));
        // When this config value is set from a file it is not accessible via the yang tree...
        // so we just write it once here just in case.
        try {
            SingleTransactionDataBroker.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.create(Config.class), config);
        } catch (TransactionCommitFailedException e) {
            LOG.error("Failed to write mdsalutil config", e);
        }
    }

    @Override
    public boolean isUpgradeInProgress() {
        return isUpgradeInProgress.get();
    }

    @Override
    public void add(@Nonnull Config newDataObject) {
        isUpgradeInProgress.set(newDataObject.isUpgradeInProgress());
        LOG.info("UpgradeStateListener.add: isUpgradeInProgress = {}", newDataObject.isUpgradeInProgress());
    }

    @Override
    public void remove(@Nonnull Config removedDataObject) {
        isUpgradeInProgress.set(false);
        LOG.info("UpgradeStateListener.remove: isUpgradeInProgress = {}", false);
    }

    @Override
    public void update(@Nonnull Config originalDataObject, @Nonnull Config updatedDataObject) {
        isUpgradeInProgress.set(updatedDataObject.isUpgradeInProgress());
        LOG.info("UpgradeStateListener.update: isUpgradeInProgress = {}", updatedDataObject.isUpgradeInProgress());
    }
}
