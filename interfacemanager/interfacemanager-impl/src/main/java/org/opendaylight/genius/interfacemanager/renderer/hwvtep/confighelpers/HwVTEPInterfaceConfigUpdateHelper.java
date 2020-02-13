/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.hwvtep.confighelpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.mdsal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwVTEPInterfaceConfigUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPInterfaceConfigUpdateHelper.class);

    private HwVTEPInterfaceConfigUpdateHelper() {
    }

    public static List<ListenableFuture<Void>> updateConfiguration(ManagedNewTransactionRunner txRunner,
            InstanceIdentifier<Node> physicalSwitchNodeId, InstanceIdentifier<Node> globalNodeId,
            Interface interfaceNew, IfTunnel ifTunnel) {
        LOG.info("updating hwvtep configuration for {}", interfaceNew.getName());

        // Create hwvtep through OVSDB plugin
        if (globalNodeId != null) {
            return updateBfdMonitoring(txRunner, globalNodeId, physicalSwitchNodeId, ifTunnel);
        } else {
            LOG.debug("specified physical switch is not connected {}", physicalSwitchNodeId);
            return Collections.emptyList();
        }
    }

    /*
     * BFD monitoring interval and enable/disable attributes can be modified
     */
    public static List<ListenableFuture<Void>> updateBfdMonitoring(ManagedNewTransactionRunner txRunner,
            InstanceIdentifier<Node> globalNodeId, InstanceIdentifier<Node> physicalSwitchId, IfTunnel ifTunnel) {
        TunnelsBuilder tunnelsBuilder = new TunnelsBuilder();
        InstanceIdentifier<TerminationPoint> localTEPInstanceIdentifier = SouthboundUtils
                .createTEPInstanceIdentifier(globalNodeId, ifTunnel.getTunnelSource());
        InstanceIdentifier<TerminationPoint> remoteTEPInstanceIdentifier = SouthboundUtils
                .createTEPInstanceIdentifier(globalNodeId, ifTunnel.getTunnelDestination());
        InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier = SouthboundUtils.createTunnelsInstanceIdentifier(
                physicalSwitchId, localTEPInstanceIdentifier, remoteTEPInstanceIdentifier);

        LOG.debug("updating bfd monitoring parameters for the hwvtep {}", tunnelsInstanceIdentifier);
        tunnelsBuilder.withKey(new TunnelsKey(new HwvtepPhysicalLocatorRef(localTEPInstanceIdentifier),
                new HwvtepPhysicalLocatorRef(remoteTEPInstanceIdentifier)));
        List<BfdParams> bfdParams = new ArrayList<>();
        SouthboundUtils.fillBfdParameters(bfdParams, ifTunnel);
        tunnelsBuilder.setBfdParams(bfdParams);
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> tx.merge(tunnelsInstanceIdentifier, tunnelsBuilder.build(), CREATE_MISSING_PARENTS)));
    }
}
