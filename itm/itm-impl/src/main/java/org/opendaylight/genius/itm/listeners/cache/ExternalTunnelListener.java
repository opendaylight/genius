/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ExternalTunnelListener extends
        AsyncClusteredDataTreeChangeListenerBase<ExternalTunnel,ExternalTunnelListener>
        implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalTunnelListener.class);
    private final DataBroker broker;

    @Inject
    public ExternalTunnelListener(final DataBroker dataBroker) {
        super(ExternalTunnel.class, ExternalTunnelListener.class);
        this.broker = dataBroker;
        registerListener(LogicalDatastoreType.CONFIGURATION, this.broker);
    }

    @PostConstruct
    public void start() {
        LOG.info("Tunnel Interface State Listener Started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("Tunnel Interface State Listener Closed");
    }

    @Override
    protected void remove(InstanceIdentifier<ExternalTunnel> identifier, ExternalTunnel del) {
        ItmUtils.itmCache.removeExternalTunnelfromExternalTunnelKeyCache(del.getKey());
    }

    @Override
    protected void update(InstanceIdentifier<ExternalTunnel> identifier, ExternalTunnel original,
                          ExternalTunnel update) {
        ItmUtils.itmCache.addExternalTunnelKeyToExternalTunnelCache(update);
    }

    @Override
    protected void add(InstanceIdentifier<ExternalTunnel> identifier, ExternalTunnel add) {
        ItmUtils.itmCache.addExternalTunnelKeyToExternalTunnelCache(add);
    }

    @Override
    protected ExternalTunnelListener getDataTreeChangeListener() {
        return this;
    }

    @Override
    protected InstanceIdentifier<ExternalTunnel> getWildCardPath() {
        return InstanceIdentifier.create(ExternalTunnelList.class).child(ExternalTunnel.class);
    }

}
