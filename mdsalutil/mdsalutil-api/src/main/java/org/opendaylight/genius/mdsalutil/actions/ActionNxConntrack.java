/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
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
        super(ActionType.nx_conntrack, new String[] {Integer.toString(flags), Long.toString(zoneSrc), Integer.toString(
                conntrackZone), Short.toString(recircTable)}, actionKey);
        this.flags = flags;
        this.zoneSrc = zoneSrc;
        this.conntrackZone = conntrackZone;
        this.recircTable = recircTable;
    }

    @Deprecated
    public ActionNxConntrack(String[] actionValues) {
        this(Integer.parseInt(actionValues[0]), Long.parseLong(actionValues[1]), Integer.parseInt(actionValues[2]),
                Short.parseShort(actionValues[3]));
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
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxConntrack(ctb.build()).build());
        ab.setKey(new ActionKey(newActionKey));
        return ab.build();
    }
}
