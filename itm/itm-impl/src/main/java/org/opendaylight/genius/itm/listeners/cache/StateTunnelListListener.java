/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners.cache;

import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataChangeListenerBase;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StateTunnelListListener extends AsyncClusteredDataTreeChangeListenerBase<StateTunnelList,StateTunnelListListener>
        implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(StateTunnelListListener.class);
    private final DataBroker broker;

    /**
     * Responsible for listening to tunnel interface state change
     *
     */
    @Inject
     public StateTunnelListListener(final DataBroker broker) {
         super(StateTunnelList.class, StateTunnelListListener.class);
             this.broker = broker;
         try {
             registerListener(LogicalDatastoreType.OPERATIONAL, broker);
          } catch (final Exception e) {
             LOG.error("StateTunnelListListener DataChange listener registration fail!", e);
         }
    }

    @PostConstruct
    public void start() throws Exception {
        LOG.info("Tunnel Interface State Listener Started");
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        LOG.info("Tunnel Interface State Listener Closed");
    }

    @Override
    protected void remove(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList del) {
        DataStoreCache.remove(ITMConstants.TUNNEL_STATE_CACHE_NAME, del.getTunnelInterfaceName());
    }

    @Override
    protected void update(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList original,
    StateTunnelList update) {
        DataStoreCache.add(ITMConstants.TUNNEL_STATE_CACHE_NAME, update.getTunnelInterfaceName(), update);
    }

    @Override
    protected void add(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList add) {
        DataStoreCache.add(ITMConstants.TUNNEL_STATE_CACHE_NAME, add.getTunnelInterfaceName(), add);
    }

    @Override
    protected StateTunnelListListener getDataTreeChangeListener() {
        return this;
    }

    @Override
    protected InstanceIdentifier<StateTunnelList> getWildCardPath() {
        return InstanceIdentifier.builder(TunnelsState.class).
                child(StateTunnelList.class).build();
    }

}
