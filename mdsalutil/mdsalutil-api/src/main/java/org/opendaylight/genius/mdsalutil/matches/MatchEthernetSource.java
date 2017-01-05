/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

/**
 * Ethernet source match.
 */
public class MatchEthernetSource extends MatchInfoHelper<EthernetMatch, EthernetMatchBuilder> {
    private final MacAddress address;

    public MatchEthernetSource(MacAddress address) {
        super(MatchFieldType.eth_src, new String[] {address.getValue()});
        this.address = address;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, EthernetMatch value) {
        matchBuilder.setEthernetMatch(value);
    }

    @Override
    protected void populateBuilder(EthernetMatchBuilder builder) {
        builder.setEthernetSource(new EthernetSourceBuilder().setAddress(address).build());
    }

    public MacAddress getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MatchEthernetSource that = (MatchEthernetSource) o;

        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}
