/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.NodeConnectorInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelEndPointInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
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

public class OvsInterfaceStateAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateAddHelper.class);

    public static List<ListenableFuture<Void>> addState(DataBroker dataBroker, IdManagerService idManager,
                                                        IMdsalApiManager mdsalApiManager,
                                                        InstanceIdentifier<FlowCapableNodeConnector> key,
                                                        String interfaceName,
                                                        FlowCapableNodeConnector fcNodeConnectorNew) {
        boolean unableToProcess = false;
        //Retrieve Port No from nodeConnectorId
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(key.firstIdentifierOf(NodeConnector.class)).getId();
        long portNo = ItmScaleUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == ITMConstants.INVALID_PORT_NO) {
            LOG.trace("Cannot derive port number, not proceeding with Interface State "
                    + "addition for interface: {}", interfaceName);
            return null;
        }

        LOG.info("adding interface state to Oper DS for interface: {}", interfaceName);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();

        Interface.OperStatus operStatus = Interface.OperStatus.Up;
        Interface.AdminStatus adminStatus = Interface.AdminStatus.Up;

        // Fetch the interface/Tunnel from config DS if exists
        TunnelEndPointInfo tunnelEndPointInfo = ItmScaleUtils.getTunnelEndPointInfoFromCache(interfaceName);

        if (tunnelEndPointInfo != null) {
            DpnTepInterfaceInfo dpnTepConfigInfo = ItmScaleUtils.getDpnTepInterfaceFromCache(
                    new BigInteger(tunnelEndPointInfo.getSrcDpnId()),
                    new BigInteger(tunnelEndPointInfo.getDstDpnId()));
            if (dpnTepConfigInfo != null) {
                StateTunnelList stateTnl = TunnelUtils.addStateEntry(tunnelEndPointInfo, interfaceName,
                        defaultOperationalShardTransaction, idManager, operStatus, adminStatus, nodeConnectorId);

                // SF419 This will be onl tunnel If so not required
                // If this interface is a tunnel interface, create the tunnel ingress flow,and start tunnel monitoring
                handleTunnelMonitoringAddition(futures, dataBroker, mdsalApiManager,
                        nodeConnectorId, defaultOperationalShardTransaction, stateTnl.getIfIndex(),
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
            NodeConnectorInfo nodeConnectorInfo = new NodeConnectorInfo(key, fcNodeConnectorNew);
            ItmScaleUtils.addNodeConnectorInfoToCache(interfaceName, nodeConnectorInfo);
        }
        //futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }

    public static void handleTunnelMonitoringAddition(List<ListenableFuture<Void>> futures, DataBroker dataBroker,
                                                      IMdsalApiManager mdsalApiManager,
                                                      NodeConnectorId nodeConnectorId, WriteTransaction transaction,
                                                      Integer ifindex, DpnTepInterfaceInfo dpnTepConfigInfo,
                                                      String interfaceName, long portNo) {
        BigInteger dpId = ItmScaleUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        TunnelUtils.makeTunnelIngressFlow(futures, mdsalApiManager, dpnTepConfigInfo, dpId, portNo, interfaceName,
                ifindex, NwConstants.ADD_FLOW);
        futures.add(transaction.submit());
    }
}