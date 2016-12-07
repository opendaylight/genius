/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;

/**
 * Action to load an NXM register.
 */
public class ActionRegLoad extends ActionInfo {
    private final Class<? extends NxmNxReg> register;
    private final int start;
    private final int end;
    private final long load;

    public ActionRegLoad(Class<? extends NxmNxReg> register, int start, int end, long load) {
        this(0, register, start, end, load);
    }

    public ActionRegLoad(int actionKey, Class<? extends NxmNxReg> register, int start, int end, long load) {
        super(ActionType.nx_load_reg, new String[0], actionKey);
        this.register = register;
        this.start = start;
        this.end = end;
        this.load = load;
    }

    @Deprecated
    public ActionRegLoad(Class<? extends NxmNxReg> register, String[] actionValues) {
        this(register, Integer.parseInt(actionValues[0]), Integer.parseInt(actionValues[1]),
                Long.parseLong(actionValues[2]));
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        Dst dst = new DstBuilder().setDstChoice(new DstNxRegCaseBuilder().setNxReg(register).build())
                .setStart(start)
                .setEnd(end)
                .build();
        NxRegLoadBuilder nxRegLoadBuilder = new NxRegLoadBuilder().setDst(dst).setValue(BigInteger.valueOf(load));

        return new ActionBuilder()
                .setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegLoad(
                        nxRegLoadBuilder.build()).build())
                .setKey(new ActionKey(newActionKey))
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

    public long getLoad() {
        return load;
    }
}
