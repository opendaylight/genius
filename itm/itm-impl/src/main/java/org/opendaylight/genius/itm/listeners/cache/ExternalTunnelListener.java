/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners.cache;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredAsyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ExternalTunnelListener extends AbstractClusteredAsyncDataTreeChangeListener<ExternalTunnel> {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalTunnelListener.class);

    @Inject
    public ExternalTunnelListener(final DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ExternalTunnelList.class).child(ExternalTunnel.class),
              Executors.newSingleThreadExecutor("ExternalTunnelListener", LOG));
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<ExternalTunnel> instanceIdentifier,
                       @NonNull ExternalTunnel externalTunnel) {
        ItmUtils.ITM_CACHE.removeExternalTunnelfromExternalTunnelKeyCache(externalTunnel.key());
    }

    @Override
    public void update(@NonNull InstanceIdentifier<ExternalTunnel> instanceIdentifier,
                       @NonNull ExternalTunnel originalExternalTunnel, @NonNull ExternalTunnel updatedExternalTunnel) {
        ItmUtils.ITM_CACHE.addExternalTunnelKeyToExternalTunnelCache(updatedExternalTunnel);
    }

    @Override
    public void add(@NonNull InstanceIdentifier<ExternalTunnel> instanceIdentifier,
                    @NonNull ExternalTunnel externalTunnel) {
        ItmUtils.ITM_CACHE.addExternalTunnelKeyToExternalTunnelCache(externalTunnel);
    }

    @Override
    @PreDestroy
    public void close() {
        super.close();
        Executors.shutdownAndAwaitTermination(getExecutorService());
    }
}
