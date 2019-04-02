/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

/**
 * Match IPv6 ND SLL match.
 */
public class MatchIpv6NdSll extends MatchInfoHelper<Ipv6Match, Ipv6MatchBuilder> {

    private final MacAddress address;

    public MacAddress getAddress() {
        return address;
    }

    public MatchIpv6NdSll(MacAddress address) {
        this.address = address;
    }


    @Override
    protected void applyValue(MatchBuilder matchBuilder, Ipv6Match value) {
        matchBuilder.setLayer3Match(value);
    }

    @Override
    protected void populateBuilder(Ipv6MatchBuilder builder) {
        builder.setIpv6NdSll(address);
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

        MatchIpv6NdSll that = (MatchIpv6NdSll) other;

        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MatchIpv6NdSll[" + address + "]";
    }
}