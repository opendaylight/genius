/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;

/**
 * ARP target transport address match.
 */
public class MatchArpTpa extends MatchInfoHelper<ArpMatch, ArpMatchBuilder> {
    private static final long serialVersionUID = 7430165617164633413L;

    private final Ipv4Prefix address;

    public MatchArpTpa(Ipv4Prefix address) {
        this.address = address;
    }

    public MatchArpTpa(long ip, long mask) {
        this(new Ipv4Prefix(NWUtil.longToIpv4(ip, mask)));
    }

    public MatchArpTpa(String ip, String mask) {
        this(new Ipv4Prefix(ip + "/" + mask));
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, ArpMatch value) {
        matchBuilder.setLayer3Match(value);
    }

    @Override
    protected void populateBuilder(ArpMatchBuilder builder) {
        builder.setArpTargetTransportAddress(address);
    }

    public Ipv4Prefix getAddress() {
        return address;
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

        MatchArpTpa that = (MatchArpTpa) other;

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
        return "MatchArpTpa[" + address + "]";
    }

}
