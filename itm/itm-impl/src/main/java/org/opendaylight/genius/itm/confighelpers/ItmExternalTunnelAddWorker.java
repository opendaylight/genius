/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.genius.infra.Datastore.Configuration;
import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;

import com.google.common.base.Optional;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmExternalTunnelAddWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ItmExternalTunnelAddWorker.class);

    private final DataBroker dataBroker;
    private final ItmConfig itmConfig;
    private final DPNTEPsInfoCache dpnTEPsInfoCache;

    public ItmExternalTunnelAddWorker(DataBroker dataBroker, ItmConfig itmConfig, DPNTEPsInfoCache dpnTEPsInfoCache) {
        this.dataBroker = dataBroker;
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
                String subnetMaskStr = firstEndPt.getSubnetMask().stringValue();
                boolean useOfTunnel = ItmUtils.falseIfNull(firstEndPt.isOptionOfTunnel());
                List<TunnelOptions> tunOptions = ItmUtils.buildTunnelOptions(firstEndPt, itmConfig);
                SubnetUtils utils = new SubnetUtils(subnetMaskStr);
                String dcGwyIpStr = extIp.stringValue();
                IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
                IpAddress gwyIpAddress =
                        utils.getInfo().isInRange(dcGwyIpStr) ? gatewayIpObj : firstEndPt.getGwIpAddress();
                LOG.debug(" Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {},"
                        + " source IP - {}, DC Gateway IP - {} gateway IP - {}", trunkInterfaceName, interfaceName,
                        firstEndPt.getIpAddress(), extIp, gwyIpAddress);
                Interface iface = ItmUtils.buildTunnelInterface(teps.getDPNID(), trunkInterfaceName,
                    String.format("%s %s", ItmUtils.convertTunnelTypetoString(tunType), "Trunk Interface"), true,
                    tunType, firstEndPt.getIpAddress(), extIp, gwyIpAddress, firstEndPt.getVLANID(), false, false,
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

    public void buildTunnelsFromDpnToExternalEndPoint(List<BigInteger> dpnId, IpAddress extIp,
                                                      Class<? extends TunnelTypeBase> tunType,
                                                      TypedWriteTransaction<Configuration> tx) {
        Collection<DPNTEPsInfo> cfgDpnList = dpnId == null ? dpnTEPsInfoCache.getAllPresent()
                        : ItmUtils.getDpnTepListFromDpnId(dpnTEPsInfoCache, dpnId);
        buildTunnelsToExternalEndPoint(cfgDpnList, extIp, tunType, tx);
    }

    public void buildHwVtepsTunnels(List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> cfgdHwVteps,
                                    TypedReadWriteTransaction<Configuration> tx) {
        if (!(cfgdDpnList != null && !cfgdDpnList.isEmpty() || cfgdHwVteps != null && !cfgdHwVteps.isEmpty())) {
            throw new UnsupportedOperationException();
        }

        Integer monitorInterval = ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL;
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;

        if (null != cfgdDpnList && !cfgdDpnList.isEmpty()) {
            LOG.trace("calling tunnels from OVS {}",cfgdDpnList);
            cfgdDpnList.forEach(dpntePsInfo -> tunnelsFromOVS(dpntePsInfo, tx, monitorInterval, monitorProtocol));
        }
        if (null != cfgdHwVteps && !cfgdHwVteps.isEmpty()) {
            LOG.trace("calling tunnels from hwTep {}",cfgdHwVteps);
            cfgdHwVteps.forEach(hwVtep -> {
                try {
                    Optional<TransportZone> optional = tx.read(InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(hwVtep.getTransportZone())).build()).get();
                    if (optional.isPresent()) {
                        tunnelsFromhWVtep(hwVtep, optional.get(), tx, monitorInterval, monitorProtocol);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("Tunnel Creation failed for {} due to ", hwVtep.getTransportZone(), e);
                }
            });
        }
    }

    private void tunnelsFromOVS(DPNTEPsInfo dpn, TypedReadWriteTransaction<Configuration> tx,
                                Integer monitorInterval, Class<? extends TunnelMonitoringTypeBase> monitorProtocol) {
        LOG.trace("processing dpn {}" , dpn);
        if (dpn.getTunnelEndPoints() != null && !dpn.getTunnelEndPoints().isEmpty()) {
            for (TunnelEndPoints tep : dpn.getTunnelEndPoints()) {
                for (TzMembership zone:  nullToEmpty(tep.getTzMembership())) {
                    try {
                        Optional<TransportZone> optional = tx.read(InstanceIdentifier
                                .builder(TransportZones.class).child(TransportZone.class,
                                        new TransportZoneKey(zone.getZoneName())).build()).get();
                        if (optional.isPresent()) {
                            createTunnelsFromOVSinTransportZone(optional.get(), dpn, tep, tx, monitorInterval,
                                monitorProtocol);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        LOG.error("Tunnel Creation failed for {} due to ", zone.getZoneName(), e);
                    }
                }
            }
        }
    }

    private void createTunnelsFromOVSinTransportZone(TransportZone transportZone, DPNTEPsInfo dpn, TunnelEndPoints tep,
                                                     TypedReadWriteTransaction<Configuration> tx,
                                                     Integer monitorInterval,
                                                     Class<? extends TunnelMonitoringTypeBase> monitorProtocol) {
        //do we need to check tunnel type?
        if (transportZone.getSubnets() != null && !transportZone.getSubnets().isEmpty()) {
            for (Subnets sub : transportZone.getSubnets()) {
                if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                    for (DeviceVteps hwVtepDS : sub.getDeviceVteps()) {
                        //dont mesh if hwVteps and OVS-tep have same ip-address
                        if (hwVtepDS.getIpAddress().equals(tep.getIpAddress())) {
                            continue;
                        }
                        final String cssID = dpn.getDPNID().toString();
                        String nodeId = hwVtepDS.getNodeId();
                        boolean useOfTunnel = ItmUtils.falseIfNull(tep.isOptionOfTunnel());
                        LOG.trace("wire up {} and {}",tep, hwVtepDS);
                        if (!wireUp(dpn.getDPNID(), tep.getPortname(), sub.getVlanId(),
                                tep.getIpAddress(), useOfTunnel, nodeId, hwVtepDS.getIpAddress(),
                                tep.getSubnetMask(), sub.getGatewayIp(), sub.getPrefix(),
                                transportZone.getTunnelType(), false, monitorInterval, monitorProtocol, tx)) {
                            LOG.error("Unable to build tunnel {} -- {}",
                                    tep.getIpAddress(), hwVtepDS.getIpAddress());
                        }
                        //TOR-OVS
                        LOG.trace("wire up {} and {}", hwVtepDS,tep);
                        if (!wireUp(hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                                cssID, tep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(),
                                tep.getSubnetMask(), transportZone.getTunnelType(), false, monitorInterval,
                                monitorProtocol, tx)) {
                            LOG.error("Unable to build tunnel {} -- {}",
                                    hwVtepDS.getIpAddress(), tep.getIpAddress());
                        }
                    }
                }
            }
        }
    }

    private void tunnelsFromhWVtep(HwVtep hwVtep, TransportZone tzone,
                                   TypedWriteTransaction<Configuration> transaction, Integer monitorInterval,
                                   Class<? extends TunnelMonitoringTypeBase> monitorProtocol) {
        final Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
        //do we need to check tunnel type?
        if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
            for (Subnets sub : tzone.getSubnets()) {
                if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                    for (DeviceVteps hwVtepDS : sub.getDeviceVteps()) {
                        if (hwVtepDS.getIpAddress().equals(hwVtep.getHwIp())) {
                            continue;//dont mesh with self
                        }
                        LOG.trace("wire up {} and {}",hwVtep, hwVtepDS);
                        if (!wireUp(hwVtep.getTopoId(), hwVtep.getNodeId(), hwVtep.getHwIp(),
                                hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(), hwVtep.getIpPrefix(),
                                hwVtep.getGatewayIP(), sub.getPrefix(), tunType, false,
                                monitorInterval, monitorProtocol, transaction)) {
                            LOG.error("Unable to build tunnel {} -- {}",
                                    hwVtep.getHwIp(), hwVtepDS.getIpAddress());
                        }
                        //TOR2-TOR1
                        LOG.trace("wire up {} and {}", hwVtepDS,hwVtep);
                        if (!wireUp(hwVtep.getTopoId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                                hwVtep.getNodeId(), hwVtep.getHwIp(), sub.getPrefix(), sub.getGatewayIp(),
                                hwVtep.getIpPrefix(), tunType, false, monitorInterval,
                                monitorProtocol, transaction)) {
                            LOG.error("Unable to build tunnel {} -- {}",
                                    hwVtepDS.getIpAddress(), hwVtep.getHwIp());
                        }
                    }
                }
                if (sub.getVteps() != null && !sub.getVteps().isEmpty()) {
                    for (Vteps vtep : sub.getVteps()) {
                        if (vtep.getIpAddress().equals(hwVtep.getHwIp())) {
                            continue;
                        }
                        //TOR-OVS
                        String cssID = vtep.getDpnId().toString();
                        LOG.trace("wire up {} and {}",hwVtep, vtep);
                        if (!wireUp(hwVtep.getTopoId(), hwVtep.getNodeId(), hwVtep.getHwIp(), cssID,
                                vtep.getIpAddress(), hwVtep.getIpPrefix(), hwVtep.getGatewayIP(),
                                sub.getPrefix(), tunType,false, monitorInterval, monitorProtocol,
                                transaction)) {
                            LOG.error("Unable to build tunnel {} -- {}",
                                    hwVtep.getHwIp(), vtep.getIpAddress());
                        }
                        //OVS-TOR
                        LOG.trace("wire up {} and {}", vtep,hwVtep);
                        boolean useOfTunnel = ItmUtils.falseIfNull(vtep.isOptionOfTunnel());
                        if (!wireUp(vtep.getDpnId(), vtep.getPortname(), sub.getVlanId(), vtep.getIpAddress(),
                                useOfTunnel, hwVtep.getNodeId(),hwVtep.getHwIp(),sub.getPrefix(),
                                sub.getGatewayIp(),hwVtep.getIpPrefix(), tunType, false,
                                monitorInterval, monitorProtocol, transaction)) {
                            LOG.debug("wireUp returned false");
                        }
                    }
                }
            }
        }
    }

    //for tunnels from TOR device
    private boolean wireUp(String topoId, String srcNodeid, IpAddress srcIp, String dstNodeId, IpAddress dstIp,
                           IpPrefix srcSubnet, IpAddress gwIp, IpPrefix dstSubnet,
                           Class<? extends TunnelTypeBase> tunType, Boolean monitorEnabled, Integer monitorInterval,
                           Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                           TypedWriteTransaction<Configuration> transaction) {
        IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
        IpAddress gwyIpAddress = srcSubnet.equals(dstSubnet) ? gatewayIpObj : gwIp;
        String parentIf =  ItmUtils.getHwParentIf(topoId, srcNodeid);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(parentIf,
                srcIp.stringValue(), dstIp.stringValue(), tunTypeStr);
        LOG.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, "
                + "source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp,
                dstIp, gwyIpAddress);
        Interface hwTunnelIf = ItmUtils.buildHwTunnelInterface(tunnelIfName,
                String.format("%s %s", tunType.getName(), "Trunk Interface"), true, topoId, srcNodeid, tunType, srcIp,
                dstIp, gwyIpAddress, monitorEnabled, monitorProtocol, monitorInterval);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(tunnelIfName)).build();
        LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, hwTunnelIf);
        ItmUtils.ITM_CACHE.addInterface(hwTunnelIf);
        transaction.merge(ifIID, hwTunnelIf, true);
        // also update itm-state ds?
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(getExternalTunnelKey(dstNodeId),
                        getExternalTunnelKey(srcNodeid), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(getExternalTunnelKey(srcNodeid),
                getExternalTunnelKey(dstNodeId), tunType, tunnelIfName);
        transaction.merge(path, tnl, true);
        ItmUtils.ITM_CACHE.addExternalTunnel(tnl);
        return true;
    }

    //for tunnels from OVS
    private boolean wireUp(BigInteger dpnId, String portname, Integer vlanId, IpAddress srcIp, Boolean remoteIpFlow,
                           String dstNodeId, IpAddress dstIp, IpPrefix srcSubnet, IpAddress gwIp, IpPrefix dstSubnet,
                           Class<? extends TunnelTypeBase> tunType, Boolean monitorEnabled, Integer monitorInterval,
                           Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                           TypedWriteTransaction<Configuration> transaction) {
        IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
        IpAddress gwyIpAddress = srcSubnet.equals(dstSubnet) ? gatewayIpObj : gwIp;
        String parentIf = ItmUtils.getInterfaceName(dpnId, portname, vlanId);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(parentIf,
                srcIp.stringValue(), dstIp.stringValue(), tunTypeStr);
        LOG.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, "
                + "source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp,
                dstIp, gwyIpAddress);
        Interface extTunnelIf = ItmUtils.buildTunnelInterface(dpnId, tunnelIfName,
                String.format("%s %s", tunType.getName(), "Trunk Interface"), true, tunType, srcIp, dstIp, gwyIpAddress,
                vlanId, false,monitorEnabled, monitorProtocol, monitorInterval, remoteIpFlow, null);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey(tunnelIfName)).build();
        LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, extTunnelIf);
        transaction.merge(ifIID, extTunnelIf, true);
        ItmUtils.ITM_CACHE.addInterface(extTunnelIf);
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(getExternalTunnelKey(dstNodeId),
                        dpnId.toString(), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(dpnId.toString(),
                getExternalTunnelKey(dstNodeId),
                tunType, tunnelIfName);
        transaction.merge(path, tnl, true);
        ItmUtils.ITM_CACHE.addExternalTunnel(tnl);
        return true;
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
