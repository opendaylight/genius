/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for OvsdbNode creation/removal/update in Network Topology Operational DS.
 * This is used to handle add/update/remove of TEPs of switches into/from ITM.
 */
@Singleton
public class OvsdbTerminationPointListener implements DataTreeChangeListener<OvsdbTerminationPointAugmentation>,
        AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTerminationPointListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final ItmTepUtils itmTepUtils;
    private final ListenerRegistration<OvsdbTerminationPointListener> registration;

    @Inject
    public OvsdbTerminationPointListener(final DataBroker dataBroker,
                                         final ItmTepUtils itmTepUtils, final JobCoordinator jobCoordinator) {
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.itmTepUtils = itmTepUtils;
        InstanceIdentifier<OvsdbTerminationPointAugmentation> path =
            InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class)
            .child(TerminationPoint.class).augmentation(OvsdbTerminationPointAugmentation.class).build();
        DataTreeIdentifier<OvsdbTerminationPointAugmentation> dtIid =
            new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, path);
        registration = dataBroker.registerDataTreeChangeListener(dtIid, this);
    }

    @Override
    public void close() {
        registration.close();
    }

    private void update(@Nonnull InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                        @Nonnull OvsdbTerminationPointAugmentation tpOld,
                        @Nonnull OvsdbTerminationPointAugmentation tpNew) {
        /*
         * All tunnel config comes once, so ignore subsequent updates for now
         */
        LOG.trace("TpUpdate:old={},new={}",tpOld, tpNew);
        final Map<String, String> externalIds = itmTepUtils.getIfaceExternalIds(tpNew);
        if (handleTpUpdate(tpOld, tpNew, externalIds)) {
            LOG.debug("Creating Tunnel State for ovsdb termination point {}", tpNew.getName());
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + tpNew.getName(),
                () -> addTunnelState(tpNew,externalIds, itmTepUtils));
        }
    }

    private boolean handleTpUpdate(OvsdbTerminationPointAugmentation tpOld, OvsdbTerminationPointAugmentation tpNew,
                                   Map<String, String> externalIds) {
        // OfPort comes in an update, all rest in create, so we will only check for it
        if (!isOfportConfigured(tpOld, tpNew)) {
            LOG.debug("OfPort not present, skipping");
            return false;
        }
        if (!externalIds.containsKey(itmTepUtils.IFACE_EXTERNAL_ID_TUNNEL_TYPE)) {
            LOG.debug("tunnelType missing, skipping");
            return false;
        }
        return true;
    }

    private boolean isOfportConfigured(OvsdbTerminationPointAugmentation tpOld,
                                       OvsdbTerminationPointAugmentation tpNew) {
        if ((tpOld == null || tpOld.getOfport() == null) && tpNew.getOfport() != null) {
            return true;
        }
        return false;
    }

    @Override
    public void onDataTreeChanged(
        @Nonnull Collection<DataTreeModification<OvsdbTerminationPointAugmentation>> changes) {
        for (DataTreeModification<OvsdbTerminationPointAugmentation> change : changes) {
            LOG.trace("Change: {}", change); //TODO: Temporary for debugging.
            final DataObjectModification<OvsdbTerminationPointAugmentation> mod = change.getRootNode();
            final InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier =
                change.getRootPath().getRootIdentifier();
            switch (mod.getModificationType()) {
                case DELETE:
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    update(identifier, mod.getDataBefore(), mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static List<ListenableFuture<Void>> addTunnelState(OvsdbTerminationPointAugmentation tp,
                                                         Map<String, String> externalIds,
                                                         ItmTepUtils itmTepUtils) {

        LOG.debug("Invoking addTunnel state for {} ", tp.getName());
        StateTunnelListKey tlKey = new StateTunnelListKey(tp.getName());
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        StateTunnelList tunnelStateList;
        TunnelOperStatus tunnelOperStatus;
        //TODO: Handle state as per BFD enable/disable
        tunnelOperStatus = TunnelOperStatus.Up;
        boolean tunnelState = true;

        // Create new Tunnel State
        try {
            /*
             * FIXME: A defensive try-catch to find issues without
             * disrupting existing behavior.
             */
            String srcIp = itmTepUtils.getOption(itmTepUtils.TUNNEL_OPTIONS_LOCAL_IP, tp.getOptions());
            String dstIp = itmTepUtils.getOption(itmTepUtils.TUNNEL_OPTIONS_REMOTE_IP, tp.getOptions());
            tunnelStateList = ItmUtils.buildStateTunnelList(itmTepUtils.getDataBroker(), tlKey, tp.getName(),
                tunnelState, tunnelOperStatus,
                new IpAddress(srcIp.toCharArray()),
                new IpAddress((dstIp.toCharArray())),
                externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_DPNID),
                externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_PEER_ID),
                ItmUtils.TUNNEL_TYPE_MAP.get(externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_TUNNEL_TYPE)));
            LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", tunnelStateList, stListId);
            ITMBatchingUtils.write(stListId, tunnelStateList, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        } catch (Exception e) {
            LOG.warn("Exception trying to create tunnel state for {}", tp.getName(), e);
        }

        return Collections.EMPTY_LIST;
    }
}
