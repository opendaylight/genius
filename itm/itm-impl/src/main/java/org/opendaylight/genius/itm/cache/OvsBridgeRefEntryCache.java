/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class OvsBridgeRefEntryCache extends DataObjectCache<OvsBridgeRefEntry> {
    private final IInterfaceManager interfaceManager;
    private ConcurrentMap<BigInteger, OvsBridgeRefEntry> ovsBridgeRefEntryMap = new ConcurrentHashMap<>();

    @Inject
    public OvsBridgeRefEntryCache(DataBroker dataBroker, CacheProvider cacheProvider,
                                  IInterfaceManager interfaceManager) {
        super(OvsBridgeRefEntry.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(OvsBridgeRefInfo.class).child(OvsBridgeRefEntry.class).build(),
                cacheProvider);
        this.interfaceManager = interfaceManager;
    }

    @PostConstruct
    public void start() {
        if (!interfaceManager.isItmDirectTunnelsEnabled()) {
            this.close();
        }
    }

    @Override
    protected void added(InstanceIdentifier<OvsBridgeRefEntry> path, OvsBridgeRefEntry ovsBridgeEntry) {
        ovsBridgeRefEntryMap.put(ovsBridgeEntry.getKey().getDpid(), ovsBridgeEntry);
    }

    @Override
    protected void removed(InstanceIdentifier<OvsBridgeRefEntry> path, OvsBridgeRefEntry ovsBridgeEntry) {
        ovsBridgeRefEntryMap.remove(ovsBridgeEntry.getKey().getDpid());
    }

    public OvsBridgeRefEntry getOvsBridgeRefEntryFromCache(BigInteger dpnId) {
        return ovsBridgeRefEntryMap.get(dpnId);
    }
}
