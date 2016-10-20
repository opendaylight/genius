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

/**
 * Created by edimjai on 8/4/2016.
 */
public class ItmMonitoringListener  extends AsyncClusteredDataTreeChangeListenerBase<TunnelMonitorParams, ItmMonitoringListener>
    implements AutoCloseable {


  private static final Logger logger = LoggerFactory.getLogger(ItmMonitoringListener.class);

  public ItmMonitoringListener(final DataBroker broker) {
    super(TunnelMonitorParams.class, ItmMonitoringListener.class);

    try {
      registerListener(LogicalDatastoreType.OPERATIONAL, broker);
    } catch (final Exception e) {
      logger.error("ItmMonitoring DataChange listener registration fail!", e);
    }
  }

  @Override
  public void close() {

    logger.info("ItmMonitoring listener Closed");
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
