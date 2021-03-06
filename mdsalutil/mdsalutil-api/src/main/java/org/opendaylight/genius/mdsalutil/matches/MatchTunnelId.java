/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Tunnel identifier match.
 */
public class MatchTunnelId extends MatchInfoHelper<Tunnel, TunnelBuilder> {

    private final Uint64 tunnelId;
    private final Uint64 tunnelMask;

    public MatchTunnelId(Uint64 tunnelId) {
        this.tunnelId = tunnelId;
        this.tunnelMask = null;
    }

    public MatchTunnelId(Uint64 tunnelId, Uint64 tunnelMask) {
        this.tunnelId = tunnelId;
        this.tunnelMask = tunnelMask;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, Tunnel value) {
        matchBuilder.setTunnel(value);
    }

    @Override
    protected void populateBuilder(TunnelBuilder builder) {
        builder.setTunnelId(tunnelId);
        if (tunnelMask != null) {
            builder.setTunnelMask(tunnelMask);
        }
    }

    public Uint64 getTunnelId() {
        return tunnelId;
    }

    public Uint64 getTunnelMask() {
        return tunnelMask;
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

        MatchTunnelId that = (MatchTunnelId) other;

        if (tunnelId != null ? !tunnelId.equals(that.tunnelId) : that.tunnelId != null) {
            return false;
        }
        return tunnelMask != null ? tunnelMask.equals(that.tunnelMask) : that.tunnelMask == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tunnelId != null ? tunnelId.hashCode() : 0);
        result = 31 * result + (tunnelMask != null ? tunnelMask.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MatchTunnelId[tunnelId=" + tunnelId + ", tunnelMask=" + tunnelMask + "]";
    }

}
