/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.io.Serializable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

public abstract class AbstractActionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ActionType actionType;

    protected AbstractActionInfo(ActionType actionType) {
        this.actionType = actionType;
    }

    public final ActionType getActionType() {
        return actionType;
    }

    public abstract Action buildAction();

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
