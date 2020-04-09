/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItmExternalTunnelDeleteWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ItmExternalTunnelDeleteWorker.class);

    private ItmExternalTunnelDeleteWorker() {

    }

    public static void deleteTunnels(Collection<DPNTEPsInfo> dpnTepsList, Collection<DPNTEPsInfo> meshedDpnList,
                                     IpAddress extIp, Class<? extends TunnelTypeBase> tunType,
                                     TypedWriteTransaction<Configuration> tx) {
        LOG.trace(" Delete Tunnels towards DC Gateway with Ip  {}", extIp);
        if (dpnTepsList == null || dpnTepsList.isEmpty()) {
            LOG.debug("no vtep to delete");
            return;
        }
        for (DPNTEPsInfo teps : dpnTepsList) {
            TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
            // The membership in the listener will always be 1, to get the actual membership
            // read from the DS
            List<TzMembership> originalTzMembership = ItmUtils.getOriginalTzMembership(firstEndPt,
                    teps.getDPNID(), meshedDpnList);
            if (originalTzMembership.size() == 1) {
                String interfaceName = firstEndPt.getInterfaceName();
                String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(interfaceName,
                        firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
                InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
                tx.delete(trunkIdentifier);
                ItmUtils.ITM_CACHE.removeInterface(trunkInterfaceName);

                InstanceIdentifier<ExternalTunnel> path =
                        InstanceIdentifier.create(ExternalTunnelList.class).child(ExternalTunnel.class,
                               ItmUtils.getExternalTunnelKey(extIp.stringValue(), teps.getDPNID().toString(), tunType));
                tx.delete(path);
                LOG.debug("Deleting tunnel towards DC gateway, Tunnel interface name {} ", trunkInterfaceName);
                ItmUtils.ITM_CACHE.removeExternalTunnel(trunkInterfaceName);
                // Release the Ids for the trunk interface Name
                ItmUtils.releaseIdForTrunkInterfaceName(interfaceName,
                        firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
            }
        }
    }

    public static void deleteTunnels(Collection<DPNTEPsInfo> dpnTepsList, IpAddress extIp,
                                     Class<? extends TunnelTypeBase> tunType, TypedWriteTransaction<Configuration> tx) {
        LOG.trace(" Delete Tunnels towards DC Gateway with Ip  {}", extIp);

        if (dpnTepsList == null || dpnTepsList.isEmpty()) {
            LOG.debug("no vtep to delete");
            return;
        }
        for (DPNTEPsInfo teps : dpnTepsList) {
            TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
            String interfaceName = firstEndPt.getInterfaceName();
            String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(interfaceName,
                    firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
            tx.delete(trunkIdentifier);
            ItmUtils.ITM_CACHE.removeInterface(trunkInterfaceName);

            InstanceIdentifier<ExternalTunnel> path =
                    InstanceIdentifier.create(ExternalTunnelList.class).child(ExternalTunnel.class,
                            ItmUtils.getExternalTunnelKey(extIp.stringValue(), teps.getDPNID().toString(), tunType));
            tx.delete(path);
            LOG.debug("Deleting tunnel towards DC gateway, Tunnel interface name {} ", trunkInterfaceName);
            ItmUtils.ITM_CACHE.removeExternalTunnel(trunkInterfaceName);
            // Release the Ids for the trunk interface Name
            ItmUtils.releaseIdForTrunkInterfaceName(interfaceName,
                    firstEndPt.getIpAddress().stringValue(), extIp.stringValue(), tunType.getName());
        }
    }

    public static void deleteHwVtepsTunnels(List<DPNTEPsInfo> delDpnList, List<HwVtep> cfgdHwVteps,
                                            TransportZone originalTZone, TypedReadWriteTransaction<Configuration> tx,
                                            ItmConfig itmConfig) throws ExecutionException, InterruptedException {
        if (delDpnList != null || cfgdHwVteps != null) {
            tunnelsDeletion(delDpnList, cfgdHwVteps, originalTZone, tx, itmConfig);
        }
    }

    private static void tunnelsDeletion(List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> cfgdhwVteps,
                                        TransportZone originalTZone, TypedReadWriteTransaction<Configuration> tx,
                                        ItmConfig itmConfig) throws ExecutionException, InterruptedException {
        if (cfgdDpnList != null) {
            for (DPNTEPsInfo dpn : cfgdDpnList) {
                if (dpn.getTunnelEndPoints() != null) {
                    for (TunnelEndPoints srcTep : dpn.getTunnelEndPoints()) {
                        for (TzMembership zone : srcTep.nonnullTzMembership()) {
                            deleteTunnelsInTransportZone(zone.getZoneName(), dpn, srcTep, cfgdhwVteps, tx);
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
                        deleteTrunksTorTor(hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(),
                                hwTepRemote.getTopoId(), hwTepRemote.getNodeId(), hwTepRemote.getHwIp(),
                                TunnelTypeVxlan.class, tx);
                    }
                }
                // do we need to check tunnel type?
                if (originalTZone.getDeviceVteps() != null) {
                    for (DeviceVteps hwVtepDS : originalTZone.getDeviceVteps()) {
                        LOG.trace("hwtepDS exists {}", hwVtepDS);
                        // do i need to check node-id?
                        // for mlag case and non-m-lag case, isnt it enough to just check ipaddress?
                        if (Objects.equals(hwVtepDS.getIpAddress(), hwTep.getHwIp())) {
                            continue;// dont delete tunnels with self
                        }
                        // TOR-TOR
                        LOG.trace("deleting tor-tor {} and {}", hwTep, hwVtepDS);
                        deleteTrunksTorTor(hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(),
                                hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(), hwVtepDS.getIpAddress(),
                                originalTZone.getTunnelType(), tx);
                    }
                }
                if (originalTZone.getVteps() != null) {

                    String portName = itmConfig.getPortname() == null ? ITMConstants.DUMMY_PORT
                            : itmConfig.getPortname();
                    int vlanId = itmConfig.getVlanId() != null ? itmConfig.getVlanId().toJava()
                                                                 : ITMConstants.DUMMY_VLANID;

                    for (Vteps vtep : originalTZone.getVteps()) {
                        // TOR-OVS
                        LOG.trace("deleting tor-css-tor {} and {}", hwTep, vtep);
                        String parentIf = ItmUtils.getInterfaceName(vtep.getDpnId(), portName, vlanId);
                        deleteTrunksOvsTor(vtep.getDpnId(), parentIf,
                                vtep.getIpAddress(), hwTep.getTopoId(), hwTep.getNodeId(), hwTep.getHwIp(),
                                originalTZone.getTunnelType(), tx);
                    }
                }
            }
        }
    }

    private static void deleteTunnelsInTransportZone(String zoneName, DPNTEPsInfo dpn, TunnelEndPoints srcTep,
            List<HwVtep> cfgdhwVteps, TypedReadWriteTransaction<Configuration> tx)
                    throws InterruptedException, ExecutionException {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(zoneName)).build();
        Optional<TransportZone> tz = tx.read(tzonePath).get();
        if (tz.isPresent()) {
            TransportZone tzone = tz.get();
            // do we need to check tunnel type?
            if (tzone.getDeviceVteps() != null) {
                for (DeviceVteps hwVtepDS : tzone.getDeviceVteps()) {
                    // OVS-TOR-OVS
                    deleteTrunksOvsTor(dpn.getDPNID(), srcTep.getInterfaceName(),
                            srcTep.getIpAddress(), hwVtepDS.getTopologyId(), hwVtepDS.getNodeId(),
                            hwVtepDS.getIpAddress(), tzone.getTunnelType(), tx);
                }
            }

            if (cfgdhwVteps != null && !cfgdhwVteps.isEmpty()) {
                for (HwVtep hwVtep : cfgdhwVteps) {
                    deleteTrunksOvsTor(dpn.getDPNID(), srcTep.getInterfaceName(),
                            srcTep.getIpAddress(), hwVtep.getTopoId(), hwVtep.getNodeId(), hwVtep.getHwIp(),
                            TunnelTypeVxlan.class, tx);
                }
            }
        }
    }

    private static void deleteTrunksOvsTor(Uint64 dpnid, String interfaceName, IpAddress cssIpAddress,
                      String topologyId, String nodeId, IpAddress hwIpAddress, Class<? extends TunnelTypeBase> tunType,
                      TypedReadWriteTransaction<Configuration> tx) throws ExecutionException, InterruptedException {
        // OVS-TOR
        if (trunkExists(dpnid.toString(), nodeId, tunType, tx)) {
            LOG.trace("deleting tunnel from {} to {} ", dpnid.toString(), nodeId);
            String parentIf = interfaceName;
            String fwdTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    cssIpAddress.stringValue(), hwIpAddress.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(fwdTrunkIf);
            tx.delete(trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(nodeId, dpnid.toString(), tunType));
            tx.delete(path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", dpnid.toString(), nodeId);
        }
        // TOR-OVS
        if (trunkExists(nodeId, dpnid.toString(), tunType, tx)) {
            LOG.trace("deleting tunnel from {} to {} ", nodeId, dpnid.toString());

            String parentIf = ItmUtils.getHwParentIf(topologyId, nodeId);
            String revTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    hwIpAddress.stringValue(), cssIpAddress.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(revTrunkIf);
            tx.delete(trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(dpnid.toString(), nodeId, tunType));
            tx.delete(path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", nodeId, dpnid.toString());
        }
    }

    private static void deleteTrunksTorTor(String topologyId1, String nodeId1, IpAddress hwIpAddress1,
                   String topologyId2, String nodeId2, IpAddress hwIpAddress2, Class<? extends TunnelTypeBase> tunType,
                   TypedReadWriteTransaction<Configuration> tx) throws ExecutionException, InterruptedException {
        // TOR1-TOR2
        if (trunkExists(nodeId1, nodeId2, tunType, tx)) {
            LOG.trace("deleting tunnel from {} to {} ", nodeId1, nodeId2);
            String parentIf = ItmUtils.getHwParentIf(topologyId1, nodeId1);
            String fwdTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    hwIpAddress1.stringValue(), hwIpAddress2.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(fwdTrunkIf);
            tx.delete(trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(nodeId2, nodeId1, tunType));
            tx.delete(path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", nodeId1, nodeId2);
        }
        // TOR2-TOR1
        if (trunkExists(nodeId2, nodeId1, tunType, tx)) {
            LOG.trace("deleting tunnel from {} to {} ", nodeId2, nodeId1);

            String parentIf = ItmUtils.getHwParentIf(topologyId2, nodeId2);
            String revTrunkIf = ItmUtils.getTrunkInterfaceName(parentIf,
                    hwIpAddress2.stringValue(), hwIpAddress1.stringValue(), tunType.getName());
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(revTrunkIf);
            tx.delete(trunkIdentifier);

            InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                    .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(nodeId1, nodeId2, tunType));
            tx.delete(path);
        } else {
            LOG.trace(" trunk from {} to {} already deleted", nodeId2, nodeId1);
        }
    }

    private static boolean trunkExists(String srcDpnOrNode, String dstDpnOrNode,
                                       Class<? extends TunnelTypeBase> tunType,
                                       TypedReadWriteTransaction<Configuration> tx)
            throws ExecutionException, InterruptedException {
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class, ItmUtils.getExternalTunnelKey(dstDpnOrNode, srcDpnOrNode, tunType));
        return tx.exists(path).get();
    }
}
