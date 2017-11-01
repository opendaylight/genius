/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv6MatchBuilder;

/**
 * ICMPv6 match.
 */
public class MatchIcmpv6 extends MatchInfoHelper<Icmpv6Match, Icmpv6MatchBuilder> {
    private static final long serialVersionUID = -3950935400381748445L;

    private final short type;
    private final short code;

    public MatchIcmpv6(short type, short code) {
        this.type = type;
        this.code = code;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, Icmpv6Match value) {
        matchBuilder.setIcmpv6Match(value);
    }

    @Override
    protected void populateBuilder(Icmpv6MatchBuilder builder) {
        builder.setIcmpv6Type(type).setIcmpv6Code(code);
    }

    public short getType() {
        return type;
    }

    public short getCode() {
        return code;
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

        MatchIcmpv6 that = (MatchIcmpv6) other;

        if (type != that.type) {
            return false;
        }
        return code == that.code;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + type;
        result = 31 * result + code;
        return result;
    }

    @Override
    public String toString() {
        return "MatchIcmpv6[type=" + type + ", code=" + code + "]";
    }

}
