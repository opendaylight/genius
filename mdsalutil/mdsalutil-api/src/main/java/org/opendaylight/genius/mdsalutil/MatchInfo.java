/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.io.Serializable;
import java.util.Comparator;

public abstract class MatchInfo implements Serializable, MatchInfoBase {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    protected final <T> int compareTo(MatchInfoBase other, Comparator<T> comparator) {
        if (this.getClass().equals(other.getClass())) {
            if (this.equals(other)) {
                return 0 ;
            } else {
                return comparator.compare((T) this, (T) other);
            }
        } else {
            return this.getClass().getName().compareTo(other.getClass().getName());
        }
    }
}
