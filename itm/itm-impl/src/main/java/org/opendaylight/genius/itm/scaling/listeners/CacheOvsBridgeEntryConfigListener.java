/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.scaling.listeners;

import java.util.Collection;
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
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.BridgeTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for bridgeEntry creation/removal/update in Configuration DS
 * and update the OvsbridgeEntryCache as per changes in DS.
 *
 */
@Singleton
public class CacheOvsBridgeEntryConfigListener implements ClusteredDataTreeChangeListener<OvsBridgeEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(CacheOvsBridgeEntryConfigListener.class);

    private ListenerRegistration<CacheOvsBridgeEntryConfigListener> registration;
    private final DataTreeIdentifier<OvsBridgeEntry> treeId =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());

    @Inject
    @SuppressWarnings("checkstyle:IllegalCatch")
    public CacheOvsBridgeEntryConfigListener(final DataBroker dataBroker, final IInterfaceManager interfaceManager) {
        try {
            LOG.trace("Registering on path: {}", treeId);
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                LOG.debug("ITM Direct Tunnels is Enabled, hence registering this listener");
                registration = dataBroker.registerDataTreeChangeListener(treeId,
                        CacheOvsBridgeEntryConfigListener.this);
            } else {
                LOG.debug("ITM Direct Tunnels is not Enabled, therefore not registering this listener");
            }
        } catch (final Exception e) {
            LOG.warn("CacheOvsBridgeEntryConfigListener registration failed", e);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    protected InstanceIdentifier<OvsBridgeEntry> getWildcardPath() {
        return InstanceIdentifier.create(BridgeTunnelInfo.class).child(OvsBridgeEntry.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<OvsBridgeEntry>> changes) {
        for (DataTreeModification<OvsBridgeEntry> change : changes) {
            final DataObjectModification<OvsBridgeEntry> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    TunnelMetaUtils.removeFromBridgeEntryCache(mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    TunnelMetaUtils.addBridgeEntryToCache(mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }


}
