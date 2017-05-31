/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.io.Serializable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

public abstract class ActionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int actionKey;

    public ActionInfo(int actionKey) {
        this.actionKey = actionKey;
    }

    public int getActionKey() {
        return actionKey;
    }

    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public abstract Action buildAction(int newActionKey);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "ActionInfo{actionKey = " + actionKey + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ActionInfo that = (ActionInfo) other;

        return actionKey == that.actionKey;
    }

    @Override
    public int hashCode() {
        return actionKey;
    }
}
