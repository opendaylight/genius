/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cache.DpnsTepsStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfoBuilder;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DirectTunnelUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelUtils.class);

    private final DataBroker dataBroker;
    private final TunnelStateCache tunnelStateCache;

    // Key by tunnel name. Value is source and destination DpnId
    private ConcurrentHashMap<String, TunnelEndPointInfo> tunnelEndpointMap = new ConcurrentHashMap();
    // Key is the tunnel Name. Value is NodeConnector Id and value
    private ConcurrentHashMap<String, NodeConnectorInfo> unprocessedNodeConnectorMap = new ConcurrentHashMap();

    @Inject
    public DirectTunnelUtils(DataBroker dataBroker, TunnelStateCache tunnelStateCache) {
        this.dataBroker = dataBroker;
        this.tunnelStateCache = tunnelStateCache;
    }

    public static BigInteger getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.getValue().replace(":", "");
            return new BigInteger(dpIdStr, 16);
        }
        return null;
    }

    public static BigInteger getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return new BigInteger(portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[1]);
    }

    public static long getPortNumberFromNodeConnectorId(NodeConnectorId portId) {
        String portNo = getPortNoFromNodeConnectorId(portId);
        try {
            return Long.parseLong(portNo);
        } catch (NumberFormatException ex) {
            LOG.trace("Unable to retrieve port number from nodeconnector id for {}", portId);
        }
        return ITMConstants.INVALID_PORT_NO;
    }

    private static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[2];
    }

    public long getNodeConnectorIdFromInterface(String interfaceName) {
        StateTunnelList stateTnl = TunnelUtils.getTunnelFromOperationalDS(interfaceName, dataBroker , tunnelStateCache);
        if (stateTnl != null) {
            return Long.parseLong(stateTnl.getPortNumber());
        }
        return ITMConstants.INVALID_PORT_NO;
    }

    public static NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
        return new NodeId(ncId.getValue().substring(0,ncId.getValue().lastIndexOf(":")));
    }

    public static String generateMacAddress(long portNo) {
        String unformattedMAC = getDeadBeefBytesForMac().or(fillPortNumberToMac(portNo)).toString(16);
        return unformattedMAC.replaceAll("(.{2})", "$1" + ITMConstants.MAC_SEPARATOR)
                .substring(0, ITMConstants.MAC_STRING_LENGTH);
    }

    public static BigInteger getDeadBeefBytesForMac() {
        return new BigInteger("FFFFFFFF", 16).and(new BigInteger(ITMConstants.DEAD_BEEF_MAC_PREFIX, 16)).shiftLeft(16);
    }

    public static BigInteger fillPortNumberToMac(long portNumber) {
        return new BigInteger("FFFF", 16).and(BigInteger.valueOf(portNumber));
    }

    public void removeTepFromDpnTepInterfaceConfigDS(BigInteger srcDpnId, DpnsTepsStateCache dpnsTepsStateCache) {
        Collection<DpnsTeps> dpnsTeps = dpnsTepsStateCache.getAllPresent();
        for (DpnsTeps dpnTep : dpnsTeps) {
            if (!dpnTep.getSourceDpnId().equals(srcDpnId)) {
                List<RemoteDpns> remoteDpns = dpnTep.getRemoteDpns();
                for (RemoteDpns remoteDpn : remoteDpns) {
                    if (remoteDpn.getDestinationDpnId().equals(srcDpnId)) {
                        // Remote the SrcDpnId from the remote List. Remove it from COnfig DS. 4
                        // This will be reflected in cache by the ClusteredDTCN. Not removing it here !
                        //Caution :- Batching Delete !!
                        InstanceIdentifier<RemoteDpns> remoteDpnII = buildRemoteDpnsInstanceIdentifier(
                                dpnTep.getSourceDpnId(), remoteDpn.getDestinationDpnId());
                        ITMBatchingUtils.delete(remoteDpnII, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
                        break;
                    }
                }
            } else {
                // The source DPn id is the one to be removed
                InstanceIdentifier<DpnsTeps> dpnsTepsII = buildDpnsTepsInstanceIdentifier(dpnTep.getSourceDpnId());
                ITMBatchingUtils.delete(dpnsTepsII, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
            }
        }
    }

    private InstanceIdentifier<DpnsTeps> buildDpnsTepsInstanceIdentifier(BigInteger srcDpnId) {
        DpnsTepsKey dpnsTepsKey = new DpnsTepsKey(srcDpnId);
        return InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class, dpnsTepsKey).build();
    }

    private InstanceIdentifier<RemoteDpns> buildRemoteDpnsInstanceIdentifier(BigInteger srcDpnId, BigInteger dstDpnId) {
        DpnsTepsKey dpnsTepsKey = new DpnsTepsKey(srcDpnId);
        RemoteDpnsKey remoteDpnsKey = new RemoteDpnsKey(dstDpnId);
        return InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class, dpnsTepsKey)
                .child(RemoteDpns.class, remoteDpnsKey).build();
    }

    //Start: TunnelEndPoint Cache
    public void addTunnelEndPointInfoToCache(String tunnelName, String srcEndPtInfo, String dstEndPtInfo) {
        TunnelEndPointInfo tunnelEndPointInfo = new TunnelEndPointInfoBuilder().setSrcEndPointInfo(srcEndPtInfo)
                .setDstEndPointInfo(dstEndPtInfo).build();
        tunnelEndpointMap.put(tunnelName, tunnelEndPointInfo);
    }

    public void removeFromTunnelEndPointInfoCache(String tunnelName) {
        tunnelEndpointMap.remove(tunnelName);
    }

    public TunnelEndPointInfo getTunnelEndPointInfoFromCache(String tunnelName) {
        return tunnelEndpointMap.get(tunnelName);
    }

    //Start: Unprocessed Node Connector Cache
    public void addNodeConnectorInfoToCache(String tunnelName, NodeConnectorInfo ncInfo) {
        unprocessedNodeConnectorMap.put(tunnelName, ncInfo);
    }

    public NodeConnectorInfo getUnprocessedNodeConnector(String tunnelName) {
        return unprocessedNodeConnectorMap.get(tunnelName);
    }

    public void removeNodeConnectorInfoFromCache(String tunnelName) {
        unprocessedNodeConnectorMap.remove(tunnelName);
    }

    // Convert Interface Oper State to Tunnel Oper state
    public TunnelOperStatus convertInterfaceToTunnelOperState(Interface.OperStatus opState) {
        TunnelOperStatus tunnelOperStatus;
        switch (opState) {
            case Up:
                tunnelOperStatus = TunnelOperStatus.Up;
                break;
            case Down:
                tunnelOperStatus = TunnelOperStatus.Down;
                break;
            case Unknown:
                tunnelOperStatus = TunnelOperStatus.Unknown;
                break;
            default:
                tunnelOperStatus = TunnelOperStatus.Ignore;
        }
        return tunnelOperStatus;
    }

    // Given the tunnel name find out if its internal or external
    public boolean isInternal(DpnsTepsStateCache dpnTepStateCache, String tunnelName) {
        TunnelEndPointInfo endPointInfo = getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache
                    .getDpnTepInterfaceInfo(endPointInfo.getSrcEndPointInfo(), endPointInfo.getDstEndPointInfo());
            return dpnTepInfo != null && dpnTepInfo.isInternal();
        }
        return false;
    }

    public boolean isConfigAvailable(DpnsTepsStateCache dpnTepStateCache, String tunnelName) {
        TunnelEndPointInfo endPointInfo = getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache
                    .getDpnTepInterfaceInfo(endPointInfo.getSrcEndPointInfo(), endPointInfo.getDstEndPointInfo());
            return dpnTepInfo != null;
        }
        return false;
    }

    public boolean isInternalBasedOnState(String tunnelName) {
        StateTunnelList stateTunnelList =
                TunnelUtils.getTunnelFromOperationalDS(tunnelName, dataBroker, tunnelStateCache);
        return stateTunnelList != null && TepTypeInternal.class.equals(stateTunnelList.getDstInfo().getTepDeviceType());
    }

    // Given the source and destination DPN id get the tunnel name
    public String getInterfaceNameFromDPNIds(BigInteger srcDpnId, BigInteger dstDpnId,
                                             DpnsTepsStateCache dpnsTepsStateCache) {
        DpnTepInterfaceInfo dpnTepInterfaceInfo =
                dpnsTepsStateCache.getDpnTepInterfaceInfo(srcDpnId.toString(), dstDpnId.toString());
        if (dpnTepInterfaceInfo != null) {
            return dpnTepInterfaceInfo.getTunnelName();
        } else {
            return getInterfaceNameFromDS(srcDpnId, dstDpnId);// Fetch it from DS
        }
    }

    private String getInterfaceNameFromDS(BigInteger srcDpnId, BigInteger dstDpnId) {
        InstanceIdentifier<DpnsTeps> dpnsTepsII = buildDpnsTepsInstanceIdentifier(srcDpnId);
        Optional<DpnsTeps> dpnsTeps = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, dpnsTepsII, dataBroker);
        if (dpnsTeps.isPresent()) {
            List<RemoteDpns> remoteDpns = dpnsTeps.get().getRemoteDpns();
            if (remoteDpns != null) {
                for (RemoteDpns remoteDpn : remoteDpns) {
                    if (remoteDpn.getDestinationDpnId().equals(dstDpnId)) {
                        return remoteDpn.getTunnelName();// Get tunnel Name
                    }
                }
                LOG.debug("Destination DPN supplied for getInterfaceName is not valid {}", dstDpnId);
            }
        } else {
            LOG.debug("Source DPN supplied for getInterfaceName is not valid {}", srcDpnId);
        }
        return null;
    }

    public List<Action> getEgressActionsForInterface(String interfaceName, Long tunnelKey, Integer actionKey,
                                                     Boolean isDefaultEgress, DpnsTepsStateCache dpnsTepsStateCache) {
        List<ActionInfo> listActionInfo = getEgressActionInfosForInterface(interfaceName, tunnelKey,
                actionKey == null ? 0 : actionKey, isDefaultEgress, dpnsTepsStateCache);
        List<Action> actionsList = new ArrayList<>();
        listActionInfo.forEach(actionInfo -> actionsList.add(actionInfo.buildAction()));
        return actionsList;
    }

    private List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, Long tunnelKey, int actionKeyStart,
                                                              Boolean isDefaultEgress,
                                                              DpnsTepsStateCache dpnsTepsStateCache) {
        TunnelEndPointInfo tunnelEndPointInfo = getTunnelEndPointInfoFromCache(interfaceName);
        DpnTepInterfaceInfo interfaceInfo =
                TunnelUtils.getTunnelFromConfigDS(interfaceName, dataBroker, dpnsTepsStateCache, tunnelEndPointInfo);
        if (interfaceInfo == null) {
            LOG.info("Interface information not present in config DS for " + interfaceName);
            return Collections.emptyList();
        }
        StateTunnelList ifState = TunnelUtils.getTunnelFromOperationalDS(interfaceName, dataBroker, tunnelStateCache);
        if (ifState == null) {
            LOG.info("Interface information not present in oper DS for " + interfaceName);
            return Collections.emptyList();
        }
        String tunnelType = ItmUtils.convertTunnelTypetoString(interfaceInfo.getTunnelType());
        return getEgressActionInfosForInterface(tunnelType, ifState.getPortNumber(), tunnelKey, actionKeyStart,
                isDefaultEgress, ifState.getIfIndex());
    }


    public static List<ActionInfo> getEgressActionInfosForInterface(String tunnelType,String portNo,
                                                                    Long tunnelKey, int actionKeyStart,
                                                                    boolean isDefaultEgress, int ifIndex) {
        List<ActionInfo> result = new ArrayList<>();
        switch (tunnelType) {
            case ITMConstants.TUNNEL_TYPE_GRE:
            case ITMConstants.TUNNEL_TYPE_MPLSoGRE:
                // Invoke IFM RPC and pass it on to the caller.
                LOG.warn("Interface Type {} not handled by ITM", tunnelType);
                break;
            case ITMConstants.TUNNEL_TYPE_VXLAN:
                //TODO tunnel_id to encode GRE key, once it is supported
                // Until then, tunnel_id should be "cleaned", otherwise it stores the value coming from a VXLAN tunnel
                if (tunnelKey == null) {
                    tunnelKey = 0L;
                }
                result.add(new ActionSetFieldTunnelId(actionKeyStart++ , BigInteger.valueOf(tunnelKey)));
                result.add(new ActionOutput(actionKeyStart, new Uri(portNo)));
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", tunnelType);
                break;
        }
        return result;
    }
}