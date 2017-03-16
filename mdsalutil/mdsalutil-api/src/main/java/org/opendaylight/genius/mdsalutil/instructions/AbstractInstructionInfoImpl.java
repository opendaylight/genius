/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.instructions;

import org.opendaylight.genius.mdsalutil.InstructionInfo;

/**
 * Abstract base class for InstructionInfo implementations, to enforce
 * implementation of equals(), hashCode() and toString.
 *
 * @author Michael Vorburger.ch
 */
/* can remain package local instead of public (unless there are InstructionInfo impls elsewhere?) */
abstract class AbstractInstructionInfoImpl implements InstructionInfo {

    @Override
    public final boolean equals(Object obj) {
        return equals2(obj);
    }

    @Override
    public final int hashCode() {
        return hashCode2();
    }

    @Override
    public String toString() {
        return toString2();
    }

    protected abstract boolean equals2(Object other);

    protected abstract int hashCode2();

    protected abstract String toString2();
}
