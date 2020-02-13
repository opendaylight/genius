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
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeInterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for bridgeEntry creation/removal/update in Configuration
 * DS and update the bridgeEntryCache as per changes in DS.
 *
 */
@Singleton
public class CacheBridgeEntryConfigListener implements ClusteredDataTreeChangeListener<BridgeEntry> {

    private static final Logger LOG = LoggerFactory.getLogger(CacheBridgeEntryConfigListener.class);

    private final InterfaceMetaUtils interfaceMetaUtils;
    private final ListenerRegistration<CacheBridgeEntryConfigListener> registration;
    private final DataTreeIdentifier<BridgeEntry> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.create(BridgeInterfaceInfo.class).child(BridgeEntry.class));

    @Inject
    public CacheBridgeEntryConfigListener(@Reference final DataBroker dataBroker,
                                          final InterfaceMetaUtils interfaceMetaUtils) {
        LOG.trace("Registering on path: {}", treeId);
        this.interfaceMetaUtils = interfaceMetaUtils;
        registration = dataBroker.registerDataTreeChangeListener(treeId, CacheBridgeEntryConfigListener.this);
    }

    @PreDestroy
    public void close() {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<BridgeEntry>> changes) {
        for (DataTreeModification<BridgeEntry> change : changes) {
            final DataObjectModification<BridgeEntry> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    if (mod.getDataBefore() != null) {
                        interfaceMetaUtils.removeFromBridgeEntryCache(mod.getDataBefore());
                    }
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    interfaceMetaUtils.addBridgeEntryToCache(mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }
}
