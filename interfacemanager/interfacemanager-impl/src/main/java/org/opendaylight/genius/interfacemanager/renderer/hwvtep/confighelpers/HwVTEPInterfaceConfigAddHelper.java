/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers;

import static org.opendaylight.controller.md.sal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;
import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
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

public final class HwVTEPInterfaceConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPInterfaceConfigAddHelper.class);

    private HwVTEPInterfaceConfigAddHelper() { }

    public static List<ListenableFuture<Void>> addConfiguration(ManagedNewTransactionRunner txRunner,
            InstanceIdentifier<Node> physicalSwitchNodeId, InstanceIdentifier<Node> globalNodeId,
            Interface interfaceNew, IfTunnel ifTunnel) {
        LOG.info("adding hwvtep configuration for {}", interfaceNew.getName());

        // create hwvtep through ovsdb plugin
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        // Topology config shard
        if (globalNodeId != null) {
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> addTerminationPoints(tx, globalNodeId, ifTunnel)));
        }
        // Default operational shard
        // TODO Move this to another listener, reacing to the config write above
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
            InterfaceMetaUtils.createTunnelToInterfaceMap(interfaceNew.getName(), physicalSwitchNodeId,
                    tx, ifTunnel);
            if (globalNodeId != null) {
                SouthboundUtils.addStateEntry(interfaceNew, interfaceNew.augmentation(IfTunnel.class), tx);
            }
        }));
        return futures;
    }

    /*
     * For each hwvtep configuration, we need to configure Physical LocatorTable
     * of hwvtep schema with destination IP and tunnel-type. The configuration
     * needs to be done for both local endpoint as well as remote endpoint
     */
    public static void addTerminationPoints(TypedWriteTransaction<Configuration> transaction,
            InstanceIdentifier<Node> globalNodeId, IfTunnel ifTunnel) {
        // InstanceIdentifier<TerminationPoint> localTEP =
        // createLocalPhysicalLocatorEntryIfNotPresent(futures,
        // dataBroker,transaction, ifTunnel, globalNodeId);
        createRemotePhysicalLocatorEntry(transaction, globalNodeId, ifTunnel.getTunnelDestination());
        // InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier =
        // createTunnelTableEntry(transaction, physicalSwitchNodeId, localTEP,
        // remoteTEP);
    }

    private static InstanceIdentifier<TerminationPoint> createRemotePhysicalLocatorEntry(
            TypedWriteTransaction<Configuration> transaction,
            InstanceIdentifier<Node> nodeIid, IpAddress destIPAddress) {
        String remoteIp = destIPAddress.getIpv4Address().getValue();
        LOG.debug("creating remote physical locator entry {}", remoteIp);
        TerminationPointKey tpKey = SouthboundUtils.getTerminationPointKey(remoteIp);
        InstanceIdentifier<TerminationPoint> tpPath = SouthboundUtils.createInstanceIdentifier(nodeIid, tpKey);
        createPhysicalLocatorEntry(transaction, tpPath, tpKey, destIPAddress);
        return tpPath;
    }

    /*
     * This method writes the termination end point details to the topology
     * Config DS
     */
    private static void createPhysicalLocatorEntry(TypedWriteTransaction<Configuration> transaction,
            InstanceIdentifier<TerminationPoint> tpPath, TerminationPointKey terminationPointKey,
            IpAddress destIPAddress) {
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        HwvtepPhysicalLocatorAugmentationBuilder tpAugmentationBuilder = new HwvtepPhysicalLocatorAugmentationBuilder();
        tpBuilder.withKey(terminationPointKey);
        tpBuilder.setTpId(terminationPointKey.getTpId());
        tpAugmentationBuilder.setEncapsulationType(EncapsulationTypeVxlanOverIpv4.class);
        SouthboundUtils.setDstIp(tpAugmentationBuilder, destIPAddress);
        tpBuilder.addAugmentation(HwvtepPhysicalLocatorAugmentation.class, tpAugmentationBuilder.build());
        LOG.debug("creating physical locator entry for {}", terminationPointKey);
        transaction.put(tpPath, tpBuilder.build(), CREATE_MISSING_PARENTS);
    }
}
