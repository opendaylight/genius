/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import com.google.common.collect.ComparisonChain;
import java.util.Comparator;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.NxMatchFieldType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfTcpSrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.tcp.src.grouping.NxmOfTcpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.tcp.src.grouping.NxmOfTcpSrcBuilder;

/**
 * Nicira extension TCP source port match.
 */
public class NxMatchTcpSourcePort extends NxMatchInfoHelper<NxmOfTcpSrc, NxmOfTcpSrcBuilder> {
    private final int port;
    private final int mask;

    public NxMatchTcpSourcePort(int port, int mask) {
        super(NxmOfTcpSrcKey.class, NxMatchFieldType.nx_tcp_src_with_mask, new long[] {port, mask});
        this.port = port;
        this.mask = mask;
    }

    @Override
    protected void applyValue(NxAugMatchNodesNodeTableFlowBuilder matchBuilder, NxmOfTcpSrc value) {
        matchBuilder.setNxmOfTcpSrc(value);
    }

    @Override
    protected void populateBuilder(NxmOfTcpSrcBuilder builder) {
        builder.setPort(new PortNumber(port));
        builder.setMask(mask);
    }

    public int getPort() {
        return port;
    }

    public int getMask() {
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

        NxMatchTcpSourcePort that = (NxMatchTcpSourcePort) other;

        if (port != that.port) {
            return false;
        }
        return mask == that.mask;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + port;
        result = 31 * result + mask;
        return result;
    }

    @Override
    public int compareTo(MatchInfoBase other) {
        return compareTo(other, new Comparator<NxMatchTcpSourcePort>() {
            @Override
            public int compare(NxMatchTcpSourcePort left, NxMatchTcpSourcePort right) {
                return ComparisonChain.start()
                  .compare(left.port, right.port)
                  .compare(left.mask, right.mask)
                  .result();
            }
        });
    }

}
