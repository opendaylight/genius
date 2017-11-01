/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.genius.mdsalutil.packet.IPProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;

/**
 * IP protocol match.
 */
public class MatchIpProtocol extends MatchInfoHelper<IpMatch, IpMatchBuilder> {
    private static final long serialVersionUID = 6050181624776409340L;

    public static final MatchIpProtocol TCP = new MatchIpProtocol(IPProtocols.TCP.shortValue());
    public static final MatchIpProtocol UDP = new MatchIpProtocol(IPProtocols.UDP.shortValue());
    public static final MatchIpProtocol ICMP = new MatchIpProtocol(IPProtocols.ICMP.shortValue());
    public static final MatchIpProtocol ICMPV6 = new MatchIpProtocol(IPProtocols.IPV6ICMP.shortValue());

    private final short protocol;

    public MatchIpProtocol(short protocol) {
        this.protocol = protocol;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, IpMatch value) {
        matchBuilder.setIpMatch(value);
    }

    @Override
    protected void populateBuilder(IpMatchBuilder builder) {
        builder.setIpProtocol(protocol);
    }

    public short getProtocol() {
        return protocol;
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

        MatchIpProtocol that = (MatchIpProtocol) other;

        return protocol == that.protocol;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + protocol;
        return result;
    }

    @Override
    public String toString() {
        return "MatchIpProtocol[" + protocol + "]";
    }

}
