/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;

/**
 * IPv4 destination match.
 */
public class MatchIpv4Destination extends MatchInfoHelper<Ipv4Match, Ipv4MatchBuilder> {
    private final Ipv4Prefix prefix;

    public MatchIpv4Destination(Ipv4Prefix prefix) {
        this.prefix = prefix;
    }

    public MatchIpv4Destination(long ip, long mask) {
        this.prefix = new Ipv4Prefix(MDSALUtil.longToIp(ip, mask));
    }

    public MatchIpv4Destination(String ip, String mask) {
        this.prefix = new Ipv4Prefix(ip + "/" + mask);
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, Ipv4Match value) {
        matchBuilder.setLayer3Match(value);
    }

    @Override
    protected void populateBuilder(Ipv4MatchBuilder builder) {
        builder.setIpv4Destination(prefix);
    }

    public Ipv4Prefix getPrefix() {
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

        MatchIpv4Destination that = (MatchIpv4Destination) other;

        return prefix != null ? prefix.equals(that.prefix) : that.prefix == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        return result;
    }
}
