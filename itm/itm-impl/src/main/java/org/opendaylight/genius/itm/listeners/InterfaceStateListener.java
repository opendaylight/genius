/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedTunnelsStateCache;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateAddHelper;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateRemoveHelper;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.utils.GeniusEventLogger;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceStateListener extends AbstractSyncDataTreeChangeListener<Interface> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStateListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final IInterfaceManager interfaceManager;
    private final ItmTunnelAggregationHelper tunnelAggregationHelper;
    private final TunnelStateCache tunnelStateCache;
    private final UnprocessedTunnelsStateCache unprocessedTunnelsStateCache;

    @Inject
    public InterfaceStateListener(final DataBroker dataBroker, IInterfaceManager iinterfacemanager,
            final ItmTunnelAggregationHelper tunnelAggregation, JobCoordinator jobCoordinator,
            TunnelStateCache tunnelStateCache, UnprocessedTunnelsStateCache unprocessedTunnelsStateCache) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(InterfacesState.class).child(Interface.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.interfaceManager = iinterfacemanager;
        this.tunnelAggregationHelper = tunnelAggregation;
        this.tunnelStateCache = tunnelStateCache;
        this.unprocessedTunnelsStateCache = unprocessedTunnelsStateCache;
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<Interface> instanceIdentifier, @Nonnull Interface iface) {
        LOG.trace("Interface added: {}", iface);
        if (ItmUtils.isItmIfType(iface.getType())) {
            GeniusEventLogger.logInfo(getClass(), " ADD ", iface.getName());
            LOG.debug("Interface of type Tunnel added: {}", iface.getName());
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + iface.getName(), () -> ItmTunnelStateAddHelper
                    .addTunnel(iface, interfaceManager, dataBroker));
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper.updateLogicalTunnelState(iface, ItmTunnelAggregationHelper.ADD_TUNNEL,
                                                                 dataBroker);
            }
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<Interface> instanceIdentifier, @Nonnull Interface iface) {
        LOG.trace("Interface deleted: {}", iface);
        if (ItmUtils.isItmIfType(iface.getType())) {
            GeniusEventLogger.logInfo(getClass(), " REMOVE ", iface.getName());
            LOG.debug("Tunnel interface deleted: {}", iface.getName());
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + iface.getName(),
                () -> ItmTunnelStateRemoveHelper.removeTunnel(iface, dataBroker));
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper.updateLogicalTunnelState(iface, ItmTunnelAggregationHelper.DEL_TUNNEL,
                                                                 dataBroker);
            }
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<Interface> instanceIdentifier, @Nonnull Interface originalInterface,
                       @Nonnull Interface updatedInterface) {
        /*
         * update contains only delta, may not include iftype Note: This assumes
         * type can't be edited on the fly
         */
        if (ItmUtils.isItmIfType(originalInterface.getType())) {
            GeniusEventLogger.logInfo(getClass(), " UPDATE ", updatedInterface.getName());
            LOG.trace("Interface updated. Old: {} New: {}", originalInterface, updatedInterface);
            OperStatus operStatus = updatedInterface.getOperStatus();
            if (!Objects.equals(originalInterface.getOperStatus(), updatedInterface.getOperStatus())) {
                LOG.debug("Tunnel Interface {} changed state to {}", originalInterface.getName(), operStatus);
                jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + originalInterface.getName(),
                    () -> updateTunnel(updatedInterface));
            }
            if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
                tunnelAggregationHelper.updateLogicalTunnelState(originalInterface, updatedInterface,
                                                                 ItmTunnelAggregationHelper.MOD_TUNNEL, dataBroker);
            }
        }
    }

    private List<ListenableFuture<Void>> updateTunnel(Interface updated) throws Exception {
        LOG.debug("Invoking ItmTunnelStateUpdateHelper for Interface {} ", updated);
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        StateTunnelListKey tlKey = ItmUtils.getTunnelStateKey(updated);
        LOG.trace("TunnelStateKey: {} for interface: {}", tlKey, updated.getName());
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        Optional<StateTunnelList> tunnelsState = tunnelStateCache.get(stListId);
        StateTunnelListBuilder stlBuilder;
        TunnelOperStatus tunnelOperStatus;
        boolean tunnelState = OperStatus.Up.equals(updated.getOperStatus());
        switch (updated.getOperStatus()) {
            case Up:
                tunnelOperStatus = TunnelOperStatus.Up;
                break;
            case Down:
                tunnelOperStatus = TunnelOperStatus.Down;
                break;
            case Unknown:
                tunnelOperStatus = TunnelOperStatus.Unknown;
                break;
            default:
                tunnelOperStatus = TunnelOperStatus.Ignore;
        }
        if (tunnelsState.isPresent()) {
            stlBuilder = new StateTunnelListBuilder(tunnelsState.get());
            stlBuilder.setTunnelState(tunnelState);
            stlBuilder.setOperState(tunnelOperStatus);
            StateTunnelList stList = stlBuilder.build();
            LOG.trace("Batching the updation of tunnel_state: {} for Id: {}", stList, stListId);
            ITMBatchingUtils.update(stListId, stList, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        } else {
            LOG.debug("Tunnel is not yet added but an update has come in for {},so cache it",updated.getName());
            unprocessedTunnelsStateCache.add(updated.getName(),tunnelOperStatus);
        }

        return Collections.singletonList(writeTransaction.submit());
    }
}
