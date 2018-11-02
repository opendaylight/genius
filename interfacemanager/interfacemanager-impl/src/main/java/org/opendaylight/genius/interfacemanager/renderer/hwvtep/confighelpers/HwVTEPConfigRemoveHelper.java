/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwVTEPConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPConfigRemoveHelper.class);

    private HwVTEPConfigRemoveHelper() {
    }

    public static List<ListenableFuture<Void>> removeConfiguration(ManagedNewTransactionRunner txRunner,
                                                                   Interface interfaceOld,
                                                                   InstanceIdentifier<Node> physicalSwitchNodeId,
                                                                   InstanceIdentifier<Node> globalNodeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        LOG.info("removing hwvtep configuration for {}", interfaceOld.getName());
        if (globalNodeId != null) {
            IfTunnel ifTunnel = interfaceOld.augmentation(IfTunnel.class);
            //removeTunnelTableEntry(defaultOperShardTransaction, ifTunnel, physicalSwitchNodeId);
            // Topology configuration shard
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> removeTerminationEndPoint(tx, ifTunnel, globalNodeId)));
            // Default operational shard
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                    InterfaceManagerCommonUtils.deleteStateEntry(tx, interfaceOld.getName());
                    InterfaceMetaUtils.removeTunnelToInterfaceMap(physicalSwitchNodeId, tx, ifTunnel);
                }
            ));
        }
        return futures;
    }

    private static void removeTerminationEndPoint(TypedWriteTransaction<Configuration> transaction, IfTunnel ifTunnel,
            InstanceIdentifier<Node> globalNodeId) {
        LOG.info("removing remote termination end point {}", ifTunnel.getTunnelDestination());
        TerminationPointKey tpKey = SouthboundUtils
                .getTerminationPointKey(ifTunnel.getTunnelDestination().getIpv4Address().getValue());
        InstanceIdentifier<TerminationPoint> tpPath = SouthboundUtils.createInstanceIdentifier(globalNodeId, tpKey);
        transaction.delete(tpPath);
    }
}
