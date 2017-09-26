/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import java.math.BigInteger;
import java.util.Map;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

/**
 * In port match.
 */
public class MatchInPort extends MatchInfo {

    private final BigInteger dpId;
    private final long portNumber;

    public MatchInPort(BigInteger dpId, long portNumber) {
        this.dpId = dpId;
        this.portNumber = portNumber;
    }

    @Override
    public void createInnerMatchBuilder(Map<Class<?>, Object> mapMatchBuilder) {
        // Nothing to do
    }

    @Override
    public void setMatch(MatchBuilder matchBuilder, Map<Class<?>, Object> mapMatchBuilder) {
        String nodeConnectorId = "openflow:" + dpId + ":" + portNumber;
        matchBuilder.setInPort(new NodeConnectorId(nodeConnectorId));
    }

    public BigInteger getDpId() {
        return dpId;
    }

    public long getPortNumber() {
        return portNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        MatchInPort that = (MatchInPort) other;

        if (portNumber != that.portNumber) {
            return false;
        }
        return dpId != null ? dpId.equals(that.dpId) : that.dpId == null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (dpId == null ? 0 : dpId.hashCode());
        result = prime * result + (int) (portNumber ^ portNumber >>> 32);
        return result;
    }

    @Override
    public String toString() {
        return "MatchInPort[dpId=" + dpId + ", portNumber=" + portNumber + "]";
    }
}
