/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for Internal Tunnel in Configuration DS.
 * This deletes the entries incase of upgrade from non-ItmDirectTunnel way to ItmDirectTunnel.
 */

public class InternalTunnelListener extends AbstractSyncDataTreeChangeListener<InternalTunnel> {
    private static final Logger LOG = LoggerFactory.getLogger(InternalTunnelListener.class);
    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;

    public InternalTunnelListener(final DataBroker dataBroker, final JobCoordinator coordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(InternalTunnel.class));
        this.dataBroker = dataBroker;
        this.coordinator = coordinator;
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<InternalTunnel> key, @Nonnull InternalTunnel interfaceOld) {
        LOG.debug("REMOVE hit in InternalTunnelListenerITM");
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<InternalTunnel> key, @Nonnull InternalTunnel internalTunnelOld,
                       @Nonnull InternalTunnel internalTunnelNew) {
        LOG.debug("UPDATE hit in InternalTunnelListenerITM");
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<InternalTunnel> key, @Nonnull InternalTunnel internalTunnel) {
        LOG.debug("ADD of Interface {} received in InternalTunnelListenerITM");
        RendererInternalTunnelDeleteWorker configWorker = new RendererInternalTunnelDeleteWorker(key, internalTunnel);
        coordinator.enqueueJob(getKeyForEnqueuing(internalTunnel), configWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    private String getKeyForEnqueuing(InternalTunnel internalTunnel) {
        return "tun" + internalTunnel.getSourceDPN() + internalTunnel.getDestinationDPN();
    }

    private static class RendererInternalTunnelDeleteWorker implements Callable<List<ListenableFuture<Void>>> {
        final InstanceIdentifier<InternalTunnel> key;
        final InternalTunnel internalTunnel;

        RendererInternalTunnelDeleteWorker(InstanceIdentifier<InternalTunnel> key,
                                                  InternalTunnel internalTunnelNew) {
            this.key = key;
            this.internalTunnel = internalTunnelNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            LOG.debug("Deleting Internal Tunnel Config for src Dpn {}, Dst Dpn {} as ITM direct tunnel is enabled",
                    internalTunnel.getSourceDPN(), internalTunnel.getDestinationDPN());
            ITMBatchingUtils.delete(key, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "RendererInternalTunnelDeleteWorker{"
                    + "key=" + key
                    + ", internalTunnel=" + internalTunnel + '\''
                    + '}';
        }
    }
}