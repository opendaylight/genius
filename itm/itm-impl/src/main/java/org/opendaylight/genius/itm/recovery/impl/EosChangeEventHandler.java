/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class EosChangeEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EosChangeEventHandler.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DataBroker dataBroker;
    private final TunnelStateCache tunnelStateCache;
    private final ManagedNewTransactionRunner txRunner;
    private final DirectTunnelUtils directTunnelUtils;

    @Inject
    public EosChangeEventHandler(DataBroker dataBroker, TunnelStateCache tunnelStateCache,
                                 DirectTunnelUtils directTunnelUtils) {
        LOG.info("registering EOS change handlers");
        this.tunnelStateCache = tunnelStateCache;
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.directTunnelUtils = directTunnelUtils;
    }

    public void recoverUnknownTunnelsOnEosSwitch() {
        LOG.debug("EosChangeListener: recovering unknown tunnels");

        if (!directTunnelUtils.entityOwner()) {
            LOG.debug("Not an entity owner, returning...");
            return;
        }
        List<StateTunnelList> unknownTunnels = getUnknownTunnelsFromDefaultOper();
        if (unknownTunnels == null) {
            LOG.debug("ITM oper DS is empty.");
            EVENT_LOGGER.debug("ITM-EosChange, oper DS is empty.");
        } else if (unknownTunnels.isEmpty()) {
            LOG.debug("No unknown tunnels in ITM oper DS");
        } else {
            ConcurrentMap<String, String> tunnelNameToNodeConnectorIdValue = prepareKeyForInventoryOper(unknownTunnels);
            Set<ConcurrentMap.Entry<String,String>> entrySet = tunnelNameToNodeConnectorIdValue.entrySet();
            ReadOnlyTransaction readOnlyTx = dataBroker.newReadOnlyTransaction();
            for (ConcurrentMap.Entry<String,String> entry : entrySet) {
                String tunnelName = entry.getKey();
                String nodeConnectorIdValue = entry.getValue();
                String nodeIdValue = nodeConnectorIdValue.substring(0, nodeConnectorIdValue.lastIndexOf(":"));
                NodeConnectorId nodeConnectorId = new NodeConnectorId(nodeConnectorIdValue);
                NodeId nodeId = new NodeId(nodeIdValue);
                InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(nodeId))
                        .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();

                try {
                    if (readOnlyTx.exists(LogicalDatastoreType.OPERATIONAL, ncIdentifier).checkedGet()) {
                        txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                            updateTunnelState(tunnelName, tx);
                        });
                    }
                } catch (ReadFailedException rfe) {
                    LOG.error("EosChangeListener: Inventory oper read failed for {}, Reason: {}",
                            ncIdentifier, rfe.getMessage());
                }
            }
        }
    }


    ////////private method area////////
    private List<StateTunnelList> getUnknownTunnelsFromDefaultOper() {
        List<StateTunnelList> unknownTunnels = new ArrayList<>();
        Collection<StateTunnelList> tunnelList = tunnelStateCache.getAllPresent();
        if (!tunnelList.isEmpty()) {
            for (StateTunnelList stateTunnelEntry : tunnelList) {
                if (stateTunnelEntry.getOperState().equals(Interface.OperStatus.Unknown)) {
                    unknownTunnels.add(stateTunnelEntry);
                }
            }
            return unknownTunnels;
        } else {
            return null;
        }
    }

    private ConcurrentMap<String, String> prepareKeyForInventoryOper(List<StateTunnelList> unknownTunnels) {
        ConcurrentMap<String, String> tunnelNameToNodeConnectorIdValue = new ConcurrentHashMap<>();
        for (StateTunnelList unknownTunnel:unknownTunnels) {
            tunnelNameToNodeConnectorIdValue.put(unknownTunnel.getTunnelInterfaceName(),
                    "openflow:" + unknownTunnel.getSrcInfo().getTepDeviceId() + ":" + unknownTunnel.getPortNumber());
        }
        return tunnelNameToNodeConnectorIdValue;
    }

    private void updateTunnelState(String interfaceName, WriteTransaction writeOnlyTx) {
        StateTunnelListBuilder stateTnlBuilder = new StateTunnelListBuilder();
        stateTnlBuilder.setKey(new StateTunnelListKey(interfaceName));
        stateTnlBuilder.setTunnelState(true);
        stateTnlBuilder.setOperState(TunnelOperStatus.Up);
        InstanceIdentifier<StateTunnelList> tnlStateId = ItmUtils.buildStateTunnelListId(
                new StateTunnelListKey(interfaceName));
        writeOnlyTx.merge(LogicalDatastoreType.OPERATIONAL, tnlStateId, stateTnlBuilder.build(), false);
    }
}
