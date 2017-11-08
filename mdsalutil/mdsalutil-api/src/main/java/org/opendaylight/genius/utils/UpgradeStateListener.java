/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.Config;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeStateListener extends AbstractClusteredSyncDataTreeChangeListener<Config> implements UpgradeState {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeStateListener.class);

    private AtomicBoolean isUpgradeInProgress = new AtomicBoolean(false);

    @Inject
    public UpgradeStateListener(final DataBroker dataBroker) {
        super(dataBroker, new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Config.class)));
        LOG.info("UpgradeStateListener(): isUpgradeInProgress = {}", this.isUpgradeInProgress.get());
        register();
    }

    public InstanceIdentifier<Config> getWildCardPath() {
        return InstanceIdentifier.create(Config.class);
    }

    public UpgradeStateListener getDataTreeChangeListener() {
        return this;
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
        LOG.info("UpgradeStateListener.update: isUpgradeInProgress = {}", false);
    }

    @Override
    public void update(@Nonnull Config originalDataObject, Config updatedDataObject) {
        isUpgradeInProgress.set(updatedDataObject.isUpgradeInProgress());
        LOG.info("UpgradeStateListener.remove: isUpgradeInProgress = {}", updatedDataObject.isUpgradeInProgress());
    }
}
