/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.mdsal.binding.util.Datastore;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.IfIndexesTunnelMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210._if.indexes.tunnel.map.IfIndexTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OfPortStateAddWorker {

    private static final Logger LOG = LoggerFactory.getLogger(OfPortStateAddWorker.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DirectTunnelUtils directTunnelUtils;
    private final OfDpnTep dpnTep;
    private final ManagedNewTransactionRunner txRunner;

    public OfPortStateAddWorker(final DirectTunnelUtils directTunnelUtils, final OfDpnTep dpnTep,
                                final ManagedNewTransactionRunner txRunner) {
        this.directTunnelUtils = directTunnelUtils;
        this.dpnTep = dpnTep;
        this.txRunner = txRunner;
    }

    public List<? extends ListenableFuture<?>> addState(NodeConnectorInfo nodeConnectorInfo)
            throws ExecutionException, InterruptedException, OperationFailedException {
        // When this method is invoked, all parameters necessary should be available
        // Retrieve Port No from nodeConnectorId
        NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(
                nodeConnectorInfo.getNodeConnectorId().firstIdentifierOf(NodeConnector.class)).getId();
        String ofPortName = nodeConnectorInfo.getNodeConnector().getName();
        long portNo = DirectTunnelUtils.getPortNumberFromNodeConnectorId(nodeConnectorId);
        EVENT_LOGGER.debug("ITM-Of Port State, ADD to oper DS {}", ofPortName);
        if (portNo == ITMConstants.INVALID_PORT_NO) {
            LOG.error("Cannot derive port number, not proceeding with of-port State "
                    + "addition for of-port: {}", ofPortName);
            return null;
        }

        LOG.info("adding of-port state to Oper DS for interface: {}", ofPortName);
        OfTep ofTep = addStateEntry(ofPortName, portNo);

        // If this interface is a of-port interface, create the of-port ingress flow
        if (ofTep != null) {
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                tx -> {
                    Uint64 dpId = DirectTunnelUtils.getDpnFromNodeConnectorId(nodeConnectorId);
                    directTunnelUtils.addTunnelIngressFlow(tx, dpId, portNo, ofPortName,
                            ofTep.getIfIndex().toJava(),
                            ofTep.getTepIp().getIpv4Address());
                }));
        }
        EVENT_LOGGER.debug("ITM-of-port,ADD Table 0 flow for {} completed", ofPortName);
        return Collections.emptyList();
    }

    private OfTep addStateEntry(String ofPortName, long portNo)
            throws ExecutionException, InterruptedException, OperationFailedException {
        final int ifIndex = directTunnelUtils.allocateId(IfmConstants.IFM_IDPOOL_NAME, ofPortName);
        if (ifIndex == ITMConstants.INVALID_ID) {
            LOG.trace("aborting addStateEntry for {}, due to invalid Id", ofPortName);
            return null;
        }
        createLportTagInterfaceMap(ofPortName, ifIndex);
        final OfTepBuilder ofTepBuilder = new OfTepBuilder();

        Interface.OperStatus operStatus = Interface.OperStatus.Up;

        TunnelOperStatus tunnelOperStatus = DirectTunnelUtils.convertInterfaceToTunnelOperState(operStatus);

        OfTepKey ofTepKey = new OfTepKey(ofPortName);
        ofTepBuilder.withKey(ofTepKey).setTunnelType(dpnTep.getTunnelType()).setOfPortName(ofPortName)
                .setTepIp(dpnTep.getTepIp()).setPortNumber(String.valueOf(portNo))
                .setIfIndex(Uint16.valueOf(ifIndex)).setOfTepState(tunnelOperStatus)
                .setSourceDpnId(dpnTep.getSourceDpnId());

        InstanceIdentifier<OfTep> ofTepsListId = ItmUtils.buildStateOfTepListId(ofTepKey);
        LOG.trace("Batching the Creation of tunnel_state: {} for Id: {}", ofTepBuilder.build(), ofTepsListId);
        ITMBatchingUtils.write(ofTepsListId, ofTepBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        return ofTepBuilder.build();
    }

    private void createLportTagInterfaceMap(String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}", infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class)
                .child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        IfIndexTunnel ifIndexInterface = new IfIndexTunnelBuilder().setIfIndex(ifIndex)
                .withKey(new IfIndexTunnelKey(ifIndex)).setInterfaceName(infName).build();
        ITMBatchingUtils.write(id, ifIndexInterface, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }
}
