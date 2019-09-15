/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.cloudscaler.api.TombstonedNodeManager;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OfEndPointCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepRemoveWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedMoveWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.recovery.impl.ItmServiceRecoveryHandler;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.tepsinnothostedtransportzone.UnknownVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
@Singleton
public class TransportZoneListener extends AbstractSyncDataTreeChangeListener<TransportZone>
        implements RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(TransportZoneListener.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final IMdsalApiManager mdsalManager;
    private final ItmConfig itmConfig;
    private final ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker;
    private final ItmInternalTunnelAddWorker itmInternalTunnelAddWorker;
    private final ItmExternalTunnelAddWorker externalTunnelAddWorker;
    private final DPNTEPsInfoCache dpnTEPsInfoCache;
    private final ManagedNewTransactionRunner txRunner;
    private final DataTreeEventCallbackRegistrar eventCallbacks;
    private final TombstonedNodeManager tombstonedNodeManager;

    @Inject
    public TransportZoneListener(final DataBroker dataBroker,
                                 final IMdsalApiManager mdsalManager,
                                 final ItmConfig itmConfig, final JobCoordinator jobCoordinator,
                                 final TunnelMonitoringConfig tunnelMonitoringConfig,
                                 final DPNTEPsInfoCache dpnTEPsInfoCache,
                                 final TunnelStateCache tunnelStateCache,
                                 final DirectTunnelUtils directTunnelUtils,
                                 final DpnTepStateCache dpnTepStateCache, final OvsBridgeEntryCache ovsBridgeEntryCache,
                                 final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                 final IInterfaceManager interfaceManager,
                                 final OfEndPointCache ofEndPointCache,
                                 final ServiceRecoveryRegistry serviceRecoveryRegistry,
                                 final DataTreeEventCallbackRegistrar eventCallbacks,
                                 final TombstonedNodeManager tombstonedNodeManager) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(TransportZones.class).child(TransportZone.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.mdsalManager = mdsalManager;
        this.itmConfig = itmConfig;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        this.tombstonedNodeManager = tombstonedNodeManager;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.eventCallbacks = eventCallbacks;
        initializeTZNode();
        this.itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig, interfaceManager, dpnTepStateCache, ovsBridgeEntryCache,
                ovsBridgeRefEntryCache, tunnelStateCache, directTunnelUtils, ofEndPointCache, itmConfig,
                tombstonedNodeManager);
        this.itmInternalTunnelAddWorker = new ItmInternalTunnelAddWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig, itmConfig, directTunnelUtils, interfaceManager,
                ovsBridgeRefEntryCache, ofEndPointCache, eventCallbacks);
        this.externalTunnelAddWorker = new ItmExternalTunnelAddWorker(itmConfig, dpnTEPsInfoCache);
        serviceRecoveryRegistry.addRecoverableListener(ItmServiceRecoveryHandler.getServiceRegistryKey(),
                this);
    }

    @Override
    public void registerListener() {
        register();
    }

    @Override
    public void deregisterListener() {
        close();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void initializeTZNode() {
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.create(TransportZones.class);
        txRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.CONFIGURATION, tx -> {
            FluentFuture<Boolean> tzones = tx.exists(path);
            if (!tzones.get()) {
                TransportZonesBuilder tzb = new TransportZonesBuilder();
                tx.put(path, tzb.build());
            }
        }).isDone();
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<TransportZone> instanceIdentifier,
                       @NonNull TransportZone transportZone) {
        LOG.debug("Received Transport Zone Remove Event: {}", transportZone);
        boolean allowTunnelDeletion;

        // check if TZ received for removal is default-transport-zone,
        // if yes, then check if it is received from northbound, then
        // do not entertain request and skip tunnels remove operation
        // if def-tz removal request is due to def-tz-enabled flag is disabled or
        // due to change in def-tz-tunnel-type, then allow def-tz tunnels deletion
        if (ITMConstants.DEFAULT_TRANSPORT_ZONE.equalsIgnoreCase(transportZone.getZoneName())) {
            // Get TunnelTypeBase object for tunnel-type configured in config file
            Class<? extends TunnelTypeBase> tunType = ItmUtils.getTunnelType(itmConfig.getDefTzTunnelType());

            if (!itmConfig.isDefTzEnabled() || !Objects.equals(transportZone.getTunnelType(), tunType)) {
                allowTunnelDeletion = true;
            } else {
                // this is case when def-tz removal request is from Northbound.
                allowTunnelDeletion = false;
                LOG.error("Deletion of {} is an incorrect usage",ITMConstants.DEFAULT_TRANSPORT_ZONE);
            }
        } else {
            allowTunnelDeletion = true;
        }

        if (allowTunnelDeletion) {
            //TODO : DPList code can be refactor with new specific class
            // which implement TransportZoneValidator
            EVENT_LOGGER.debug("ITM-Transportzone,TunnelDeletion {}", transportZone.getZoneName());
            List<DPNTEPsInfo> opDpnList = createDPNTepInfo(transportZone);
            List<HwVtep> hwVtepList = createhWVteps(transportZone);
            LOG.trace("Delete: Invoking deleteTunnels in ItmManager with DpnList {}", opDpnList);
            if (!opDpnList.isEmpty() || !hwVtepList.isEmpty()) {
                LOG.trace("Delete: Invoking ItmManager with hwVtep List {} ", hwVtepList);
                jobCoordinator.enqueueJob(transportZone.getZoneName(),
                        new ItmTepRemoveWorker(opDpnList, hwVtepList, transportZone, mdsalManager,
                                itmInternalTunnelDeleteWorker, dpnTEPsInfoCache, txRunner, itmConfig));

                if (transportZone.getVteps() != null && !transportZone.getVteps().isEmpty()) {
                    List<UnknownVteps> unknownVteps = convertVtepListToUnknownVtepList(transportZone.getVteps());
                    LOG.trace("Moving Transport Zone {} to tepsInNotHostedTransportZone Oper Ds.",
                            transportZone.getZoneName());
                    jobCoordinator.enqueueJob(transportZone.getZoneName(),
                            new ItmTepsNotHostedAddWorker(unknownVteps, transportZone.getZoneName(),
                                    dataBroker, txRunner));
                }
            }
        }
    }

    @Override
    public void update(@NonNull InstanceIdentifier<TransportZone> instanceIdentifier,
                       @NonNull TransportZone originalTransportZone, @NonNull TransportZone updatedTransportZone) {
        LOG.debug("Received Transport Zone Update Event: Old - {}, Updated - {}", originalTransportZone,
                updatedTransportZone);
        EVENT_LOGGER.debug("ITM-Transportzone,UPDATE {}", updatedTransportZone.getZoneName());
        List<DPNTEPsInfo> oldDpnTepsList = createDPNTepInfo(originalTransportZone);
        List<DPNTEPsInfo> newDpnTepsList = createDPNTepInfo(updatedTransportZone);
        List<DPNTEPsInfo> oldDpnTepsListcopy = new ArrayList<>();
        oldDpnTepsListcopy.addAll(oldDpnTepsList);
        LOG.trace("oldcopy0 {}", oldDpnTepsListcopy);
        List<DPNTEPsInfo> newDpnTepsListcopy = new ArrayList<>();
        newDpnTepsListcopy.addAll(newDpnTepsList);
        LOG.trace("newcopy0 {}", newDpnTepsListcopy);

        oldDpnTepsList.removeAll(newDpnTepsListcopy);
        newDpnTepsList.removeAll(oldDpnTepsListcopy);

        LOG.trace("oldDpnTepsList {}", oldDpnTepsList);
        LOG.trace("newDpnTepsList {}", newDpnTepsList);
        LOG.trace("oldcopy {}", oldDpnTepsListcopy);
        LOG.trace("newcopy {}", newDpnTepsListcopy);
        LOG.trace("oldcopy Size {}", oldDpnTepsList.size());
        LOG.trace("newcopy Size {}", newDpnTepsList.size());

        boolean equalLists = newDpnTepsList.size() == oldDpnTepsList.size()
                && newDpnTepsList.containsAll(oldDpnTepsList);
        LOG.trace("Is List Duplicate {} ", equalLists);
        if (!newDpnTepsList.isEmpty() && !equalLists) {
            LOG.trace("Adding TEPs ");
            jobCoordinator.enqueueJob(updatedTransportZone.getZoneName(),
                    new ItmTepAddWorker(newDpnTepsList, Collections.emptyList(), dataBroker, mdsalManager,
                            itmInternalTunnelAddWorker, externalTunnelAddWorker));
        }
        if (!oldDpnTepsList.isEmpty() && !equalLists) {
            LOG.trace("Removing TEPs ");
            jobCoordinator.enqueueJob(updatedTransportZone.getZoneName(),
                    new ItmTepRemoveWorker(oldDpnTepsList, Collections.emptyList(), originalTransportZone, mdsalManager,
                            itmInternalTunnelDeleteWorker, dpnTEPsInfoCache, txRunner, itmConfig));
        }
        List<HwVtep> oldHwList = createhWVteps(originalTransportZone);
        List<HwVtep> newHwList = createhWVteps(updatedTransportZone);
        List<HwVtep> oldHwListcopy = new ArrayList<>();
        oldHwListcopy.addAll(oldHwList);
        LOG.trace("oldHwListcopy0 {}", oldHwListcopy);
        List<HwVtep> newHwListcopy = new ArrayList<>();
        newHwListcopy.addAll(newHwList);
        LOG.trace("newHwListcopy0 {}", newHwListcopy);

        oldHwList.removeAll(newHwListcopy);
        newHwList.removeAll(oldHwListcopy);
        LOG.trace("oldHwList {}", oldHwList);
        LOG.trace("newHwList {}", newHwList);
        LOG.trace("oldHwListcopy {}", oldHwListcopy);
        LOG.trace("newHwListcopy {}", newHwListcopy);
        if (!newHwList.isEmpty()) {
            LOG.trace("Adding HW TEPs ");
            jobCoordinator.enqueueJob(updatedTransportZone.getZoneName(), new ItmTepAddWorker(Collections.emptyList(),
                    newHwList, dataBroker, mdsalManager, itmInternalTunnelAddWorker, externalTunnelAddWorker));
        }
        if (!oldHwList.isEmpty()) {
            LOG.trace("Removing HW TEPs ");
            jobCoordinator.enqueueJob(updatedTransportZone.getZoneName(),
                    new ItmTepRemoveWorker(Collections.emptyList(), oldHwList, originalTransportZone, mdsalManager,
                            itmInternalTunnelDeleteWorker, dpnTEPsInfoCache, txRunner, itmConfig));
        }
    }

    @Override
    public void add(@NonNull TransportZone transportZone) {
        LOG.debug("Received Transport Zone Add Event: {}", transportZone);
        EVENT_LOGGER.debug("ITM-Transportzone,ADD {}", transportZone.getZoneName());
        List<DPNTEPsInfo> opDpnList = createDPNTepInfo(transportZone);
        //avoiding adding duplicates from nothosted to new dpnlist.
        List<DPNTEPsInfo> duplicateFound = new ArrayList<>();
        List<DPNTEPsInfo> notHostedDpnList = getDPNTepInfoFromNotHosted(transportZone, opDpnList);
        for (DPNTEPsInfo notHostedDPN:notHostedDpnList) {
            for (DPNTEPsInfo newlyAddedDPN:opDpnList) {
                if (newlyAddedDPN.getDPNID().compareTo(notHostedDPN.getDPNID()) == 0
                        || newlyAddedDPN.getTunnelEndPoints().get(0).getIpAddress()
                        .equals(notHostedDPN.getTunnelEndPoints().get(0).getIpAddress())) {
                    duplicateFound.add(notHostedDPN);
                }
            }
        }
        notHostedDpnList.removeAll(duplicateFound);
        opDpnList.addAll(notHostedDpnList);

        List<HwVtep> hwVtepList = createhWVteps(transportZone);
        LOG.trace("Add: Operational dpnTepInfo - Before invoking ItmManager {}", opDpnList);
        if (!opDpnList.isEmpty() || !hwVtepList.isEmpty()) {
            LOG.trace("Add: Invoking ItmManager with DPN List {} ", opDpnList);
            LOG.trace("Add: Invoking ItmManager with hwVtep List {} ", hwVtepList);
            jobCoordinator.enqueueJob(transportZone.getZoneName(),
                    new ItmTepAddWorker(opDpnList, hwVtepList, dataBroker, mdsalManager, itmInternalTunnelAddWorker,
                            externalTunnelAddWorker));
        }
    }

    private List<DPNTEPsInfo> getDPNTepInfoFromNotHosted(TransportZone tzNew, List<DPNTEPsInfo> opDpnList) {
        List<DPNTEPsInfo> notHostedOpDpnList = new ArrayList<>();
        if (isNewTZExistInNotHostedTZ(tzNew)) {
            notHostedOpDpnList = createDPNTepInfoFromNotHosted(tzNew, opDpnList);
        }
        return notHostedOpDpnList;
    }

    private List<DPNTEPsInfo> createDPNTepInfoFromNotHosted(TransportZone tzNew, List<DPNTEPsInfo> opDpnList) {
        Map<Uint64, List<TunnelEndPoints>> mapNotHostedDPNToTunnelEndpt = new ConcurrentHashMap<>();
        List<DPNTEPsInfo> notHostedDpnTepInfo = new ArrayList<>();
        String newZoneName = tzNew.getZoneName();
        List<TzMembership> zones = ItmUtils.createTransportZoneMembership(newZoneName);
        Class<? extends TunnelTypeBase> tunnelType  = tzNew.getTunnelType();

        TepsInNotHostedTransportZone tepsInNotHostedTransportZone = getNotHostedTransportZone(newZoneName).get();
        if (tepsInNotHostedTransportZone == null) {
            return notHostedDpnTepInfo;
        }
        List<UnknownVteps> unVtepsLst = tepsInNotHostedTransportZone.getUnknownVteps();
        List<Vteps> vtepsList = new ArrayList<>();
        if (unVtepsLst != null && !unVtepsLst.isEmpty()) {
            for (UnknownVteps vteps : unVtepsLst) {
                Uint64 dpnID = vteps.getDpnId();
                IpAddress ipAddress = vteps.getIpAddress();
                String portName = itmConfig.getPortname() == null ? ITMConstants.DUMMY_PORT : itmConfig.getPortname();
                int vlanId = itmConfig.getVlanId() != null ? itmConfig.getVlanId().toJava()
                                                             : ITMConstants.DUMMY_VLANID;
                boolean useOfTunnel = ItmUtils.falseIfNull(vteps.isOfTunnel());
                String tos = vteps.getOptionTunnelTos();
                if (tos == null) {
                    tos = itmConfig.getDefaultTunnelTos();
                }
                TunnelEndPoints tunnelEndPoints =
                        ItmUtils.createTunnelEndPoints(dpnID, ipAddress, portName, useOfTunnel, vlanId, zones,
                                tunnelType, tos);
                List<TunnelEndPoints> tunnelEndPointsList = mapNotHostedDPNToTunnelEndpt.get(dpnID);
                if (tunnelEndPointsList != null) {
                    tunnelEndPointsList.add(tunnelEndPoints);
                } else {
                    tunnelEndPointsList = new ArrayList<>();
                    tunnelEndPointsList.add(tunnelEndPoints);
                    mapNotHostedDPNToTunnelEndpt.put(dpnID, tunnelEndPointsList);
                }
                Vteps newVtep = createVtepFromUnKnownVteps(dpnID,ipAddress);
                vtepsList.add(newVtep);

                // Enqueue 'remove TEP from TepsNotHosted list' operation
                // into DataStoreJobCoordinator
                jobCoordinator.enqueueJob(newZoneName,
                        new ItmTepsNotHostedRemoveWorker(newZoneName, ipAddress, dpnID, dataBroker, txRunner));
            }
        }
        //avoiding duplicate vteps which are already present in dpn list pushed from NBI
        List<Vteps> foundDuplicatevtepsList = new ArrayList<>();
        for (Vteps notHostedVteps:vtepsList) {
            for (DPNTEPsInfo newlyAddedDPN:opDpnList) {
                if (notHostedVteps.getDpnId().compareTo(newlyAddedDPN.getDPNID()) == 0
                        || newlyAddedDPN.getTunnelEndPoints().get(0).getIpAddress()
                        .equals(notHostedVteps.getIpAddress())) {
                    foundDuplicatevtepsList.add(notHostedVteps);
                }
            }
        }
        vtepsList.removeAll(foundDuplicatevtepsList);

        // Enqueue 'add TEP received from southbound OVSDB into ITM config DS' operation
        // into DataStoreJobCoordinator
        jobCoordinator.enqueueJob(newZoneName, new ItmTepsNotHostedMoveWorker(vtepsList, newZoneName, txRunner));

        if (mapNotHostedDPNToTunnelEndpt.size() > 0) {
            for (Entry<Uint64, List<TunnelEndPoints>> entry: mapNotHostedDPNToTunnelEndpt.entrySet()) {
                DPNTEPsInfo newDpnTepsInfo = ItmUtils.createDPNTepInfo(entry.getKey(), entry.getValue());
                notHostedDpnTepInfo.add(newDpnTepsInfo);
            }
        }
        return notHostedDpnTepInfo;

    }

    private Vteps createVtepFromUnKnownVteps(Uint64 dpnID, IpAddress ipAddress) {
        VtepsKey vtepkey = new VtepsKey(dpnID);
        Vteps vtepObj = new VtepsBuilder().setDpnId(dpnID).setIpAddress(ipAddress).withKey(vtepkey)
                .build();
        return vtepObj;
    }

    private  List<UnknownVteps> convertVtepListToUnknownVtepList(List<Vteps> vteps) {
        List<UnknownVteps> unknownVtepsList = new ArrayList<>();
        for (Vteps vtep : vteps) {
            UnknownVtepsKey vtepkey = new UnknownVtepsKey(vtep.getDpnId());
            UnknownVteps vtepObj =
                    new UnknownVtepsBuilder().setDpnId(vtep.getDpnId()).setIpAddress(vtep.getIpAddress())
                            .withKey(vtepkey).setOfTunnel(vtep.isOptionOfTunnel()).build();
            unknownVtepsList.add(vtepObj);
        }
        return unknownVtepsList;
    }

    private boolean isNewTZExistInNotHostedTZ(TransportZone tzNew) {
        boolean isPresent = false;
        if (getNotHostedTransportZone(tzNew.getZoneName()).isPresent()) {
            isPresent = true;
        }
        return isPresent;
    }

    public  Optional<TepsInNotHostedTransportZone> getNotHostedTransportZone(String transportZoneName) {
        InstanceIdentifier<TepsInNotHostedTransportZone> notHostedTzPath = InstanceIdentifier
                .builder(NotHostedTransportZones.class).child(TepsInNotHostedTransportZone.class,
                        new TepsInNotHostedTransportZoneKey(transportZoneName)).build();
        Optional<TepsInNotHostedTransportZone> tepsInNotHostedTransportZoneOptional =
                ItmUtils.read(LogicalDatastoreType.OPERATIONAL, notHostedTzPath, dataBroker);
        return tepsInNotHostedTransportZoneOptional;
    }

    private List<DPNTEPsInfo> createDPNTepInfo(TransportZone transportZone) {

        Map<Uint64, List<TunnelEndPoints>> mapDPNToTunnelEndpt = new ConcurrentHashMap<>();
        List<DPNTEPsInfo> dpnTepInfo = new ArrayList<>();
        List<TzMembership> zones = ItmUtils.createTransportZoneMembership(transportZone.getZoneName());
        Class<? extends TunnelTypeBase> tunnelType = transportZone.getTunnelType();
        LOG.trace("Transport Zone_name: {}", transportZone.getZoneName());
        List<Vteps> vtepsList = transportZone.getVteps();

        String portName = itmConfig.getPortname() == null ? ITMConstants.DUMMY_PORT : itmConfig.getPortname();
        int vlanId = itmConfig.getVlanId() != null ? itmConfig.getVlanId().toJava() : ITMConstants.DUMMY_VLANID;

        if (vtepsList != null && !vtepsList.isEmpty()) {
            for (Vteps vteps : vtepsList) {
                Uint64 dpnID = vteps.getDpnId();
                IpAddress ipAddress = vteps.getIpAddress();
                boolean useOfTunnel = itmConfig.isUseOfTunnels();
                String tos = vteps.getOptionTunnelTos();
                if (tos == null) {
                    tos = itmConfig.getDefaultTunnelTos();
                }
                LOG.trace("DpnID: {}, ipAddress: {}", dpnID, ipAddress);
                TunnelEndPoints tunnelEndPoints = ItmUtils.createTunnelEndPoints(dpnID, ipAddress, portName,
                        useOfTunnel, vlanId, zones, tunnelType, tos);
                EVENT_LOGGER.debug("ITM-createDPNTepInfo for {} {}", dpnID, ipAddress);
                List<TunnelEndPoints> tunnelEndPointsList = mapDPNToTunnelEndpt.get(dpnID);
                if (tunnelEndPointsList != null) {
                    LOG.trace("Existing DPN info list in the Map: {} ", dpnID);
                    tunnelEndPointsList.add(tunnelEndPoints);
                } else {
                    LOG.trace("Adding new DPN info list to the Map: {} ", dpnID);
                    tunnelEndPointsList = new ArrayList<>();
                    tunnelEndPointsList.add(tunnelEndPoints);
                    mapDPNToTunnelEndpt.put(dpnID, tunnelEndPointsList);

                }
            }
        }

        if (!mapDPNToTunnelEndpt.isEmpty()) {
            LOG.trace("List of dpns in the Map: {} ", mapDPNToTunnelEndpt.keySet());
            for (Entry<Uint64, List<TunnelEndPoints>> entry : mapDPNToTunnelEndpt.entrySet()) {
                DPNTEPsInfo newDpnTepsInfo = ItmUtils.createDPNTepInfo(entry.getKey(), entry.getValue());
                dpnTepInfo.add(newDpnTepsInfo);
            }
        }
        return dpnTepInfo;
    }

    private List<HwVtep> createhWVteps(TransportZone transportZone) {
        List<HwVtep> hwVtepsList = new ArrayList<>();

        String zoneName = transportZone.getZoneName();
        Class<? extends TunnelTypeBase> tunnelType = transportZone.getTunnelType();
        LOG.trace("Transport Zone_name: {}", zoneName);
        List<DeviceVteps> deviceVtepsList = transportZone.getDeviceVteps();
        if (deviceVtepsList != null) {
            for (DeviceVteps vteps : deviceVtepsList) {
                String topologyId = vteps.getTopologyId();
                String nodeId = vteps.getNodeId();
                IpAddress ipAddress = vteps.getIpAddress();
                LOG.trace("topo-id: {}, node-id: {}, ipAddress: {}", topologyId, nodeId, ipAddress);
                HwVtep hwVtep = ItmUtils.createHwVtepObject(topologyId, nodeId, ipAddress,
                        tunnelType, transportZone);

                LOG.trace("Adding new HwVtep {} info ", hwVtep.getHwIp());
                hwVtepsList.add(hwVtep);
            }
        }
        LOG.trace("returning hwvteplist {}", hwVtepsList);
        return hwVtepsList;
    }
}
