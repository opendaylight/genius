/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.base.Optional;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.tunnel.optional.params.TunnelOptions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmExternalTunnelAddWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ItmExternalTunnelAddWorker.class);
    private static final IpAddress GATEWAY_IP_OBJ = IpAddressBuilder.getDefaultInstance("0.0.0.0");

    private final ItmConfig itmConfig;
    private final DPNTEPsInfoCache dpnTEPsInfoCache;

    public ItmExternalTunnelAddWorker(ItmConfig itmConfig, DPNTEPsInfoCache dpnTEPsInfoCache) {
        this.itmConfig = itmConfig;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
    }

    public void buildTunnelsToExternalEndPoint(Collection<DPNTEPsInfo> cfgDpnList, IpAddress extIp,
                                               Class<? extends TunnelTypeBase> tunType,
                                               TypedWriteTransaction<Configuration> tx) {
        if (null != cfgDpnList) {
            for (DPNTEPsInfo teps : cfgDpnList) {
                // CHECK -- Assumption -- Only one End Point / Dpn for GRE/Vxlan Tunnels
                TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
                String interfaceName = firstEndPt.getInterfaceName();
                String tunTypeStr = tunType.getName();
                String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(interfaceName,
                        firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunTypeStr);
                boolean useOfTunnel = ItmUtils.falseIfNull(firstEndPt.isOptionOfTunnel());
                List<TunnelOptions> tunOptions = ItmUtils.buildTunnelOptions(firstEndPt, itmConfig);
                IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
                IpAddress gwyIpAddress = gatewayIpObj;
                LOG.debug(" Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {},"
                                + " source IP - {}, DC Gateway IP - {} gateway IP - {}", trunkInterfaceName,
                        interfaceName, firstEndPt.getIpAddress(), extIp, gwyIpAddress);
                Interface iface = ItmUtils.buildTunnelInterface(teps.getDPNID(), trunkInterfaceName,
                        trunkInterface(ItmUtils.convertTunnelTypetoString(tunType)),
                        true, tunType, firstEndPt.getIpAddress(), extIp, false, false,
                        ITMConstants.DEFAULT_MONITOR_PROTOCOL, null, useOfTunnel, tunOptions);

                LOG.debug(" Trunk Interface builder - {} ", iface);
                InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
                LOG.debug(" Trunk Interface Identifier - {} ", trunkIdentifier);
                LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", trunkIdentifier, iface);
                tx.merge(trunkIdentifier, iface, true);
                // update external_tunnel_list ds
                InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                        .child(ExternalTunnel.class, new ExternalTunnelKey(extIp.stringValue(),
                                teps.getDPNID().toString(), tunType));
                ExternalTunnel tnl = ItmUtils.buildExternalTunnel(teps.getDPNID().toString(),
                        extIp.stringValue(), tunType, trunkInterfaceName);
                tx.merge(path, tnl, true);
            }
        }
    }

    public void buildTunnelsFromDpnToExternalEndPoint(List<Uint64> dpnId, IpAddress extIp,
                                                      Class<? extends TunnelTypeBase> tunType,
                                                      TypedWriteTransaction<Configuration> tx) {
        Collection<DPNTEPsInfo> cfgDpnList = dpnId == null ? dpnTEPsInfoCache.getAllPresent()
                : ItmUtils.getDpnTepListFromDpnId(dpnTEPsInfoCache, dpnId);
        buildTunnelsToExternalEndPoint(cfgDpnList, extIp, tunType, tx);
    }

    public void buildHwVtepsTunnels(List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> cfgdHwVteps,
                                    TypedReadWriteTransaction<Configuration> tx) {

        Integer monitorInterval = ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL;
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;

        if (null != cfgdDpnList && !cfgdDpnList.isEmpty()) {
            LOG.trace("calling tunnels from OVS {}",cfgdDpnList);
            tunnelsFromOVS(cfgdDpnList, tx, monitorInterval, monitorProtocol);
        }
        if (null != cfgdHwVteps && !cfgdHwVteps.isEmpty()) {
            LOG.trace("calling tunnels from hwTep {}",cfgdHwVteps);
            cfgdHwVteps.forEach(hwVtep -> {
                try {
                    tunnelsFromhWVtep(hwVtep, tx, monitorInterval, monitorProtocol);
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("Tunnel Creation failed for {} due to ", hwVtep.getTransportZone(), e);
                }
            });
        }
    }

    private void tunnelsFromOVS(List<DPNTEPsInfo> cfgdDpnList, TypedReadWriteTransaction<Configuration> tx,
                                Integer monitorInterval, Class<? extends TunnelMonitoringTypeBase> monitorProtocol) {
        for (DPNTEPsInfo dpn : cfgdDpnList) {
            LOG.trace("processing dpn {}", dpn);
            if (dpn.getTunnelEndPoints() != null && !dpn.getTunnelEndPoints().isEmpty()) {
                for (TunnelEndPoints tep : dpn.getTunnelEndPoints()) {
                    for (TzMembership zone : tep.nonnullTzMembership()) {
                        try {
                            createTunnelsFromOVSinTransportZone(zone.getZoneName(), dpn, tep, tx, monitorInterval,
                                    monitorProtocol);
                        } catch (ExecutionException | InterruptedException e) {
                            LOG.error("Tunnel Creation failed for {} due to ", zone.getZoneName(), e);
                        }
                    }
                }
            }
        }
    }

    private void createTunnelsFromOVSinTransportZone(String zoneName, DPNTEPsInfo dpn, TunnelEndPoints tep,
                                                   TypedReadWriteTransaction<Configuration> tx, Integer monitorInterval,
                                                   Class<? extends TunnelMonitoringTypeBase> monitorProtocol)
            throws ExecutionException, InterruptedException {
        Optional<TransportZone> transportZoneOptional = tx.read(InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(zoneName)).build()).get();
        if (transportZoneOptional.isPresent()) {
            TransportZone transportZone = transportZoneOptional.get();
            //do we need to check tunnel type?
            if (transportZone.getDeviceVteps() != null && !transportZone.getDeviceVteps().isEmpty()) {
                String portName = itmConfig.getPortname() == null ? ITMConstants.DUMMY_PORT : itmConfig.getPortname();
                int vlanId = itmConfig.getVlanId() != null ? itmConfig.getVlanId().toJava()
                                                             : ITMConstants.DUMMY_VLANID;
                for (DeviceVteps hwVtepDS : transportZone.getDeviceVteps()) {
                    //dont mesh if hwVteps and OVS-tep have same ip-address
                    if (Objects.equals(hwVtepDS.getIpAddress(), tep.getIpAddress())) {
                        continue;
                    }
                    final String cssID = dpn.getDPNID().toString();
                    String nodeId = hwVtepDS.getNodeId();
                    boolean useOfTunnel = ItmUtils.falseIfNull(tep.isOptionOfTunnel());
                    LOG.trace("wire up {} and {}",tep, hwVtepDS);
                    if (!wireUp(dpn.getDPNID(), portName, vlanId, tep.getIpAddress(), useOfTunnel, nodeId,
                            hwVtepDS.getIpAddress(), transportZone.getTunnelType(), false,
                            monitorInterval, monitorProtocol, tx)) {
                        LOG.error("Unable to build tunnel {} -- {}",
                                tep.getIpAddress(), hwVtepDS.getIpAddress());
                    }
                    //TOR-OVS
                    LOG.trace("wire up {} and {}", hwVtepDS,tep);
                    if (!wireUp(hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                            cssID, tep.getIpAddress(),
                            transportZone.getTunnelType(), false, monitorInterval,
                            monitorProtocol, tx)) {
                        LOG.error("Unable to build tunnel {} -- {}",
                                hwVtepDS.getIpAddress(), tep.getIpAddress());
                    }

                }
            }
        }
    }

    private void tunnelsFromhWVtep(HwVtep hwTep, TypedReadWriteTransaction<Configuration> tx,
                                   Integer monitorInterval, Class<? extends TunnelMonitoringTypeBase> monitorProtocol)
            throws ExecutionException, InterruptedException {
        Optional<TransportZone> transportZoneOptional = tx.read(InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(hwTep.getTransportZone())).build()).get();
        Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
        if (transportZoneOptional.isPresent()) {
            TransportZone tzone = transportZoneOptional.get();
            String portName = itmConfig.getPortname() == null ? ITMConstants.DUMMY_PORT : itmConfig.getPortname();
            int vlanId = itmConfig.getVlanId() != null ? itmConfig.getVlanId().toJava() : ITMConstants.DUMMY_VLANID;
            //do we need to check tunnel type?
            if (tzone.getDeviceVteps() != null && !tzone.getDeviceVteps().isEmpty()) {
                for (DeviceVteps hwVtepDS : tzone.getDeviceVteps()) {
                    if (Objects.equals(hwVtepDS.getIpAddress(), hwTep.getHwIp())) {
                        continue;//dont mesh with self
                    }
                    LOG.trace("wire up {} and {}",hwTep, hwVtepDS);
                    if (!wireUp(hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(),
                            hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                            tunType, false,
                            monitorInterval, monitorProtocol, tx)) {
                        LOG.error("Unable to build tunnel {} -- {}",
                                hwTep.getHwIp(), hwVtepDS.getIpAddress());
                    }
                    //TOR2-TOR1
                    LOG.trace("wire up {} and {}", hwVtepDS,hwTep);
                    if (!wireUp(hwTep.getTopoId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                            hwTep.getNodeId(), hwTep.getHwIp(),
                            tunType, false, monitorInterval,
                            monitorProtocol, tx)) {
                        LOG.error("Unable to build tunnel {} -- {}",
                                hwVtepDS.getIpAddress(), hwTep.getHwIp());
                    }
                }
            }
            for (Vteps vtep : tzone.getVteps()) {
                if (Objects.equals(vtep.getIpAddress(), hwTep.getHwIp())) {
                    continue;
                }
                //TOR-OVS
                String cssID = vtep.getDpnId().toString();
                LOG.trace("wire up {} and {}",hwTep, vtep);
                if (!wireUp(hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(), cssID,
                        vtep.getIpAddress(),
                        tunType,false, monitorInterval, monitorProtocol,
                        tx)) {
                    LOG.error("Unable to build tunnel {} -- {}",
                            hwTep.getHwIp(), vtep.getIpAddress());
                }
                //OVS-TOR
                LOG.trace("wire up {} and {}", vtep,hwTep);
                boolean useOfTunnel = ItmUtils.falseIfNull(vtep.isOptionOfTunnel());
                if (!wireUp(vtep.getDpnId(), portName, vlanId, vtep.getIpAddress(),
                        useOfTunnel, hwTep.getNodeId(),hwTep.getHwIp(),
                        tunType, false,
                        monitorInterval, monitorProtocol, tx)) {
                    LOG.debug("wireUp returned false");
                }
            }
        }
    }

    //for tunnels from TOR device
    private boolean wireUp(String topoId, String srcNodeid, IpAddress srcIp, String dstNodeId, IpAddress dstIp,
                           Class<? extends TunnelTypeBase> tunType,
                           Boolean monitorEnabled, Integer monitorInterval,
                           Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                           TypedWriteTransaction<Configuration> tx) {
        IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
        IpAddress gwyIpAddress = gatewayIpObj;
        String parentIf =  ItmUtils.getHwParentIf(topoId, srcNodeid);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(parentIf,
                srcIp.stringValue(), dstIp.stringValue(), tunTypeStr);
        LOG.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, "
                        + "source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp,
                dstIp, gwyIpAddress);
        Interface hwTunnelIf = ItmUtils.buildHwTunnelInterface(tunnelIfName,
                trunkInterface(tunType), true, topoId, srcNodeid, tunType, srcIp,
                dstIp, gwyIpAddress, monitorEnabled, monitorProtocol, monitorInterval);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(tunnelIfName)).build();
        LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, hwTunnelIf);
        ItmUtils.ITM_CACHE.addInterface(hwTunnelIf);
        tx.merge(ifIID, hwTunnelIf, true);
        // also update itm-state ds?
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(getExternalTunnelKey(dstNodeId),
                        getExternalTunnelKey(srcNodeid), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(getExternalTunnelKey(srcNodeid),
                getExternalTunnelKey(dstNodeId), tunType, tunnelIfName);
        tx.merge(path, tnl, true);
        ItmUtils.ITM_CACHE.addExternalTunnel(tnl);
        return true;
    }

    //for tunnels from OVS
    private boolean wireUp(Uint64 dpnId, String portname, Integer vlanId, IpAddress srcIp, Boolean remoteIpFlow,
                           String dstNodeId, IpAddress dstIp,
                           Class<? extends TunnelTypeBase> tunType, Boolean monitorEnabled, Integer monitorInterval,
                           Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                           TypedWriteTransaction<Configuration> tx) {

        IpAddress gwyIpAddress = GATEWAY_IP_OBJ;

        String parentIf = ItmUtils.getInterfaceName(dpnId, portname, vlanId);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(parentIf,
                srcIp.stringValue(), dstIp.stringValue(), tunTypeStr);
        LOG.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, "
                        + "source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp,
                dstIp, gwyIpAddress);
        Interface extTunnelIf = ItmUtils.buildTunnelInterface(dpnId, tunnelIfName,
                trunkInterface(tunType), true, tunType, srcIp, dstIp,
                false, monitorEnabled, monitorProtocol, monitorInterval, remoteIpFlow, null);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey(tunnelIfName)).build();
        LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, extTunnelIf);
        tx.merge(ifIID, extTunnelIf, true);
        ItmUtils.ITM_CACHE.addInterface(extTunnelIf);
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(getExternalTunnelKey(dstNodeId),
                        dpnId.toString(), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(dpnId.toString(),
                getExternalTunnelKey(dstNodeId),
                tunType, tunnelIfName);
        tx.merge(path, tnl, true);
        ItmUtils.ITM_CACHE.addExternalTunnel(tnl);
        return true;
    }

    private static String trunkInterface(Class<? extends TunnelTypeBase> tunType) {
        return trunkInterface(tunType.getName());
    }

    private static String trunkInterface(String tunType) {
        return tunType + " Trunk Interface";
    }

    @SuppressFBWarnings("RV_CHECK_FOR_POSITIVE_INDEXOF")
    static String getExternalTunnelKey(String nodeid) {
        final int index = nodeid.indexOf("physicalswitch");
        if (index > 0) {
            nodeid = nodeid.substring(0, index - 1);
        }
        return nodeid;
    }

}
