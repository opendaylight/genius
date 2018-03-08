/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.srm.ServiceRecoveryInterface;
import org.opendaylight.genius.srm.ServiceRecoveryRegistry;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
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

    @Inject
    public ItmTzInstanceRecoveryHandler(DataBroker dataBroker,
                                        JobCoordinator jobCoordinator,
                                        ServiceRecoveryRegistry serviceRecoveryRegistry) {
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    private String buildServiceRegistryKey() {
        return GeniusItmTz.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        LOG.info("Trigerred recovery of ITM Instance - TZ Name {}", entityId);
        try {
            recoverTransportZone(entityId);
        } catch (InterruptedException e) {
            LOG.debug("ITM instance TZ not recovered");
        }
    }

    public void recoverTransportZone(String entityId) throws InterruptedException {
        // Get the transport Zone from the transport Zone Name
        InstanceIdentifier<TransportZone> tzII = ItmUtils.getTransportZoneIdentifierFromName(entityId);
        TransportZone tz = ItmUtils.getTransportZoneFromConfigDS(tzII, dataBroker);
        if (tz != null) {
            // Delete the transportZone and re create it
            ItmTransportZoneRemoveWorker removeWorker = new ItmTransportZoneRemoveWorker(tzII, tz);
            jobCoordinator.enqueueJob(entityId, removeWorker);
            // ITM is not able to work with back to back delete and create so sleep is included
            Thread.sleep(5000);
            ItmTransportZoneAddWorker addWorker = new ItmTransportZoneAddWorker(tzII, tz);
            jobCoordinator.enqueueJob(entityId, addWorker);
        }
    }

    private class ItmTransportZoneAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<TransportZone> tzII;
        TransportZone tz;

        ItmTransportZoneAddWorker(InstanceIdentifier<TransportZone> tzII, TransportZone tz) {
            this.tzII = tzII;
            this.tz = tz;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            writeTransaction.put(LogicalDatastoreType.CONFIGURATION, tzII, tz);
            futures.add(writeTransaction.submit());
            return futures;
        }

        @Override
        public String toString() {
            return "ItmTransportZoneAddWorker{" + "transportZoneId=" + tzII + ", transportZone=" + tz + '}';
        }
    }

    private class ItmTransportZoneRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<TransportZone> tzII;
        TransportZone tz;

        ItmTransportZoneRemoveWorker(InstanceIdentifier<TransportZone> tzII, TransportZone tz) {
            this.tzII = tzII;
            this.tz = tz;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction deleteTransaction = dataBroker.newWriteOnlyTransaction();
            deleteTransaction.delete(LogicalDatastoreType.CONFIGURATION, tzII);
            futures.add(deleteTransaction.submit());
            return futures;
        }

        @Override
        public String toString() {
            return "ItmTransportZoneRemoveWorker{" + "transportZoneId=" + tzII + ", transportZone=" + tz + '}';
        }
    }
}