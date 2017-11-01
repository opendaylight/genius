/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.protocol.match.fields.PbbBuilder;

/**
 * PBB ISID match.
 */
public class MatchPbbIsid extends MatchInfoHelper<ProtocolMatchFields, ProtocolMatchFieldsBuilder> {
    private static final long serialVersionUID = -3892452639988430283L;

    private final long isid;

    public MatchPbbIsid(long isid) {
        this.isid = isid;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, ProtocolMatchFields value) {
        matchBuilder.setProtocolMatchFields(value);
    }

    @Override
    protected void populateBuilder(ProtocolMatchFieldsBuilder builder) {
        builder.setPbb(new PbbBuilder().setPbbIsid(isid).build());
    }

    public long getIsid() {
        return isid;
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

        MatchPbbIsid that = (MatchPbbIsid) other;

        return isid == that.isid;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (isid ^ isid >>> 32);
        return result;
    }

    @Override
    public String toString() {
        return "MatchPbbIsid[" + isid + "]";
    }

}
