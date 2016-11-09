/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ItmExternalTunnelAddWorker {
    private static final Logger logger = LoggerFactory.getLogger(ItmExternalTunnelAddWorker.class);

    private static Boolean monitorEnabled;
    private static Integer monitorInterval;
    private static Class<? extends TunnelMonitoringTypeBase> monitorProtocol;

    private static final FutureCallback<Void> DEFAULT_CALLBACK =
            new FutureCallback<Void>() {
                public void onSuccess(Void result) {
                    logger.debug("Success in Datastore operation");
                }

                public void onFailure(Throwable error) {
                    logger.error("Error in Datastore operation", error);
                }

            };

    public static List<ListenableFuture<Void>> buildTunnelsToExternalEndPoint(DataBroker dataBroker, IdManagerService idManagerService,
                                                                              List<DPNTEPsInfo> cfgDpnList, IpAddress extIp, Class<? extends TunnelTypeBase> tunType) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        if (null != cfgDpnList) {
            for (DPNTEPsInfo teps : cfgDpnList) {
                // CHECK -- Assumption -- Only one End Point / Dpn for GRE/Vxlan Tunnels
                TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
                String interfaceName = firstEndPt.getInterfaceName();
                String tunTypeStr = tunType.getName();
                String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(idManagerService, interfaceName, firstEndPt.getIpAddress().getIpv4Address().getValue(), extIp.getIpv4Address().getValue(), tunTypeStr);
                char[] subnetMaskArray = firstEndPt.getSubnetMask().getValue();
                String subnetMaskStr = String.valueOf(subnetMaskArray);
                SubnetUtils utils = new SubnetUtils(subnetMaskStr);
                String dcGwyIpStr = String.valueOf(extIp.getValue());
                IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
                IpAddress gwyIpAddress = (utils.getInfo().isInRange(dcGwyIpStr)) ? gatewayIpObj : firstEndPt.getGwIpAddress();
                logger.debug(" Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {}, source IP - {}, DC Gateway IP - {} gateway IP - {}", trunkInterfaceName, interfaceName, firstEndPt.getIpAddress(), extIp, gwyIpAddress);
                Interface iface = ItmUtils.buildTunnelInterface(teps.getDPNID(), trunkInterfaceName, String.format("%s %s", ItmUtils.convertTunnelTypetoString(tunType), "Trunk Interface"), true, tunType, firstEndPt.getIpAddress(), extIp, gwyIpAddress, firstEndPt.getVLANID(), false,false,ITMConstants.DEFAULT_MONITOR_PROTOCOL,null);
                logger.debug(" Trunk Interface builder - {} ", iface);
                InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
                logger.debug(" Trunk Interface Identifier - {} ", trunkIdentifier);
                logger.trace(" Writing Trunk Interface to Config DS {}, {} ", trunkIdentifier, iface);
                t.merge(LogicalDatastoreType.CONFIGURATION, trunkIdentifier, iface, true);
                // update external_tunnel_list ds
                InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(
                        ExternalTunnelList.class)
                        .child(ExternalTunnel.class, new ExternalTunnelKey(extIp.toString(), teps.getDPNID().toString(), tunType));
                ExternalTunnel tnl = ItmUtils.buildExternalTunnel(  teps.getDPNID().toString(), extIp.toString(),
                        tunType, trunkInterfaceName);
                t.merge(LogicalDatastoreType.CONFIGURATION, path, tnl, true);
            }
            futures.add(t.submit());
        }
        return futures;
    }

    public static List<ListenableFuture<Void>> buildTunnelsFromDpnToExternalEndPoint(DataBroker dataBroker, IdManagerService idManagerService,
                                                                                     List<BigInteger> dpnId, IpAddress extIp, Class<? extends TunnelTypeBase> tunType) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        List<DPNTEPsInfo> cfgDpnList = (dpnId == null) ? ItmUtils.getTunnelMeshInfo(dataBroker) : ItmUtils.getDPNTEPListFromDPNId(dataBroker, dpnId);
        futures = buildTunnelsToExternalEndPoint(dataBroker, idManagerService, cfgDpnList, extIp, tunType);
        return futures;
    }

    public static List<ListenableFuture<Void>> buildHwVtepsTunnels(DataBroker dataBroker, IdManagerService idManagerService, List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> cfgdHwVteps) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        monitorInterval = ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL;
        monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
        if (null != cfgdDpnList && !cfgdDpnList.isEmpty()) {
            logger.trace("calling tunnels from css {}",cfgdDpnList);
            tunnelsFromCSS(cfgdDpnList, idManagerService , futures, t , dataBroker);

        }
        if (null != cfgdHwVteps && !cfgdHwVteps.isEmpty() ) {
            logger.trace("calling tunnels from hwTep {}",cfgdHwVteps);
            tunnelsFromhWVtep(cfgdHwVteps, idManagerService, futures, t, dataBroker);
        }

        if ((cfgdDpnList != null && !cfgdDpnList.isEmpty()) || (cfgdHwVteps != null && !cfgdHwVteps.isEmpty()))
            futures.add(t.submit());
        return futures;
    }

    private static void tunnelsFromCSS(List<DPNTEPsInfo> cfgdDpnList, IdManagerService idManagerService, List<ListenableFuture<Void>> futures, WriteTransaction t, DataBroker dataBroker) {
        Boolean monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        for (DPNTEPsInfo dpn : cfgdDpnList) {
            logger.trace("processing dpn {}" , dpn);
            if (dpn.getTunnelEndPoints() != null && !dpn.getTunnelEndPoints().isEmpty())
                for (TunnelEndPoints tep : dpn.getTunnelEndPoints()) {
                    InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class).child(TransportZone.class, new TransportZoneKey((tep.getTransportZone()))).build();
                    Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker);
                    if (tZoneOptional.isPresent()) {
                        TransportZone tZone = tZoneOptional.get();
                        //do we need to check tunnel type?
                        if (tZone.getSubnets() != null && !tZone.getSubnets().isEmpty()) {
                            for (Subnets sub : tZone.getSubnets()) {
                                if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                                    for (DeviceVteps hwVtepDS : sub.getDeviceVteps()) {
                                        //dont mesh if hwVteps and CSS-tep have same ip-address
                                        if(hwVtepDS.getIpAddress().equals(tep.getIpAddress()))
                                            continue;
                                        String cssID = dpn.getDPNID().toString();
                                        String nodeId = hwVtepDS.getNodeId();
                                        logger.trace("wire up {} and {}",tep, hwVtepDS);
                                        if (!wireUp(dpn.getDPNID(), tep.getPortname(), sub.getVlanId(), tep.getIpAddress(), nodeId, hwVtepDS.getIpAddress(), tep.getSubnetMask(),
                                                sub.getGatewayIp(), sub.getPrefix(), tZone.getTunnelType(),false, monitorProtocol, monitorInterval, idManagerService, dataBroker, futures, t))
                                            logger.error("Unable to build tunnel {} -- {}", tep.getIpAddress(), hwVtepDS.getIpAddress());
                                        //TOR-CSS
                                        logger.trace("wire up {} and {}", hwVtepDS,tep);
                                        if (!wireUp(hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(), cssID, tep.getIpAddress(), sub.getPrefix(),
                                                sub.getGatewayIp(), tep.getSubnetMask(), tZone.getTunnelType(), false, monitorProtocol, monitorInterval, idManagerService, dataBroker, futures, t))
                                            logger.error("Unable to build tunnel {} -- {}", hwVtepDS.getIpAddress(), tep.getIpAddress());

                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    private static void tunnelsFromhWVtep(List<HwVtep> cfgdHwVteps, IdManagerService idManagerService, List<ListenableFuture<Void>> futures, WriteTransaction t, DataBroker dataBroker) {
        for (HwVtep hwTep : cfgdHwVteps) {
            InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class).child(TransportZone.class, new TransportZoneKey((hwTep.getTransportZone()))).build();
            Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker);
            Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
            Boolean monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
            Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
            if (tZoneOptional.isPresent()) {
                TransportZone tZone = tZoneOptional.get();
                //do we need to check tunnel type?
                if (tZone.getSubnets() != null && !tZone.getSubnets().isEmpty()) {
                    for (Subnets sub : tZone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            for (DeviceVteps hwVtepDS : sub.getDeviceVteps()) {
                                if (hwVtepDS.getIpAddress().equals(hwTep.getHwIp()))
                                    continue;//dont mesh with self
                                logger.trace("wire up {} and {}",hwTep, hwVtepDS);
                                if (!wireUp(hwTep.getTopo_id(), hwTep.getNode_id(), hwTep.getHwIp(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                                        hwTep.getIpPrefix(), hwTep.getGatewayIP(), sub.getPrefix(), tunType,false,monitorProtocol,
                                                monitorInterval, idManagerService, dataBroker, futures, t))
                                    logger.error("Unable to build tunnel {} -- {}", hwTep.getHwIp(), hwVtepDS.getIpAddress());
                                //TOR2-TOR1
                                logger.trace("wire up {} and {}", hwVtepDS,hwTep);
                                if (!wireUp(hwTep.getTopo_id(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(), hwTep.getNode_id(), hwTep.getHwIp(),
                                        sub.getPrefix(), sub.getGatewayIp(), hwTep.getIpPrefix(), tunType, false,monitorProtocol, monitorInterval, idManagerService, dataBroker, futures, t))
                                    logger.error("Unable to build tunnel {} -- {}", hwVtepDS.getIpAddress(), hwTep.getHwIp());
                            }
                        }
                        if (sub.getVteps() != null && !sub.getVteps().isEmpty()) {
                            for (Vteps vtep : sub.getVteps()) {
                                if(vtep.getIpAddress().equals(hwTep.getHwIp()))
                                    continue;
                                //TOR-CSS
                                String cssID = vtep.getDpnId().toString();
                                logger.trace("wire up {} and {}",hwTep, vtep);
                                if(!wireUp(hwTep.getTopo_id(), hwTep.getNode_id(), hwTep.getHwIp(), cssID, vtep.getIpAddress(), hwTep.getIpPrefix(),
                                        hwTep.getGatewayIP(), sub.getPrefix(), tunType,false, monitorProtocol, monitorInterval, idManagerService, dataBroker, futures, t ))
                                    logger.error("Unable to build tunnel {} -- {}", hwTep.getHwIp(), vtep.getIpAddress());
                                //CSS-TOR
                                logger.trace("wire up {} and {}", vtep,hwTep);
                                if(!wireUp(vtep.getDpnId(), vtep.getPortname(), sub.getVlanId(), vtep.getIpAddress(),
                                                hwTep.getNode_id(),hwTep.getHwIp(),sub.getPrefix(), sub.getGatewayIp(),hwTep.getIpPrefix(),
                                                tunType,false,monitorProtocol, monitorInterval,idManagerService, dataBroker, futures, t ));
                            }

                        }
                    }
                }
            }
        }
    }

    //for tunnels from TOR device
    private static boolean wireUp(String topo_id, String srcNodeid, IpAddress srcIp, String dstNodeId, IpAddress dstIp, IpPrefix srcSubnet,
                                  IpAddress gWIp, IpPrefix dstSubnet, Class<? extends TunnelTypeBase> tunType,Boolean monitorEnabled,  Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                  Integer monitorInterval,IdManagerService idManagerService, DataBroker dataBroker, List<ListenableFuture<Void>> futures, WriteTransaction t) {
        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        IpAddress gwyIpAddress = (srcSubnet.equals(dstSubnet)) ? gatewayIpObj : gWIp;
        String parentIf =  ItmUtils.getHwParentIf(topo_id, srcNodeid);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(idManagerService, parentIf,
                srcIp.getIpv4Address().getValue(), dstIp.getIpv4Address().getValue(), tunTypeStr);
        logger.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp, dstIp, gwyIpAddress);
        Interface hwTunnelIf = ItmUtils.buildHwTunnelInterface(tunnelIfName, String.format("%s %s", tunType.getName(), "Trunk Interface"),
                true, topo_id, srcNodeid, tunType, srcIp, dstIp, gwyIpAddress, monitorEnabled, monitorProtocol, monitorInterval);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(tunnelIfName)).build();
        logger.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, hwTunnelIf);
        ItmUtils.itmCache.addInterface(hwTunnelIf);
        t.merge(LogicalDatastoreType.CONFIGURATION, ifIID, hwTunnelIf, true);
        // also update itm-state ds?
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(
                ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey( getExternalTunnelKey(dstNodeId), getExternalTunnelKey(srcNodeid), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(  getExternalTunnelKey(srcNodeid),
                getExternalTunnelKey(dstNodeId),
                tunType, tunnelIfName);
        t.merge(LogicalDatastoreType.CONFIGURATION, path, tnl, true);
        ItmUtils.itmCache.addExternalTunnel(tnl);
        return true;
    }

    //for tunnels from CSS
    private static boolean wireUp(BigInteger dpnId, String portname, Integer vlanId, IpAddress srcIp, String dstNodeId, IpAddress dstIp, IpPrefix srcSubnet,
                                  IpAddress gWIp, IpPrefix dstSubnet, Class<? extends TunnelTypeBase> tunType, Boolean monitorEnabled, Class<? extends TunnelMonitoringTypeBase> monitorProtocol, Integer monitorInterval,
                                  IdManagerService idManagerService, DataBroker dataBroker, List<ListenableFuture<Void>> futures, WriteTransaction t) {
        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        IpAddress gwyIpAddress = (srcSubnet.equals(dstSubnet)) ? gatewayIpObj : gWIp;
        String parentIf = ItmUtils.getInterfaceName(dpnId, portname, vlanId);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(idManagerService, parentIf,
                srcIp.getIpv4Address().getValue(), dstIp.getIpv4Address().getValue(), tunTypeStr);
        logger.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp, dstIp, gwyIpAddress);
        Interface extTunnelIf = ItmUtils.buildTunnelInterface(dpnId, tunnelIfName, String.format("%s %s", tunType.getName(), "Trunk Interface"), true, tunType, srcIp, dstIp, gwyIpAddress, vlanId, false,monitorEnabled, monitorProtocol, monitorInterval);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(tunnelIfName)).build();
        logger.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, extTunnelIf);
        t.merge(LogicalDatastoreType.CONFIGURATION, ifIID, extTunnelIf, true);
        ItmUtils.itmCache.addInterface(extTunnelIf);
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(
                ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(getExternalTunnelKey(dstNodeId), dpnId.toString(), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(  dpnId.toString(),
                getExternalTunnelKey(dstNodeId),
                tunType, tunnelIfName);
        t.merge(LogicalDatastoreType.CONFIGURATION, path, tnl, true);
        ItmUtils.itmCache.addExternalTunnel(tnl);
        return true;
    }

    static String getExternalTunnelKey(String nodeid) {
        if (nodeid.indexOf("physicalswitch") > 0) {
            nodeid = nodeid.substring(0, nodeid.indexOf("physicalswitch") - 1);
        }
        return nodeid;
    }

}

