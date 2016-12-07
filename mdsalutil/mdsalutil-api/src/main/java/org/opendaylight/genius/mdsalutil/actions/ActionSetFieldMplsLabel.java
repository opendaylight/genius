/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;

/**
 * Set MPLS label field action.
 */
public class ActionSetFieldMplsLabel extends ActionInfo {
    private final long label;

    public ActionSetFieldMplsLabel(long label) {
        this(0, label);
    }

    public ActionSetFieldMplsLabel(int actionKey, long label) {
        super(ActionType.set_field_mpls_label, new String[] {Long.toString(label)}, actionKey);
        this.label = label;
    }

    public ActionSetFieldMplsLabel(String[] actionValues) {
        super(ActionType.set_field_mpls_label, actionValues);
        this.label = Long.parseLong(actionValues[0]);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
            .setAction(
                new SetFieldCaseBuilder()
                    .setSetField(
                        new SetFieldBuilder()
                            .setProtocolMatchFields(
                                new ProtocolMatchFieldsBuilder().setMplsLabel(label).build())
                            .build())
                    .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }
}
