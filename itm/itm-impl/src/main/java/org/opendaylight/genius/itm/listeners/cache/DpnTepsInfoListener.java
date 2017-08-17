/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners.cache;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Created by Hema Gopalkrishnan on 18-08-2016.
 */
@Singleton
public class DpnTepsInfoListener extends AsyncClusteredDataTreeChangeListenerBase<DPNTEPsInfo,DpnTepsInfoListener>
        implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DpnTepsInfoListener.class);
    private final DataBroker broker;

    /**
     * Responsible for listening to DPNTEPsInfo change.
     *
     */
    @Inject
    public DpnTepsInfoListener(final DataBroker dataBroker) {
        super(DPNTEPsInfo.class, DpnTepsInfoListener.class);
        DataStoreCache.create(ITMConstants.DPN_TEPs_Info_CACHE_NAME) ;
        this.broker = dataBroker;
    }

    @PostConstruct
    public void start() {
        registerListener(LogicalDatastoreType.CONFIGURATION, this.broker);
        LOG.info("dpnTepsInfo Listener Started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("dpnTepsInfo Listener Closed");
    }

    @Override
    protected void remove(InstanceIdentifier<DPNTEPsInfo> identifier, DPNTEPsInfo del) {
        LOG.debug("Remove from cache {} Invoked for data Obj {}",
                ITMConstants.DPN_TEPs_Info_CACHE_NAME, del.getDPNID());
        DataStoreCache.remove(ITMConstants.DPN_TEPs_Info_CACHE_NAME, del.getDPNID());
    }

    @Override
    protected void update(InstanceIdentifier<DPNTEPsInfo> identifier, DPNTEPsInfo original,
                          DPNTEPsInfo update) {
        LOG.debug("Update to cache {} Invoked for data Obj {}",
                ITMConstants.DPN_TEPs_Info_CACHE_NAME, update.getDPNID());
        DataStoreCache.add(ITMConstants.DPN_TEPs_Info_CACHE_NAME, update.getDPNID(), update);
    }

    @Override
    protected void add(InstanceIdentifier<DPNTEPsInfo> identifier, DPNTEPsInfo add) {
        LOG.debug(" Add to cache {} Invoked for data Obj {}",
                ITMConstants.DPN_TEPs_Info_CACHE_NAME, add.getDPNID());
        DataStoreCache.add(ITMConstants.DPN_TEPs_Info_CACHE_NAME, add.getDPNID(), add);
    }

    @Override
    protected DpnTepsInfoListener getDataTreeChangeListener() {
        return this;
    }

    @Override
    protected InstanceIdentifier<DPNTEPsInfo> getWildCardPath() {
        return InstanceIdentifier.builder(DpnEndpoints.class)
                .child(DPNTEPsInfo.class).build();
    }

}
