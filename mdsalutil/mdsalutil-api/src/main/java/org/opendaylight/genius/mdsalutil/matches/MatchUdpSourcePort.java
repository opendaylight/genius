/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;

/**
 * UDP source port match.
 */
public class MatchUdpSourcePort extends MatchInfoHelper<UdpMatch, UdpMatchBuilder> {
    private static final long serialVersionUID = 3246856414238531734L;

    private final int port;

    public MatchUdpSourcePort(int port) {
        this.port = port;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, UdpMatch value) {
        matchBuilder.setLayer4Match(value);
    }

    @Override
    protected void populateBuilder(UdpMatchBuilder builder) {
        builder.setUdpSourcePort(new PortNumber(port));
    }

    public int getPort() {
        return port;
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

        MatchUdpSourcePort that = (MatchUdpSourcePort) other;

        return port == that.port;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "MatchUdpSourcePort[" + port + "]";
    }

}
