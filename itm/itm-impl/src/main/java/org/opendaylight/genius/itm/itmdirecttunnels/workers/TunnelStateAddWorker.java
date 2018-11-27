/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.workers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TunnelStateAddWorker {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelStateAddWorker.class);

    private final DirectTunnelUtils directTunnelUtils;
    private final ManagedNewTransactionRunner txRunner;

    public TunnelStateAddWorker(final DirectTunnelUtils directTunnelUtils, final ManagedNewTransactionRunner txRunner) {
        this.directTunnelUtils = directTunnelUtils;
        this.txRunner = txRunner;
    }

    public List<ListenableFuture<Void>> addState(TunnelStateInfo tunnelStateInfo)
            throws ExecutionException, InterruptedException, OperationFailedException {

        // When this method is invoked, all parameters necessary should be available
        // Retrieve Port No from nodeConnectorId
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(tunnelStateInfo.getNodeConnectorInfo()
                .getNodeConnectorId().firstIdentifierOf(NodeConnector.class)).getId();
        String interfaceName = tunnelStateInfo.getNodeConnectorInfo().getNodeConnector().getName();
        long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (portNo == ITMConstants.INVALID_PORT_NO) {
            LOG.error("Cannot derive port number, not proceeding with Interface State addition for interface: {}",
                interfaceName);
            return Collections.emptyList();
        }

        LOG.info("adding interface state to Oper DS for interface: {}", interfaceName);

        // Fetch the interface/Tunnel from config DS if exists
        // If it doesnt exists then "park" the processing and comeback to it when the data is available and
        // this will be triggered by the corres. listener. Caching and de-caching has to be synchronized.
        StateTunnelList stateTnl = addStateEntry(interfaceName, portNo, tunnelStateInfo);

        // This will be only tunnel If so not required
        // If this interface is a tunnel interface, create the tunnel ingress flow,
        // and start tunnel monitoring
        if (stateTnl != null) {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx ->
                directTunnelUtils.makeTunnelIngressFlow(DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId),
                portNo, interfaceName, stateTnl.getIfIndex(), NwConstants.DEL_FLOW)));
        }
        return Collections.emptyList();
    }

    private StateTunnelList addStateEntry(String interfaceName, long portNo, TunnelStateInfo tunnelStateInfo)
            throws ExecutionException, InterruptedException, OperationFailedException {
        LOG.debug("Start addStateEntry adding interface state for {}", interfaceName);
        final StateTunnelListBuilder stlBuilder = new StateTunnelListBuilder();
        Class<? extends TunnelTypeBase> tunnelType;
        DPNTEPsInfo srcDpnTepsInfo = tunnelStateInfo.getSrcDpnTepsInfo();

        DpnTepInterfaceInfo dpnTepInfo = tunnelStateInfo.getDpnTepInterfaceInfo();
        LOG.debug("Source Dpn TEP Interface Info {}", dpnTepInfo);
        tunnelType = dpnTepInfo.getTunnelType();

        final SrcInfoBuilder srcInfoBuilder =
                new SrcInfoBuilder().setTepDeviceId(tunnelStateInfo.getTunnelEndPointInfo().getSrcEndPointInfo());
        final DstInfoBuilder dstInfoBuilder =
                new DstInfoBuilder().setTepDeviceId(tunnelStateInfo.getTunnelEndPointInfo().getDstEndPointInfo());
        LOG.trace("Source Dpn TEP Info {}",srcDpnTepsInfo);
        TunnelEndPoints srcEndPtInfo = srcDpnTepsInfo.getTunnelEndPoints().get(0);
        srcInfoBuilder.setTepIp(srcEndPtInfo.getIpAddress());
        // As ITM Direct Tunnels deals with only Internal Tunnels.
        // Relook at this when it deals with external as well
        srcInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        DPNTEPsInfo dstDpnTePsInfo = tunnelStateInfo.getDstDpnTepsInfo();
        LOG.trace("Dest Dpn TEP Info {}", dstDpnTePsInfo);
        TunnelEndPoints dstEndPtInfo = dstDpnTePsInfo.getTunnelEndPoints().get(0);
        dstInfoBuilder.setTepIp(dstEndPtInfo.getIpAddress());
        // As ITM Direct Tunnels deals with only Internal Tunnels.
        // Relook at this when it deals with external as well
        dstInfoBuilder.setTepDeviceType(TepTypeInternal.class);

        Interface.OperStatus operStatus = Interface.OperStatus.Up;

        // ITM Direct Tunnels NOT SETTING THE TEP TYPe coz its not available. CHECK IF REQUIRED
        TunnelOperStatus tunnelOperStatus = DirectTunnelUtils.convertInterfaceToTunnelOperState(operStatus);
        boolean tunnelState = operStatus.equals(Interface.OperStatus.Up);

        StateTunnelListKey tlKey = new StateTunnelListKey(interfaceName);
        stlBuilder.setKey(tlKey).setOperState(tunnelOperStatus).setTunnelState(tunnelState)
        .setDstInfo(dstInfoBuilder.build()).setSrcInfo(srcInfoBuilder.build()).setTransportType(tunnelType)
        .setPortNumber(String.valueOf(portNo));
        int ifIndex = directTunnelUtils.allocateId(IfmConstants.IFM_IDPOOL_NAME, interfaceName);
        createLportTagInterfaceMap(interfaceName, ifIndex);
        stlBuilder.setIfIndex(ifIndex);
        InstanceIdentifier<StateTunnelList> stListId = ItmUtils.buildStateTunnelListId(tlKey);
        LOG.info("Batching the Creation of tunnel_state: {} for Id: {}", stlBuilder.build(), stListId);
        ITMBatchingUtils.write(stListId, stlBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        return stlBuilder.build();
    }

    private void createLportTagInterfaceMap(String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}", infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        IfIndexTunnel ifIndexInterface = new IfIndexTunnelBuilder().setIfIndex(ifIndex)
            .setKey(new IfIndexTunnelKey(ifIndex)).setInterfaceName(infName).build();
        ITMBatchingUtils.write(id, ifIndexInterface, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }
}
