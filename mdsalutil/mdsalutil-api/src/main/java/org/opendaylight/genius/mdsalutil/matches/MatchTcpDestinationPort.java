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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;

/**
 * TCP destination port match.
 */
public class MatchTcpDestinationPort extends MatchInfoHelper<TcpMatch, TcpMatchBuilder> {
    private static final long serialVersionUID = 3207416827575349278L;

    private final int port;

    public MatchTcpDestinationPort(int port) {
        this.port = port;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, TcpMatch value) {
        matchBuilder.setLayer4Match(value);
    }

    @Override
    protected void populateBuilder(TcpMatchBuilder builder) {
        builder.setTcpDestinationPort(new PortNumber(port));
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

        MatchTcpDestinationPort that = (MatchTcpDestinationPort) other;

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
        return "MatchTcpDestinationPort[" + port + "]";
    }

}
