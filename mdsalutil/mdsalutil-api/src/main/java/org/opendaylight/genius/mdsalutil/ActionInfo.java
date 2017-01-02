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

    private final int m_actionKey;

    public ActionInfo(int actionKey) {
        m_actionKey = actionKey;
    }

    public int getActionKey() {
        return m_actionKey;
    }

    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public abstract Action buildAction(int actionKey);

    @Override
    public String toString() {
        return "ActionInfo{actionKey = " + m_actionKey + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionInfo that = (ActionInfo) o;

        return m_actionKey == that.m_actionKey;
    }

    @Override
    public int hashCode() {
        return m_actionKey;
    }
}
