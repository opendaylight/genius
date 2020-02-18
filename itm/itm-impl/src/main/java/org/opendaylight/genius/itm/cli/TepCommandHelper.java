/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.genius.itm.cache.UnprocessedTunnelsStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.VtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("checkstyle:RegexpSingleLineJava")
public class TepCommandHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TepCommandHelper.class);

    private static final AtomicInteger CHECK = new AtomicInteger();

    private final DataBroker dataBroker;
    private final RetryingManagedNewTransactionRunner txRunner;
    private final ItmConfig itmConfig;
    private final UnprocessedTunnelsStateCache unprocessedTunnelsStateCache;

    /*
     * boolean flag add_or_delete --- can be set to true if the last called tep
     * command is Tep-add else set to false when Tep-delete is called
     * tepCommandHelper object is created only once in session initiated
     */
    private final Map<String, List<Vteps>> transportZonesHashMap = new HashMap<>();
    private List<TransportZone> transportZoneArrayList = new ArrayList<>();
    private final List<Vteps> vtepDelCommitList = new ArrayList<>();

    @Inject
    public TepCommandHelper(final DataBroker dataBroker, final ItmConfig itmConfig,
                            final UnprocessedTunnelsStateCache unprocessedTunnelsStateCache) {
        this.dataBroker = dataBroker;
        this.txRunner = new RetryingManagedNewTransactionRunner(dataBroker);
        this.itmConfig = itmConfig;
        this.unprocessedTunnelsStateCache = unprocessedTunnelsStateCache;
    }

    @PostConstruct
    public void start() throws ExecutionException, InterruptedException {
        LOG.info("TepCommandHelper Started");
    }

    @PreDestroy
    public void close() {
        LOG.info("TepCommandHelper Closed");
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void createLocalCache(Uint64 dpnId, String ipAddress,
                                 String transportZone) throws TepException {

        CHECK.incrementAndGet();
        IpAddress ipAddressObj;

        final VtepsKey vtepkey = new VtepsKey(dpnId);

        ipAddressObj = IpAddressBuilder.getDefaultInstance(ipAddress);

        if (checkTepPerTzPerDpn(transportZone, dpnId)) {
            handleError("Only one end point per transport Zone per Dpn is allowed");
            return;
        }

        Vteps vtepCli = new VtepsBuilder().setDpnId(dpnId).setIpAddress(ipAddressObj).withKey(vtepkey)
                .build();

        if (transportZonesHashMap.containsKey(transportZone)) {
            List<Vteps> vtepListTemp = transportZonesHashMap.get(transportZone);
            if (!vtepListTemp.contains(vtepCli)) {
                vtepListTemp.add(vtepCli);
            }
        } else {
            List<Vteps> vtepListTemp = new ArrayList<>();
            vtepListTemp.add(vtepCli);
            transportZonesHashMap.put(transportZone, vtepListTemp);
        }
    }

    /**
     * Gets the transport zone.
     *
     * @param transportZoneName
     *            the tzone
     * @return the transport zone
     */
    public TransportZone getTransportZone(String transportZoneName) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(transportZoneName)).build();
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker).orElse(null);
    }

    /**
     * Gets all transport zones.
     *
     * @return all transport zones
     */
    public TransportZones getAllTransportZones() {
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker).orElse(null);
    }


    public boolean checkTepPerTzPerDpn(String tzone, Uint64 dpnId) {
        // check in local cache
        if (transportZonesHashMap.containsKey(tzone)) {
            List<Vteps> vtepList = transportZonesHashMap.get(tzone);
            for (Vteps vtep : vtepList) {
                if (Objects.equals(vtep.getDpnId(), dpnId)) {
                    return true;
                }
            }
        }

        // check in DS
        InstanceIdentifier<TransportZone> tzonePath =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> transportZoneOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker);
        if (transportZoneOptional.isPresent()) {
            TransportZone tz = transportZoneOptional.get();
            for (Vteps vtep : tz.getVteps()) {
                if (Objects.equals(vtep.getDpnId(), dpnId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void buildTeps() {
        TransportZone transportZone = null;
        try {
            LOG.debug("no of teps added {}", CHECK);
            if (transportZonesHashMap != null && !transportZonesHashMap.isEmpty()) {
                transportZoneArrayList = new ArrayList<>();
                for (Entry<String, List<Vteps>> mapEntry : transportZonesHashMap.entrySet()) {
                    String tz = mapEntry.getKey();
                    LOG.debug("transportZonesHashMap {}", tz);
                    List<Vteps> vtepListTemp = mapEntry.getValue();
                    InstanceIdentifier<TransportZone> transportZonePath =
                            InstanceIdentifier.builder(TransportZones.class)
                                    .child(TransportZone.class, new TransportZoneKey(tz)).build();
                    Optional<TransportZone> transportZoneOptional =
                            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, transportZonePath, dataBroker);
                    LOG.debug("read container from DS");
                    if (transportZoneOptional.isPresent()) {
                        TransportZone tzoneFromDs = transportZoneOptional.get();
                        LOG.debug("read tzone container {}", tzoneFromDs);
                        if (tzoneFromDs.getTunnelType() == null
                                || tzoneFromDs.getTunnelType().equals(TunnelTypeVxlan.class)) {
                            transportZone =
                                    new TransportZoneBuilder().withKey(new TransportZoneKey(tz))
                                            .setTunnelType(TunnelTypeVxlan.class)
                                            .setZoneName(tz).setVteps(vtepListTemp).build();
                        } else if (tzoneFromDs.getTunnelType().equals(TunnelTypeGre.class)) {
                            transportZone =
                                    new TransportZoneBuilder().withKey(new TransportZoneKey(tz))
                                            .setTunnelType(TunnelTypeGre.class).setVteps(vtepListTemp)
                                            .setZoneName(tz).build();
                        }
                    } else {
                        transportZone =
                                new TransportZoneBuilder().withKey(new TransportZoneKey(tz))
                                        .setTunnelType(TunnelTypeVxlan.class).setZoneName(tz).setVteps(vtepListTemp)
                                        .build();
                    }
                    LOG.debug("tzone object {}", transportZone);
                    transportZoneArrayList.add(transportZone);
                }
                TransportZones transportZones =
                        new TransportZonesBuilder().setTransportZone(transportZoneArrayList).build();
                InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
                LOG.debug("InstanceIdentifier {}", path);
                Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                    tx -> tx.merge(path, transportZones, true)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                    MoreExecutors.directExecutor());
                LOG.debug("wrote to Config DS {}", transportZones);
                transportZonesHashMap.clear();
                transportZoneArrayList.clear();
                LOG.debug("Everything cleared");
            } else {
                LOG.debug("NO vteps were configured");
            }
        } catch (RuntimeException e) {
            LOG.error("Error building TEPs", e);
        }
    }

    public List<String> showTeps(boolean monitorEnabled, int monitorInterval) throws TepException {
        boolean flag = false;
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, dataBroker);
        if (transportZonesOptional.isPresent()) {
            List<String> result = new ArrayList<>();
            TransportZones transportZones = transportZonesOptional.get();
            if (transportZones.getTransportZone() == null || transportZones.getTransportZone().isEmpty()) {
                handleError("No teps configured");
                return result;
            }
            result.add(String.format("Tunnel Monitoring (for VXLAN tunnels): %s", monitorEnabled ? "On" : "Off"));
            result.add(String.format("Tunnel Monitoring Interval (for VXLAN tunnels): %d", monitorInterval));
            result.add(System.lineSeparator());
            result.add(String.format("%-16s  %-16s  %-16s  %-12s  %-12s %-12s %-16s %-12s", "TransportZone",
                    "TunnelType", "SubnetMask", "GatewayIP", "VlanID", "DpnID", "IPAddress", "PortName"));
            result.add("---------------------------------------------------------------------------------------------"
                    + "---------------------------------");
            for (TransportZone tz : transportZones.getTransportZone()) {
                if (tz.getVteps() == null || tz.getVteps().isEmpty()) {
                    continue;
                }
                for (Vteps vtep : tz.getVteps()) {
                    flag = true;
                    String strTunnelType ;
                    if (TunnelTypeGre.class.equals(tz.getTunnelType())) {
                        strTunnelType = ITMConstants.TUNNEL_TYPE_GRE;
                    } else {
                        strTunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                    }
                    result.add(String.format("%-16s  %-16s  %-12s %-16s",
                            tz.getZoneName(), strTunnelType,
                            vtep.getDpnId().toString(), vtep.getIpAddress().stringValue()));
                }
            }
            if (flag) {
                return result;
            } else {
                return Collections.singletonList("No teps to display");
            }
        } else {
            return Collections.singletonList("No teps configured");
        }
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    public void showCache(String cacheName) {
        final Collection<String> cacheContent;
        switch (cacheName) {
            case ITMConstants.INTERNAL_TUNNEL_CACHE_NAME:
                cacheContent = ItmUtils.ITM_CACHE.getAllInternalInterfaces();
                break;
            case ITMConstants.EXTERNAL_TUNNEL_CACHE_NAME:
                cacheContent = ItmUtils.ITM_CACHE.getAllExternalInterfaces();
                break;
            case ITMConstants.UNPROCESSED_TUNNELS_CACHE_NAME:
                cacheContent = unprocessedTunnelsStateCache.getAllUnprocessedTunnels();
                break;
            default:
                System.out.println(" " + cacheName + " is not a valid Cache Name ");
                return;
        }
        System.out.println("Dumping the data in cache for " + cacheName);
        System.out.println("Number of data in cache " + cacheContent.size());
        if (!cacheContent.isEmpty()) {
            for (String key : cacheContent) {
                System.out.println(key + " ");
            }
        } else {
            System.out.println("No data in cache for " + cacheName);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void deleteVtep(Uint64 dpnId, String ipAddress,
                           String transportZone) throws TepException {

        final VtepsKey vtepkey = new VtepsKey(dpnId);

        IpAddress ipAddressObj = IpAddressBuilder.getDefaultInstance(ipAddress);

        Vteps vtepCli;

        InstanceIdentifier<Vteps> vpath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(transportZone))
                .child(Vteps.class, vtepkey).build();

        // check if present in tzones and delete from cache
        boolean existsInCache = isInCache(dpnId, ipAddress, transportZone);
        if (!existsInCache) {
            Optional<Vteps> vtepOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, vpath, dataBroker);
            if (vtepOptional.isPresent()) {
                vtepCli = vtepOptional.get();
                if (Objects.equals(vtepCli.getIpAddress(), ipAddressObj)) {
                    vtepDelCommitList.add(vtepCli);
                }
            } else {
                handleError("Vtep Doesnt exist");
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public <T extends DataObject> void deleteOnCommit() {
        List<InstanceIdentifier<T>> vtepPaths = new ArrayList<>();
        List<InstanceIdentifier<T>> subnetPaths = new ArrayList<>();
        List<Vteps> vtepDelList = new ArrayList<>();
        List<InstanceIdentifier<T>> allPaths = new ArrayList<>();
        try {
            if (vtepDelCommitList != null && !vtepDelCommitList.isEmpty()) {
                InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
                Optional<TransportZones> transportZonesOptional =
                        ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
                if (transportZonesOptional.isPresent()) {
                    List<TransportZone> transportZones = transportZonesOptional.get().nonnullTransportZone();
                    for (TransportZone tz : transportZones) {
                        if (tz.getVteps() == null || tz.getVteps().isEmpty()) {
                            continue;
                        }
                        vtepDelList.addAll(vtepDelCommitList);
                        for (Vteps vtep : vtepDelList) {
                            InstanceIdentifier<T> vpath =
                                    (InstanceIdentifier<T>) InstanceIdentifier
                                            .builder(TransportZones.class)
                                            .child(TransportZone.class, tz.key())
                                            .child(Vteps.class, vtep.key()).build();
                            vtepPaths.add(vpath);
                        }
                    }

                    allPaths.addAll(vtepPaths);
                    allPaths.addAll(subnetPaths);
                    Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                        tx -> allPaths.forEach(tx::delete)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                        MoreExecutors.directExecutor());
                }
                vtepPaths.clear();
                subnetPaths.clear();
                allPaths.clear();
                vtepDelCommitList.clear();
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    public void showState(Collection<StateTunnelList> tunnelLists, boolean tunnelMonitorEnabled) throws TepException {
        if (tunnelLists == null || tunnelLists.isEmpty()) {
            handleError("No Internal Tunnels Exist");
            return;
        }
        if (!tunnelMonitorEnabled) {
            System.out.println("Tunnel Monitoring is Off");
        }
        String displayFormat = "%-16s  %-16s  %-16s  %-16s  %-16s  %-10s  %-10s";
        System.out.println(String.format(displayFormat, "Tunnel Name", "Source-DPN",
                "Destination-DPN", "Source-IP", "Destination-IP", "Trunk-State", "Transport Type"));
        System.out.println("-----------------------------------------------------------------------------------------"
                + "--------------------------------------------");

        for (StateTunnelList tunnelInst : tunnelLists) {
            // Display only the internal tunnels
            if (TepTypeInternal.class.equals(tunnelInst.getDstInfo().getTepDeviceType())) {
                String tunnelInterfaceName = tunnelInst.getTunnelInterfaceName();
                LOG.trace("tunnelInterfaceName::: {}", tunnelInterfaceName);
                String tunnelState = ITMConstants.TUNNEL_STATE_UNKNOWN;
                if (tunnelInst.getOperState() == TunnelOperStatus.Up) {
                    tunnelState = ITMConstants.TUNNEL_STATE_UP;
                } else if (tunnelInst.getOperState() == TunnelOperStatus.Down) {
                    tunnelState = ITMConstants.TUNNEL_STATE_DOWN;
                }
                Class<? extends TunnelTypeBase> tunType = tunnelInst.getTransportType();
                String tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                if (TunnelTypeVxlan.class.equals(tunType)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                } else if (tunType.equals(TunnelTypeGre.class)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_GRE;
                } else if (tunType.equals(TunnelTypeMplsOverGre.class)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_MPLSoGRE;
                } else if (tunType.equals(TunnelTypeLogicalGroup.class)) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_LOGICAL_GROUP_VXLAN;
                }
                System.out.println(String.format(displayFormat, tunnelInst.getTunnelInterfaceName(),
                        tunnelInst.getSrcInfo().getTepDeviceId(), tunnelInst.getDstInfo().getTepDeviceId(),
                        tunnelInst.getSrcInfo().getTepIp().stringValue(),
                        tunnelInst.getDstInfo().getTepIp().stringValue(), tunnelState, tunnelType));
            }
        }
    }

    // Show DPN-ID and Bridge mapping
    public void showBridges(Map<Uint64, OvsdbBridgeRef> dpnIdBridgeRefMap) {
        System.out.println(String.format("%-16s  %-16s  %-36s%n", "DPN-ID", "Bridge-Name", "Bridge-UUID")
                + "------------------------------------------------------------------------");
        dpnIdBridgeRefMap.forEach((dpnId, ovsdbBridgeRef) -> {
            String szBridgeId = ovsdbBridgeRef.getValue().firstKeyOf(Node.class).getNodeId().getValue();
            String bridgeUUID = szBridgeId.substring(13, 49);
            String bridgeName = szBridgeId.substring(57);
            System.out.println(String.format("%-16s  %-16s  %-36s", dpnId, bridgeName, bridgeUUID));
        });
    }

    // deletes from ADD-cache if it exists.
    public boolean isInCache(Uint64 dpnId, String ipAddress,
                             String transportZone) throws TepException {
        boolean exists = false;
        final VtepsKey vtepkey = new VtepsKey(dpnId);
        IpAddress ipAddressObj = IpAddressBuilder.getDefaultInstance(ipAddress);

        Vteps vtepCli = new VtepsBuilder().setDpnId(dpnId).setIpAddress(ipAddressObj).withKey(vtepkey).build();

        if (transportZonesHashMap.containsKey(transportZone)) {
            List<Vteps> vtepListTemp = transportZonesHashMap.get(transportZone);
            if (vtepListTemp.contains(vtepCli)) {
                exists = true; // return true if tzones has vtep
                vtepListTemp.remove(vtepCli);
                if (vtepListTemp.size() == 0) {
                    transportZonesHashMap.remove(transportZone);
                }
            } else {
                handleError("Vtep has not been configured");
            }
        }
        return exists;
    }

    public void configureTunnelType(String transportZoneName, String tunnelType) throws ExecutionException,
            InterruptedException {
        LOG.debug("configureTunnelType {} for transportZone {}", tunnelType, transportZoneName);

        TransportZone transportZoneFromConfigDS = ItmUtils.getTransportZoneFromConfigDS(transportZoneName, dataBroker);
        Class<? extends TunnelTypeBase> tunType;

        if (transportZoneFromConfigDS != null) {
            if (!transportZoneName.equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
                LOG.debug("Transport zone {} with tunnel type {} already exists. No action required.",
                        transportZoneName, tunnelType);
                return;
            } else {
                tunnelType = StringUtils.upperCase(tunnelType);
                tunType = ItmUtils.TUNNEL_TYPE_MAP.get(tunnelType);
                if (Objects.equals(transportZoneFromConfigDS.getTunnelType(), tunType)) {
                    // default-TZ already exists and tunnel-type is not changed during
                    // controller restart, then nothing to do now. Just return.
                    return;
                }
            }
        }

        // get tunnel-type
        tunnelType = StringUtils.upperCase(tunnelType);
        tunType = ItmUtils.TUNNEL_TYPE_MAP.get(tunnelType);


        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tzones = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);

        TransportZone tzone = new TransportZoneBuilder().withKey(new TransportZoneKey(transportZoneName))
                .setTunnelType(tunType).build();
        List<TransportZone> tzList = new ArrayList<>();
        if (tzones.isPresent()) {
            final List<TransportZone> lst = tzones.get().getTransportZone();
            if (lst != null) {
                tzList.addAll(lst);
            }
        }
        tzList.add(tzone);
        TransportZones transportZones = new TransportZonesBuilder().setTransportZone(tzList).build();
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.put(LogicalDatastoreType.CONFIGURATION,
                path, transportZones, true)).get();

    }

    public void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol) {
        InstanceIdentifier<TunnelMonitorParams> path = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        Optional<TunnelMonitorParams> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, dataBroker);
        Class<? extends TunnelMonitoringTypeBase> monitorType ;
        if (storedTunnelMonitor.isPresent() && storedTunnelMonitor.get().getMonitorProtocol() != null) {
            monitorType = storedTunnelMonitor.get().getMonitorProtocol();
        } else {
            if (monitorProtocol != null && monitorProtocol.equalsIgnoreCase(ITMConstants.MONITOR_TYPE_LLDP)) {
                monitorType = TunnelMonitoringTypeLldp.class;
            } else {
                monitorType = TunnelMonitoringTypeBfd.class;
            }
        }
        if (!storedTunnelMonitor.isPresent() || storedTunnelMonitor.get().isEnabled() != monitorEnabled) {
            TunnelMonitorParams tunnelMonitor = new TunnelMonitorParamsBuilder().setEnabled(monitorEnabled)
                    .setMonitorProtocol(monitorType).build();
            Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> tx.merge(path, tunnelMonitor, true)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                MoreExecutors.directExecutor());
        }
    }

    public void configureTunnelMonitorInterval(int interval) {
        InstanceIdentifier<TunnelMonitorInterval> path =
                InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        Optional<TunnelMonitorInterval> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path,
                dataBroker);
        if (!storedTunnelMonitor.isPresent() || storedTunnelMonitor.get().getInterval().toJava() != interval) {
            TunnelMonitorInterval tunnelMonitor = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
            Futures.addCallback(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> tx.merge(path, tunnelMonitor, true)), ItmUtils.DEFAULT_WRITE_CALLBACK,
                MoreExecutors.directExecutor());
        }
    }

    public void handleError(String errorMessage) throws TepException {
        throw new TepException(errorMessage);
    }
}
