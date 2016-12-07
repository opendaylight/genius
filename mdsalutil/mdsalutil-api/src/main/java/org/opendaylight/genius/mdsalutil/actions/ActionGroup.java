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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;

/**
 * Group action.
 */
public class ActionGroup extends ActionInfo {
    private final long groupId;

    public ActionGroup(long groupId) {
        this(0, groupId);
    }

    public ActionGroup(int actionKey, long groupId) {
        super(ActionType.group, new String[] { Long.toString(groupId)}, actionKey);
        this.groupId = groupId;
    }

    @Deprecated
    public ActionGroup(String[] actionValues) {
        this(Long.parseLong(actionValues[0]));
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder().setAction(
                new GroupActionCaseBuilder().setGroupAction(
                        new GroupActionBuilder().setGroupId(groupId).build()).build())
                .setKey(new ActionKey(newActionKey)).build();
    }

    public long getGroupId() {
        return groupId;
    }
}
