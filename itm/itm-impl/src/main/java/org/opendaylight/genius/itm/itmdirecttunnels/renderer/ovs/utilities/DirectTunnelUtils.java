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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionDrop;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
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
    private static final String ENTITY = "ovsBridgeRefEntryOperation";

    private final DataBroker dataBroker;
    private final JobCoordinator jobCoordinator;
    private final ManagedNewTransactionRunner txRunner;
    private final DpnTepStateCache dpnTepStateCache;
    private final TunnelStateCache tunnelStateCache;

    @Inject
    public DirectTunnelUtils(DataBroker dataBroker, JobCoordinator jobCoordinator, DpnTepStateCache dpnTepStateCache,
                             TunnelStateCache tunnelStateCache) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.jobCoordinator = jobCoordinator;
        this.dpnTepStateCache = dpnTepStateCache;
        this.tunnelStateCache = tunnelStateCache;
    }

    public BigInteger getDpnId(DatapathId datapathId) {
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
            LOG.error("Unable to retrieve port number from nodeconnector id for {} ", portId, ex);
            return ITMConstants.INVALID_PORT_NO;
        }
    }

    private static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        return portId.getValue().split(ITMConstants.OF_URI_SEPARATOR)[2];
    }

    public static NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
        return new NodeId(ncId.getValue().substring(0,ncId.getValue().lastIndexOf(":")));
    }

    public static String generateMacAddress(long portNo) {
        String unformattedMAC = getDeadBeefBytesForMac().or(fillPortNumberToMac(portNo)).toString(16);
        return unformattedMAC.replaceAll("(.{2})", "$1" + ITMConstants.MAC_SEPARATOR)
                .substring(0, ITMConstants.MAC_STRING_LENGTH);
    }

    private static BigInteger getDeadBeefBytesForMac() {
        return new BigInteger("FFFFFFFF", 16).and(new BigInteger(ITMConstants.DEAD_BEEF_MAC_PREFIX, 16)).shiftLeft(16);
    }

    private static BigInteger fillPortNumberToMac(long portNumber) {
        return new BigInteger("FFFF", 16).and(BigInteger.valueOf(portNumber));
    }

    // Convert Interface Oper State to Tunnel Oper state
    public static TunnelOperStatus convertInterfaceToTunnelOperState(Interface.OperStatus opState) {

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
        jobCoordinator.enqueueJob(ENTITY,
            () -> Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx
                -> tx.put(LogicalDatastoreType.OPERATIONAL, bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(),
                    true))));

    }

    public void deleteOvsBridgeRefEntry(BigInteger dpnId) {
        LOG.debug("Deleting bridge ref entry for dpn: {}", dpnId);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId = getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        jobCoordinator.enqueueJob(ENTITY,
            () -> Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx
                -> tx.delete(LogicalDatastoreType.OPERATIONAL, bridgeEntryId))));
    }

    private static InstanceIdentifier<OvsBridgeRefEntry>
        getOvsBridgeRefEntryIdentifier(OvsBridgeRefEntryKey bridgeRefEntryKey) {
        return InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                .child(OvsBridgeRefEntry.class, bridgeRefEntryKey).build();
    }

    public boolean isNodeConnectorPresent(NodeConnectorId nodeConnectorId) throws ReadFailedException {
        NodeId nodeId = getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();
        return SingleTransactionDataBroker
                .syncReadOptional(dataBroker, LogicalDatastoreType.OPERATIONAL, ncIdentifier).isPresent();
    }

    public boolean isNodePresent(NodeConnectorId nodeConnectorId) throws ReadFailedException {
        NodeId nodeID = getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,
                new NodeKey(nodeID)).build();
        return SingleTransactionDataBroker
                .syncReadOptional(dataBroker, LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier).isPresent();
    }

    public List<Action> getEgressActionsForInterface(String interfaceName, long tunnelKey, Integer actionKey) {
        List<ActionInfo> listActionInfo = getEgressActionInfosForInterface(interfaceName, tunnelKey,
                actionKey == null ? 0 : actionKey);
        return listActionInfo.stream().map(ActionInfo::buildAction).collect(Collectors.toList());
    }

    private List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, long tunnelKey,
                                                              int actionKeyStart) {
        DpnTepInterfaceInfo interfaceInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);
        if (interfaceInfo == null) {
            LOG.error("Interface information not present in config DS for {}", interfaceName);
            return Collections.singletonList(new ActionDrop());
        }
        Optional<StateTunnelList> ifState;
        try {
            ifState = tunnelStateCache.get(tunnelStateCache.getStateTunnelListIdentifier(interfaceName));
        } catch (ReadFailedException e) {
            LOG.error("Interface information not present in oper DS for {} ", interfaceName, e);
            return Collections.singletonList(new ActionDrop());
        }
        if (ifState.isPresent()) {
            String tunnelType = ItmUtils.convertTunnelTypetoString(interfaceInfo.getTunnelType());
            return getEgressActionInfosForInterface(tunnelType, ifState.get().getPortNumber(), tunnelKey,
                    actionKeyStart);
        }
        LOG.error("Interface information not present in oper DS for {}", interfaceName);
        return Collections.singletonList(new ActionDrop());
    }


    private static List<ActionInfo> getEgressActionInfosForInterface(String tunnelType, String portNo, Long tunnelKey,
                                                                    int actionKeyStart) {
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
                result.add(new ActionSetFieldTunnelId(actionKeyStart++,
                        BigInteger.valueOf(tunnelKey != null ? tunnelKey : 0L)));
                result.add(new ActionOutput(actionKeyStart, new Uri(portNo)));
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", tunnelType);
                break;
        }
        return result;
    }
}