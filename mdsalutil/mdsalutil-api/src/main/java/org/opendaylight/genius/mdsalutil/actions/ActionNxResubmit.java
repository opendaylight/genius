/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.add.group.input.buckets.bucket.action.action.NxActionResubmitRpcAddGroupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmitBuilder;

/**
 * NX resubmit action.
 */
public class ActionNxResubmit extends ActionInfo {

    private final short table;

    public ActionNxResubmit() {
        this(0, (short) 0);
    }

    public ActionNxResubmit(short table) {
        this(0, table);
    }

    public ActionNxResubmit(int actionKey, short table) {
        super(actionKey);
        this.table = table;
    }

    public short getTable() {
        return table;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        NxResubmitBuilder nxarsb = new NxResubmitBuilder();
        nxarsb.setTable(table);
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new NxActionResubmitRpcAddGroupCaseBuilder().setNxResubmit(nxarsb.build()).build());
        ab.setKey(new ActionKey(newActionKey));
        return ab.build();
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

        ActionNxResubmit that = (ActionNxResubmit) other;

        return table == that.table;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + table;
        return result;
    }

    @Override
    public String toString() {
        return "ActionNxResubmit [actionKey=" + getActionKey() + ", table=" + table + "]";
    }

}
