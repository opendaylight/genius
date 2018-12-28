/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedMoveWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.recovery.impl.ItmServiceRecoveryHandler;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NotHostedTransportZoneListener extends AbstractSyncDataTreeChangeListener<TepsInNotHostedTransportZone>
        implements RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(NotHostedTransportZoneListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final ItmConfig itmConfig;

    @Inject
    public NotHostedTransportZoneListener(final DataBroker dataBroker,
                                          final ItmConfig itmConfig,
                                          final JobCoordinator jobCoordinator,
                                          final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(NotHostedTransportZones.class).child(TepsInNotHostedTransportZone.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;

        serviceRecoveryRegistry.addRecoverableListener(ItmServiceRecoveryHandler.getServiceRegistryKey(), this);
    }

    @Override
    public void registerListener() {
        register();
    }

    @Override
    public void deregisterListener() {
        close();
    }

    @Override
    public void add(@Nonnull TepsInNotHostedTransportZone tepsInNotHostedTransportZone) {
        LOG.error("Received Transport Zone Add Event: {} in NotHostedTransportZones", tepsInNotHostedTransportZone);
        String zoneName = tepsInNotHostedTransportZone.getZoneName();
        InstanceIdentifier<TransportZone> tzoneIid =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(zoneName))
                        .build();
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        CheckedFuture<Optional<TransportZone>, ReadFailedException> tzone = transaction
                .read(LogicalDatastoreType.CONFIGURATION, tzoneIid);
        try {
            if (tzone.get().isPresent()) {
                LOG.info("Found not hosted transport zone {} in reading TransportZones", zoneName);
                TepsInNotHostedTransportZone tepsInNotHostedTZ = ItmUtils.getNotHostedTransportZone(zoneName,
                        dataBroker).get();
                if (tepsInNotHostedTZ == null) {
                    return ;
                }
                List<UnknownVteps> unVtepsLst = tepsInNotHostedTransportZone.getUnknownVteps();
                List<Vteps> vtepsList = new ArrayList<>();
                if (unVtepsLst != null && !unVtepsLst.isEmpty()) {
                    for (UnknownVteps vteps : unVtepsLst) {
                        BigInteger dpnID = vteps.getDpnId();
                        IpAddress ipAddress = vteps.getIpAddress();

                        Vteps newVtep = ItmUtils.createVtepFromUnKnownVteps(dpnID,ipAddress,ITMConstants.DUMMY_PORT);
                        vtepsList.add(newVtep);

                        // Enqueue 'remove TEP from TepsNotHosted list' operation
                        // into DataStoreJobCoordinator
                        ItmTepsNotHostedRemoveWorker removeWorker = new ItmTepsNotHostedRemoveWorker(
                                zoneName, ipAddress, dpnID, dataBroker);
                        jobCoordinator.enqueueJob(zoneName, removeWorker);
                    }
                    ItmTepsNotHostedMoveWorker
                            moveWorker = new ItmTepsNotHostedMoveWorker(vtepsList, zoneName, dataBroker);
                    jobCoordinator.enqueueJob(zoneName, moveWorker);

                }
            } else {
                LOG.info("Not Found not hosted transport zone {} in reading TransportZones!!! Nothing to do.",
                        zoneName);
                //No action needed.
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error in reading TransportZones{}", e);
        }
    }
}
