/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeInterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class listens for bridgeEntry creation/removal/update in Configuration DS
 * and update the bridgeEntryCache as per changes in DS.
 *
 */
@Singleton
public class CacheBridgeEntryConfigListener implements ClusteredDataTreeChangeListener<BridgeEntry>{

    private static final Logger LOG = LoggerFactory.getLogger(CacheBridgeEntryConfigListener.class);
    private final DataBroker db;
    private ListenerRegistration<CacheBridgeEntryConfigListener> registration;

    @Inject
    public CacheBridgeEntryConfigListener(final DataBroker dataBroker) {
        this.db = dataBroker;
    }

    @PostConstruct
    public void start() throws Exception {
        registerListener(this.db);
        LOG.info("CacheBridgeEntryConfigListener started");
    }

    @PreDestroy
    public void close() throws Exception {
        if(registration != null) {
            registration.close();
        }
    }

    private void registerListener(DataBroker dataBroker) {
        final DataTreeIdentifier<BridgeEntry> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = dataBroker.registerDataTreeChangeListener(treeId, CacheBridgeEntryConfigListener.this);
        } catch (final Exception e) {
            LOG.warn("CacheBridgeEntryConfigListener registration failed", e);
        }
    }

    protected InstanceIdentifier<BridgeEntry> getWildcardPath() {
        return InstanceIdentifier.create(BridgeInterfaceInfo.class).child(BridgeEntry.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<BridgeEntry>> changes) {
        for (DataTreeModification<BridgeEntry> change : changes) {
        final DataObjectModification<BridgeEntry> mod = change.getRootNode();
            switch (mod.getModificationType()) {
            case DELETE:
                InterfaceMetaUtils.removeFromBridgeEntryCache(mod.getDataBefore());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                InterfaceMetaUtils.addBridgeEntryToCache(mod.getDataAfter());
                break;
            default:
                throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }


}
