/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yangtools.util.MoreObjects2;

public class ActionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ActionType m_actionType;

    private String[] m_asActionValues = null;
    private BigInteger[] m_aBigIntValues;
    private int m_actionKey = 0;

    public ActionInfo(ActionInfo action) {
        super();
        m_actionType = action.m_actionType;
        m_actionKey = action.m_actionKey;
        m_asActionValues = Arrays.copyOf(action.m_asActionValues, action.m_asActionValues.length);
    }

    public ActionInfo(ActionType actionType, String[] asActionValues) {
        m_actionType = actionType;
        m_actionKey = 0;
        m_asActionValues = asActionValues;
    }

    public ActionInfo(ActionType actionType, String[] asActionValues, int actionKey) {
        m_actionType = actionType;
        m_actionKey = actionKey;
        m_asActionValues = asActionValues;
    }

    public ActionInfo(ActionType actionType, BigInteger[] aBigIntValues) {
        m_actionType = actionType;
        m_actionKey = 0;
        m_aBigIntValues = aBigIntValues;
    }

    public ActionInfo(ActionType actionType, BigInteger[] aBigIntValues, int actionKey) {
        m_actionType = actionType;
        m_actionKey = actionKey;
        m_aBigIntValues = aBigIntValues;
    }

    public void setActionKey(int key) {
        m_actionKey = key;
    }

    public int getActionKey() {
        return m_actionKey;
    }

    public Action buildAction() {
        return m_actionType.buildAction(this);
    }

    public ActionType getActionType() {
        return m_actionType;
    }

    public String[] getActionValues() {
        return m_asActionValues;
    }

    public BigInteger[] getBigActionValues() {
        return m_aBigIntValues;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("actionType", m_actionType)
                .add("actionValues", Arrays.deepToString(m_asActionValues))
                .add("bigActionValues", Arrays.deepToString(m_aBigIntValues))
                .add("actionKey", m_actionKey).toString();
    }

    @Override
    public int hashCode() {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.hashCode(). deepHashCode() would have to be used for nested
        // arrays.
        return Objects.hash(m_actionType, Arrays.hashCode(m_asActionValues), Arrays.hashCode(m_aBigIntValues),
                m_actionKey);
    }

    @Override
    public boolean equals(Object obj) {
        // BEWARE, Caveat Emptor: Array ([]) type fields must use
        // Arrays.equals(). deepEquals() would have to be used for nested
        // arrays. Use == only for primitive types; if ever changing
        // those field types, must change to Objects.equals.
        return MoreObjects2.equalsHelper(this, obj,
            (self, other) -> Objects.equals(self.m_actionType, other.m_actionType)
                          && Arrays.equals(self.m_asActionValues, other.m_asActionValues)
                          && Arrays.equals(self.m_aBigIntValues, other.m_aBigIntValues)
                          && self.m_actionKey == other.m_actionKey);
    }
}
