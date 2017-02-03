/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;

public abstract class AbstractSwitchEntity {
    private static final long serialVersionUID = 1L;

    private BigInteger dpnId;

    public AbstractSwitchEntity(BigInteger dpnId) {
        this.dpnId = dpnId;
    }

    public BigInteger getDpnId() {
        return dpnId;
    }

    public void setDpnId(BigInteger dpnId) {
        this.dpnId = dpnId;
    }

    // Force subclasses to implement hashCode() & equals() WITH m_dpnId!
    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
