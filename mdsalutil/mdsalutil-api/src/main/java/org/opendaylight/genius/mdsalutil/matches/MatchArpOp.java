/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;

/**
 * ARP OP match.
 */
public class MatchArpOp extends MatchInfoHelper<ArpMatch, ArpMatchBuilder> {
    private static final long serialVersionUID = -2425999612387545525L;

    public static final MatchArpOp REQUEST = new MatchArpOp(NwConstants.ARP_REQUEST);
    public static final MatchArpOp REPLY = new MatchArpOp(NwConstants.ARP_REPLY);

    private final int op;

    public MatchArpOp(int op) {
        this.op = op;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, ArpMatch value) {
        matchBuilder.setLayer3Match(value);
    }

    @Override
    protected void populateBuilder(ArpMatchBuilder builder) {
        builder.setArpOp(op);
    }

    public int getOp() {
        return op;
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

        MatchArpOp that = (MatchArpOp) other;

        return op == that.op;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + op;
        return result;
    }

    @Override
    public String toString() {
        return "MatchArpOp[" + op + "]";
    }
}
