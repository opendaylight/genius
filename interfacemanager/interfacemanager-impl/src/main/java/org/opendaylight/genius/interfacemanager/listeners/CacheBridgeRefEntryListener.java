/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collection;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for bridgeRefEntry creation/removal/update in Operational
 * DS and update the bridgeRefEntryCache as per changes in DS.
 *
 */
@Singleton
public class CacheBridgeRefEntryListener implements ClusteredDataTreeChangeListener<BridgeRefEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(CacheBridgeRefEntryListener.class);

    private final InterfaceMetaUtils interfaceMetaUtils;
    private final ListenerRegistration<CacheBridgeRefEntryListener> registration;
    private final DataTreeIdentifier<BridgeRefEntry> treeId =
            DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.create(BridgeRefInfo.class).child(BridgeRefEntry.class));

    @Inject
    public CacheBridgeRefEntryListener(@Reference DataBroker dataBroker, InterfaceMetaUtils interfaceMetaUtils) {
        LOG.trace("Registering on path: {}", treeId);
        this.interfaceMetaUtils = interfaceMetaUtils;
        registration = dataBroker.registerDataTreeChangeListener(treeId, CacheBridgeRefEntryListener.this);
    }

    @PreDestroy
    public void close() {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<BridgeRefEntry>> changes) {
        for (DataTreeModification<BridgeRefEntry> change : changes) {
            final DataObjectModification<BridgeRefEntry> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    /*
                     * Note: Do we want to retain entry in cache? Ref Entry missing
                     * means OVS being disconnected for now. It will either come
                     * back, or will require config to be deleted.
                     *
                     * Removing for now, can consider this as future optimization.
                     *
                     */
                    if (mod.getDataBefore() != null) {
                        interfaceMetaUtils.removeFromBridgeRefEntryCache(mod.getDataBefore());
                    }
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    interfaceMetaUtils.addBridgeRefEntryToCache(mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }
}
