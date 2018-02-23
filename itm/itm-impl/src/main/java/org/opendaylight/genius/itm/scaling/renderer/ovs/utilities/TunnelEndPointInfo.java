/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

public final class TunnelEndPointInfo {
    private final  String srcEndPointInfo;
    private final String dstEndPointInfo;

    public TunnelEndPointInfo(Builder builder) {
        this.srcEndPointInfo  = builder.srcEndPointInfo;
        this.dstEndPointInfo = builder.dstEndPointInfo;
    }

    public String getDstDpnId() {
        return dstEndPointInfo;
    }

    public String getSrcDpnId() {
        return srcEndPointInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TunnelEndPointInfo that = (TunnelEndPointInfo) obj;

        if (!getSrcDpnId().equals(that.getSrcDpnId())) {
            return false;
        }
        return getDstDpnId().equals(that.getDstDpnId());
    }

    @Override
    public int hashCode() {
        int result = getSrcDpnId().hashCode();
        result = 31 * result + getDstDpnId().hashCode();
        return result;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TunnelEndPointInfo from) {
        return new Builder(from);
    }

    public static final class Builder {
        private String srcEndPointInfo;
        private String dstEndPointInfo;

        private Builder() {
        }

        private Builder(TunnelEndPointInfo from) {
            this.srcEndPointInfo = from.srcEndPointInfo;
            this.dstEndPointInfo = from.dstEndPointInfo;
        }

        public Builder srcEndPointInfo(String value) {
            this.srcEndPointInfo = value;
            return this;
        }

        public Builder dstEndPointInfo(String value) {
            this.dstEndPointInfo = value;
            return this;
        }

        public TunnelEndPointInfo build() {
            return new TunnelEndPointInfo(this);
        }
    }
}
