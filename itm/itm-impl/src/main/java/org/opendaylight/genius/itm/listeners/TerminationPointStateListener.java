/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.confighelpers.TepStateUpdateWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmFlowUtils;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TerminationPointStateListener implements DataTreeChangeListener<OvsdbTerminationPointAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointStateListener.class);
    private final ListenerRegistration<TerminationPointStateListener> registration;
    private final DataTreeIdentifier<OvsdbTerminationPointAugmentation> treeId = new DataTreeIdentifier<>(
        LogicalDatastoreType.OPERATIONAL,
        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
            .child(TerminationPoint.class).augmentation(OvsdbTerminationPointAugmentation.class));

    private final DataBroker dataBroker;
    private final ItmTepUtils itmTepUtils;
    private final ItmFlowUtils itmFlowUtils;
    private final JobCoordinator jobCoordinator;
    private final ItmConfig itmConfig;

    @Inject
    public TerminationPointStateListener(final DataBroker dataBroker, final ItmConfig itmConfig,
                                         final ItmTepUtils itmTepUtils, final ItmFlowUtils itmFlowUtils,
                                         JobCoordinator jobCoordinator) {
        this.dataBroker = dataBroker;
        this.itmTepUtils = itmTepUtils;
        this.itmFlowUtils = itmFlowUtils;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;
        registration = dataBroker.registerDataTreeChangeListener(treeId, TerminationPointStateListener.this);
    }

    private void update(@Nonnull InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                        @Nonnull OvsdbTerminationPointAugmentation tpOld,
                        @Nonnull OvsdbTerminationPointAugmentation tpNew) {
        /*
         * All tunnel config comes once, so ignore subsequent updates for now
         */
        if (isTunnelZoneConfigured(tpNew) && !isTunnelZoneConfigured(tpOld)) {
            LOG.debug("Received Update/Add Notification for ovsdb termination point {}", tpNew.getName());
            TepStateUpdateWorker tepStateUpdateWorker =
                new TepStateUpdateWorker(dataBroker, itmTepUtils, itmFlowUtils, identifier, tpNew);
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + tpNew.getName(), tepStateUpdateWorker);
        }
    }

    private boolean isTunnelZoneConfigured(OvsdbTerminationPointAugmentation tp) {
        if (tp != null && tp.getInterfaceExternalIds() != null) {
            for (InterfaceExternalIds externalId : tp.getInterfaceExternalIds()) {
                if (itmTepUtils.IFACE_EXTERNAL_ID_TUNNEL_ZONE.equals(externalId.getExternalIdKey())) {
                    return true;
                }
            }
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
}
