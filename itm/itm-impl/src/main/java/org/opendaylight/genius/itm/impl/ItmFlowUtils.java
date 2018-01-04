/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelDestinationIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.tunnel.zones.tunnel.zone.Vteps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ItmFlowUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmFlowUtils.class);
    private final DataBroker dataBroker;
    private final IMdsalApiManager mdsalApiManager;
    private final IdManagerService idManager;

    @Inject
    public ItmFlowUtils(final DataBroker dataBroker, final IMdsalApiManager mdsalApiManager,
                        final IdManagerService idManager) {
        this.dataBroker = dataBroker;
        this.mdsalApiManager = mdsalApiManager;
        this.idManager = idManager;
    }

    public static void makeTunnelIngressFlow(TepState tepState) {
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new MatchInPort(tepState.getDpnId(), tepState.getTepOfPort()));
        matches.add(new NxMatchTunnelDestinationIp(String.valueOf(tepState.getTepIp().getValue())));
        List<InstructionInfo> mkInstructions = new ArrayList<>();
        short tableId = NwConstants.VXLAN_TRUNK_INTERFACE_TABLE;
        if (TunnelTypeMplsOverGre.class.equals(tepState.getTepType())) {
            tableId = NwConstants.GRE_TRUNK_INTERFACE_TABLE;
        }
        mkInstructions.add(new InstructionGotoTable(tableId));
    }
}
