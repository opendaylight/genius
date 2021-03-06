/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.hwvtep.statehelpers;

import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;
import static org.opendaylight.mdsal.binding.util.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdStatusKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwVTEPInterfaceStateUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPInterfaceStateUpdateHelper.class);

    private HwVTEPInterfaceStateUpdateHelper() {
    }

    public static List<? extends ListenableFuture<?>> updatePhysicalSwitch(ManagedNewTransactionRunner txRunner,
            InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier, Tunnels tunnelsNew) {
        LOG.debug("updating physical switch for tunnels");
        return Collections.singletonList(txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
            String interfaceName = InterfaceMetaUtils
                    .getInterfaceForTunnelInstanceIdentifier(tunnelsInstanceIdentifier.toString(), tx);
            if (interfaceName != null) {
                // update opstate of interface if TEP has gone down/up as a result of
                // BFD monitoring
                InterfaceManagerCommonUtils.updateOpState(tx, interfaceName,
                        getTunnelOpState(tunnelsNew.getBfdStatus()));
            }
        }));
    }

    private static OperStatus getTunnelOpState(Map<BfdStatusKey, BfdStatus> tunnelBfdStatus) {
        OperStatus livenessState = OperStatus.Down;
        if (tunnelBfdStatus != null && !tunnelBfdStatus.isEmpty()) {
            for (BfdStatus bfdState : tunnelBfdStatus.values()) {
                if (SouthboundUtils.BFD_OP_STATE.equalsIgnoreCase(bfdState.getBfdStatusKey())) {
                    String bfdOpState = bfdState.getBfdStatusValue();
                    if (SouthboundUtils.BFD_STATE_UP.equalsIgnoreCase(bfdOpState)) {
                        livenessState = OperStatus.Up;
                    } else {
                        livenessState = OperStatus.Down;
                    }
                    break;
                }
            }
        }
        return livenessState;
    }

    public static List<? extends ListenableFuture<?>> startBfdMonitoring(ManagedNewTransactionRunner txRunner,
            InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier, Tunnels tunnelsNew) {
        LOG.debug("starting bfd monitoring for the hwvtep {}", tunnelsInstanceIdentifier);

        TunnelsBuilder tunnelsBuilder = new TunnelsBuilder();
        tunnelsBuilder.withKey(new TunnelsKey(tunnelsNew.getLocalLocatorRef(), tunnelsNew.getRemoteLocatorRef()));
        tunnelsBuilder.setLocalLocatorRef(tunnelsNew.getLocalLocatorRef());
        tunnelsBuilder.setRemoteLocatorRef(tunnelsNew.getLocalLocatorRef());
        List<BfdParams> bfdParams = new ArrayList<>();
        SouthboundUtils.fillBfdParameters(bfdParams, null);
        tunnelsBuilder.setBfdParams(bfdParams);
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> tx.mergeParentStructurePut(tunnelsInstanceIdentifier, tunnelsBuilder.build())));
    }
}
