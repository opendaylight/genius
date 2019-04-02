/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.ericmatches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.EricAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.Icmpv6NdOptionsTypeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.eric.of.icmpv6.nd.options.type.grouping.EricOfIcmpv6NdOptionsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.eric.of.icmpv6.nd.options.type.grouping.EricOfIcmpv6NdOptionsTypeBuilder;

/**
 * MatchNdOptionType match.
 */
public class MatchNdOptionType
        extends EricMatchInfoHelper<EricOfIcmpv6NdOptionsType, EricOfIcmpv6NdOptionsTypeBuilder> {

    private Short ndOptionType;

    public MatchNdOptionType(Short ndOptionType) {
        super(Icmpv6NdOptionsTypeKey.class);
        this.ndOptionType = ndOptionType;
    }

    @Override
    protected void applyValue(EricAugMatchNodesNodeTableFlowBuilder matchBuilder, EricOfIcmpv6NdOptionsType value) {
        matchBuilder.setEricOfIcmpv6NdOptionsType(value);
    }

    @Override
    protected void populateBuilder(EricOfIcmpv6NdOptionsTypeBuilder builder) {
        builder.setIcmpv6NdOptionsType(ndOptionType);
    }

    public Short getNdOptionType() {
        return ndOptionType;
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

        MatchNdOptionType that = (MatchNdOptionType) other;

        return this.ndOptionType.equals(that.ndOptionType);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result +  ndOptionType;
        return result;
    }

    @Override
    public String toString() {
        return "MatchNdOptionType[" + ndOptionType + "]";
    }

}
