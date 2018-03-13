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
public class OvsBridgeEntryConfigCache implements ClusteredDataTreeChangeListener<OvsBridgeEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(OvsBridgeEntryConfigCache.class);

    private ListenerRegistration<OvsBridgeEntryConfigCache> registration;
    private final DataTreeIdentifier<OvsBridgeEntry> treeId =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());
    private ConcurrentMap<BigInteger, OvsBridgeEntry> ovsBridgeEntryMap = new ConcurrentHashMap<>();

    @Inject
    @SuppressWarnings("checkstyle:IllegalCatch")
    public OvsBridgeEntryConfigCache(final DataBroker dataBroker, final IInterfaceManager interfaceManager) {
        try {
            LOG.trace("Registering on path: {}", treeId);
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                LOG.debug("ITM Direct Tunnels is Enabled, hence registering this listener");
                registration = dataBroker.registerDataTreeChangeListener(treeId, OvsBridgeEntryConfigCache.this);
            } else {
                LOG.debug("ITM Direct Tunnels is not Enabled, therefore not registering this listener");
            }
        } catch (final Exception e) {
            LOG.warn("OvsBridgeEntryConfigCache registration failed", e);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    private InstanceIdentifier<OvsBridgeEntry> getWildcardPath() {
        return InstanceIdentifier.create(BridgeTunnelInfo.class).child(OvsBridgeEntry.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<OvsBridgeEntry>> changes) {
        for (DataTreeModification<OvsBridgeEntry> change : changes) {
            final DataObjectModification<OvsBridgeEntry> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    ovsBridgeEntryMap.remove(mod.getDataBefore().getDpid());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    ovsBridgeEntryMap.put(mod.getDataAfter().getDpid(), mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    public OvsBridgeEntry get(BigInteger dpnId) {
        return ovsBridgeEntryMap.get(dpnId);
    }
}
