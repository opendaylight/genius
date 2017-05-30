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
 * implementation of equals(), hashCode() and toString().
 */
/* can remain package local instead of public (unless there are InstructionInfo impls elsewhere?) */
abstract class AbstractInstructionInfoImpl implements InstructionInfo {
    private static final long serialVersionUID = 1L;

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

}
