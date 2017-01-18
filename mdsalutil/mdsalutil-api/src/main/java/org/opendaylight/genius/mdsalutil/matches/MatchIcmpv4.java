/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;

/**
 * ICMPv4 match.
 */
public class MatchIcmpv4 extends MatchInfoHelper<Icmpv4Match, Icmpv4MatchBuilder> {
    private final short type;
    private final short code;

    public MatchIcmpv4(short type, short code) {
        this.type = type;
        this.code = code;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, Icmpv4Match value) {
        matchBuilder.setIcmpv4Match(value);
    }

    @Override
    protected void populateBuilder(Icmpv4MatchBuilder builder) {
        builder.setIcmpv4Type(type).setIcmpv4Code(code);
    }

    public short getType() {
        return type;
    }

    public short getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MatchIcmpv4 that = (MatchIcmpv4) o;

        if (type != that.type) return false;
        return code == that.code;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) type;
        result = 31 * result + (int) code;
        return result;
    }
}
