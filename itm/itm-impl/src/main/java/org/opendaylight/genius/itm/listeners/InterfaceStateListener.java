/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateAddHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateRemoveHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateUpdateHelper;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceStateListener extends AsyncDataTreeChangeListenerBase<Interface, InterfaceStateListener>
        implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStateListener.class);

    private final DataBroker broker;
    private final IInterfaceManager ifaceManager;
    private final ItmTunnelAggregationHelper tunnelAggregationHelper;

    @Inject
    public InterfaceStateListener(final DataBroker dataBroker,IInterfaceManager iinterfacemanager,
            final ItmTunnelAggregationHelper tunnelAggregation) {

        super(Interface.class, InterfaceStateListener.class);
        this.broker = dataBroker;
        this.ifaceManager = iinterfacemanager;
        this.tunnelAggregationHelper = tunnelAggregation;
    }

    @PostConstruct
    public void start() {

        registerListener(this.broker);
        LOG.info("Interface state listener Started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("Interface state listener Closed");
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void registerListener(final DataBroker db) {

        try {
            registerListener(LogicalDatastoreType.OPERATIONAL,db);
        } catch (final Exception e) {
            LOG.error("ITM Interfaces State listener registration fail!", e);
            throw new IllegalStateException("ITM Interfaces State listener registration failed.", e);
        }
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected InterfaceStateListener getDataTreeChangeListener() {
        return this;
    }

    @Override
    protected void add(InstanceIdentifier<Interface> identifier, Interface iface) {

        LOG.trace("Interface added: {}", iface);
        if (ItmUtils.isItmIfType(iface.getType())) {
            LOG.debug("Interface of type Tunnel added: {}", iface.getName());
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            ItmTunnelAddWorker itmTunnelAddWorker = new ItmTunnelAddWorker(iface);
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + iface.getName(), itmTunnelAddWorker);
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper.updateLogicalTunnelState(iface, ItmTunnelAggregationHelper.ADD_TUNNEL, broker);
            }
        }
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> identifier, Interface iface) {

        LOG.trace("Interface deleted: {}", iface);
        if (ItmUtils.isItmIfType(iface.getType())) {
            LOG.debug("Tunnel interface deleted: {}", iface.getName());
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            ItmTunnelRemoveWorker itmTunnelRemoveWorker = new ItmTunnelRemoveWorker(iface);
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + iface.getName(), itmTunnelRemoveWorker);
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper.updateLogicalTunnelState(iface, ItmTunnelAggregationHelper.DEL_TUNNEL, broker);
            }
        }
    }

    @Override
    protected void update(InstanceIdentifier<Interface> identifier, Interface original, Interface update) {
        /*
         * update contains only delta, may not include iftype Note: This assumes
         * type can't be edited on the fly
         */
        if (ItmUtils.isItmIfType(original.getType())) {
            LOG.trace("Interface updated. Old: {} New: {}", original, update);
            OperStatus operStatus = update.getOperStatus();
            if (!Objects.equals(original.getOperStatus(), update.getOperStatus())) {
                LOG.debug("Tunnel Interface {} changed state to {}", original.getName(), operStatus);
                DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
                ItmTunnelUpdateWorker itmTunnelUpdateWorker = new ItmTunnelUpdateWorker(original, update);
                jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + original.getName(), itmTunnelUpdateWorker);
            }
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper.updateLogicalTunnelState(original, update,
                                                                 ItmTunnelAggregationHelper.MOD_TUNNEL, broker);
            }
        }
    }

    private class ItmTunnelAddWorker implements Callable<List<ListenableFuture<Void>>> {
        private Interface iface;

        ItmTunnelAddWorker(Interface iface) {
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return ItmTunnelStateAddHelper.addTunnel(iface,ifaceManager, broker);
        }

    }

    private class ItmTunnelRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        private Interface iface;

        ItmTunnelRemoveWorker(Interface iface) {
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return ItmTunnelStateRemoveHelper.removeTunnel(iface, broker);
        }

    }

    private class ItmTunnelUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        private Interface updatedIface;
        private Interface originalIface;

        ItmTunnelUpdateWorker(Interface originalIface, Interface updatedIface) {
            this.updatedIface = updatedIface;
            this.originalIface = updatedIface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            return ItmTunnelStateUpdateHelper.updateTunnel(updatedIface, broker);
        }

    }
}
