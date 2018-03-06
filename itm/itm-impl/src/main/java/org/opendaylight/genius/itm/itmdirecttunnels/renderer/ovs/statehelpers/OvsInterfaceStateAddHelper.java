/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnsTepsStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.itm.utils.NodeConnectorInfoBuilder;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create the entries in Tunnels-State OperDS.
 * Install Ingress Flow
 */

public final class OvsInterfaceStateAddHelper {

    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateAddHelper.class);

    private OvsInterfaceStateAddHelper() {
    }

    public static List<ListenableFuture<Void>> addState(DataBroker dataBroker, IdManagerService idManager,
                                                        IMdsalApiManager mdsalApiManager,
                                                        InstanceIdentifier<FlowCapableNodeConnector> key,
                                                        String interfaceName,
                                                        FlowCapableNodeConnector fcNodeConnectorNew,
                                                        DPNTEPsInfoCache dpntePsInfoCache,
                                                        DpnsTepsStateCache dpnTepStateCache,
                                                        DirectTunnelUtils directTunnelUtils) {
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
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        ManagedNewTransactionRunner managedNewTransactionRunner = new ManagedNewTransactionRunnerImpl(dataBroker);

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface/Tunnel from config DS if exists
        TunnelEndPointInfo tunnelEndPointInfo = directTunnelUtils.getTunnelEndPointInfoFromCache(interfaceName);

        if (tunnelEndPointInfo != null) {
            DpnTepInterfaceInfo dpnTepConfigInfo =
                    dpnTepStateCache.getDpnTepInterfaceInfo(tunnelEndPointInfo.getSrcEndPointInfo(),
                            tunnelEndPointInfo.getDstEndPointInfo());
            if (dpnTepConfigInfo != null) {
                StateTunnelList stateTnl = TunnelUtils.addStateEntry(tunnelEndPointInfo, interfaceName,
                        managedNewTransactionRunner, idManager, operStatus, adminStatus, nodeConnectorId,
                        dpntePsInfoCache);

                // SF419 This will be onl tunnel If so not required
                // If this interface is a tunnel interface, create the tunnel ingress flow,and start tunnel monitoring
                handleTunnelMonitoringAddition(futures, dataBroker, mdsalApiManager,
                        nodeConnectorId, managedNewTransactionRunner, stateTnl.getIfIndex(),
                        dpnTepConfigInfo, interfaceName, portNo);
            } else {
                LOG.error("DpnTepINfo is NULL while addState for interface {} ", interfaceName);
                unableToProcess = true;
            }
        } else {
            LOG.error("TunnelEndPointInfo is NULL while addState for interface {} ", interfaceName);
            unableToProcess = true;
        }
        if (unableToProcess) {
            LOG.debug(" Unable to process the NodeConnector ADD event for {} as Config not available."
                    + "Hence parking it", interfaceName);
            NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfoBuilder().setNodeConnectorId(key)
                    .setNodeConnector(fcNodeConnectorNew).build();
            directTunnelUtils.addNodeConnectorInfoToCache(interfaceName, nodeConnectorInfo);
        }
        //futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }

    public static void handleTunnelMonitoringAddition(List<ListenableFuture<Void>> futures, DataBroker dataBroker,
                                                      IMdsalApiManager mdsalApiManager,
                                                      NodeConnectorId nodeConnectorId,
                                                      ManagedNewTransactionRunner transaction,
                                                      Integer ifindex, DpnTepInterfaceInfo dpnTepConfigInfo,
                                                      String interfaceName, long portNo) {
        //TODO
    }
}