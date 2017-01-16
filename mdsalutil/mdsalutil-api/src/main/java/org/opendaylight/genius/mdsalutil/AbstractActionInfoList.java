/*
 * Copyright © 2016, 2017 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

/**
 * List&lt;ActionInfo&gt; with method to build List&lt;Action&gt; from it.
 *
 * @author Michael Vorburger
 */
public abstract class AbstractActionInfoList {

    private final List<ActionInfo> actionInfos = new ArrayList<>();

    protected AbstractActionInfoList(List<ActionInfo> actionInfos) {
        super();
        if (actionInfos != null) {
            this.actionInfos.addAll(actionInfos);
        }
    }

    public List<ActionInfo> getActionInfos() {
        return Collections.unmodifiableList(actionInfos);
    }

    public List<Action> buildActions() {
        int newActionKey = 0;
        List<Action> actions = new ArrayList<>(actionInfos.size());
        for (ActionInfo actionInfo: actionInfos) {
            ActionType actionType = actionInfo.getActionType();
            actions.add(actionType.buildAction(newActionKey++, actionInfo));
        }
        return actions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractActionInfoList that = (AbstractActionInfoList) o;

        return actionInfos != null ? actionInfos.equals(that.actionInfos) : that.actionInfos == null;
    }

    @Override
    public int hashCode() {
        return actionInfos != null ? actionInfos.hashCode() : 0;
    }
}
