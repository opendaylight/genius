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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeEntryCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepRemoveWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedMoveWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedRemoveWorker;
import org.opendaylight.genius.itm.confighelpers.OvsdbTepRemoveConfigHelper;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final IMdsalApiManager mdsalManager;
    private final ItmConfig itmConfig;
    private final ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker;
    private final ItmInternalTunnelAddWorker itmInternalTunnelAddWorker;
    private final ItmExternalTunnelAddWorker externalTunnelAddWorker;
    private final DPNTEPsInfoCache dpnTEPsInfoCache;
    private final OvsdbTepRemoveConfigHelper ovsdbTepRemoveConfigHelper;

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
                                 final ServiceRecoveryRegistry serviceRecoveryRegistry) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(TransportZones.class).child(TransportZone.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        initializeTZNode(dataBroker);
        this.mdsalManager = mdsalManager;
        this.itmConfig = itmConfig;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        this.itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig, interfaceManager, dpnTepStateCache, ovsBridgeEntryCache,
                ovsBridgeRefEntryCache, tunnelStateCache, directTunnelUtils);
        this.itmInternalTunnelAddWorker = new ItmInternalTunnelAddWorker(dataBroker, jobCoordinator,
                tunnelMonitoringConfig, itmConfig, directTunnelUtils, interfaceManager, ovsBridgeRefEntryCache);
        this.externalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig, dpnTEPsInfoCache);
        serviceRecoveryRegistry.addRecoverableListener(ItmServiceRecoveryHandler.getServiceRegistryKey(), this);
        this.ovsdbTepRemoveConfigHelper = new OvsdbTepRemoveConfigHelper();
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
    private void initializeTZNode(DataBroker db) {
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.create(TransportZones.class);
        CheckedFuture<Optional<TransportZones>, ReadFailedException> tzones = transaction
                .read(LogicalDatastoreType.CONFIGURATION, path);
        try {
            if (!tzones.get().isPresent()) {
                TransportZonesBuilder tzb = new TransportZonesBuilder();
                transaction.put(LogicalDatastoreType.CONFIGURATION, path, tzb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing TransportZones {}", e);
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<TransportZone> instanceIdentifier,
                       @Nonnull TransportZone transportZone) {
        LOG.debug("Received Transport Zone Remove Event: {}", transportZone);
        boolean allowTunnelDeletion;

        // check if TZ received for removal is default-transport-zone,
        // if yes, then check if it is received from northbound, then
        // do not entertain request and skip tunnels remove operation
        // if def-tz removal request is due to def-tz-enabled flag is disabled or
        // due to change in def-tz-tunnel-type, then allow def-tz tunnels deletion
        if (transportZone.getZoneName().equalsIgnoreCase(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
            // Get TunnelTypeBase object for tunnel-type configured in config file
            Class<? extends TunnelTypeBase> tunType = ItmUtils.getTunnelType(itmConfig.getDefTzTunnelType());

            if (!itmConfig.isDefTzEnabled() || !transportZone.getTunnelType().equals(tunType)) {
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
            List<DPNTEPsInfo> opDpnList = createDPNTepInfo(transportZone);
            List<HwVtep> hwVtepList = createhWVteps(transportZone);
            LOG.trace("Delete: Invoking deleteTunnels in ItmManager with DpnList {}", opDpnList);
            if (!opDpnList.isEmpty() || !hwVtepList.isEmpty()) {
                LOG.trace("Delete: Invoking ItmManager with hwVtep List {} ", hwVtepList);
                jobCoordinator.enqueueJob(transportZone.getZoneName(),
                        new ItmTepRemoveWorker(opDpnList, hwVtepList, transportZone, dataBroker, mdsalManager,
                                itmInternalTunnelDeleteWorker, dpnTEPsInfoCache));
            }
        }
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<TransportZone> instanceIdentifier,
                       @Nonnull TransportZone originalTransportZone, @Nonnull TransportZone updatedTransportZone) {
        LOG.debug("Received Transport Zone Update Event: Old - {}, Updated - {}", originalTransportZone,
                  updatedTransportZone);
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
        if (!newDpnTepsList.isEmpty()) {
            LOG.trace("Adding TEPs ");
            jobCoordinator.enqueueJob(updatedTransportZone.getZoneName(),
                    new ItmTepAddWorker(newDpnTepsList, Collections.emptyList(), dataBroker, mdsalManager, itmConfig,
                            itmInternalTunnelAddWorker, externalTunnelAddWorker, dpnTEPsInfoCache));
        }
        if (!oldDpnTepsList.isEmpty()) {
            LOG.trace("Removing TEPs ");
            jobCoordinator.enqueueJob(updatedTransportZone.getZoneName(),
                    new ItmTepRemoveWorker(oldDpnTepsList, Collections.emptyList(), originalTransportZone, dataBroker,
                            mdsalManager, itmInternalTunnelDeleteWorker, dpnTEPsInfoCache));
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
                    newHwList, dataBroker, mdsalManager, itmConfig, itmInternalTunnelAddWorker, externalTunnelAddWorker,
                    dpnTEPsInfoCache));
        }
        if (!oldHwList.isEmpty()) {
            LOG.trace("Removing HW TEPs ");
            jobCoordinator.enqueueJob(updatedTransportZone.getZoneName(), new ItmTepRemoveWorker(
                    Collections.emptyList(), oldHwList, originalTransportZone, dataBroker, mdsalManager,
                    itmInternalTunnelDeleteWorker, dpnTEPsInfoCache));
        }
    }

    @Override
    public void add(@Nonnull TransportZone transportZone) {
        LOG.debug("Received Transport Zone Add Event: {}", transportZone);
        List<DPNTEPsInfo> opDpnList = createDPNTepInfo(transportZone);
        List<HwVtep> hwVtepList = createhWVteps(transportZone);
        opDpnList.addAll(getDPNTepInfoFromNotHosted(transportZone));
        LOG.trace("Add: Operational dpnTepInfo - Before invoking ItmManager {}", opDpnList);
        if (!opDpnList.isEmpty() || !hwVtepList.isEmpty()) {
            LOG.trace("Add: Invoking ItmManager with DPN List {} ", opDpnList);
            LOG.trace("Add: Invoking ItmManager with hwVtep List {} ", hwVtepList);
            jobCoordinator.enqueueJob(transportZone.getZoneName(),
                    new ItmTepAddWorker(opDpnList, hwVtepList, dataBroker, mdsalManager, itmConfig,
                            itmInternalTunnelAddWorker, externalTunnelAddWorker, dpnTEPsInfoCache));
        }
    }

    private List<DPNTEPsInfo> getDPNTepInfoFromNotHosted(TransportZone tzNew) {
        List<DPNTEPsInfo> notHostedOpDpnList = new ArrayList<>();
        if (isNewTZExistInNotHostedTZ(tzNew)) {
            notHostedOpDpnList = createDPNTepInfoFromNotHosted(tzNew);
        }
        return notHostedOpDpnList;
    }

    private List<DPNTEPsInfo> createDPNTepInfoFromNotHosted(TransportZone tzNew) {
        Map<BigInteger, List<TunnelEndPoints>> mapNotHostedDPNToTunnelEndpt = new ConcurrentHashMap<>();
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
                BigInteger dpnID = vteps.getDpnId();
                String port = ITMConstants.DUMMY_PORT;
                int vlanID = ITMConstants.DUMMY_VLANID;
                IpPrefix ipPrefix = IpPrefixBuilder.getDefaultInstance(ITMConstants.DUMMY_PREFIX);
                IpAddress gatewayIP = IpAddressBuilder.getDefaultInstance(ITMConstants.DUMMY_GATEWAY_IP);
                IpAddress ipAddress = vteps.getIpAddress();
                boolean useOfTunnel = ItmUtils.falseIfNull(vteps.isOfTunnel());
                String tos = vteps.getOptionTunnelTos();
                if (tos == null) {
                    tos = itmConfig.getDefaultTunnelTos();
                }
                TunnelEndPoints tunnelEndPoints =
                        ItmUtils.createTunnelEndPoints(dpnID, ipAddress, port, useOfTunnel,vlanID, ipPrefix,
                                gatewayIP, zones, tunnelType, tos);
                List<TunnelEndPoints> tunnelEndPointsList = mapNotHostedDPNToTunnelEndpt.get(dpnID);
                if (tunnelEndPointsList != null) {
                    tunnelEndPointsList.add(tunnelEndPoints);
                } else {
                    tunnelEndPointsList = new ArrayList<>();
                    tunnelEndPointsList.add(tunnelEndPoints);
                    mapNotHostedDPNToTunnelEndpt.put(dpnID, tunnelEndPointsList);
                }
                Vteps newVtep = createVtepFromUnKnownVteps(dpnID,ipAddress,ITMConstants.DUMMY_PORT);
                vtepsList.add(newVtep);

                // Enqueue 'remove TEP from TepsNotHosted list' operation
                // into DataStoreJobCoordinator
                ItmTepsNotHostedRemoveWorker
                    removeWorker = new ItmTepsNotHostedRemoveWorker(newZoneName, ipAddress, dpnID, dataBroker,
                        ovsdbTepRemoveConfigHelper);
                jobCoordinator.enqueueJob(newZoneName, removeWorker);
            }
        }

        // Enqueue 'add TEP received from southbound OVSDB into ITM config DS' operation
        // into DataStoreJobCoordinator
        ItmTepsNotHostedMoveWorker
            moveWorker = new ItmTepsNotHostedMoveWorker(vtepsList, newZoneName, dataBroker);
        jobCoordinator.enqueueJob(newZoneName, moveWorker);

        if (mapNotHostedDPNToTunnelEndpt.size() > 0) {
            for (Entry<BigInteger, List<TunnelEndPoints>> entry: mapNotHostedDPNToTunnelEndpt.entrySet()) {
                DPNTEPsInfo newDpnTepsInfo = ItmUtils.createDPNTepInfo(entry.getKey(), entry.getValue());
                notHostedDpnTepInfo.add(newDpnTepsInfo);
            }
        }
        return notHostedDpnTepInfo;

    }

    private Vteps createVtepFromUnKnownVteps(BigInteger dpnID, IpAddress ipAddress, String port) {
        VtepsKey vtepkey = new VtepsKey(dpnID, port);
        Vteps vtepObj = new VtepsBuilder().setDpnId(dpnID).setIpAddress(ipAddress).withKey(vtepkey)
                .setPortname(port).build();
        return vtepObj;
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

        Map<BigInteger, List<TunnelEndPoints>> mapDPNToTunnelEndpt = new ConcurrentHashMap<>();
        List<DPNTEPsInfo> dpnTepInfo = new ArrayList<>();
        List<TzMembership> zones = ItmUtils.createTransportZoneMembership(transportZone.getZoneName());
        Class<? extends TunnelTypeBase> tunnelType = transportZone.getTunnelType();
        LOG.trace("Transport Zone_name: {}", transportZone.getZoneName());
        List<Subnets> subnetsList = transportZone.getSubnets();
        if (subnetsList != null) {
            for (Subnets subnet : subnetsList) {
                IpPrefix ipPrefix = subnet.getPrefix();
                IpAddress gatewayIP = subnet.getGatewayIp();
                int vlanID = subnet.getVlanId();
                LOG.trace("IpPrefix: {}, gatewayIP: {}, vlanID: {} ", ipPrefix, gatewayIP, vlanID);
                List<Vteps> vtepsList = subnet.getVteps();
                if (vtepsList != null && !vtepsList.isEmpty()) {
                    for (Vteps vteps : vtepsList) {
                        BigInteger dpnID = vteps.getDpnId();
                        String port = vteps.getPortname();
                        IpAddress ipAddress = vteps.getIpAddress();
                        boolean useOfTunnel = ItmUtils.falseIfNull(vteps.isOptionOfTunnel());
                        String tos = vteps.getOptionTunnelTos();
                        if (tos == null) {
                            tos = itmConfig.getDefaultTunnelTos();
                        }
                        LOG.trace("DpnID: {}, port: {}, ipAddress: {}", dpnID, port, ipAddress);
                        TunnelEndPoints tunnelEndPoints = ItmUtils.createTunnelEndPoints(dpnID, ipAddress, port,
                            useOfTunnel, vlanID,  ipPrefix, gatewayIP, zones, tunnelType, tos);
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
            }
        }

        if (!mapDPNToTunnelEndpt.isEmpty()) {
            LOG.trace("List of dpns in the Map: {} ", mapDPNToTunnelEndpt.keySet());
            for (Entry<BigInteger, List<TunnelEndPoints>> entry : mapDPNToTunnelEndpt.entrySet()) {
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
        List<Subnets> subnetsList = transportZone.getSubnets();
        if (subnetsList != null) {
            for (Subnets subnet : subnetsList) {
                IpPrefix ipPrefix = subnet.getPrefix();
                IpAddress gatewayIP = subnet.getGatewayIp();
                int vlanID = subnet.getVlanId();
                LOG.trace("IpPrefix: {}, gatewayIP: {}, vlanID: {} ", ipPrefix, gatewayIP, vlanID);
                List<DeviceVteps> deviceVtepsList = subnet.getDeviceVteps();
                if (deviceVtepsList != null) {
                    for (DeviceVteps vteps : deviceVtepsList) {
                        String topologyId = vteps.getTopologyId();
                        String nodeId = vteps.getNodeId();
                        IpAddress ipAddress = vteps.getIpAddress();
                        LOG.trace("topo-id: {}, node-id: {}, ipAddress: {}", topologyId, nodeId, ipAddress);
                        HwVtep hwVtep = ItmUtils.createHwVtepObject(topologyId, nodeId, ipAddress, ipPrefix, gatewayIP,
                                vlanID, tunnelType, transportZone);

                        LOG.trace("Adding new HwVtep {} info ", hwVtep.getHwIp());
                        hwVtepsList.add(hwVtep);
                    }
                }
            }
        }
        LOG.trace("returning hwvteplist {}", hwVtepsList);
        return hwVtepsList;
    }
}
