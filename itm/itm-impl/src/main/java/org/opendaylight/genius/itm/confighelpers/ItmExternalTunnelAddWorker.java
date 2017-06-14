/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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

    private static Boolean monitorEnabled;
    private static Integer monitorInterval;
    private static Class<? extends TunnelMonitoringTypeBase> monitorProtocol;
    private static List<DPNTEPsInfo> cfgDpnList;

    private static final FutureCallback<Void> DEFAULT_CALLBACK =
        new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                LOG.debug("Success in Datastore operation");
            }

            @Override
            public void onFailure(Throwable error) {
                LOG.error("Error in Datastore operation", error);
            }

    };

    public static List<ListenableFuture<Void>> buildTunnelsToExternalEndPoint(DataBroker dataBroker,
                                                                              IdManagerService idManagerService,
                                                                              IpAddress extIp,
                                                                              Class<? extends TunnelTypeBase> tunType,
                                                                              ItmConfig itmConfig) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        cfgDpnList = ItmUtils.getTunnelMeshInfo(dataBroker) ;
        if (null != cfgDpnList) {
            for (DPNTEPsInfo teps : cfgDpnList) {
                // CHECK -- Assumption -- Only one End Point / Dpn for GRE/Vxlan Tunnels
                TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
                String interfaceName = firstEndPt.getInterfaceName();
                String tunTypeStr = tunType.getName();
                String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(idManagerService, interfaceName,
                        new String(firstEndPt.getIpAddress().getValue()),
                        new String(extIp.getValue()), tunTypeStr);
                char[] subnetMaskArray = firstEndPt.getSubnetMask().getValue();
                boolean useOfTunnel = ItmUtils.falseIfNull(firstEndPt.isOptionOfTunnel());
                List<TunnelOptions> tunOptions = ItmUtils.buildTunnelOptions(firstEndPt, itmConfig);
                String subnetMaskStr = String.valueOf(subnetMaskArray);
                SubnetUtils utils = new SubnetUtils(subnetMaskStr);
                String dcGwyIpStr = String.valueOf(extIp.getValue());
                IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
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
                transaction.merge(LogicalDatastoreType.CONFIGURATION, trunkIdentifier, iface, true);
                // update external_tunnel_list ds
                InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                        .child(ExternalTunnel.class, new ExternalTunnelKey(extIp.toString(),
                                teps.getDPNID().toString(), tunType));
                ExternalTunnel tnl = ItmUtils.buildExternalTunnel(teps.getDPNID().toString(), extIp.toString(),
                        tunType, trunkInterfaceName);
                transaction.merge(LogicalDatastoreType.CONFIGURATION, path, tnl, true);
            }
            futures.add(transaction.submit());
        }
        return futures;
    }

    public static List<ListenableFuture<Void>> buildTunnelsFromDpnToExternalEndPoint(DataBroker dataBroker,
                                                                                     IdManagerService idManagerService,
                                                                                     List<BigInteger> dpnId,
                                                                                     IpAddress extIp,
                                                                                     Class<? extends TunnelTypeBase>
                                                                                             tunType,
                                                                                     ItmConfig itmConfig) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        List<DPNTEPsInfo> cfgDpnList = dpnId == null ? ItmUtils.getTunnelMeshInfo(dataBroker)
                        : ItmUtils.getDpnTepListFromDpnId(dataBroker, dpnId);
        futures = buildTunnelsToExternalEndPoint(dataBroker, idManagerService, extIp, tunType, itmConfig);
        return futures;
    }

    public static List<ListenableFuture<Void>> buildHwVtepsTunnels(DataBroker dataBroker,
                                                                   IdManagerService idManagerService,
                                                                   List<DPNTEPsInfo> cfgdDpnList,
                                                                   List<HwVtep> cfgdHwVteps) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        monitorInterval = ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL;
        monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
        if (null != cfgdDpnList && !cfgdDpnList.isEmpty()) {
            LOG.trace("calling tunnels from css {}",cfgdDpnList);
            tunnelsFromCSS(cfgdDpnList, idManagerService , futures, writeTransaction , dataBroker);
        }
        if (null != cfgdHwVteps && !cfgdHwVteps.isEmpty()) {
            LOG.trace("calling tunnels from hwTep {}",cfgdHwVteps);
            tunnelsFromhWVtep(cfgdHwVteps, idManagerService, futures, writeTransaction, dataBroker);
        }

        if (cfgdDpnList != null && !cfgdDpnList.isEmpty() || cfgdHwVteps != null && !cfgdHwVteps.isEmpty()) {
            futures.add(writeTransaction.submit());
        }
        return futures;
    }

    private static void tunnelsFromCSS(List<DPNTEPsInfo> cfgdDpnList, IdManagerService idManagerService,
                                       List<ListenableFuture<Void>> futures, WriteTransaction transaction,
                                       DataBroker dataBroker) {
        Boolean monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
        Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        for (DPNTEPsInfo dpn : cfgdDpnList) {
            LOG.trace("processing dpn {}" , dpn);
            if (dpn.getTunnelEndPoints() != null && !dpn.getTunnelEndPoints().isEmpty()) {
                for (TunnelEndPoints tep : dpn.getTunnelEndPoints()) {
                    for (TzMembership zone: tep.getTzMembership()) {
                        createTunnelsFromOVSinTransportZone(zone.getZoneName(), dpn, tep, idManagerService,
                                futures, transaction, dataBroker);
                    }
                }
            }
        }
    }

    private static void createTunnelsFromOVSinTransportZone(String zoneName, DPNTEPsInfo dpn, TunnelEndPoints tep,
                                                            IdManagerService idManagerService,
                                                            List<ListenableFuture<Void>> futures,
                                                            WriteTransaction transaction, DataBroker dataBroker) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(zoneName)).build();
        Optional<TransportZone> transportZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                tzonePath, dataBroker);
        if (transportZoneOptional.isPresent()) {
            TransportZone transportZone = transportZoneOptional.get();
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
                                    transportZone.getTunnelType(), false, monitorProtocol, monitorInterval,
                                    idManagerService, dataBroker, futures, transaction)) {
                                LOG.error("Unable to build tunnel {} -- {}",
                                        tep.getIpAddress(), hwVtepDS.getIpAddress());
                            }
                            //TOR-OVS
                            LOG.trace("wire up {} and {}", hwVtepDS,tep);
                            if (!wireUp(hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                                    cssID, tep.getIpAddress(), sub.getPrefix(), sub.getGatewayIp(),
                                    tep.getSubnetMask(), transportZone.getTunnelType(), false, monitorProtocol,
                                    monitorInterval, idManagerService, dataBroker, futures, transaction)) {
                                LOG.error("Unable to build tunnel {} -- {}",
                                        hwVtepDS.getIpAddress(), tep.getIpAddress());
                            }

                        }
                    }
                }
            }
        }
    }

    private static void tunnelsFromhWVtep(List<HwVtep> cfgdHwVteps, IdManagerService idManagerService,
                                          List<ListenableFuture<Void>> futures, WriteTransaction transaction,
                                          DataBroker dataBroker) {
        for (HwVtep hwTep : cfgdHwVteps) {
            InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                    .child(TransportZone.class, new TransportZoneKey(hwTep.getTransportZone())).build();
            Optional<TransportZone> transportZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                    tzonePath, dataBroker);
            Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
            Boolean monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
            Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
            if (transportZoneOptional.isPresent()) {
                TransportZone tzone = transportZoneOptional.get();
                //do we need to check tunnel type?
                if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                    for (Subnets sub : tzone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            for (DeviceVteps hwVtepDS : sub.getDeviceVteps()) {
                                if (hwVtepDS.getIpAddress().equals(hwTep.getHwIp())) {
                                    continue;//dont mesh with self
                                }
                                LOG.trace("wire up {} and {}",hwTep, hwVtepDS);
                                if (!wireUp(hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(),
                                        hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(), hwTep.getIpPrefix(),
                                        hwTep.getGatewayIP(), sub.getPrefix(), tunType,false,monitorProtocol,
                                        monitorInterval, idManagerService, dataBroker, futures, transaction)) {
                                    LOG.error("Unable to build tunnel {} -- {}",
                                            hwTep.getHwIp(), hwVtepDS.getIpAddress());
                                }
                                //TOR2-TOR1
                                LOG.trace("wire up {} and {}", hwVtepDS,hwTep);
                                if (!wireUp(hwTep.getTopoId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                                        hwTep.getNodeId(), hwTep.getHwIp(), sub.getPrefix(), sub.getGatewayIp(),
                                        hwTep.getIpPrefix(), tunType, false, monitorProtocol, monitorInterval,
                                        idManagerService, dataBroker, futures, transaction)) {
                                    LOG.error("Unable to build tunnel {} -- {}",
                                            hwVtepDS.getIpAddress(), hwTep.getHwIp());
                                }
                            }
                        }
                        if (sub.getVteps() != null && !sub.getVteps().isEmpty()) {
                            for (Vteps vtep : sub.getVteps()) {
                                if (vtep.getIpAddress().equals(hwTep.getHwIp())) {
                                    continue;
                                }
                                //TOR-OVS
                                String cssID = vtep.getDpnId().toString();
                                LOG.trace("wire up {} and {}",hwTep, vtep);
                                if (!wireUp(hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(), cssID,
                                        vtep.getIpAddress(), hwTep.getIpPrefix(), hwTep.getGatewayIP(),
                                        sub.getPrefix(), tunType,false, monitorProtocol, monitorInterval,
                                        idManagerService, dataBroker, futures, transaction)) {
                                    LOG.error("Unable to build tunnel {} -- {}",
                                            hwTep.getHwIp(), vtep.getIpAddress());
                                }
                                //OVS-TOR
                                LOG.trace("wire up {} and {}", vtep,hwTep);
                                boolean useOfTunnel = ItmUtils.falseIfNull(vtep.isOptionOfTunnel());
                                if (!wireUp(vtep.getDpnId(), vtep.getPortname(), sub.getVlanId(), vtep.getIpAddress(),
                                        useOfTunnel, hwTep.getNodeId(),hwTep.getHwIp(),sub.getPrefix(),
                                        sub.getGatewayIp(),hwTep.getIpPrefix(), tunType,false,monitorProtocol,
                                        monitorInterval, idManagerService, dataBroker, futures, transaction)) {
                                    //do nothing
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    //for tunnels from TOR device
    private static boolean wireUp(String topoId, String srcNodeid, IpAddress srcIp, String dstNodeId, IpAddress dstIp,
                                  IpPrefix srcSubnet, IpAddress gwIp, IpPrefix dstSubnet,
                                  Class<? extends TunnelTypeBase> tunType,Boolean monitorEnabled,
                                  Class<? extends TunnelMonitoringTypeBase> monitorProtocol, Integer monitorInterval,
                                  IdManagerService idManagerService, DataBroker dataBroker,
                                  List<ListenableFuture<Void>> futures, WriteTransaction transaction) {
        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        IpAddress gwyIpAddress = srcSubnet.equals(dstSubnet) ? gatewayIpObj : gwIp;
        String parentIf =  ItmUtils.getHwParentIf(topoId, srcNodeid);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(idManagerService, parentIf,
                new String(srcIp.getValue()), new String(dstIp.getValue()), tunTypeStr);
        LOG.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, "
                + "source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp,
                dstIp, gwyIpAddress);
        Interface hwTunnelIf = ItmUtils.buildHwTunnelInterface(tunnelIfName,
                String.format("%s %s", tunType.getName(), "Trunk Interface"), true, topoId, srcNodeid, tunType, srcIp,
                dstIp, gwyIpAddress, monitorEnabled, monitorProtocol, monitorInterval);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(tunnelIfName)).build();
        LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, hwTunnelIf);
        ItmUtils.itmCache.addInterface(hwTunnelIf);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, ifIID, hwTunnelIf, true);
        // also update itm-state ds?
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(getExternalTunnelKey(dstNodeId),
                        getExternalTunnelKey(srcNodeid), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(getExternalTunnelKey(srcNodeid),
                getExternalTunnelKey(dstNodeId), tunType, tunnelIfName);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, path, tnl, true);
        ItmUtils.itmCache.addExternalTunnel(tnl);
        return true;
    }

    //for tunnels from OVS
    private static boolean wireUp(BigInteger dpnId, String portname, Integer vlanId, IpAddress srcIp,
                                  Boolean remoteIpFlow, String dstNodeId, IpAddress dstIp, IpPrefix srcSubnet,
                                  IpAddress gwIp, IpPrefix dstSubnet, Class<? extends TunnelTypeBase> tunType,
                                  Boolean monitorEnabled, Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                  Integer monitorInterval, IdManagerService idManagerService, DataBroker dataBroker,
                                  List<ListenableFuture<Void>> futures, WriteTransaction transaction) {
        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        IpAddress gwyIpAddress = srcSubnet.equals(dstSubnet) ? gatewayIpObj : gwIp;
        String parentIf = ItmUtils.getInterfaceName(dpnId, portname, vlanId);
        String tunTypeStr = tunType.getName();
        String tunnelIfName = ItmUtils.getTrunkInterfaceName(idManagerService, parentIf,
                new String(srcIp.getValue()), new String(dstIp.getValue()), tunTypeStr);
        LOG.debug(" Creating ExternalTrunk Interface with parameters Name - {}, parent I/f name - {}, "
                + "source IP - {}, destination IP - {} gateway IP - {}", tunnelIfName, parentIf, srcIp,
                dstIp, gwyIpAddress);
        Interface extTunnelIf = ItmUtils.buildTunnelInterface(dpnId, tunnelIfName,
                String.format("%s %s", tunType.getName(), "Trunk Interface"), true, tunType, srcIp, dstIp, gwyIpAddress,
                vlanId, false,monitorEnabled, monitorProtocol, monitorInterval, remoteIpFlow, null);
        InstanceIdentifier<Interface> ifIID = InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey(tunnelIfName)).build();
        LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", ifIID, extTunnelIf);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, ifIID, extTunnelIf, true);
        ItmUtils.itmCache.addInterface(extTunnelIf);
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(getExternalTunnelKey(dstNodeId),
                        dpnId.toString(), tunType));
        ExternalTunnel tnl = ItmUtils.buildExternalTunnel(dpnId.toString(),
                getExternalTunnelKey(dstNodeId),
                tunType, tunnelIfName);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, path, tnl, true);
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
