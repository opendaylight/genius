/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItmTz;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTzInstanceRecoveryHandler implements ServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTzInstanceRecoveryHandler.class);

    private final JobCoordinator jobCoordinator;
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final EntityOwnershipService entityOwnershipService;


    @Inject
    public ItmTzInstanceRecoveryHandler(DataBroker dataBroker,
                                        JobCoordinator jobCoordinator,
                                        ServiceRecoveryRegistry serviceRecoveryRegistry,
                                        EntityOwnershipUtils entityOwnershipUtils,
                                        EntityOwnershipService entityOwnershipService) {
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.entityOwnershipService = entityOwnershipService;
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(getServiceRegistryKey(), this);
    }

    private String getServiceRegistryKey() {
        return GeniusItmTz.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        if (!entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY)) {
            return;
        }
        LOG.info("Trigerred recovery of ITM Instance - TZ Name {}", entityId);
        try {
            recoverTransportZone(entityId);
        } catch (InterruptedException e) {
            LOG.debug("ITM instance TZ not recovered");
        }
    }

    private void recoverTransportZone(String entityId) throws InterruptedException {
        // Get the transport Zone from the transport Zone Name
        InstanceIdentifier<TransportZone> tzII = ItmUtils.getTZInstanceIdentifier(entityId);
        TransportZone tz = ItmUtils.getTransportZoneFromConfigDS(entityId , dataBroker);
        if (tz != null) {
            // Delete the transportZone and re create it
            jobCoordinator.enqueueJob(entityId, () -> Collections.singletonList(
                txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                    tx -> tx.delete(LogicalDatastoreType.CONFIGURATION, tzII))),
                    ITMConstants.JOB_MAX_RETRIES);
            jobCoordinator.enqueueJob(entityId, () -> Collections.singletonList(
                txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                    tx -> tx.put(LogicalDatastoreType.CONFIGURATION, tzII, tz))),
                    ITMConstants.JOB_MAX_RETRIES);
        }
    }
}