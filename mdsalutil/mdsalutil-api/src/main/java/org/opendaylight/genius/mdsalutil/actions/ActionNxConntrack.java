/*
 * Copyright © 2016, 2017 Red Hat, Inc. and others.
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.NxConntrackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.nx.conntrack.CtActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.nx.conntrack.CtActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.ofpact.actions.ofpact.actions.NxActionCtMarkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.ofpact.actions.ofpact.actions.NxActionNatCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.ofpact.actions.ofpact.actions.nx.action.ct.mark._case.NxActionCtMarkBuilder;
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

    public ActionNxConntrack(int flags, long zoneSrc, int conntrackZone, short recircTable) {
        this(0, flags, zoneSrc, conntrackZone, recircTable, Collections.emptyList());
    }

    public ActionNxConntrack(int flags, long zoneSrc, int conntrackZone, short recircTable,
            List<NxCtAction> ctActions) {
        this(0, flags, zoneSrc, conntrackZone, recircTable, ctActions);
    }

    public ActionNxConntrack(int actionKey, int flags, long zoneSrc, int conntrackZone, short recircTable) {
        this(actionKey, flags, zoneSrc, conntrackZone, recircTable, Collections.emptyList());
    }

    public ActionNxConntrack(int actionKey, int flags, long zoneSrc, int conntrackZone, short recircTable,
            List<NxCtAction> ctActions) {
        super(actionKey);
        this.flags = flags;
        this.zoneSrc = zoneSrc;
        this.conntrackZone = conntrackZone;
        this.recircTable = recircTable;
        this.ctActions.addAll(ctActions);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
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

    public int getFlags() {
        return flags;
    }

    public long getZoneSrc() {
        return zoneSrc;
    }

    public int getConntrackZone() {
        return conntrackZone;
    }

    public short getRecircTable() {
        return recircTable;
    }

    public List<NxCtAction> getCtActions() {
        return ctActions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        ActionNxConntrack that = (ActionNxConntrack) other;

        if (flags != that.flags) {
            return false;
        }
        if (zoneSrc != that.zoneSrc) {
            return false;
        }
        if (conntrackZone != that.conntrackZone) {
            return false;
        }
        if (recircTable != that.recircTable) {
            return false;
        }
        return ctActions.equals(that.ctActions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + flags;
        result = 31 * result + (int) (zoneSrc ^ zoneSrc >>> 32);
        result = 31 * result + conntrackZone;
        result = 31 * result + recircTable;
        result = 31 * result + ctActions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ActionNxConntrack [flags=" + flags + ", zoneSrc=" + zoneSrc + ", conntrackZone=" + conntrackZone
                + ", recircTable=" + recircTable + ", ctActions=" + ctActions + ", getActionKey()=" + getActionKey()
                + "]";
    }

    public interface NxCtAction {
        CtActions buildCtActions();
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

        public int getFlags() {
            return flags;
        }

        public int getRangePresent() {
            return rangePresent;
        }

        public IpAddress getIpAddressMin() {
            return ipAddressMin;
        }

        public IpAddress getIpAddressMax() {
            return ipAddressMax;
        }

        public int getPortMin() {
            return portMin;
        }

        public int getPortMax() {
            return portMax;
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

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            NxNat that = (NxNat) other;

            if (flags != that.flags) {
                return false;
            }
            if (rangePresent != that.rangePresent) {
                return false;
            }
            if (ipAddressMin != null ? !ipAddressMin.equals(that.ipAddressMin) : that.ipAddressMin != null) {
                return false;
            }
            if (ipAddressMax != null ? !ipAddressMax.equals(that.ipAddressMax) : that.ipAddressMax != null) {
                return false;
            }
            if (portMin != that.portMin) {
                return false;
            }
            return portMax == that.portMax;
        }

        @Override
        public int hashCode() {
            int result = flags;
            result = 31 * result + rangePresent;
            result = 31 * result + ipAddressMin.hashCode();
            result = 31 * result + ipAddressMax.hashCode();
            result = 31 * result + portMin;
            result = 31 * result + portMax;
            return result;
        }

        @Override
        public String toString() {
            return "NxNat [flags=" + flags + ", rangePresent=" + rangePresent + ", ipAddressMin=" + ipAddressMin
                    + ", ipAddressMax=" + ipAddressMax + ", portMin=" + portMin + ", portMax=" + portMax + "]";
        }
    }

    public static class NxCtMark implements NxCtAction {
        private final long ctMark;

        public NxCtMark(long ctMark) {
            this.ctMark = ctMark;
        }

        public long getCtMark() {
            return ctMark;
        }

        @Override
        public CtActions buildCtActions() {
            NxActionCtMarkBuilder nxActionCtMarkBuilder = new NxActionCtMarkBuilder()
                    .setCtMark(ctMark);

            CtActionsBuilder ctActionsBuilder = new CtActionsBuilder();
            NxActionCtMarkCaseBuilder caseBuilder = new NxActionCtMarkCaseBuilder();
            caseBuilder.setNxActionCtMark(nxActionCtMarkBuilder.build());
            ctActionsBuilder.setOfpactActions(caseBuilder.build());
            return ctActionsBuilder.build();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            NxCtMark that = (NxCtMark) other;

            return ctMark == that.ctMark;
        }

        @Override
        public int hashCode() {
            return 31 * (int) (ctMark ^ ctMark >>> 32);
        }
    }
}
