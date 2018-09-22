/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunIpv4DstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.ipv4.dst.grouping.NxmNxTunIpv4Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.ipv4.dst.grouping.NxmNxTunIpv4DstBuilder;

/**
 * Nicira extension tunnel destination IP match.
 */
public class NxMatchTunnelDestinationIp extends NxMatchInfoHelper<NxmNxTunIpv4Dst, NxmNxTunIpv4DstBuilder> {
    private final Ipv4Address address;

    public NxMatchTunnelDestinationIp(String address) {
        this(new Ipv4Address(address));
    }

    public NxMatchTunnelDestinationIp(Ipv4Address address) {
        super(NxmNxTunIpv4DstKey.class);
        this.address = address;
    }

    @Override
    protected void applyValue(NxAugMatchNodesNodeTableFlowBuilder matchBuilder, NxmNxTunIpv4Dst value) {
        matchBuilder.setNxmNxTunIpv4Dst(value);
    }

    @Override
    protected void populateBuilder(NxmNxTunIpv4DstBuilder builder) {
        builder.setIpv4Address(address);
    }

    public Ipv4Address getAddress() {
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

        NxMatchTunnelDestinationIp that = (NxMatchTunnelDestinationIp) other;

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
        return "NxMatchTunnelDestinationIp[" + address + "]";
    }
}
