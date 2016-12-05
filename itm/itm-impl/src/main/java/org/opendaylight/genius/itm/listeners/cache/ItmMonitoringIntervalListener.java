/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners.cache;

import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by edimjai on 8/4/2016.
 */
@Singleton
public class ItmMonitoringIntervalListener extends AsyncClusteredDataTreeChangeListenerBase<TunnelMonitorInterval, ItmMonitoringIntervalListener>
{

  private static final Logger logger = LoggerFactory.getLogger(ItmMonitoringIntervalListener.class);

  @Inject
  public ItmMonitoringIntervalListener(final DataBroker dataBroker) {
    super(TunnelMonitorInterval.class, ItmMonitoringIntervalListener.class);

    try {
      registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    } catch (final Exception e) {
      logger.error("ItmMonitoring Interval listener registration fail!", e);
    }
  }

  @PostConstruct
  public void start() throws Exception {
    logger.info("ItmMonitoringIntervalListener Started");
  }

  @PreDestroy
  public void close() throws Exception {
    logger.info("ItmMonitoringIntervalListener Closed");
  }

  @Override
  protected void remove(InstanceIdentifier<TunnelMonitorInterval> key, TunnelMonitorInterval dataObjectModification) {
    DataStoreCache.remove(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME, "Interval");

  }

  @Override
  protected void update(InstanceIdentifier<TunnelMonitorInterval> identifier, TunnelMonitorInterval original,
                        TunnelMonitorInterval update) {
    DataStoreCache.add(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME, "Interval", update);
  }

  @Override
  protected void add(InstanceIdentifier<TunnelMonitorInterval> identifier, TunnelMonitorInterval add) {
    DataStoreCache.add(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME, "Interval", add);
  }

  @Override protected InstanceIdentifier<TunnelMonitorInterval> getWildCardPath() {
    return InstanceIdentifier.create(TunnelMonitorInterval.class);
  }

  @Override
  protected ItmMonitoringIntervalListener getDataTreeChangeListener() {
    return this;
  }

}
