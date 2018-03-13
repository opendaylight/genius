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
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DpnsTepsCache implements ClusteredDataTreeChangeListener<DpnsTeps> {
    private static final Logger LOG = LoggerFactory.getLogger(DpnsTepsCache.class);

    private ListenerRegistration<DpnsTepsCache> registration;
    private final DataTreeIdentifier<DpnsTeps> treeId =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());
    private ConcurrentMap<BigInteger, DpnsTeps> dpnsTepsCache = new ConcurrentHashMap<>();
    private ConcurrentMap<String, DpnTepInterfaceInfo> dpnsTepsInfInfoCache = new ConcurrentHashMap<>();

    @Inject
    @SuppressWarnings("checkstyle:IllegalCatch")
    public DpnsTepsCache(final DataBroker dataBroker, final IInterfaceManager interfaceManager) {
        try {
            LOG.trace("Registering on path: {}", treeId);
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                LOG.debug("ITM Direct Tunnels is Enabled, hence registering this listener");
                registration = dataBroker.registerDataTreeChangeListener(treeId, DpnsTepsCache.this);
            } else {
                LOG.debug("ITM Direct Tunnels is not Enabled, therefore not registering this listener");
            }
        } catch (final Exception e) {
            LOG.warn("DpnsTepsCache registration failed", e);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    private InstanceIdentifier<DpnsTeps> getWildcardPath() {
        return InstanceIdentifier.create(DpnTepsState.class).child(DpnsTeps.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<DpnsTeps>> changes) {
        for (DataTreeModification<DpnsTeps> change : changes) {
            final DataObjectModification<DpnsTeps> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    removeFromDpnTepInterfaceCache(mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    addDpnsTepsToCache(mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    private void addDpnsTepsToCache(DpnsTeps dpnsTeps) {
        dpnsTepsCache.put(dpnsTeps.getKey().getSourceDpnId(), dpnsTeps);
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            final String key  = getKey(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                    .setTunnelName(remoteDpns.getTunnelName())
                    .setGroupId(dpnsTeps.getGroupId())
                    .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                    .setIsMonitoringEnabled(remoteDpns.isInternal())
                    .setTunnelType(dpnsTeps.getTunnelType()).build();
            dpnsTepsInfInfoCache.put(key, value);
        }
    }

    private void removeFromDpnTepInterfaceCache(DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            String key = getKey(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            dpnsTepsInfInfoCache.remove(key);
        }
    }

    protected String getKey(BigInteger src, BigInteger dst) {
        return src + ":" + dst;
    }

    public Collection<DpnsTeps> getAllPresent() {
        return dpnsTepsCache.values();
    }

    public DpnsTeps get(BigInteger srcDpnId) {
        return dpnsTepsCache.get(srcDpnId);
    }

    public DpnTepInterfaceInfo getDpnTepInterfaceInfo(BigInteger srcDpnId, BigInteger dstDpnId) {
        return dpnsTepsInfInfoCache.get(getKey(srcDpnId,dstDpnId));
    }
}
