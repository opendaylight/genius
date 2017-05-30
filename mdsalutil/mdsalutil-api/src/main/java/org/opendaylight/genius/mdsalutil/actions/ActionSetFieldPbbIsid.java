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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.protocol.match.fields.PbbBuilder;

/**
 * Set PBB ISID field action.
 */
public class ActionSetFieldPbbIsid extends ActionInfo {
    private static final long serialVersionUID = 1L;

    private final long isid;

    public ActionSetFieldPbbIsid(long isid) {
        this(0, isid);
    }

    public ActionSetFieldPbbIsid(int actionKey, long isid) {
        super(actionKey);
        this.isid = isid;
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
                            .setProtocolMatchFields(
                                new ProtocolMatchFieldsBuilder()
                                    .setPbb(new PbbBuilder().setPbbIsid(isid).build())
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

        ActionSetFieldPbbIsid that = (ActionSetFieldPbbIsid) other;

        return isid == that.isid;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (isid ^ isid >>> 32);
        return result;
    }
}
