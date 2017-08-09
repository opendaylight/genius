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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmTepAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepRemoveWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedMoveWorker;
import org.opendaylight.genius.itm.confighelpers.ItmTepsNotHostedRemoveWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMManager;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TepsNotHostedInTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TepsNotHostedInTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVteps;
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
public class TransportZoneListener extends AsyncDataTreeChangeListenerBase<TransportZone, TransportZoneListener>
        implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TransportZoneListener.class);
    private final DataBroker dataBroker;
    private final IdManagerService idManagerService;
    private final IMdsalApiManager mdsalManager;
    private final ITMManager itmManager;
    private final ItmConfig itmConfig;

    @Inject
    public TransportZoneListener(final DataBroker dataBroker, final IdManagerService idManagerService,
                                 final IMdsalApiManager mdsalManager,final ITMManager itmManager,
                                 final ItmConfig itmConfig) {
        super(TransportZone.class, TransportZoneListener.class);
        this.dataBroker = dataBroker;
        this.idManagerService = idManagerService;
        initializeTZNode(dataBroker);
        this.itmManager = itmManager;
        this.mdsalManager = mdsalManager;
        this.itmConfig = itmConfig;
    }

    @PostConstruct
    public void start() {
        registerListener(LogicalDatastoreType.CONFIGURATION, this.dataBroker);
        LOG.info("tzChangeListener Started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("tzChangeListener Closed");
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
    protected InstanceIdentifier<TransportZone> getWildCardPath() {
        return InstanceIdentifier.create(TransportZones.class).child(TransportZone.class);
    }

    @Override
    protected TransportZoneListener getDataTreeChangeListener() {
        return TransportZoneListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<TransportZone> key, TransportZone tzOld) {
        LOG.debug("Received Transport Zone Remove Event: {}, {}", key, tzOld);

        boolean allowTunnelDeletion = false;

        // check if TZ received for removal is default-transport-zone,
        // if yes, then check if it is received from northbound, then
        // do not entertain request and skip tunnels remove operation
        // if def-tz removal request is due to def-tz-enabled flag is disabled or
        // due to change in def-tz-tunnel-type, then allow def-tz tunnels deletion
        if (tzOld.getZoneName().equalsIgnoreCase(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
            // Get TunnelTypeBase object for tunnel-type configured in config file
            Class<? extends TunnelTypeBase> tunType = ItmUtils.getTunnelType(itmConfig.getDefTzTunnelType());

            if ((!itmConfig.isDefTzEnabled()) || (!tzOld.getTunnelType().equals(tunType))) {
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
            List<DPNTEPsInfo> opDpnList = createDPNTepInfo(tzOld);
            List<HwVtep> hwVtepList = createhWVteps(tzOld);
            LOG.trace("Delete: Invoking deleteTunnels in ItmManager with DpnList {}", opDpnList);
            if (!opDpnList.isEmpty() || !hwVtepList.isEmpty()) {
                LOG.trace("Delete: Invoking ItmManager with hwVtep List {} " , hwVtepList);
                DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
                ItmTepRemoveWorker removeWorker =
                        new ItmTepRemoveWorker(opDpnList, hwVtepList, tzOld, dataBroker,
                                idManagerService, mdsalManager);
                coordinator.enqueueJob(tzOld.getZoneName(), removeWorker);
            }
        }
    }

    @Override
    protected void update(InstanceIdentifier<TransportZone> key, TransportZone tzOld, TransportZone tzNew) {
        LOG.debug("Received Transport Zone Update Event: Key - {}, Old - {}, Updated - {}", key, tzOld, tzNew);
        List<DPNTEPsInfo> oldDpnTepsList = createDPNTepInfo(tzOld);
        List<DPNTEPsInfo> newDpnTepsList = createDPNTepInfo(tzNew);
        List<DPNTEPsInfo> oldDpnTepsListcopy = new ArrayList<>();
        oldDpnTepsListcopy.addAll(oldDpnTepsList);
        LOG.trace("oldcopy0 {}", oldDpnTepsListcopy);
        List<DPNTEPsInfo> newDpnTepsListcopy = new ArrayList<>();
        newDpnTepsListcopy.addAll(newDpnTepsList);
        LOG.trace("newcopy0 {}", newDpnTepsListcopy);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();

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
            ItmTepAddWorker addWorker = new ItmTepAddWorker(newDpnTepsList, Collections.emptyList(), dataBroker,
                    idManagerService, mdsalManager, itmConfig);
            coordinator.enqueueJob(tzNew.getZoneName(), addWorker);
        }
        if (!oldDpnTepsList.isEmpty()) {
            LOG.trace("Removing TEPs ");
            ItmTepRemoveWorker removeWorker = new ItmTepRemoveWorker(oldDpnTepsList, Collections.emptyList(),
                    tzOld, dataBroker, idManagerService, mdsalManager);
            coordinator.enqueueJob(tzNew.getZoneName(), removeWorker);
        }
        List<HwVtep> oldHwList = createhWVteps(tzOld);
        List<HwVtep> newHwList = createhWVteps(tzNew);
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
            ItmTepAddWorker addWorker = new ItmTepAddWorker(Collections.emptyList(), newHwList, dataBroker,
                    idManagerService, mdsalManager, itmConfig);
            coordinator.enqueueJob(tzNew.getZoneName(), addWorker);
        }
        if (!oldHwList.isEmpty()) {
            LOG.trace("Removing HW TEPs ");
            ItmTepRemoveWorker removeWorker = new ItmTepRemoveWorker(Collections.emptyList(), oldHwList,
                    tzOld, dataBroker, idManagerService, mdsalManager);
            coordinator.enqueueJob(tzNew.getZoneName(), removeWorker);
        }
    }

    @Override
    protected void add(InstanceIdentifier<TransportZone> key, TransportZone tzNew) {
        LOG.debug("Received Transport Zone Add Event: {}, {}", key, tzNew);
        List<DPNTEPsInfo> opDpnList = createDPNTepInfo(tzNew);
        List<HwVtep> hwVtepList = createhWVteps(tzNew);
        opDpnList.addAll(getDPNTepInfoFromNotHosted(tzNew));
        LOG.trace("Add: Operational dpnTepInfo - Before invoking ItmManager {}", opDpnList);
        if (!opDpnList.isEmpty() || !hwVtepList.isEmpty()) {
            LOG.trace("Add: Invoking ItmManager with DPN List {} ", opDpnList);
            LOG.trace("Add: Invoking ItmManager with hwVtep List {} ", hwVtepList);
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            ItmTepAddWorker addWorker = new ItmTepAddWorker(opDpnList, hwVtepList, dataBroker, idManagerService,
                    mdsalManager, itmConfig);
            coordinator.enqueueJob(tzNew.getZoneName(), addWorker);
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
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();

        TepsNotHostedInTransportZone tepNotHostedTransportZone = getNotHostedTransportZone(newZoneName).get();
        if (tepNotHostedTransportZone == null) {
            return notHostedDpnTepInfo;
        }
        List<UnknownVteps> unVtepsLst = tepNotHostedTransportZone.getUnknownVteps();
        List<Vteps> vtepsList = new ArrayList<Vteps>();
        if (unVtepsLst != null && !unVtepsLst.isEmpty()) {
            for (UnknownVteps vteps : unVtepsLst) {
                BigInteger dpnID = vteps.getDpnId();
                String port = ITMConstants.DUMMY_PORT;
                int vlanID = ITMConstants.DUMMY_VLANID;
                IpPrefix ipPrefix = new IpPrefix(ITMConstants.DUMMY_PREFIX.toCharArray());
                IpAddress gatewayIP = new IpAddress(ITMConstants.DUMMY_GATEWAY_IP.toCharArray());
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
                    removeWorker = new ItmTepsNotHostedRemoveWorker(newZoneName, ipAddress, dpnID, dataBroker);
                coordinator.enqueueJob(newZoneName, removeWorker);
            }
        }

        // Enqueue 'add TEP received from southbound OVSDB into ITM config DS' operation
        // into DataStoreJobCoordinator
        ItmTepsNotHostedMoveWorker
            moveWorker = new ItmTepsNotHostedMoveWorker(vtepsList, newZoneName, dataBroker);
        coordinator.enqueueJob(newZoneName, moveWorker);

        if (mapNotHostedDPNToTunnelEndpt.size() > 0) {
            Set<BigInteger> keys = mapNotHostedDPNToTunnelEndpt.keySet();
            for (BigInteger key: keys) {
                DPNTEPsInfo newDpnTepsInfo = ItmUtils.createDPNTepInfo(key, mapNotHostedDPNToTunnelEndpt.get(key));
                notHostedDpnTepInfo.add(newDpnTepsInfo);
            }
        }
        return notHostedDpnTepInfo;

    }

    private Vteps createVtepFromUnKnownVteps(BigInteger dpnID, IpAddress ipAddress, String port) {
        VtepsKey vtepkey = new VtepsKey(dpnID, port);
        Vteps vtepObj = new VtepsBuilder().setDpnId(dpnID).setIpAddress(ipAddress).setKey(vtepkey)
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

    public  Optional<TepsNotHostedInTransportZone> getNotHostedTransportZone(String transportZoneName) {
        InstanceIdentifier<TepsNotHostedInTransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TepsNotHostedInTransportZone.class,
                        new TepsNotHostedInTransportZoneKey(transportZoneName)).build();
        Optional<TepsNotHostedInTransportZone> tzNotHostedOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker);
        return tzNotHostedOptional;
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
            Set<BigInteger> keys = mapDPNToTunnelEndpt.keySet();
            LOG.trace("List of dpns in the Map: {} ", keys);
            for (BigInteger key : keys) {
                DPNTEPsInfo newDpnTepsInfo = ItmUtils.createDPNTepInfo(key, mapDPNToTunnelEndpt.get(key));
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

                        if (hwVtepsList != null) {
                            LOG.trace("Existing hwVteps");
                            hwVtepsList.add(hwVtep);
                        } else {
                            LOG.trace("Adding new HwVtep {} info ", hwVtep.getHwIp());
                            hwVtepsList.add(hwVtep);
                        }
                    }
                }
            }
        }
        LOG.trace("returning hwvteplist {}", hwVtepsList);
        return hwVtepsList;
    }
}
