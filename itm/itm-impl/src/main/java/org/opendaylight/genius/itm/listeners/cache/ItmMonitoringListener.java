/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners.cache;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.listeners.TunnelMonitorChangeListener;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
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
public class ItmMonitoringListener  extends AsyncClusteredDataTreeChangeListenerBase<TunnelMonitorParams, ItmMonitoringListener>
{


  private static final Logger logger = LoggerFactory.getLogger(ItmMonitoringListener.class);

  @Inject
  public ItmMonitoringListener(final DataBroker dataBroker) {
    super(TunnelMonitorParams.class, ItmMonitoringListener.class);
    DataStoreCache.create(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME);

    try {
      registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    } catch (final Exception e) {
      logger.error("ItmMonitoring DataChange listener registration fail!", e);
    }
  }

  @PostConstruct
  public void start() throws Exception {
    logger.info("ItmMonitoring Started");
  }

  @PreDestroy
  public void close() throws Exception {
    logger.info("ItmMonitoring Closed");
  }

  @Override
  protected void remove(InstanceIdentifier<TunnelMonitorParams> key, TunnelMonitorParams dataObjectModification) {
    DataStoreCache.remove(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME, "MonitorParams");
  }

  @Override
  protected void update(InstanceIdentifier<TunnelMonitorParams> identifier, TunnelMonitorParams original,
                        TunnelMonitorParams update) {
    DataStoreCache.add(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME, "MonitorParams", update);
  }

  @Override
  protected void add(InstanceIdentifier<TunnelMonitorParams> identifier, TunnelMonitorParams add) {
    DataStoreCache.add(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME, "MonitorParams", add);


  }

  @Override protected InstanceIdentifier<TunnelMonitorParams> getWildCardPath() {
    return InstanceIdentifier.create(TunnelMonitorParams.class);
  }

  @Override
  protected ItmMonitoringListener getDataTreeChangeListener() {
    return this;
  }

}
