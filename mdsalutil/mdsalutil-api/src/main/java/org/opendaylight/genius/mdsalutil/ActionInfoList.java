/*
 * Copyright © 2016, 2017 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

/**
 * List&lt;ActionInfo&gt; with method to build List&lt;Action&gt; from it.
 *
 * @author Michael Vorburger
 */
public class ActionInfoList implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<ActionInfo> actionInfos = new ArrayList<>();

    public ActionInfoList(List<ActionInfo> actionInfos) {
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
            actions.add(actionInfo.buildAction(newActionKey++));
        }
        return actions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ActionInfoList that = (ActionInfoList) other;

        return actionInfos != null ? actionInfos.equals(that.actionInfos) : that.actionInfos == null;
    }

    @Override
    public int hashCode() {
        return actionInfos != null ? actionInfos.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ActionInfoList" + actionInfos.toString();
    }
}
