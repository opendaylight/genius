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

    public BigInteger getMetadataValue() {
        return metadataValue;
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder().setAction(
                new SetFieldCaseBuilder().setSetField(
                        new SetFieldBuilder().setMetadata(
                                new MetadataBuilder().setMetadata(metadataValue).build())
                                .build()).build()).withKey(new ActionKey(newActionKey)).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ActionSetFieldMeta other = (ActionSetFieldMeta) obj;
        if (metadataValue == null) {
            if (other.metadataValue != null) {
                return false;
            }
        } else if (!metadataValue.equals(other.metadataValue)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (metadataValue == null ? 0 : metadataValue.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetFieldMeta [metadataValue=" + metadataValue + ", getActionKey()=" + getActionKey() + "]";
    }

}
