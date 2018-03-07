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
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for Internal Tunnel in Configuration DS.
 * This deletes the entries incase of upgrade from non-ItmDirectTunnel way to ItmDirectTunnel.
 */
@Singleton
public class InternalTunnelListener extends AsyncDataTreeChangeListenerBase<InternalTunnel, InternalTunnelListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InternalTunnelListener.class);
    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;

    @Inject
    public InternalTunnelListener(final DataBroker dataBroker, final IInterfaceManager interfaceManager,
                                  final JobCoordinator coordinator) {
        super(InternalTunnel.class, InternalTunnelListener.class);
        this.dataBroker = dataBroker;
        this.coordinator = coordinator;
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.debug("Itm Scale Improvement is Enabled, hence registering InternalTunnelListener");
            this.registerListener(LogicalDatastoreType.CONFIGURATION, this.dataBroker);
        } else {
            LOG.debug("Itm Scale Improvement is not Enabled, therefore not registering InternalTunnelListener");
        }
    }

    @Override
    protected InstanceIdentifier<InternalTunnel> getWildCardPath() {
        return InstanceIdentifier.create(TunnelList.class).child(InternalTunnel.class);
    }

    @Override
    protected InternalTunnelListener getDataTreeChangeListener() {
        return InternalTunnelListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<InternalTunnel> key, InternalTunnel interfaceOld) {
        LOG.debug("REMOVE hit in InternalTunnelListenerITM");
    }

    @Override
    protected void update(InstanceIdentifier<InternalTunnel> key, InternalTunnel internalTunnelOld,
                          InternalTunnel internalTunnelNew) {
        LOG.debug("UPDATE hit in InternalTunnelListenerITM");
    }

    @Override
    protected void add(InstanceIdentifier<InternalTunnel> key, InternalTunnel internalTunnel) {
        LOG.debug("ADD of Interface {} received in InternalTunnelListenerITM");
        RendererInternalTunnelDeleteWorker configWorker = new RendererInternalTunnelDeleteWorker(key, internalTunnel);
        coordinator.enqueueJob(getKeyForEnqueuing(internalTunnel), configWorker, ITMConstants.JOB_MAX_RETRIES);
    }

    private String getKeyForEnqueuing(InternalTunnel internalTunnel) {
        return "tun" + internalTunnel.getSourceDPN() + internalTunnel.getDestinationDPN();
    }

    private static class RendererInternalTunnelDeleteWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<InternalTunnel> key;
        InternalTunnel internalTunnel;

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