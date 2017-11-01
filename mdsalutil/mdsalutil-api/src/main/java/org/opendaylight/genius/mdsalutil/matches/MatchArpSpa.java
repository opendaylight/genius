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
 * ARP source transport address match.
 */
public class MatchArpSpa extends MatchInfoHelper<ArpMatch, ArpMatchBuilder> {
    private static final long serialVersionUID = -2758380170787667924L;

    private final Ipv4Prefix address;

    public MatchArpSpa(Ipv4Prefix address) {
        this.address = address;
    }

    public MatchArpSpa(long ip, long mask) {
        this(new Ipv4Prefix(NWUtil.longToIpv4(ip, mask)));
    }

    public MatchArpSpa(String ip, String mask) {
        this(new Ipv4Prefix(ip + "/" + mask));
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, ArpMatch value) {
        matchBuilder.setLayer3Match(value);
    }

    @Override
    protected void populateBuilder(ArpMatchBuilder builder) {
        builder.setArpSourceTransportAddress(address);
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

        MatchArpSpa that = (MatchArpSpa) other;

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
        return "MatchArpSpa[" + address + "]";
    }
}
