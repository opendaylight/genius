/*
 * Copyright (c) 2018 Alten Calsoftlabs India Pvt Ltd. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv6MatchBuilder;

/**
 * Set ICMPv6 code action.
 */
public class ActionSetIcmpv6Code extends ActionInfo {

    private final short code;

    public ActionSetIcmpv6Code(short code) {
        this(0, code);
    }

    public ActionSetIcmpv6Code(int actionKey, short code) {
        super(actionKey);
        this.code = code;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
                .setAction(new SetFieldCaseBuilder()
                        .setSetField(new SetFieldBuilder()
                                .setIcmpv6Match(new Icmpv6MatchBuilder()
                                        .setIcmpv6Type(code)
                                        .build())
                                .build())
                        .build())
                .setKey(new ActionKey(newActionKey))
                .build();
    }

    public short getCode() {
        return code;
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

        ActionSetIcmpv6Code that = (ActionSetIcmpv6Code) other;

        return code == that.code;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + code;
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetIcmpv6Code [code=" + code + ", getActionKey()=" + getActionKey() + "]";
    }

}
