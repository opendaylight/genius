/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import org.opendaylight.genius.mdsalutil.NxMatchFieldType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfUdpSrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.udp.src.grouping.NxmOfUdpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.udp.src.grouping.NxmOfUdpSrcBuilder;


/**
 * Nicira extension UDP source port match.
 */
public class NxMatchUdpSourcePort extends NxMatchInfoHelper<NxmOfUdpSrc, NxmOfUdpSrcBuilder> {
    private final int port;
    private final int mask;

    public NxMatchUdpSourcePort(int port, int mask) {
        super(NxMatchFieldType.nx_udp_src_with_mask, new long[] {port, mask});
        this.port = port;
        this.mask = mask;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, NxmOfUdpSrc value) {
        NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmOfUdpSrc(value).build();
        GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilder
                .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                nxAugMatch, NxmOfUdpSrcKey.class);
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
    }

    @Override
    protected void populateBuilder(NxmOfUdpSrcBuilder builder) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NxMatchUdpSourcePort that = (NxMatchUdpSourcePort) o;

        if (port != that.port) return false;
        return mask == that.mask;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + port;
        result = 31 * result + mask;
        return result;
    }
}
