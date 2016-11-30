/*
 * Copyright (c) 2016 Red Hat and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import java.math.BigInteger;
import org.opendaylight.yangtools.concepts.Builder;

/**
 * Builder for ActionInfo.
 * This class, even if not directly called from anywhere statically, is needed
 * by the {@link XtendBeanGenerator} in order to be able to generate code which creates
 * ActionInfo instances.  This is because the XtendBeanGenerator cannot figure out by
 * itself which constructor to call for instances of this type.
 *
 * @author Michael Vorburger
 */
public class ActionInfoBuilder implements Builder<ActionInfo> {

    private ActionType actionType;
    private int actionKey = 0;
    // Do NOT use List<String/BigInteger> here.. XtendBeanGenerator needs this to match with ActionInfo's types
    private String[] actionValues;
    private String[][] actionValuesMatrix;
    private BigInteger[] bigActionValues;

    @Override
    public ActionInfo build() {
        if (actionType == null) {
            throw new IllegalStateException("actionType must be set");
        } else if (actionValues != null && bigActionValues == null) {
            return new ActionInfo(actionType, actionValues, actionKey);
        } else if (actionValues == null && bigActionValues != null) {
            return new ActionInfo(actionType, bigActionValues, actionKey);
        } else if (actionValues != null && bigActionValues != null) {
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

    public String[] getActionValues() {
        return actionValues;
    }

    public void setActionValues(String[] actionValues) {
        this.actionValues = actionValues;
    }

    public String[][] getActionValuesMatrix() {
        return actionValuesMatrix;
    }

    public void setActionValuesMatrix(String[][] actionValuesMatrix) {
        this.actionValuesMatrix = actionValuesMatrix;
    }

    public BigInteger[] getBigActionValues() {
        return bigActionValues;
    }

    public void setBigActionValues(BigInteger[] bigActionValues) {
        this.bigActionValues = bigActionValues;
    }
}
