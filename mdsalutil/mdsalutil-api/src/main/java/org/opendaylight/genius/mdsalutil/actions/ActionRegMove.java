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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfMplsLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Action to move an NXM register.
 */
public class ActionRegMove extends ActionInfo {

    private final Class<? extends NxmNxReg> register;
    private final int start;
    private final int end;

    public ActionRegMove(Class<? extends NxmNxReg> register, int start, int end) {
        this(0, register, start, end);
    }

    public ActionRegMove(int actionKey, Class<? extends NxmNxReg> register, int start, int end) {
        super(actionKey);
        this.register = register;
        this.start = start;
        this.end = end;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        Src src = new SrcBuilder()
                .setSrcChoice(new SrcNxRegCaseBuilder().setNxReg(register).build())
                .setStart(start)
                .setEnd(end)
                .build();

        Dst dst = new DstBuilder()
                .setDstChoice(new DstOfMplsLabelCaseBuilder().setOfMplsLabel(Empty.getInstance()).build())
                .setStart(start)
                .setEnd(end)
                .build();

        NxRegMoveBuilder nxRegMoveBuilder = new NxRegMoveBuilder().setSrc(src).setDst(dst);
        return new ActionBuilder()
                .setAction(new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegMove(
                        nxRegMoveBuilder.build()).build())
                .withKey(new ActionKey(newActionKey))
                .build();
    }

    public Class<? extends NxmNxReg> getRegister() {
        return register;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
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

        ActionRegMove that = (ActionRegMove) other;

        if (start != that.start) {
            return false;
        }
        if (end != that.end) {
            return false;
        }
        return register != null ? register.equals(that.register) : that.register == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (register != null ? register.hashCode() : 0);
        result = 31 * result + start;
        result = 31 * result + end;
        return result;
    }

    @Override
    public String toString() {
        return "ActionRegMove [register=" + register + ", start=" + start + ", end=" + end + ", getActionKey()="
                + getActionKey() + "]";
    }

}
