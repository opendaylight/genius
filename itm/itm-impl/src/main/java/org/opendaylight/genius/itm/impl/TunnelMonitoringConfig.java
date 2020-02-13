/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import java.util.Optional;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains configuration for tunnel monitoring parameters.
 *
 * @author Thomas Pantelis
 */
@Singleton
public class TunnelMonitoringConfig implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelMonitoringConfig.class);

    private static final InstanceIdentifier<TunnelMonitorParams> TUNNEL_MONITOR_PARAMS_PATH =
            InstanceIdentifier.create(TunnelMonitorParams.class);

    private static final InstanceIdentifier<TunnelMonitorInterval> TUNNEL_MONITOR_INTERVAL_PATH =
            InstanceIdentifier.create(TunnelMonitorInterval.class);

    private final InstanceIdDataObjectCache<TunnelMonitorInterval> tunnelMonitorIntervalCache;
    private final InstanceIdDataObjectCache<TunnelMonitorParams> tunnelMonitorParamsCache;

    @Inject
    public TunnelMonitoringConfig(DataBroker dataBroker, CacheProvider cacheProvider) {
        tunnelMonitorIntervalCache = new InstanceIdDataObjectCache<>(TunnelMonitorInterval.class, dataBroker,
                LogicalDatastoreType.CONFIGURATION, TUNNEL_MONITOR_INTERVAL_PATH, cacheProvider);
        tunnelMonitorParamsCache = new InstanceIdDataObjectCache<>(TunnelMonitorParams.class, dataBroker,
                LogicalDatastoreType.CONFIGURATION, TUNNEL_MONITOR_PARAMS_PATH, cacheProvider);
    }

    public boolean isTunnelMonitoringEnabled() {
        try {
            Optional<TunnelMonitorParams> maybeTunnelParams = tunnelMonitorParamsCache.get(TUNNEL_MONITOR_PARAMS_PATH);
            return maybeTunnelParams.isPresent() ? maybeTunnelParams.get().isEnabled()
                    : ITMConstants.DEFAULT_MONITOR_ENABLED;
        } catch (ReadFailedException e) {
            LOG.warn("Read of {} failed", TUNNEL_MONITOR_PARAMS_PATH, e);
            return ITMConstants.DEFAULT_MONITOR_ENABLED;
        }
    }

    public Class<? extends TunnelMonitoringTypeBase> getMonitorProtocol() {
        try {
            Optional<TunnelMonitorParams> maybeTunnelParams = tunnelMonitorParamsCache.get(TUNNEL_MONITOR_PARAMS_PATH);
            return maybeTunnelParams.isPresent() ? maybeTunnelParams.get().getMonitorProtocol()
                    : ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        } catch (ReadFailedException e) {
            LOG.warn("Read of {} failed", TUNNEL_MONITOR_PARAMS_PATH, e);
            return ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        }
    }

    public int getMonitorInterval() {
        try {
            Optional<TunnelMonitorInterval> maybeTunnelInterval =
                    tunnelMonitorIntervalCache.get(TUNNEL_MONITOR_INTERVAL_PATH);
            return maybeTunnelInterval.isPresent() ? maybeTunnelInterval.get().getInterval().toJava()
                    : ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL;
        } catch (ReadFailedException e) {
            LOG.warn("Read of {} failed", TUNNEL_MONITOR_INTERVAL_PATH, e);
            return ITMConstants.DEFAULT_MONITOR_INTERVAL;
        }
    }

    @Override
    @PreDestroy
    public void close() {
        tunnelMonitorIntervalCache.close();
        tunnelMonitorParamsCache.close();
    }
}
