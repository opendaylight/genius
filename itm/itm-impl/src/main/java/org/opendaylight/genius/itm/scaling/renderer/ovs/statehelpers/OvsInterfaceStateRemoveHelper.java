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
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OvsInterfaceStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsInterfaceStateRemoveHelper.class);

    public static List<ListenableFuture<Void>> removeInterfaceStateConfiguration(IdManagerService idManager,
                                                                                 IMdsalApiManager mdsalApiManager,
                                                                                 NodeConnectorId nodeConnectorIdNew,
                                                                                 NodeConnectorId nodeConnectorIdOld,
                                                                                 DataBroker dataBroker,
                                                                                 String interfaceName,
                                                                                 FlowCapableNodeConnector
                                                                                         fcNodeConnectorOld,
                                                                                 String parentInterface,
                                                                                 TunnelStateCache tunnelStateCache) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultOperationalShardTransaction = dataBroker.newWriteOnlyTransaction();

        NodeConnectorId nodeConnectorId = (nodeConnectorIdOld != null && !nodeConnectorIdNew.equals(nodeConnectorIdOld))
                ? nodeConnectorIdOld : nodeConnectorIdNew;

        BigInteger dpId = ItmScaleUtils.getDpnFromNodeConnectorId(nodeConnectorId);
        // In a genuine port delete scenario, the reason will be there in the incoming event, for all remaining
        // cases treat the event as DPN disconnect, if old and new ports are same. Else, this is a VM migration
        // scenario, and should be treated as port removal.
        if (fcNodeConnectorOld.getReason() != PortReason.Delete && nodeConnectorIdNew.equals(nodeConnectorIdOld)) {
            //Remove event is because of connection lost between controller and switch, or switch shutdown.
            // Hence, dont remove the interface but set the status as "unknown"
            OvsInterfaceStateUpdateHelper.updateInterfaceStateOnNodeRemove(interfaceName,
                fcNodeConnectorOld, dataBroker, defaultOperationalShardTransaction, tunnelStateCache);
        } else {
            LOG.debug("removing interface state for interface: {}", interfaceName);
            TunnelUtils.deleteTunnelStateEntry(interfaceName, defaultOperationalShardTransaction);
            DpnTepInterfaceInfo dpnTepInfo =
                    TunnelUtils.getTunnelFromConfigDS(interfaceName, dataBroker);

            if (dpnTepInfo != null) {
                //SF 419 This will only be tunnel interface
                TunnelMetaUtils.removeLportTagInterfaceMap(idManager, defaultOperationalShardTransaction,
                        interfaceName);
                long portNo = ItmScaleUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
                TunnelUtils.makeTunnelIngressFlow(futures, mdsalApiManager, dpnTepInfo, dpId, portNo, interfaceName, -1,
                        NwConstants.DEL_FLOW);
                /*
                // If this interface is a tunnel interface, remove the tunnel ingress flow and stop lldp monitoring
                if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
                    TunnelMetaUtils.removeLportTagInterfaceMap(idManager, defaultOperationalShardTransaction,
                     interfaceName);

                    handleTunnelMonitoringRemoval(alivenessMonitorService, mdsalApiManager, dataBroker, dpId,
                            iface.getName(), iface.getAugmentation(IfTunnel.class), defaultOperationalShardTransaction,
                            nodeConnectorId, futures);
                    return futures;
                }
                */
            } else {
                LOG.error("DPNTEPInfo is null for Tunnel Interface {}", interfaceName);
            }
            // ITM Direct Tunnels This may not be reuired if you are not maintaining this DpnToInterface Op DS. -- CHECK
            // Delete the Vpn Interface from DpnToInterface Op DS.
           /*
           TunnelUtils.deleteDpnToInterface(dataBroker, dpId, interfaceName, defaultOperationalShardTransaction);
           */
        }
        futures.add(defaultOperationalShardTransaction.submit());
        return futures;
    }
}