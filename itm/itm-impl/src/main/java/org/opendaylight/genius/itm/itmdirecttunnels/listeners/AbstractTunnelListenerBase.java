/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorCache;
import org.opendaylight.genius.itm.cache.UnprocessedNodeConnectorEndPointCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
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

abstract class AbstractTunnelListenerBase<T extends DataObject> extends AbstractClusteredSyncDataTreeChangeListener<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTunnelListenerBase.class);

    protected final DpnTepStateCache dpnTepStateCache;
    protected final DPNTEPsInfoCache dpntePsInfoCache;
    protected final UnprocessedNodeConnectorCache unprocessedNCCache;
    protected final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache;
    protected final DirectTunnelUtils directTunnelUtils;
    protected final ManagedNewTransactionRunner txRunner;

    private final EntityOwnershipUtils entityOwnershipUtils;

    AbstractTunnelListenerBase(final DataBroker dataBroker,
                               final LogicalDatastoreType logicalDatastoreType,
                               final InstanceIdentifier<T> instanceIdentifier,
                               final DpnTepStateCache dpnTepStateCache,
                               final DPNTEPsInfoCache dpntePsInfoCache,
                               final UnprocessedNodeConnectorCache unprocessedNodeConnectorCache,
                               final UnprocessedNodeConnectorEndPointCache unprocessedNodeConnectorEndPointCache,
                               final EntityOwnershipUtils entityOwnershipUtils,
                               final DirectTunnelUtils directTunnelUtils) {
        super(dataBroker, logicalDatastoreType, instanceIdentifier);
        this.dpnTepStateCache = dpnTepStateCache;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.unprocessedNCCache = unprocessedNodeConnectorCache;
        this.unprocessedNodeConnectorEndPointCache = unprocessedNodeConnectorEndPointCache;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.directTunnelUtils = directTunnelUtils;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    public boolean entityOwner() {
        return entityOwnershipUtils.isEntityOwner(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY);
    }

    public List<ListenableFuture<Void>> addState(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                 String interfaceName, FlowCapableNodeConnector fcNodeConnectorNew)
            throws ExecutionException, InterruptedException, OperationFailedException {
        NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfoBuilder().setNodeConnectorId(key)
                .setNodeConnector(fcNodeConnectorNew).build();
        // Retrieve Port No from nodeConnectorId
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == ITMConstants.INVALID_PORT_NO) {
            LOG.error("Cannot derive port number, not proceeding with Interface State "
                    + "addition for interface: {}", interfaceName);
            return Collections.emptyList();
        }

        LOG.info("adding interface state to Oper DS for interface: {}", interfaceName);

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        TunnelEndPointInfo tunnelEndPointInfo;
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        // Fetch the interface/Tunnel from config DS if exists
        // If it doesnt exists then "park" the processing and comeback to it when the data is available and
        // this will be triggered by the corres. listener. Caching and de-caching has to be synchronized.
        DpnTepInterfaceInfo dpnTepConfigInfo = null;
        try {
            directTunnelUtils.getTunnelLocks().lock(interfaceName);
            tunnelEndPointInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(interfaceName);
            if (tunnelEndPointInfo != null) {
                BigInteger srcDpnId = new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo());
                BigInteger dpnId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
                if (srcDpnId.compareTo(dpnId) != 0) {
                    //This is a preventive measure to check if the node connector is coming from the switch
                    // to which the tunnel was pushed. If it came from wrong switch due to duplicate tunnel
                    // then drop the node connector event.
                    LOG.error("The Source DPN ID {} from ITM Config does not match with the DPN ID {},"
                                    + "fetched from NodeConnector Add event. Returning here.",
                            srcDpnId, dpnId);
                    return Collections.emptyList();
                }
                dpnTepConfigInfo = dpnTepStateCache.getDpnTepInterface(
                        new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                        new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
            }
            if (tunnelEndPointInfo == null || dpnTepConfigInfo == null) {
                LOG.info("Unable to process the NodeConnector ADD event for {} as Config not available."
                        + "Hence parking it", interfaceName);
                unprocessedNCCache.add(interfaceName, nodeConnectorInfo);
                return Collections.emptyList();
            }
        } finally {
            directTunnelUtils.getTunnelLocks().unlock(interfaceName);
        }
        StateTunnelList stateTnl = addStateEntry(tunnelEndPointInfo, interfaceName,
                operStatus, adminStatus, nodeConnectorInfo);

        // This will be only tunnel If so not required
        // If this interface is a tunnel interface, create the tunnel ingress flow,
        // and start tunnel monitoring
        if (stateTnl != null) {
            int finalGroupId = dpnTepConfigInfo.getGroupId();
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> handleTunnelMonitoringAddition(tx, nodeConnectorId, stateTnl.getIfIndex(),
                        finalGroupId, interfaceName, portNo)));
        }
        return futures;
    }

    private StateTunnelList addStateEntry(TunnelEndPointInfo tunnelEndPointInfo, String interfaceName,
                                          Interface.OperStatus operStatus, Interface.AdminStatus adminStatus,
                                          NodeConnectorInfo nodeConnectorInfo)
            throws ExecutionException, InterruptedException, OperationFailedException {
        LOG.debug("Start addStateEntry adding interface state for {}", interfaceName);
        final StateTunnelListBuilder stlBuilder = new StateTunnelListBuilder();
        Class<? extends TunnelTypeBase> tunnelType;
        java.util.Optional<DPNTEPsInfo> srcDpnTepsInfo;
        java.util.Optional<DPNTEPsInfo> dstDpnTePsInfo;

        // Retrieve Port No from nodeConnectorId
        InstanceIdentifier<FlowCapableNodeConnector> key = nodeConnectorInfo.getNodeConnectorId();
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();

        final long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        DpnTepInterfaceInfo dpnTepInfo = dpnTepStateCache.getDpnTepInterface(
                new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()),
                new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
        LOG.debug("Source Dpn TEP Interface Info {}", dpnTepInfo);
        tunnelType = dpnTepInfo.getTunnelType();

        try {
            directTunnelUtils.getTunnelLocks().lock(tunnelEndPointInfo.getSrcEndPointInfo());
            srcDpnTepsInfo = dpntePsInfoCache
                    .getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo.getSrcEndPointInfo()));
            if (!srcDpnTepsInfo.isPresent()) {
                LOG.info("Unable to add State for tunnel {}. Hence Parking with key {}",
                        interfaceName, tunnelEndPointInfo.getSrcEndPointInfo());
                unprocessedNodeConnectorEndPointCache.add(tunnelEndPointInfo.getSrcEndPointInfo(), nodeConnectorInfo);
            }
        } finally {
            directTunnelUtils.getTunnelLocks().unlock(tunnelEndPointInfo.getSrcEndPointInfo());
        }

        try {
            directTunnelUtils.getTunnelLocks().lock(tunnelEndPointInfo.getDstEndPointInfo());
            dstDpnTePsInfo = dpntePsInfoCache
                    .getDPNTepFromDPNId(new BigInteger(tunnelEndPointInfo.getDstEndPointInfo()));
            if (!dstDpnTePsInfo.isPresent()) {
                LOG.info("Unable to add State for tunnel {}. Hence Parking with key {}",
                        interfaceName, tunnelEndPointInfo.getDstEndPointInfo());
                unprocessedNodeConnectorEndPointCache.add(tunnelEndPointInfo.getDstEndPointInfo(), nodeConnectorInfo);
            }
        } finally {
            directTunnelUtils.getTunnelLocks().unlock(tunnelEndPointInfo.getDstEndPointInfo());
        }

        if (!(srcDpnTepsInfo.isPresent() && dstDpnTePsInfo.isPresent())) {
            return null;
        }
        // Now do the entity owner check as all data to process the event is available
        if (!entityOwner()) {
            return null;
        }

        final SrcInfoBuilder srcInfoBuilder =
                new SrcInfoBuilder().setTepDeviceId(tunnelEndPointInfo.getSrcEndPointInfo());
        final DstInfoBuilder dstInfoBuilder =
                new DstInfoBuilder().setTepDeviceId(tunnelEndPointInfo.getDstEndPointInfo());
        LOG.debug("Source Dpn TEP Info {}", srcDpnTepsInfo);
        TunnelEndPoints srcEndPtInfo = srcDpnTepsInfo.get().getTunnelEndPoints().get(0);
        srcInfoBuilder.setTepIp(srcEndPtInfo.getIpAddress());
        // As ITM Direct Tunnels deals with only Internal Tunnels.
        // Relook at this when it deals with external as well
        srcInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        LOG.debug("Dest Dpn TEP Info {}", dstDpnTePsInfo);
        TunnelEndPoints dstEndPtInfo = dstDpnTePsInfo.get().getTunnelEndPoints().get(0);
        dstInfoBuilder.setTepIp(dstEndPtInfo.getIpAddress());
        // As ITM Direct Tunnels deals with only Internal Tunnels.
        // Relook at this when it deals with external as well
        dstInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        // ITM Direct Tunnels NOT SETTING THE TEP TYPe coz its not available. CHECK IF REQUIRED
        TunnelOperStatus tunnelOperStatus = DirectTunnelUtils.convertInterfaceToTunnelOperState(operStatus);
        boolean tunnelState = operStatus.equals(Interface.OperStatus.Up);

        StateTunnelListKey tlKey = new StateTunnelListKey(interfaceName);
        stlBuilder.withKey(tlKey)
                .setOperState(tunnelOperStatus).setTunnelState(tunnelState)
                .setDstInfo(dstInfoBuilder.build()).setSrcInfo(srcInfoBuilder.build()).setTransportType(tunnelType)
                .setPortNumber(String.valueOf(portNo));
        int ifIndex;
        ifIndex = directTunnelUtils.allocateId(ITMConstants.ITM_IDPOOL_NAME, interfaceName);
        createLportTagInterfaceMap(interfaceName, ifIndex);
        stlBuilder.setIfIndex(ifIndex);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", stlBuilder.build(), stListId);
        ITMBatchingUtils.write(stListId, stlBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        return stlBuilder.build();
    }

    private void handleTunnelMonitoringAddition(TypedWriteTransaction<Datastore.Configuration> tx,
        NodeConnectorId nodeConnectorId, Integer ifindex, Integer groupId, String interfaceName, long portNo) {
        BigInteger dpId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        directTunnelUtils.addTunnelIngressFlow(tx, dpId, portNo, interfaceName,
                ifindex);
        directTunnelUtils.addTunnelEgressFlow(tx, dpId, String.valueOf(portNo), groupId, interfaceName);
    }

    private void createLportTagInterfaceMap(String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}", infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        IfIndexTunnel ifIndexInterface = new IfIndexTunnelBuilder().setIfIndex(ifIndex)
                .withKey(new IfIndexTunnelKey(ifIndex)).setInterfaceName(infName).build();
        ITMBatchingUtils.write(id, ifIndexInterface, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    protected class InterfaceStateAddWorkerForUnprocessedNC implements Callable<List<ListenableFuture<Void>>> {
        private final InstanceIdentifier<FlowCapableNodeConnector> key;
        private final FlowCapableNodeConnector fcNodeConnectorNew;
        private final String interfaceName;

        InterfaceStateAddWorkerForUnprocessedNC(InstanceIdentifier<FlowCapableNodeConnector> key,
                                                FlowCapableNodeConnector fcNodeConnectorNew, String portName) {
            this.key = key;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : OVS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return addState(key, interfaceName, fcNodeConnectorNew);
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorkerForUnprocessedNC{"
                    + "fcNodeConnectorIdentifier=" + key
                    + ", fcNodeConnectorNew=" + fcNodeConnectorNew
                    + ", interfaceName='" + interfaceName + '\''
                    + '}';
        }
    }
}