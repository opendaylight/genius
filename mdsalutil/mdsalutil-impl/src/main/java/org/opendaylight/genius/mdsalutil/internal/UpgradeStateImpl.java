/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import javax.inject.Named;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.mdsalutil.UpgradeState;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsalutil.rev170830.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.upgrade.rev180702.UpgradeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.upgrade.rev180702.UpgradeConfigBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link UpgradeState} (genius) API implementation which makes the
 * UpgradeConfig model (from serviceutils) in the datastore available as a
 * service, with caching.
 *
 * <p>Note that the {@link UpgradeStateListener} (in genius) keeps the
 * UpgradeConfig model (from serviceutils) up-to-date with the {@link Config}
 * model (from genius).
 */
@Singleton
@Named("geniusUpgradeStateListener") // to distinguish the <bean id=".."> from serviceutils' UpgradeStateListener
@OsgiServiceProvider(classes = UpgradeState.class)
public class UpgradeStateImpl implements UpgradeState {

    private static final Logger LOG = LoggerFactory.getLogger(UpgradeStateImpl.class);

    private static final InstanceIdentifier<UpgradeConfig> CONFIG_IID = InstanceIdentifier.create(UpgradeConfig.class);
    private static final UpgradeConfig CONFIG_DEFAULT = new UpgradeConfigBuilder().setUpgradeInProgress(false).build();

    private final InstanceIdDataObjectCache<UpgradeConfig> configCache;

    public UpgradeStateImpl(@OsgiService final DataBroker dataBroker, @OsgiService CacheProvider caches) {
        configCache = new InstanceIdDataObjectCache<>(UpgradeConfig.class, dataBroker, CONFIGURATION, CONFIG_IID,
                caches);
    }

    @Override
    public boolean isUpgradeInProgress() { // TODO throws ReadFailedException
        try {
            return configCache.get(null).toJavaUtil().orElse(CONFIG_DEFAULT).isUpgradeInProgress();
        } catch (ReadFailedException e) {
            // TODO remove catch and propagate to caller; but needs to be caught in netvirt users
            LOG.error("isUpgradeInProgress() read failed, return false, may be wrong!", e);
            return false;
        }
    }
}
