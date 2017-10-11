/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.listeners.AbstractAsyncDataTreeChangeListener;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateAddHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateRemoveHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateUpdateHelper;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceStateListener extends AbstractAsyncDataTreeChangeListener<Interface> {
    private final DataBroker dataBroker;
    private final IInterfaceManager interfaceManager;
    private final ItmTunnelAggregationHelper tunnelAggregationHelper;

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStateListener.class);

    @Inject
    public InterfaceStateListener(DataBroker dataBroker, IInterfaceManager interfaceManager,
                                  final ItmTunnelAggregationHelper tunnelAggregationHelper) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(InterfacesState.class).child(Interface.class),
              Executors.newSingleThreadExecutor("InterfaceStateListener", LOG));
        this.dataBroker = dataBroker;
        this.interfaceManager = interfaceManager;
        this.tunnelAggregationHelper = tunnelAggregationHelper;
    }

    @Override
    public void add(@Nonnull Interface newInterface) {
        LOG.trace("Interface added: {}", newInterface);
        if (ItmUtils.isItmIfType(newInterface.getType())) {
            LOG.debug("Interface of type Tunnel added: {}", newInterface.getName());
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + newInterface.getName(), () -> ItmTunnelStateAddHelper
                    .addTunnel(newInterface, interfaceManager, dataBroker));
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper
                        .updateLogicalTunnelState(newInterface, ItmTunnelAggregationHelper.ADD_TUNNEL, dataBroker);
            }
        }
    }

    @Override
    public void remove(@Nonnull Interface removedInterface) {
        LOG.trace("Interface deleted: {}", removedInterface);
        if (ItmUtils.isItmIfType(removedInterface.getType())) {
            LOG.debug("Tunnel interface deleted: {}", removedInterface.getName());
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + removedInterface.getName(),
                () -> ItmTunnelStateRemoveHelper.removeTunnel(removedInterface, dataBroker));
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper
                        .updateLogicalTunnelState(removedInterface, ItmTunnelAggregationHelper.DEL_TUNNEL, dataBroker);
            }
        }
    }

    @Override
    public void update(@Nonnull Interface originalInterface, Interface updatedInterface) {
        /* update contains only delta, may not include iftype Note: This assumes type can't be edited on the fly */
        if (ItmUtils.isItmIfType(originalInterface.getType())) {
            LOG.trace("Interface updated. Old: {} New: {}", originalInterface, updatedInterface);
            OperStatus operStatus = updatedInterface.getOperStatus();
            if (!Objects.equals(originalInterface.getOperStatus(), updatedInterface.getOperStatus())) {
                LOG.debug("Tunnel Interface {} changed state to {}", originalInterface.getName(), operStatus);
                DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();

                jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + originalInterface.getName(),
                    () -> ItmTunnelStateUpdateHelper.updateTunnel(updatedInterface, dataBroker));
            }
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper.updateLogicalTunnelState(originalInterface, updatedInterface,
                                                                 ItmTunnelAggregationHelper.MOD_TUNNEL, dataBroker);
            }
        }
    }

    public void shutdown() {
        getExecutorService().shutdown();
    }
}