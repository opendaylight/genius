/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;

/**
 * Pop MPLS action.
 */
public class ActionPopMpls extends ActionInfo {

    private final int etherType;

    public ActionPopMpls(int etherType) {
        this(0, etherType);
    }

    public ActionPopMpls(int actionKey, int etherType) {
        super(actionKey);
        this.etherType = etherType;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder().setAction(
                new PopMplsActionCaseBuilder().setPopMplsAction(
                        new PopMplsActionBuilder().setEthernetType(etherType)
                                .build()).build())
                .setKey(new ActionKey(newActionKey)).build();
    }

    public int getType() {
        return etherType;
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

        ActionPopMpls that = (ActionPopMpls) other;

        return etherType == that.etherType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + etherType;
        return result;
    }

    @Override
    public String toString() {
        return "ActionPopMpls [type=" + etherType + ", getActionKey()=" + getActionKey() + "]";
    }

}
