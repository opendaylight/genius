/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import com.google.common.collect.ComparisonChain;
import java.util.Comparator;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;

/**
 * ARP source hardware address match.
 */
public class MatchArpSha extends MatchInfoHelper<ArpMatch, ArpMatchBuilder> {

    private final MacAddress address;

    public MatchArpSha(MacAddress address) {
        this.address = address;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, ArpMatch value) {
        matchBuilder.setLayer3Match(value);
    }

    @Override
    protected void populateBuilder(ArpMatchBuilder builder) {
        builder.setArpSourceHardwareAddress(new ArpSourceHardwareAddressBuilder().setAddress(address).build());
    }

    public MacAddress getAddress() {
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

        MatchArpSha that = (MatchArpSha) other;

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
        return "MatchArpSha[" + address + "]";
    }

    @Override
    public int compareTo(MatchInfoBase other) {
        return compareTo(other, new Comparator<MatchArpSha>() {
            @Override
            public int compare(MatchArpSha left, MatchArpSha right) {
                return ComparisonChain.start()
                  .compare(left.address, right.address, MacAddressComparator.INSTANCE).result();
            }
        });
    }

}
