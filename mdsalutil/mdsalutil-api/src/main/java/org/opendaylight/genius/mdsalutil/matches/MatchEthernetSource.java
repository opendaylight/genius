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
    private final MacAddress mask;

    public MatchEthernetSource(MacAddress address) {
        this(address, null);
    }

    public MatchEthernetSource(MacAddress address, MacAddress mask) {
        super(MatchFieldType.eth_src,
                mask != null ? new String[] {address.getValue(), mask.getValue()} : new String[] {address.getValue()});
        this.address = address;
        this.mask = mask;
    }

    /**
     * Create an instance; this constructor is only present for XtendBeanGenerator and must not be used.
     */
    @Deprecated
    public MatchEthernetSource(MacAddress address, MacAddress mask, BigInteger[] bigMatchValues,
            MatchFieldType matchField, long[] matchValues, String[] stringMatchValues) {
        this(address, mask);
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, EthernetMatch value) {
        matchBuilder.setEthernetMatch(value);
    }

    @Override
    protected void populateBuilder(EthernetMatchBuilder builder) {
        EthernetSourceBuilder ethernetSourceBuilder = new EthernetSourceBuilder().setAddress(address);
        if (mask != null) {
            ethernetSourceBuilder.setMask(mask);
        }
        builder.setEthernetSource(ethernetSourceBuilder.build());
    }

    public MacAddress getAddress() {
        return address;
    }

    public MacAddress getMask() {
        return mask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MatchEthernetSource that = (MatchEthernetSource) o;

        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        return mask != null ? mask.equals(that.mask) : that.mask == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (mask != null ? mask.hashCode() : 0);
        return result;
    }
}
