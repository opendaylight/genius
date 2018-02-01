/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

public final class TunnelEndPointInfo {
    private String srcEndPointInfo;
    private String dstEndPointInfo;

    public TunnelEndPointInfo(String srcEndPointInfo, String dstEndPointInfo) {
        this.srcEndPointInfo  = srcEndPointInfo;
        this.dstEndPointInfo = dstEndPointInfo;
    }

    public String getDstDpnId() {
        return dstEndPointInfo;
    }

    public void setDstDpnId(String dstDpnId) {
        this.dstEndPointInfo = dstDpnId;
    }

    public String getSrcDpnId() {
        return srcEndPointInfo;
    }

    public void setSrcDpnId(String srcDpnId) {
        this.srcEndPointInfo = srcDpnId;
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
}
