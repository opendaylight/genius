/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
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
        super(NxmOfUdpSrcKey.class);
        this.port = port;
        this.mask = mask;
    }

    @Override
    protected void applyValue(NxAugMatchNodesNodeTableFlowBuilder matchBuilder, NxmOfUdpSrc value) {
        matchBuilder.setNxmOfUdpSrc(value);
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

        NxMatchUdpSourcePort that = (NxMatchUdpSourcePort) other;

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
    public String toString() {
        return "NxMatchUdpSourcePort[" + port + "/" + mask + "]";
    }
}
