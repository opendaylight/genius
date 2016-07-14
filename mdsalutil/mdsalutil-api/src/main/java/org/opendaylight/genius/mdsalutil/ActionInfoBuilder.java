/*
 * Copyright (c) 2016 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import org.opendaylight.yangtools.concepts.Builder;

/**
 * Builder for ActionInfo.
 * This class, even if not directly called from anywhere statically, is needed
 * by the XtendBeanGenerator in order to be able to generate code which creates
 * ActionInfo instances.
 */
public class ActionInfoBuilder implements Builder<ActionInfo> {

    private ActionType actionType;
    private int actionKey = 0;
    // Don't use List here, but array, as in ActionInfo
    // Otherwise (with List) it's impossible for XtendBeanGenerator to deal w. null/empty List default values correctly
    private String[] actionValues;
    private BigInteger[] bigActionValues;

    public ActionInfo build() {
        if (actionType == null) {
            throw new IllegalStateException("actionType must be set");
        } else if (actionValues != null && bigActionValues == null) {
            return new ActionInfo(actionType, actionValues, actionKey);
        } else if (actionValues == null && bigActionValues != null) {
            return new ActionInfo(actionType, bigActionValues, actionKey);
        } else if (actionValues != null && bigActionValues != null) {
            throw new IllegalStateException("Cannot set both actionValues and bigActionValues");
        } else if (actionValues == null && bigActionValues == null) {
            throw new IllegalStateException("Must set either actionValues and bigActionValues");
        } else {
            throw new IllegalStateException("WTF?!");
        }
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public int getActionKey() {
        return actionKey;
    }

    public void setActionKey(int actionKey) {
        this.actionKey = actionKey;
    }

    public String[] getActionValues() {
        return actionValues;
    }

    public void setActionValues(String[] actionValues) {
        this.actionValues = actionValues;
    }

    public void setBigActionValues(BigInteger[] bigActionValues) {
        this.bigActionValues = bigActionValues;
    }

    public BigInteger[] getBigActionValues() {
        return bigActionValues;
    }

}
