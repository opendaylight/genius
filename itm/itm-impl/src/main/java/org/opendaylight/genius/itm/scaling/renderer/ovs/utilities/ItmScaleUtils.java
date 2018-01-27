/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.InterfaceStateAddWorkerForUnprocessedNC;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
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

/* This is IfmUtils equivalent */
public class ItmScaleUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ItmScaleUtils.class);
    // Keyed by sourceDpnId
    private static ConcurrentHashMap<BigInteger, DpnsTeps> DpnsTepsMap = new ConcurrentHashMap();
    // Keyed by String - sourceDpnId:DestinationDpnId
    private static ConcurrentHashMap<String, DpnTepInterfaceInfo> DpnTepInterfaceMap = new ConcurrentHashMap();
    // Key by tunnel name. Value is source and destination DpnId
    private static ConcurrentHashMap<String, TunnelEndPointInfo> TunnelEndPointMap = new ConcurrentHashMap();

    // Key is the tunnel Name. Value is NodeConnector Id and value
    private static ConcurrentHashMap<String, NodeConnectorInfo> UnprocessedNodeConnectorMap = new ConcurrentHashMap();

    public static BigInteger getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.getValue().replace(":", "");
            BigInteger dpnId =  new BigInteger(dpIdStr, 16);
            return dpnId;
        }
        return null;
    }

    public static BigInteger getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(ITMConstants.OF_URI_SEPARATOR);
        return new BigInteger(split[1]);
    }

    public static long getPortNumberFromNodeConnectorId(NodeConnectorId portId) {
        String portNo = getPortNoFromNodeConnectorId(portId);
        try {
            return Long.valueOf(portNo);
        } catch (NumberFormatException ex) {
            LOG.trace("Unable to retrieve port number from nodeconnector id for {}", portId);
        }
        return ITMConstants.INVALID_PORT_NO;
    }

    public static long getNodeConnectorIdFromInterface(String interfaceName, DataBroker databroker) {
        StateTunnelList stateTnl = TunnelUtils.getTunnelFromOperationalDS(interfaceName, databroker);
        if (stateTnl != null) {
            return Long.valueOf(stateTnl.getPortNumber());
        }
        return ITMConstants.INVALID_PORT_NO;
    }

    public static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(ITMConstants.OF_URI_SEPARATOR);
        return split[2];
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

    //Start: DpnsTeps Cache
    public static void addDpnsTepsToCache(BigInteger dpnId, DpnsTeps dpnsTeps) {
        DpnsTepsMap.put(dpnId, dpnsTeps);
    }

    public static void addDpnsTepsToCache(DpnsTeps dpnsTeps) {
        addDpnsTepsToCache(dpnsTeps.getKey().getSourceDpnId(), dpnsTeps);
    }

    public static void removeFromDpnsTepsCache(BigInteger dpnId) {
        DpnsTepsMap.remove(dpnId);
    }

    public static void removeFromDpnsTepsCache(DpnsTeps dpnsTeps) {
        removeFromDpnsTepsCache(dpnsTeps.getKey().getSourceDpnId());
    }

    public static DpnsTeps getDpnsTepsFromCache(BigInteger dpnId) {
        return DpnsTepsMap.get(dpnId);
    }

    //Start: DpnTepInfo Cache
    public static void addDpnTepInterfaceToCache(DpnsTeps dpnsTeps, DataBroker dataBroker,
                                                 IdManagerService idManager, IMdsalApiManager mdsalApiManager,
                                                 JobCoordinator coordinator)  {
        DpnTepInterfaceInfo dpnTepInfo = new DpnTepInterfaceInfo();
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            final String key  = getSrcDstDpnIdKey(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            DpnTepInterfaceInfo value = new DpnTepInterfaceInfo();
            value.setTunnelName(remoteDpns.getTunnelName());
            value.setGroupId(dpnsTeps.getGroupId());
            value.setMonitorEnabled(remoteDpns.isMonitorEnabled());
            value.setInternal(remoteDpns.isInternal());
            value.setTunnelType(dpnsTeps.getTunnelType());
            DpnTepInterfaceMap.put(key, value);
            addTunnelEndPointInfoToCache(remoteDpns.getTunnelName(),dpnsTeps.getSourceDpnId().toString(),
                                            remoteDpns.getDestinationDpnId().toString());
            //Process the unprocessed NodeConnector for the Tunnel, if present in the UnprocessedNodeConnectorCache
            // This may run in all node as its ClusteredDTCN but cache will be populated in only the Entit owner
            NodeConnectorInfo nodeConnectorInfo = ItmScaleUtils.getUnprocessedNodeConnector(remoteDpns.getTunnelName());
            if (nodeConnectorInfo != null) {
                LOG.debug("Processing the Unprocessed NodeConnector for Tunnel {}", remoteDpns.getTunnelName());

                // Queue the InterfaceStateAddWorker in DJC
                String portName = nodeConnectorInfo.getNodeConnector().getName();
                InterfaceStateAddWorkerForUnprocessedNC ifStateAddWorker =
                        new InterfaceStateAddWorkerForUnprocessedNC(dataBroker, idManager, mdsalApiManager,
                                nodeConnectorInfo.getNodeConnectorId(), nodeConnectorInfo.getNodeConnector(), portName);
                coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
                // Remove the NodeConnector Entry from Unprocessed Map -- Check if this is the best place to remove ?
                ItmScaleUtils.removeNodeConnectorInfoFromCache(remoteDpns.getTunnelName());
            }
        }
    }

    public static void removeFromDpnTepInterfaceCache(String srcDstDpnId) {
        DpnTepInterfaceMap.remove(srcDstDpnId);
    }

    public static void removeFromDpnTepInterfaceCache(DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            String key = getSrcDstDpnIdKey(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            removeFromDpnTepInterfaceCache(key);
            removeFromTunnelEndPointInfoCache(remoteDpns.getTunnelName());
        }
    }

    public static List<DpnsTeps> getAllDpnsTeps() {
        List<DpnsTeps> dpnsTeps = null;
        Collection<DpnsTeps> values = null ;
        values = DpnsTepsMap.values();
        if (values != null) {
            dpnsTeps = new ArrayList<>();
            for (DpnsTeps value : values) {
                dpnsTeps.add(value);
            }
        }
        return dpnsTeps;
    }

    public static void removeTepFromDpnTepInterfaceConfigDS(BigInteger srcDpnId) {
        List<DpnsTeps> dpnsTeps = getAllDpnsTeps();
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

    public static InstanceIdentifier<DpnsTeps> buildDpnsTepsInstanceIdentifier(BigInteger srcDpnId) {
        DpnsTepsKey dpnsTepsKey = new DpnsTepsKey(srcDpnId);
        return InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class, dpnsTepsKey).build();
    }

    public static InstanceIdentifier<RemoteDpns> buildRemoteDpnsInstanceIdentifier(BigInteger srcDpnId,
                                                                                   BigInteger dstDpnId) {
        DpnsTepsKey dpnsTepsKey = new DpnsTepsKey(srcDpnId);
        RemoteDpnsKey remoteDpnsKey = new RemoteDpnsKey(dstDpnId);
        return InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class, dpnsTepsKey)
                .child(RemoteDpns.class, remoteDpnsKey).build();
    }

    public static DpnTepInterfaceInfo getDpnTepInterfaceFromCache(BigInteger srcDpnId, BigInteger dstDpnId) {
        return DpnTepInterfaceMap.get(getSrcDstDpnIdKey(srcDpnId,dstDpnId));
    }

    public static String getSrcDstDpnIdKey(BigInteger srcDpnId, BigInteger dstDpnId) {
        return srcDpnId + ":" + dstDpnId;
    }

    //Start: TunnelEndPoint Cache
    public static void addTunnelEndPointInfoToCache(String tunnelName, String srcEndPtInfo, String dstEndPtInfo) {
        TunnelEndPointMap.put(tunnelName, new TunnelEndPointInfo(srcEndPtInfo, dstEndPtInfo));
    }

    public static void removeFromTunnelEndPointInfoCache(String tunnelName) {
        TunnelEndPointMap.remove(tunnelName);
    }

    public static TunnelEndPointInfo getTunnelEndPointInfoFromCache(String tunnelName) {
        return TunnelEndPointMap.get(tunnelName);
    }

    //Start: Unprocessed Node Connector Cache
    public static void addNodeConnectorInfoToCache(String tunnelName, NodeConnectorInfo ncInfo) {
        UnprocessedNodeConnectorMap.put(tunnelName, ncInfo);
    }

    public static NodeConnectorInfo getUnprocessedNodeConnector(String tunnelName) {
        return UnprocessedNodeConnectorMap.get(tunnelName);
    }

    public static void removeNodeConnectorInfoFromCache(String tunnelName) {
        UnprocessedNodeConnectorMap.remove(tunnelName);
    }

    // Convert Interface Oper State to Tunnel Oper state
    public static TunnelOperStatus convertInterfaceToTunnelOperState(Interface.OperStatus opState) {
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
    public static boolean isInternal(String tunnelName) {
        boolean isInternal = false;
        TunnelEndPointInfo endPointInfo = ItmScaleUtils.getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = ItmScaleUtils.getDpnTepInterfaceFromCache(
                    new BigInteger(endPointInfo.getSrcDpnId()), new BigInteger(endPointInfo.getDstDpnId()));
            if (dpnTepInfo != null) {
                isInternal = dpnTepInfo.isInternal();
            }
        }
        return isInternal;
    }

    public static boolean isConfigAvailable(String tunnelName) {
        boolean isConfigAvailable = false;
        TunnelEndPointInfo endPointInfo = ItmScaleUtils.getTunnelEndPointInfoFromCache(tunnelName);
        if (endPointInfo != null) {
            DpnTepInterfaceInfo dpnTepInfo = ItmScaleUtils
                    .getDpnTepInterfaceFromCache(new BigInteger(endPointInfo.getSrcDpnId()),
                            new BigInteger(endPointInfo.getDstDpnId()));
            if (dpnTepInfo != null) {
                isConfigAvailable = true;
            }
        }
        return isConfigAvailable;
    }

    public static boolean isInternalBasedOnState(String tunnelName, DataBroker dataBroker) {
        boolean isInternal  = false;
        StateTunnelList stateTunnelList = TunnelUtils.getTunnelFromOperationalDS(tunnelName, dataBroker);
        if (TepTypeInternal.class.equals(stateTunnelList.getDstInfo().getTepDeviceType())) {
            isInternal = true;
        }
        return isInternal;
    }

    public static String getInterfaceNameFromDS(BigInteger srcDpnId, BigInteger dstDpnId, DataBroker dataBroker) {
        InstanceIdentifier<DpnsTeps> dpnsTepsII = buildDpnsTepsInstanceIdentifier(srcDpnId);
        Optional<DpnsTeps> dpnsTeps = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, dpnsTepsII, dataBroker);
        if (dpnsTeps.isPresent()) {
            List<RemoteDpns> remoteDpns = dpnsTeps.get().getRemoteDpns();
            if (remoteDpns != null) {
                for (RemoteDpns remoteDpn : remoteDpns) {
                    if (remoteDpn.getDestinationDpnId().equals(dstDpnId)) {
                        // Get the tunnel Name
                        return remoteDpn.getTunnelName();
                    }
                }
                LOG.debug("Destination DPN supplied for getInterfaceName is not valid {}", dstDpnId);
            }
        } else {
            LOG.debug("Source DPN supplied for getInterfaceName is not valid {}", srcDpnId);
        }
        return null;
    }

    // Given the source and destination DPN id get the tunnel name
    public static String getInterfaceNameFromDPNIds(BigInteger srcDpnId, BigInteger dstDpnId, DataBroker dataBroker) {
        DpnTepInterfaceInfo dpnTepInterfaceInfo = ItmScaleUtils.getDpnTepInterfaceFromCache(srcDpnId,dstDpnId);
        if (dpnTepInterfaceInfo != null) {
            return dpnTepInterfaceInfo.getTunnelName();
        } else {
            // Fetch it from DS
            return getInterfaceNameFromDS(srcDpnId, dstDpnId, dataBroker);
        }
    }

    public static List<Action> getEgressActionsForInterface(String interfaceName, Long tunnelKey, Integer actionKey,
                                                            DataBroker dataBroker, Boolean isDefaultEgress) {
        List<ActionInfo> listActionInfo = getEgressActionInfosForInterface(interfaceName, tunnelKey,
                actionKey == null ? 0 : actionKey, dataBroker, isDefaultEgress);
        List<Action> actionsList = new ArrayList<>();
        for (ActionInfo actionInfo : listActionInfo) {
            actionsList.add(actionInfo.buildAction());
        }
        return actionsList;
    }

    public static List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, Long tunnelKey,
                                                                    int actionKeyStart, DataBroker dataBroker,
                                                                    Boolean isDefaultEgress) {
        DpnTepInterfaceInfo interfaceInfo = TunnelUtils.getTunnelFromConfigDS(interfaceName, dataBroker);
        if (interfaceInfo == null) {
            throw new NullPointerException("Interface information not present in config DS for " + interfaceName);
        }
        StateTunnelList ifState =
                TunnelUtils.getTunnelFromOperationalDS(interfaceName, dataBroker);
        if (ifState == null) {
            throw new NullPointerException("Interface information not present in oper DS for " + interfaceName);
        }
        String tunnelType = ItmUtils.convertTunnelTypetoString(interfaceInfo.getTunnelType());
        return getEgressActionInfosForInterface(tunnelType, ifState.getPortNumber(), tunnelKey,
                actionKeyStart, isDefaultEgress, ifState.getIfIndex());
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
                result.add(new ActionOutput(actionKeyStart++, new Uri(portNo)));
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", tunnelType);
                break;
        }
        return result;
    }
}