/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;

/**
 * Metadata match.
 */
public class MatchMetadata extends MatchInfoHelper<Metadata, MetadataBuilder> {
    private static final long serialVersionUID = -7159655597194445374L;

    private final BigInteger metadata;
    private final BigInteger mask;

    public MatchMetadata(BigInteger metadata, BigInteger mask) {
        this.metadata = metadata;
        this.mask = mask;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, Metadata value) {
        matchBuilder.setMetadata(value);
    }

    @Override
    protected void populateBuilder(MetadataBuilder builder) {
        builder.setMetadata(metadata).setMetadataMask(mask);
    }

    public BigInteger getMetadata() {
        return metadata;
    }

    public BigInteger getMask() {
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

        MatchMetadata that = (MatchMetadata) other;

        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) {
            return false;
        }
        return mask != null ? mask.equals(that.mask) : that.mask == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (mask != null ? mask.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MatchMetadata[metadata=" + metadata + ", mask=" + mask + "]";
    }

}
