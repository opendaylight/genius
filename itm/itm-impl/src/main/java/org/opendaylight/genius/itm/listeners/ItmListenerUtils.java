/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.InstructionType;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.VtepConfigSchemas;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.VtepIpPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchemaKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.vtep.config.schema.DpnIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.vtep.config.schema.DpnIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.vtep.config.schema.DpnIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.ip.pools.VtepIpPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.ip.pools.VtepIpPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmListenerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmListenerUtils.class);

    public static boolean isBridgeOnOvsdbNode(Node ovsdbNode, String bridgeName) {
        boolean found = false;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation != null) {
            List<ManagedNodeEntry> managedNodes = ovsdbNodeAugmentation.getManagedNodeEntry();
            if (managedNodes != null) {
                for (ManagedNodeEntry managedNode : managedNodes) {
                    InstanceIdentifier<?> bridgeIid = managedNode.getBridgeRef().getValue();
                    if (bridgeIid.toString().contains(bridgeName)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    public static BigInteger getLongDatapathId(Node node) {
        BigInteger dpid = BigInteger.valueOf(0);;
        String datapathId = getStrDatapathId(node);
        if (datapathId != null) {
            dpid = new BigInteger(datapathId.replaceAll(":", ""), 16);
        }
        return dpid;
    }

    public static String getStrDatapathId(Node node) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        String datapathId = null;
        if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
            datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
        }
        return datapathId;
    }

    public static TransportZone getTransportZoneFromITMConfigDS(String tzone,DataBroker dataBroker) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath,
                dataBroker);
        if (tZoneOptional.isPresent()) {
            return tZoneOptional.get();
        }
        return null;
    }

    public static TepsNotHostedInTransportZone getUnknownTransportZoneFromITMConfigDS(String unknownTz, DataBroker dataBroker) {
        InstanceIdentifier<TepsNotHostedInTransportZone> unknownTzPath = InstanceIdentifier.builder(TransportZones.class)
                .child(TepsNotHostedInTransportZone.class, new TepsNotHostedInTransportZoneKey(unknownTz)).build();
        Optional<TepsNotHostedInTransportZone> unknownTzOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, unknownTzPath,
                dataBroker);
        if (unknownTzOptional.isPresent()) {
            return unknownTzOptional.get();
        }
        return null;
    }

    public static void removeUnknownTzTepIntoTepsNotHosted(String tzName, IpAddress tepIpAddress, BigInteger dpnId, DataBroker dataBroker) {
        List<UnknownVteps> vtepList = null;
        boolean addRemoveFlag = false; // FALSE is remove case

        TepsNotHostedInTransportZone unknownTz = ItmListenerUtils.getUnknownTransportZoneFromITMConfigDS(tzName, dataBroker);
        if (unknownTz == null) {
            LOG.info("Unknown TransportZone does not exist. Nothing to do for TEP removal.");
            return;
        } else {
            vtepList = unknownTz.getUnknownVteps();
            if (vtepList == null || vtepList.isEmpty()) {
                //  case: vtep list does not exist or it has no elements
                LOG.info("Remove TEP in unknown TZ ({}) when no vtep-list in the TZ. Nothing to do.", tzName);
            } else {
                //  case: vtep list has elements
                boolean vtepFound = false;
                UnknownVteps oldVtep = null;

                for (UnknownVteps vtep : vtepList) {
                    if (vtep.getDpnId().equals(dpnId)) {
                        vtepFound = true;
                        oldVtep = vtep;
                        break;
                    }
                }
                if (vtepFound) {
                    // vtep is found, update it with tep-ip
                    vtepList.remove(oldVtep);
                    LOG.info("Remove TEP with IP ({}) in unknown TZ ({}) as new TEP into vtep-list in the TZ.", tepIpAddress, tzName);
                    addRemoveVtepIntoTepsNotHosted(vtepList, tepIpAddress, tzName, dpnId, dataBroker, addRemoveFlag);
                }
            }
        }
    }

    public static void addRemoveVtepIntoTepsNotHosted(List<UnknownVteps> updatedVtepList, IpAddress tepIpAddress,
                                               String tzName, BigInteger dpnId, DataBroker dataBroker, boolean addRemoveFlag) {
        //Create TZ node path
        InstanceIdentifier<TepsNotHostedInTransportZone> tZonepath =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TepsNotHostedInTransportZone.class, new TepsNotHostedInTransportZoneKey(tzName)).build();

        // if true, add vtep case else remove case
        if (addRemoveFlag) {
            // create vtep
            UnknownVtepsKey vtepkey = new UnknownVtepsKey(dpnId);
            UnknownVteps vtepObj = new UnknownVtepsBuilder().setDpnId(dpnId).setIpAddress(tepIpAddress).setKey(vtepkey).build();

            // Add vtep obtained into unknown TZ tep list
            updatedVtepList.add(vtepObj);
        }

        // create unknown TZ node with updated vtep list
        TepsNotHostedInTransportZone updatedTzone =
                new TepsNotHostedInTransportZoneBuilder().setKey(new TepsNotHostedInTransportZoneKey(tzName))
                        .setZoneName(tzName).setUnknownVteps(updatedVtepList).build();

        if (addRemoveFlag) {
            LOG.info("Adding TEP into unknown (TZ: {}, DPID: {}, TEP IP: {}) in ITM Config DS.", tzName, dpnId, tepIpAddress);
        } else {
            LOG.info("Removing TEP from unknown (TZ: {}, DPID: {}, TEP IP: {}) in ITM Config DS.", tzName, dpnId, tepIpAddress);
        }


        // Update TZ in Config DS.
        ItmUtils.asyncUpdate(LogicalDatastoreType.CONFIGURATION, tZonepath, updatedTzone, dataBroker,
                ItmUtils.DEFAULT_CALLBACK);
    }

    public static IpPrefix getDummySubnet() {

        // set prefix to dummy-subnet
        String dummyPrefix = "255.255.255.255/32";
        IpPrefix subnetMaskObj = null;

        // Get subnet prefix
        try {
            subnetMaskObj = new IpPrefix(dummyPrefix.toCharArray());
        } catch (Exception e) {
            LOG.error("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32");
            return null;
        }
        return subnetMaskObj;
    }
}
