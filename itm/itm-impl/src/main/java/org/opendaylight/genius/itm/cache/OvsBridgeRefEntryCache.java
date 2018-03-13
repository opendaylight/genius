/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for ovsBridgeRefEntry creation/removal/update in Operational DS
 * and update the bridgeRefEntryCache as per changes in DS.
 *
 */
@Singleton
public class OvsBridgeRefEntryCache implements ClusteredDataTreeChangeListener<OvsBridgeRefEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(OvsBridgeRefEntryCache.class);

    private ListenerRegistration<OvsBridgeRefEntryCache> registration;
    private final DataTreeIdentifier<OvsBridgeRefEntry> treeId =
            new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
    private static ConcurrentMap<BigInteger, OvsBridgeRefEntry> ovsBridgeRefEntryMap = new ConcurrentHashMap<>();

    @Inject
    @SuppressWarnings("checkstyle:IllegalCatch")
    public OvsBridgeRefEntryCache(DataBroker dataBroker, final IInterfaceManager interfaceManager) {
        try {
            LOG.trace("Registering on path: {}", treeId);
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                LOG.debug("ITM Direct Tunnels is Enabled, hence registering this listener");
                registration = dataBroker.registerDataTreeChangeListener(treeId, OvsBridgeRefEntryCache.this);
            } else {
                LOG.debug("ITM Direct Tunnels is not Enabled, therefore not registering this listener");
            }
        } catch (final Exception e) {
            LOG.warn("CacheBridgeRefEntryConfigListener registration failed", e);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    private InstanceIdentifier<OvsBridgeRefEntry> getWildcardPath() {
        return InstanceIdentifier.create(OvsBridgeRefInfo.class).child(OvsBridgeRefEntry.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<OvsBridgeRefEntry>> changes) {
        for (DataTreeModification<OvsBridgeRefEntry> change : changes) {
            final DataObjectModification<OvsBridgeRefEntry> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    ovsBridgeRefEntryMap.remove(mod.getDataBefore().getKey().getDpid());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    ovsBridgeRefEntryMap.put(mod.getDataBefore().getKey().getDpid(), mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    public OvsBridgeRefEntry get(BigInteger key) {
        return ovsBridgeRefEntryMap.get(key);
    }
}
