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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

/**
 * Ethernet destination match.
 */
public class MatchEthernetDestination extends MatchInfoHelper<EthernetMatch, EthernetMatchBuilder> {

    private final MacAddress address;
    private final MacAddress mask;

    public MatchEthernetDestination(MacAddress address) {
        this(address, null);
    }

    public MatchEthernetDestination(MacAddress address, MacAddress mask) {
        this.address = address;
        this.mask = mask;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, EthernetMatch value) {
        matchBuilder.setEthernetMatch(value);
    }

    @Override
    protected void populateBuilder(EthernetMatchBuilder builder) {
        EthernetDestinationBuilder ethernetDestinationBuilder = new EthernetDestinationBuilder().setAddress(address);
        if (mask != null) {
            ethernetDestinationBuilder.setMask(mask);
        }
        builder.setEthernetDestination(ethernetDestinationBuilder.build());
    }

    public MacAddress getAddress() {
        return address;
    }

    public MacAddress getMask() {
        return mask;
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

        MatchEthernetDestination that = (MatchEthernetDestination) other;

        if (address != null ? !address.equals(that.address) : that.address != null) {
            return false;
        }
        return mask != null ? mask.equals(that.mask) : that.mask == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (mask != null ? mask.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MatchEthernetDestination[address=" + address + ", mask=" + mask + "]";
    }

    @Override
    public int compareTo(MatchInfoBase other) {
        return compareTo(other, new Comparator<MatchEthernetDestination>() {
            @Override
            public int compare(MatchEthernetDestination left, MatchEthernetDestination right) {
                return ComparisonChain.start()
                  .compare(left.address.getValue(), right.address.getValue())
                  .compare(left.mask.getValue(),    right.mask.getValue()).result();
            }
        });
    }

}
