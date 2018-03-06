/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DirectTunnelUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTunnelUtils.class);

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;

    // Key by tunnel name. Value is source and destination DpnId
    private final ConcurrentMap<String, TunnelEndPointInfo> tunnelEndpointMap = new ConcurrentHashMap();
    // Key is the tunnel Name. Value is NodeConnector Id and value
    private final ConcurrentMap<String, NodeConnectorInfo> unprocessedNodeConnectorMap = new ConcurrentHashMap();
    // Key is the DpnId for DpnTepsInfo Timing Issue.Value is List of NodeConnector Ids
    private final ConcurrentMap<String, List<NodeConnectorInfo>> unProcessedNodeConnectorEndPtMap =
            new ConcurrentHashMap();
    private final ConcurrentMap<String, Interface.OperStatus> bfdStateMap = new ConcurrentHashMap<>();


    @Inject
    public DirectTunnelUtils(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    public BigInteger getDpnId(DatapathId datapathId) {
        if (datapathId != null) {
            String dpIdStr = datapathId.getValue().replace(":", "");
            return new BigInteger(dpIdStr, 16);
        }
        return null;
    }

    public BigInteger getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return new BigInteger(portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[1]);
    }

    public long getPortNumberFromNodeConnectorId(NodeConnectorId portId) {
        String portNo = getPortNoFromNodeConnectorId(portId);
        try {
            return Long.parseLong(portNo);
        } catch (NumberFormatException ex) {
            LOG.error("Unable to retrieve port number from nodeconnector id for {}", portId);
            return ITMConstants.INVALID_PORT_NO;
        }
    }

    private String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[2];
    }

    public NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
        return new NodeId(ncId.getValue().substring(0,ncId.getValue().lastIndexOf(":")));
    }

    public String generateMacAddress(long portNo) {
        String unformattedMAC = getDeadBeefBytesForMac().or(fillPortNumberToMac(portNo)).toString(16);
        return unformattedMAC.replaceAll("(.{2})", "$1" + ITMConstants.MAC_SEPARATOR)
                .substring(0, ITMConstants.MAC_STRING_LENGTH);
    }

    private BigInteger getDeadBeefBytesForMac() {
        return new BigInteger("FFFFFFFF", 16).and(new BigInteger(ITMConstants.DEAD_BEEF_MAC_PREFIX, 16)).shiftLeft(16);
    }

    private BigInteger fillPortNumberToMac(long portNumber) {
        return new BigInteger("FFFF", 16).and(BigInteger.valueOf(portNumber));
    }

    //Start: Unprocessed Node Connector EndPt Cache
    public void addNodeConnectorEndPtInfoToCache(String dpnId, List<NodeConnectorInfo> ncList) {
        unProcessedNodeConnectorEndPtMap.put(dpnId, ncList);
    }

    public void addNodeConnectorEndPtInfoToCache(String dpnId, NodeConnectorInfo ncInfo) {
        List<NodeConnectorInfo> ncList = getUnprocessedNodeConnectorEndPt(dpnId);
        if (ncList == null) {
            ncList = new ArrayList<NodeConnectorInfo>();
        }
        ncList.add(ncInfo);
        unProcessedNodeConnectorEndPtMap.put(dpnId, ncList);
    }

    public List<NodeConnectorInfo> getUnprocessedNodeConnectorEndPt(String dpnId) {
        return unProcessedNodeConnectorEndPtMap.get(dpnId);
    }

    public void removeNodeConnectorEndPtInfoFromCache(String dpnId) {
        unProcessedNodeConnectorEndPtMap.remove(dpnId);
    }

    public void removeNodeConnectorEndPtInfoFromCache(String dpnId, NodeConnectorInfo ncInfo) {
        List<NodeConnectorInfo> ncList = getUnprocessedNodeConnectorEndPt(dpnId);
        if (ncList != null) {
            ncList.remove(ncInfo);
            unProcessedNodeConnectorEndPtMap.put(dpnId, ncList);
        } else {
            LOG.error(" NodeConnectorInfo List for DPN Id {} is null", dpnId);
        }
    }

    //Start: TunnelEndPoint Cache accessors
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

    //Start: Unprocessed Node Connector Cache accessors
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

        java.util.Optional<TunnelOperStatus> tunnelOperStatus = TunnelOperStatus.forName(opState.getName());
        if (tunnelOperStatus.isPresent()) {
            return tunnelOperStatus.get();
        }
        return TunnelOperStatus.Ignore;
    }

    public void createOvsBridgeRefEntry(BigInteger dpnId, InstanceIdentifier<?> bridgeIid) {
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}", dpnId, bridgeIid);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId = getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        OvsBridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder = new OvsBridgeRefEntryBuilder().setKey(bridgeRefEntryKey)
                .setDpid(dpnId).setOvsBridgeReference(new OvsdbBridgeRef(bridgeIid));
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx
            -> tx.put(LogicalDatastoreType.OPERATIONAL, bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(), true));
    }

    public void deleteOvsBridgeRefEntry(BigInteger dpnId) {
        LOG.debug("Deleting bridge ref entry for dpn: {}", dpnId);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId = getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx
            -> tx.delete(LogicalDatastoreType.OPERATIONAL, bridgeEntryId));
    }

    private InstanceIdentifier<OvsBridgeRefEntry>
        getOvsBridgeRefEntryIdentifier(OvsBridgeRefEntryKey bridgeRefEntryKey) {
        return InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                .child(OvsBridgeRefEntry.class, bridgeRefEntryKey).build();
    }

    public boolean isNodeConnectorPresent(NodeConnectorId nodeConnectorId) {
        NodeId nodeId = getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();
        return ItmUtils.read(LogicalDatastoreType.OPERATIONAL, ncIdentifier, dataBroker).isPresent();
    }

    public boolean isNodePresent(NodeConnectorId nodeConnectorId) {
        NodeId nodeID = getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,
                new NodeKey(nodeID)).build();
        Optional<Node> node = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier, dataBroker);
        return node.isPresent();
    }

    public void addBfdStateToCache(String interfaceName, Interface.OperStatus operStatus) {
        bfdStateMap.put(interfaceName, operStatus);
    }

    public Interface.OperStatus removeBfdStateFromCache(String interfaceName) {
        return bfdStateMap.remove(interfaceName);
    }

    public Interface.OperStatus getBfdStateFromCache(String interfaceName) {
        return bfdStateMap.get(interfaceName);
    }
}