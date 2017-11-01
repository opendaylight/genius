/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

/**
 * IPv6 destination match.
 */
public class MatchIpv6Destination extends MatchInfoHelper<Ipv6Match, Ipv6MatchBuilder> {
    private static final long serialVersionUID = -7921403906024639394L;

    private final Ipv6Prefix prefix;

    public MatchIpv6Destination(String address) {
        this.prefix = new Ipv6Prefix(address);
    }

    public MatchIpv6Destination(Ipv6Prefix prefix) {
        this.prefix = prefix;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, Ipv6Match value) {
        matchBuilder.setLayer3Match(value);
    }

    @Override
    protected void populateBuilder(Ipv6MatchBuilder builder) {
        builder.setIpv6Destination(prefix);
    }

    public Ipv6Prefix getPrefix() {
        return prefix;
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

        MatchIpv6Destination that = (MatchIpv6Destination) other;

        return prefix != null ? prefix.equals(that.prefix) : that.prefix == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MatchIpv6Destination[" + prefix + "]";
    }

}
