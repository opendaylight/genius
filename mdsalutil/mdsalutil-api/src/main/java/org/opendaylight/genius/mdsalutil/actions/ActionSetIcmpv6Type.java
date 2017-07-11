/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
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
 * Set ICMPv6 type action.
 */
public class ActionSetIcmpv6Type extends ActionInfo {
    private final short type;

    public ActionSetIcmpv6Type(short type) {
        this(0, type);
    }

    public ActionSetIcmpv6Type(int actionKey, short type) {
        super(actionKey);
        this.type = type;
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
                        .setIcmpv6Type(type)
                        .build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public short getType() {
        return type;
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

        ActionSetIcmpv6Type that = (ActionSetIcmpv6Type) other;

        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + type;
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetIcmpv6Type [type=" + type + ", getActionKey()=" + getActionKey() + "]";
    }

}
