/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

/**
 * Set VLAN VID field action.
 */
public class ActionSetFieldVlanVid extends ActionInfo {

    private final int vlanId;

    public ActionSetFieldVlanVid(int vlanId) {
        this(0, vlanId);
    }

    public ActionSetFieldVlanVid(int actionKey, int vlanId) {
        super(actionKey);
        this.vlanId = vlanId;
    }

    public int getVlanId() {
        return vlanId;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
            .setAction(
                new SetFieldCaseBuilder()
                    .setSetField(
                        new SetFieldBuilder()
                            .setVlanMatch(
                                new VlanMatchBuilder()
                                    .setVlanId(
                                        new VlanIdBuilder()
                                            .setVlanId(new VlanId(vlanId))
                                            .setVlanIdPresent(true)
                                            .build())
                                    .build())
                            .build())
                    .build())
            .setKey(new ActionKey(newActionKey))
            .build();
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

        ActionSetFieldVlanVid that = (ActionSetFieldVlanVid) other;

        return vlanId == that.vlanId;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + vlanId;
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetFieldVlanVid [vlanId=" + vlanId + ", getActionKey()=" + getActionKey() + "]";
    }
}
