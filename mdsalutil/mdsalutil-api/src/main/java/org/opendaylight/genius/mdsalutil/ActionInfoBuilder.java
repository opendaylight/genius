/*
 * Copyright (c) 2016 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
    private List<String> actionValues = new ArrayList<>();
    private List<BigInteger> bigActionValues = new ArrayList<>();

    public ActionInfo build() {
        if (actionType == null) {
            throw new IllegalStateException("actionType must be set");
        } else if (!actionValues.isEmpty() && bigActionValues.isEmpty()) {
            return new ActionInfo(actionType, actionValues.toArray(new String[] {}), actionKey);
        } else if (actionValues.isEmpty() && !bigActionValues.isEmpty()) {
            return new ActionInfo(actionType, bigActionValues.toArray(new BigInteger[] {}), actionKey);
        } else if (!actionValues.isEmpty() && !bigActionValues.isEmpty()) {
            throw new IllegalStateException("Cannot set (add to) both actionValues and bigActionValues");
        } else {
            throw new IllegalStateException("Must add to either actionValues or bigActionValues");
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

    public List<String> getActionValues() {
        return actionValues;
    }

    public List<BigInteger> getBigActionValues() {
        return bigActionValues;
    }

}
