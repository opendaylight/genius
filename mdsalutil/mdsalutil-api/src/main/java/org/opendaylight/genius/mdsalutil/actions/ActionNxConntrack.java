/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.NxConntrackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.nx.conntrack.CtActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.nx.conntrack.CtActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.ofpact.actions.ofpact.actions.NxActionNatCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.ofpact.actions.ofpact.actions.nx.action.nat._case.NxActionNatBuilder;

/**
 * NX conntrack action.
 */
public class ActionNxConntrack extends ActionInfo {
    private final int flags;
    private final long zoneSrc;
    private final int conntrackZone;
    private final short recircTable;
    private final List<NxCtAction> ctActions = new ArrayList<>();

    public ActionNxConntrack(int flags, long zoneSrc, int conntrackZone, short recircTable,
            List<NxCtAction> ctActions) {
        super(ActionType.nx_conntrack, new String[] {Integer.toString(flags), Long.toString(zoneSrc), Integer.toString(
                conntrackZone), Short.toString(recircTable)});
        this.flags = flags;
        this.zoneSrc = zoneSrc;
        this.conntrackZone = conntrackZone;
        this.recircTable = recircTable;
        this.ctActions.addAll(ctActions);
    }

    public ActionNxConntrack(int actionKey, int flags, long zoneSrc, int conntrackZone, short recircTable,
            List<NxCtAction> ctActions) {
        super(ActionType.nx_conntrack, new String[] {Integer.toString(flags), Long.toString(zoneSrc), Integer.toString(
                conntrackZone), Short.toString(recircTable)}, actionKey);
        this.flags = flags;
        this.zoneSrc = zoneSrc;
        this.conntrackZone = conntrackZone;
        this.recircTable = recircTable;
        this.ctActions.addAll(ctActions);
    }

    @Deprecated
    public ActionNxConntrack(String[] actionValues) {
        this(Integer.parseInt(actionValues[0]), Long.parseLong(actionValues[1]), Integer.parseInt(actionValues[2]),
                Short.parseShort(actionValues[3]), Collections.emptyList());
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        NxConntrackBuilder ctb = new NxConntrackBuilder()
                .setFlags(flags)
                .setZoneSrc(zoneSrc)
                .setConntrackZone(conntrackZone)
                .setRecircTable(recircTable);

        ctb.setCtActions(this.ctActions.stream().map(NxCtAction::buildCtActions).collect(Collectors.toList()));
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxConntrack(ctb.build()).build());
        ab.setKey(new ActionKey(newActionKey));
        return ab.build();
    }


    public interface NxCtAction {
        CtActions buildCtActions();

        @Deprecated
        String[] toStrings();
    }


    public static class NxNat implements NxCtAction {
        private final int flags;
        private final int rangePresent;
        private final IpAddress ipAddressMin;
        private final IpAddress ipAddressMax;
        private final int portMin;
        private final int portMax;

        public NxNat(int actionKey, int flags, int natType, IpAddress ipAddressMin,
                IpAddress ipAddressMax,int portMin, int portMax) {
            this.flags = flags;
            this.rangePresent = natType;
            this.ipAddressMin = ipAddressMin;
            this.ipAddressMax = ipAddressMax;
            this.portMin = portMin;
            this.portMax = portMax;
        }


        @Override
        public String[] toStrings() {
            return new String[] {NwConstants.CtActionType.NxNat.name(), Integer.toString(flags),
                    Long.toString(rangePresent), ipAddressMin.toString(), ipAddressMax.toString(),
                    Integer.toString(portMin), Integer.toString(portMax)};
        }

        @Override
        public CtActions buildCtActions() {
            NxActionNatBuilder nxActionNatBuilder = new NxActionNatBuilder()
                    .setFlags(flags)
                    .setRangePresent(rangePresent)
                    .setIpAddressMin(ipAddressMin)
                    .setIpAddressMax(ipAddressMax)
                    .setPortMin(portMin)
                    .setPortMax(portMax);

            CtActionsBuilder ctActionsBuilder = new CtActionsBuilder();
            NxActionNatCaseBuilder caseBuilder = new NxActionNatCaseBuilder();
            caseBuilder.setNxActionNat(nxActionNatBuilder.build());
            ctActionsBuilder.setOfpactActions(caseBuilder.build());
            return ctActionsBuilder.build();
        }
    }

}
