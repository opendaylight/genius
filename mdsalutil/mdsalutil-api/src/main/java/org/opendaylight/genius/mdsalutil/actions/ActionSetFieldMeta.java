/*
 * Copyright (c) 2017 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;

public class ActionSetFieldMeta extends ActionInfo {
    private final BigInteger metadataValue;

    public ActionSetFieldMeta(BigInteger metadataValue) {
        this(0, metadataValue);
    }

    public ActionSetFieldMeta(int actionKey, BigInteger metadataValue) {
        super(actionKey);
        this.metadataValue = metadataValue;
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder().setAction(
                new SetFieldCaseBuilder().setSetField(
                        new SetFieldBuilder().setMetadata(
                                new MetadataBuilder().setMetadata(metadataValue).build())
                                .build()).build()).setKey(new ActionKey(newActionKey)).build();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        ActionSetFieldMeta that = (ActionSetFieldMeta) other;

        return metadataValue == that.metadataValue;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + metadataValue.hashCode();
        return result;
    }
}
