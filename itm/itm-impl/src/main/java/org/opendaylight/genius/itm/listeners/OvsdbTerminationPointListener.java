/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelStateAddHelper;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmTepUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for OvsdbNode creation/removal/update in Network Topology Operational DS.
 * This is used to handle add/update/remove of TEPs of switches into/from ITM.
 */
@Singleton
public class OvsdbTerminationPointListener implements DataTreeChangeListener<OvsdbTerminationPointAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTerminationPointListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final ItmTepUtils itmTepUtils;

    @Inject
    public OvsdbTerminationPointListener(final DataBroker dataBroker, final JobCoordinator jobCoordinator,
                                         final ItmTepUtils itmTepUtils) {
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.itmTepUtils = itmTepUtils;
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
            jobCoordinator.enqueueJob(ITMConstants.ITM_PREFIX + tpNew.getName(), () -> ItmTunnelStateAddHelper
                .addTunnel(tpNew, externalIds, dataBroker, itmTepUtils));
        }
    }

    private boolean handleTpUpdate(OvsdbTerminationPointAugmentation tpOld, OvsdbTerminationPointAugmentation tpNew,
                                   Map<String, String> externalIds) {
        // OfPort comes in an update, all rest in create, so we will only check for it
        if (!isOfportConfigured(tpOld, tpNew)) {
            return false;
        }
        if (!externalIds.containsKey(itmTepUtils.IFACE_EXTERNAL_ID_TUNNEL_TYPE)) {
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
}
