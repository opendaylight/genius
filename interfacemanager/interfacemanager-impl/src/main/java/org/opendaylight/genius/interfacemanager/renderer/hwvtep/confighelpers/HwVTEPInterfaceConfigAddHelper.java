/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeVxlanOverIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwVTEPInterfaceConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPInterfaceConfigAddHelper.class);

    public static List<ListenableFuture<Void>> addConfiguration(ManagedNewTransactionRunner txRunner,
            InstanceIdentifier<Node> physicalSwitchNodeId, InstanceIdentifier<Node> globalNodeId,
            Interface interfaceNew, IfTunnel ifTunnel) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        LOG.info("adding hwvtep configuration for {}", interfaceNew.getName());

        // create hwvtep through ovsdb plugin
        // Default operational
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            InterfaceMetaUtils.createTunnelToInterfaceMap(interfaceNew.getName(), physicalSwitchNodeId,
                    tx, ifTunnel);
            if (globalNodeId != null) {
                SouthboundUtils.addStateEntry(interfaceNew, interfaceNew.getAugmentation(IfTunnel.class), tx);
            }
        }));
        // Topology config
        if (globalNodeId != null) {
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                tx -> addTerminationPoints(tx, futures, globalNodeId, ifTunnel)));
        }
        return futures;
    }

    /*
     * For each hwvtep configuration, we need to configure Physical LocatorTable
     * of hwvtep schema with destination IP and tunnel-type. The configuration
     * needs to be done for both local endpoint as well as remote endpoint
     */
    public static void addTerminationPoints(WriteTransaction transaction, List<ListenableFuture<Void>> futures,
            InstanceIdentifier<Node> globalNodeId, IfTunnel ifTunnel) {
        // InstanceIdentifier<TerminationPoint> localTEP =
        // createLocalPhysicalLocatorEntryIfNotPresent(futures,
        // dataBroker,transaction, ifTunnel, globalNodeId);
        createRemotePhysicalLocatorEntry(transaction, futures, globalNodeId, ifTunnel.getTunnelDestination());
        // InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier =
        // createTunnelTableEntry(transaction, physicalSwitchNodeId, localTEP,
        // remoteTEP);
    }

    private static InstanceIdentifier<TerminationPoint> createRemotePhysicalLocatorEntry(WriteTransaction transaction,
            List<ListenableFuture<Void>> futures, InstanceIdentifier<Node> nodeIid, IpAddress destIPAddress) {
        String remoteIp = destIPAddress.getIpv4Address().getValue();
        LOG.debug("creating remote physical locator entry {}", remoteIp);
        TerminationPointKey tpKey = SouthboundUtils.getTerminationPointKey(remoteIp);
        InstanceIdentifier<TerminationPoint> tpPath = SouthboundUtils.createInstanceIdentifier(nodeIid, tpKey);
        createPhysicalLocatorEntry(transaction, futures, tpPath, tpKey, destIPAddress);
        return tpPath;
    }

    /*
     * This method writes the termination end point details to the topology
     * Config DS
     */
    private static void createPhysicalLocatorEntry(WriteTransaction transaction, List<ListenableFuture<Void>> futures,
            InstanceIdentifier<TerminationPoint> tpPath, TerminationPointKey terminationPointKey,
            IpAddress destIPAddress) {
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder = new HwvtepPhysicalLocatorAugmentationBuilder();
        tpBuilder.setKey(terminationPointKey);
        tpBuilder.setTpId(terminationPointKey.getTpId());
        tpAugmentationBuilder.setEncapsulationType(EncapsulationTypeVxlanOverIpv4.class);
        SouthboundUtils.setDstIp(tpAugmentationBuilder, destIPAddress);
        tpBuilder.addAugmentation(HwvtepPhysicalLocatorAugmentation.class, tpAugmentationBuilder.build());
        LOG.debug("creating physical locator entry for {}", terminationPointKey);
        transaction.put(LogicalDatastoreType.CONFIGURATION, tpPath, tpBuilder.build(), true);
    }
}
