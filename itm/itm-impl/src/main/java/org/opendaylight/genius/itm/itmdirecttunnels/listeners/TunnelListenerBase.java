/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.IfIndexesTunnelMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.DstInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.SrcInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelListenerBase<T extends DataObject> extends AbstractClusteredSyncDataTreeChangeListener<T> {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelListenerBase.class);

    private final DpnTepStateCache dpnTepStateCache;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final UnprocessedNodeConnectorCache unprocessedNCCache;
    private final DirectTunnelUtils directTunnelUtils;

    public TunnelListenerBase(final DataBroker dataBroker,
                              final LogicalDatastoreType logicalDatastoreType,
                              final InstanceIdentifier<T> instanceIdentifier,
                              final IdManagerService idManager,
                              final IMdsalApiManager mdsalApiManager,
                              final DpnTepStateCache dpnTepStateCache,
                              final DPNTEPsInfoCache dpntePsInfoCache,
                              final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache,
                              final DirectTunnelUtils directTunnelUtils) {
        super(dataBroker, logicalDatastoreType , instanceIdentifier);
        this.dpnTepStateCache = dpnTepStateCache;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.mdsalApiManager = mdsalApiManager;
        this.unprocessedNCCache = unprocessedNodeConnectorCache;
        this.idManager = idManager;
        this.directTunnelUtils = directTunnelUtils;
    }

    public List<ListenableFuture<Void>> addState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                 String interfaceName, FlowCapableNodeConnector fcNodeConnectorNew)
            throws ExecutionException, InterruptedException, OperationFailedException {
        boolean unableToProcess = false;
        //Retrieve Port No from nodeConnectorId
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == ITMConstants.INVALID_PORT_NO) {
            LOG.trace("Cannot derive port number, not proceeding with Interface State "
                    + "addition for interface: {}", interfaceName);
            return null;
        }

        LOG.info("adding interface state to Oper DS for interface: {}", interfaceName);

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface/Tunnel from config DS if exists
        TunnelEndPointInfo tunnelEndPointInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(interfaceName);

        if (tunnelEndPointInfo != null) {
            DpnTepInterfaceInfo dpnTepConfigInfo = dpnTepStateCache.getDpnTepInterface(
                    new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                    new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
            if (dpnTepConfigInfo != null) {
                StateTunnelList stateTnl = addStateEntry(tunnelEndPointInfo, interfaceName,
                        operStatus, adminStatus, nodeConnectorId);

                // SF419 This will be only tunnel If so not required
                // If this interface is a tunnel interface, create the tunnel ingress flow,
                // and start tunnel monitoring
                if (stateTnl != null) {
                    handleTunnelMonitoringAddition(nodeConnectorId, stateTnl.getIfIndex(), dpnTepConfigInfo,
                            interfaceName, portNo);
                    // Remove the NodeConnector Entry from Unprocessed Map
                }
            } else {
                LOG.error("DpnTepINfo is NULL while addState for interface {} ", interfaceName);
                unableToProcess = true;
            }
        } else {
            LOG.error("TunnelEndPointInfo is NULL while addState for interface {} ", interfaceName);
            unableToProcess = true;
        }
        if (unableToProcess) {
            LOG.debug("Unable to process the NodeConnector ADD event for {} as Config not available."
                    + "Hence parking it", interfaceName);
            NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfoBuilder().setNodeConnectorId(key)
                    .setNodeConnector(fcNodeConnectorNew).build();
            unprocessedNCCache.add(interfaceName, nodeConnectorInfo);
        }
        return Collections.emptyList();
    }

    private StateTunnelList addStateEntry(TunnelEndPointInfo tunnelEndPointInfo, String interfaceName,
                                          Interface.OperStatus operStatus, Interface.AdminStatus adminStatus,
                                          NodeConnectorId nodeConnectorId)
            throws ExecutionException, InterruptedException, OperationFailedException {
        LOG.debug("Start addStateEntry adding interface state for {}", interfaceName);
        final StateTunnelListBuilder stlBuilder = new StateTunnelListBuilder();
        Class<? extends TunnelTypeBase> tunnelType;

        //Retrieve Port No from nodeConnectorId
        final long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getDpnTepInterface(
                new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
        LOG.debug("Source Dpn TEP Interface Info {}" , dpnTepInfo);
        tunnelType = dpnTepInfo.getTunnelType();

        final SrcInfoBuilder srcInfoBuilder =
                new SrcInfoBuilder().setTepDeviceId(tunnelEndPointInfo.getSrcEndPointInfo());
        final DstInfoBuilder dstInfoBuilder =
                new DstInfoBuilder().setTepDeviceId(tunnelEndPointInfo.getDstEndPointInfo());

        /*DPNTEPsInfo srcDpnTePsInfo = (DPNTEPsInfo) DataStoreCache.get(ITMConstants.DPN_TEPs_Info_CACHE_NAME,
            new BigInteger(tunnelEndPointInfo.getSrcDpnId()));*/

        java.util.Optional<DPNTEPsInfo> srcDpnTepsInfo = dpntePsInfoCache
                .getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()));

        LOG.debug("Source Dpn TEP Info {}", srcDpnTepsInfo);
        TunnelEndPoints srcEndPtInfo = srcDpnTepsInfo.get().getTunnelEndPoints().get(0);
        srcInfoBuilder.setTepIp(srcEndPtInfo.getIpAddress());
        //As ITM Direct Tunnels deals with only Internal Tunnels.
        //Relook at this when it deals with external as well
        srcInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        java.util.Optional<DPNTEPsInfo> dstDpnTePsInfo = dpntePsInfoCache
                .getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));

        LOG.debug("Dest Dpn TEP Info {}", dstDpnTePsInfo);
        TunnelEndPoints dstEndPtInfo = dstDpnTePsInfo.get().getTunnelEndPoints().get(0);
        dstInfoBuilder.setTepIp(dstEndPtInfo.getIpAddress());
        //As ITM Direct Tunnels deals with only Internal Tunnels.
        //Relook at this when it deals with external as well
        dstInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        // ITM Direct Tunnels NOT SETTING THE TEP TYPe coz its not available. CHECK IF REQUIRED
        TunnelOperStatus tunnelOperStatus = DirectTunnelUtils.convertInterfaceToTunnelOperState(operStatus);
        boolean tunnelState = operStatus.equals(Interface.OperStatus.Up);

        StateTunnelListKey tlKey = new StateTunnelListKey(interfaceName);
        stlBuilder.setKey(tlKey)
                .setOperState(tunnelOperStatus).setTunnelState(tunnelState)
                .setDstInfo(dstInfoBuilder.build()).setSrcInfo(srcInfoBuilder.build()).setTransportType(tunnelType)
                .setPortNumber(String.valueOf(portNo));
        // ITM DIRECT TUnnels CHECK ifIndex is required ??
        int ifIndex;
        ifIndex = directTunnelUtils.allocateId(ITMConstants.ITM_IDPOOL_NAME, interfaceName);
        createLportTagInterfaceMap(interfaceName, ifIndex);
        stlBuilder.setIfIndex(ifIndex);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", stlBuilder.build(), stListId);
        ITMBatchingUtils.write(stListId, stlBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        return stlBuilder.build();
    }

    private void handleTunnelMonitoringAddition(NodeConnectorId nodeConnectorId,
                                                Integer ifindex, DpnTepInterfaceInfo dpnTepConfigInfo,
                                                String interfaceName, long portNo) {
        BigInteger dpId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        directTunnelUtils.makeTunnelIngressFlow(dpnTepConfigInfo, dpId, portNo, interfaceName,
                ifindex, NwConstants.ADD_FLOW);
    }

    private void createLportTagInterfaceMap(String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        IfIndexTunnel ifIndexInterface = new IfIndexTunnelBuilder().setIfIndex(ifIndex)
                .setKey(new IfIndexTunnelKey(ifIndex)).setInterfaceName(infName).build();
        ITMBatchingUtils.write(id, ifIndexInterface, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }
}
