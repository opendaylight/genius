/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagsMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagsMatchBuilder;

/**
 * TCP flags match.
 */
public class MatchTcpFlags extends MatchInfoHelper<TcpFlagsMatch, TcpFlagsMatchBuilder> {
    public static final MatchTcpFlags SYN = new MatchTcpFlags(1 << 1);
    public static final MatchTcpFlags ACK = new MatchTcpFlags(1 << 4);
    public static final MatchTcpFlags SYN_ACK = new MatchTcpFlags((1 << 1) + (1 << 4));

    private final int flags;

    public MatchTcpFlags(int flags) {
        super(MatchFieldType.tcp_flags, new long[] {flags});
        this.flags = flags;
    }

    /**
     * Create an instance; this constructor is only present for XtendBeanGenerator and must not be used.
     */
    @Deprecated
    public MatchTcpFlags(BigInteger[] bigMatchValues, int flags, MatchFieldType matchField, long[] matchValues,
            String[] stringMatchValues) {
        this(flags);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MatchTcpFlags that = (MatchTcpFlags) o;

        return flags == that.flags;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + flags;
        return result;
    }
}
