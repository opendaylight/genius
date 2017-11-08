/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.internal;

import javax.inject.Inject;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.impl.rev170830.Config;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeStateListener extends AsyncClusteredDataTreeChangeListenerBase<Config, UpgradeStateListener>
                                    implements UpgradeState {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeStateListener.class);

    private boolean isUpgradeInProgress;

    @Inject
    public UpgradeStateListener(final DataBroker dataBroker) {
        super(Config.class, UpgradeStateListener.class);
        this.isUpgradeInProgress = false;
        LOG.info("UpgradeStateListener(): isUpgradeInProgress = {}", this.isUpgradeInProgress);
        this.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    public InstanceIdentifier<Config> getWildCardPath() {
        return InstanceIdentifier.create(Config.class);
    }

    public void remove(InstanceIdentifier<Config> key, Config dataObjectModification) {
        this.isUpgradeInProgress = false;
        LOG.info("UpgradeStateListener.remove: isUpgradeInProgress = {}", this.isUpgradeInProgress);
    }

    public void update(InstanceIdentifier<Config> key, Config before, Config after) {
        this.isUpgradeInProgress = after.isUpgradeInProgress();
        LOG.info("UpgradeStateListener.update: isUpgradeInProgress = {}", this.isUpgradeInProgress);
    }

    public void add(InstanceIdentifier<Config> key, Config config) {
        this.isUpgradeInProgress = config.isUpgradeInProgress();
        LOG.info("UpgradeStateListener.add: isUpgradeInProgress = {}", this.isUpgradeInProgress);
    }

    public UpgradeStateListener getDataTreeChangeListener() {
        return this;
    }

    @Override
    public boolean isUpgradeInProgress() {
        return this.isUpgradeInProgress;
    }
}
