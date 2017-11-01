/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagsMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagsMatchBuilder;

/**
 * TCP flags match.
 */
public class MatchTcpFlags extends MatchInfoHelper<TcpFlagsMatch, TcpFlagsMatchBuilder> {
    private static final long serialVersionUID = -2805788931166901765L;

    public static final MatchTcpFlags SYN = new MatchTcpFlags(1 << 1);
    public static final MatchTcpFlags ACK = new MatchTcpFlags(1 << 4);
    public static final MatchTcpFlags SYN_ACK = new MatchTcpFlags((1 << 1) + (1 << 4));

    private final int flags;

    public MatchTcpFlags(int flags) {
        this.flags = flags;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, TcpFlagsMatch value) {
        matchBuilder.setTcpFlagsMatch(value);
    }

    @Override
    protected void populateBuilder(TcpFlagsMatchBuilder builder) {
        builder.setTcpFlags(flags);
    }

    public int getFlags() {
        return flags;
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

        MatchTcpFlags that = (MatchTcpFlags) other;

        return flags == that.flags;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + flags;
        return result;
    }

    @Override
    public String toString() {
        return "MatchTcpFlags[" + flags + "]";
    }

}
