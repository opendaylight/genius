/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.NxConntrackBuilder;

/**
 * NX conntrack action.
 */
public class ActionNxConntrack extends ActionInfo {
    private final int flags;
    private final long zoneSrc;
    private final int conntrackZone;
    private final short recircTable;

    public ActionNxConntrack(int flags, long zoneSrc, int conntrackZone, short recircTable) {
        this(0, flags, zoneSrc, conntrackZone, recircTable);
    }

    public ActionNxConntrack(int actionKey, int flags, long zoneSrc, int conntrackZone, short recircTable) {
        super(actionKey);
        this.flags = flags;
        this.zoneSrc = zoneSrc;
        this.conntrackZone = conntrackZone;
        this.recircTable = recircTable;
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
        return recircTable == that.recircTable;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + flags;
        result = 31 * result + (int) (zoneSrc ^ zoneSrc >>> 32);
        result = 31 * result + conntrackZone;
        result = 31 * result + recircTable;
        return result;
    }
}
