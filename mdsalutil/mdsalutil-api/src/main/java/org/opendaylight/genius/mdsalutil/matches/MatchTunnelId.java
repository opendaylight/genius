/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;

/**
 * Tunnel identifier match.
 */
public class MatchTunnelId extends MatchInfoHelper<Tunnel, TunnelBuilder> {
    private final BigInteger tunnelId;
    private final BigInteger tunnelMask;

    public MatchTunnelId(BigInteger tunnelId) {
        super(MatchFieldType.tunnel_id, new BigInteger[] {tunnelId});
        this.tunnelId = tunnelId;
        this.tunnelMask = null;
    }

    public MatchTunnelId(BigInteger tunnelId, BigInteger tunnelMask) {
        super(MatchFieldType.tunnel_id, new BigInteger[] {tunnelId, tunnelMask});
        this.tunnelId = tunnelId;
        this.tunnelMask = tunnelMask;
    }

    /**
     * Create an instance; this constructor is only present for XtendBeanGenerator and must not be used.
     */
    @Deprecated
    public MatchTunnelId(BigInteger[] bigMatchValues, MatchFieldType matchField, long[] matchValues,
            String[] stringMatchValues, BigInteger tunnelId, BigInteger tunnelMask) {
        this(tunnelId, tunnelMask);
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

    public BigInteger getTunnelId() {
        return tunnelId;
    }

    public BigInteger getTunnelMask() {
        return tunnelMask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MatchTunnelId that = (MatchTunnelId) o;

        if (tunnelId != null ? !tunnelId.equals(that.tunnelId) : that.tunnelId != null) return false;
        return tunnelMask != null ? tunnelMask.equals(that.tunnelMask) : that.tunnelMask == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tunnelId != null ? tunnelId.hashCode() : 0);
        result = 31 * result + (tunnelMask != null ? tunnelMask.hashCode() : 0);
        return result;
    }
}
