/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

/**
 * Ethernet type match.
 */
public class MatchEthernetType extends MatchInfoHelper<EthernetMatch, EthernetMatchBuilder> {
    private static final long serialVersionUID = -3618187282862692415L;

    public static final MatchEthernetType IPV4 = new MatchEthernetType(0x0800L);
    public static final MatchEthernetType ARP = new MatchEthernetType(0x0806L);
    public static final MatchEthernetType RARP = new MatchEthernetType(0x8035L);
    public static final MatchEthernetType IPV6 = new MatchEthernetType(0x86DDL);
    public static final MatchEthernetType MPLS_UNICAST = new MatchEthernetType(0x8847L);
    public static final MatchEthernetType MPLS_MULTICAST = new MatchEthernetType(0x8848L);

    private final long type;

    public MatchEthernetType(long type) {
        this.type = type;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, EthernetMatch value) {
        matchBuilder.setEthernetMatch(value);
    }

    @Override
    protected void populateBuilder(EthernetMatchBuilder builder) {
        builder.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(type)).build());
    }

    public long getType() {
        return type;
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

        MatchEthernetType that = (MatchEthernetType) other;

        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (type ^ type >>> 32);
        return result;
    }

    @Override
    public String toString() {
        return "MatchEthernetType[" + type + "]";
    }
}
