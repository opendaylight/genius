/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItmExternalTunnelDeleteWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ItmExternalTunnelDeleteWorker.class);

    private ItmExternalTunnelDeleteWorker() { }

    public static List<ListenableFuture<Void>> deleteTunnels(DataBroker dataBroker,
            Collection<DPNTEPsInfo> dpnTepsList, Collection<DPNTEPsInfo> meshedDpnList, IpAddress extIp,
            Class<? extends TunnelTypeBase> tunType) {
        LOG.trace(" Delete Tunnels towards DC Gateway with Ip  {}", extIp);

        if (dpnTepsList == null || dpnTepsList.isEmpty()) {
            LOG.debug("no vtep to delete");
            return Collections.emptyList();
        }
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        for (DPNTEPsInfo teps : dpnTepsList) {
            TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
            // The membership in the listener will always be 1, to get the actual membership
            // read from the DS
            List<TzMembership> originalTzMembership = ItmUtils.getOriginalTzMembership(firstEndPt, teps.getDPNID(),
                    meshedDpnList);
            if (originalTzMembership.size() == 1) {
                String interfaceName = firstEndPt.getInterfaceName();
                String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(interfaceName,
                        firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
                InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
                transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
                ItmUtils.ITM_CACHE.removeInterface(trunkInterfaceName);

                InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class).child(
                        ExternalTunnel.class,
                        ItmUtils.getExternalTunnelKey(extIp.stringValue(), teps.getDPNID().toString(), tunType));
                transaction.delete(LogicalDatastoreType.CONFIGURATION, path);
                LOG.debug("Deleting tunnel towards DC gateway, Tunnel interface name {} ", trunkInterfaceName);
                ItmUtils.ITM_CACHE.removeExternalTunnel(trunkInterfaceName);
                // Release the Ids for the trunk interface Name
                ItmUtils.releaseIdForTrunkInterfaceName(interfaceName,
                        firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
            }
        }
        return Collections.singletonList(transaction.submit());
    }

    public static List<ListenableFuture<Void>> deleteTunnels(DataBroker dataBroker,
            Collection<DPNTEPsInfo> dpnTepsList, IpAddress extIp, Class<? extends TunnelTypeBase> tunType) {
        LOG.trace(" Delete Tunnels towards DC Gateway with Ip  {}", extIp);

        if (dpnTepsList == null || dpnTepsList.isEmpty()) {
            LOG.debug("no vtep to delete");
            return Collections.emptyList();
        }
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        for (DPNTEPsInfo teps : dpnTepsList) {
            TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
            String interfaceName = firstEndPt.getInterfaceName();
            String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(interfaceName,
                    firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
            writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);
            ItmUtils.ITM_CACHE.removeInterface(trunkInterfaceName);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class).child(
                    ExternalTunnel.class,
                    ItmUtils.getExternalTunnelKey(extIp.stringValue(), teps.getDPNID().toString(), tunType));
            writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, path);
            LOG.debug("Deleting tunnel towards DC gateway, Tunnel interface name {} ", trunkInterfaceName);
            ItmUtils.ITM_CACHE.removeExternalTunnel(trunkInterfaceName);
            // Release the Ids for the trunk interface Name
            ItmUtils.releaseIdForTrunkInterfaceName(interfaceName,
                    firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
        }
        return Collections.singletonList(writeTransaction.submit());
    }

    public static List<ListenableFuture<Void>> deleteHwVtepsTunnels(DataBroker dataBroker,
            List<DPNTEPsInfo> delDpnList, List<HwVtep> cfgdHwVteps,
            TransportZone originalTZone) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        if (delDpnList != null || cfgdHwVteps != null) {
            tunnelsDeletion(delDpnList, cfgdHwVteps, originalTZone, writeTransaction,
                    dataBroker);
        }
        futures.add(writeTransaction.submit());
        return futures;
    }

    private static void tunnelsDeletion(List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> cfgdhwVteps,
            TransportZone originalTZone,
            WriteTransaction writeTransaction, DataBroker dataBroker) {
        if (cfgdDpnList != null) {
            for (DPNTEPsInfo dpn : cfgdDpnList) {
                if (dpn.getTunnelEndPoints() != null) {
                    for (TunnelEndPoints srcTep : dpn.getTunnelEndPoints()) {
                        for (TzMembership zone : nullToEmpty(srcTep.getTzMembership())) {
                            deleteTunnelsInTransportZone(zone.getZoneName(), dpn, srcTep, cfgdhwVteps, dataBroker,
                                    writeTransaction);
                        }
                    }
                }
            }
        }

        if (cfgdhwVteps != null && !cfgdhwVteps.isEmpty()) {
            for (HwVtep hwTep : cfgdhwVteps) {
                LOG.trace("processing hwTep from list {}", hwTep);
                for (HwVtep hwTepRemote : cfgdhwVteps) {
                    if (!hwTep.getHwIp().equals(hwTepRemote.getHwIp())) {
                        deleteTrunksTorTor(dataBroker, hwTep.getTopoId(), hwTep.getNodeId(),
                                hwTep.getHwIp(), hwTepRemote.getTopoId(), hwTepRemote.getNodeId(),
                                hwTepRemote.getHwIp(), TunnelTypeVxlan.class, writeTransaction);
                    }
                }
                // do we need to check tunnel type?
                LOG.trace("subnets under tz {} are {}", originalTZone.getZoneName(), originalTZone.getSubnets());
                if (originalTZone.getSubnets() != null && !originalTZone.getSubnets().isEmpty()) {

                    for (Subnets sub : originalTZone.getSubnets()) {
                        if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                            for (DeviceVteps hwVtepDS : sub.getDeviceVteps()) {
                                LOG.trace("hwtepDS exists {}", hwVtepDS);
                                // do i need to check node-id?
                                // for mlag case and non-m-lag case, isnt it enough to just check ipaddress?
                                if (Objects.equals(hwVtepDS.getIpAddress(), hwTep.getHwIp())) {
                                    continue;// dont delete tunnels with self
                                }
                                // TOR-TOR
                                LOG.trace("deleting tor-tor {} and {}", hwTep, hwVtepDS);
                                deleteTrunksTorTor(dataBroker, hwTep.getTopoId(), hwTep.getNodeId(),
                                        hwTep.getHwIp(), hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(),
                                        hwVtepDS.getIpAddress(), originalTZone.getTunnelType(), writeTransaction);

                            }
                        }
                        if (sub.getVteps() != null && !sub.getVteps().isEmpty()) {
                            for (Vteps vtep : sub.getVteps()) {
                                // TOR-OVS
                                LOG.trace("deleting tor-css-tor {} and {}", hwTep, vtep);
                                String parentIf = ItmUtils.getInterfaceName(vtep.getDpnId(), vtep.getPortname(),
                                        sub.getVlanId());
                                deleteTrunksOvsTor(dataBroker, vtep.getDpnId(), parentIf,
                                        vtep.getIpAddress(), hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(),
                                        originalTZone.getTunnelType(), writeTransaction);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void deleteTunnelsInTransportZone(String zoneName, DPNTEPsInfo dpn, TunnelEndPoints srcTep,
            List<HwVtep> cfgdhwVteps, DataBroker dataBroker, WriteTransaction writeTransaction) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(zoneName)).build();
        Optional<TransportZone> transportZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath,
                dataBroker);
        if (transportZoneOptional.isPresent()) {
            TransportZone tzone = transportZoneOptional.get();
            // do we need to check tunnel type?
            if (tzone.getSubnets() != null && !tzone.getSubnets().isEmpty()) {
                for (Subnets sub : tzone.getSubnets()) {
                    if (sub.getDeviceVteps() != null && !sub.getDeviceVteps().isEmpty()) {
                        for (DeviceVteps hwVtepDS : sub.getDeviceVteps()) {
                            // OVS-TOR-OVS
                            deleteTrunksOvsTor(dataBroker, dpn.getDPNID(), srcTep.getInterfaceName(),
                                    srcTep.getIpAddress(), hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(),
                                    hwVtepDS.getIpAddress(), tzone.getTunnelType(), writeTransaction);

                        }
                    }
                }
            }
            if (cfgdhwVteps != null && !cfgdhwVteps.isEmpty()) {
                for (HwVtep hwVtep : cfgdhwVteps) {
                    deleteTrunksOvsTor(dataBroker, dpn.getDPNID(), srcTep.getInterfaceName(),
                            srcTep.getIpAddress(), hwVtep.getTopoId(), hwVtep.getNodeId(), hwVtep.getHwIp(),
                            TunnelTypeVxlan.class, writeTransaction);

                }
            }
        }
    }

    private static void deleteTrunksOvsTor(DataBroker dataBroker, BigInteger dpnid,
            String interfaceName, IpAddress cssIpAddress, String topologyId, String nodeId, IpAddress hwIpAddress,
            Class<? extends TunnelTypeBase> tunType, WriteTransaction transaction) {
        // OVS-TOR
        if (trunkExists(dpnid.toString(), nodeId, tunType, dataBroker)) {
            LOG.trace("deleting tunnel from {} to {} ", dpnid.toString(), nodeId);
            String parentIf = interfaceName;
            String fwdTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    cssIpAddress.stringValue(), hwIpAddress.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(fwdTrunkIf);
            transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(nodeId, dpnid.toString(), tunType));
            transaction.delete(LogicalDatastoreType.CONFIGURATION, path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", dpnid.toString(), nodeId);
        }
        // TOR-OVS
        if (trunkExists(nodeId, dpnid.toString(), tunType, dataBroker)) {
            LOG.trace("deleting tunnel from {} to {} ", nodeId, dpnid.toString());

            String parentIf = ItmUtils.getHwParentIf(topologyId, nodeId);
            String revTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    hwIpAddress.stringValue(), cssIpAddress.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(revTrunkIf);
            transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(dpnid.toString(), nodeId, tunType));
            transaction.delete(LogicalDatastoreType.CONFIGURATION, path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", nodeId, dpnid.toString());
        }
    }

    private static void deleteTrunksTorTor(DataBroker dataBroker, String topologyId1,
            String nodeId1, IpAddress hwIpAddress1, String topologyId2, String nodeId2, IpAddress hwIpAddress2,
            Class<? extends TunnelTypeBase> tunType, WriteTransaction transaction) {
        // TOR1-TOR2
        if (trunkExists(nodeId1, nodeId2, tunType, dataBroker)) {
            LOG.trace("deleting tunnel from {} to {} ", nodeId1, nodeId2);
            String parentIf = ItmUtils.getHwParentIf(topologyId1, nodeId1);
            String fwdTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    hwIpAddress1.stringValue(), hwIpAddress2.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(fwdTrunkIf);
            transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(nodeId2, nodeId1, tunType));
            transaction.delete(LogicalDatastoreType.CONFIGURATION, path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", nodeId1, nodeId2);
        }
        // TOR2-TOR1
        if (trunkExists(nodeId2, nodeId1, tunType, dataBroker)) {
            LOG.trace("deleting tunnel from {} to {} ", nodeId2, nodeId1);

            String parentIf = ItmUtils.getHwParentIf(topologyId2, nodeId2);
            String revTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    hwIpAddress2.stringValue(), hwIpAddress1.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(revTrunkIf);
            transaction.delete(LogicalDatastoreType.CONFIGURATION, trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(nodeId1, nodeId2, tunType));
            transaction.delete(LogicalDatastoreType.CONFIGURATION, path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", nodeId2, nodeId1);
        }
    }

    private static boolean trunkExists(String srcDpnOrNode, String dstDpnOrNode,
            Class<? extends TunnelTypeBase> tunType, DataBroker dataBroker) {
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(dstDpnOrNode, srcDpnOrNode, tunType));
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker).isPresent();
    }
}
