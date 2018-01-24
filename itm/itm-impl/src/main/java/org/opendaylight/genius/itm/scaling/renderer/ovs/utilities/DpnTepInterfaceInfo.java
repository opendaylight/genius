/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DpnTepInterfaceInfo {
    private static final Logger LOG = LoggerFactory.getLogger(DpnTepInterfaceInfo.class);

    private Class<? extends TunnelTypeBase> tunnelType;
    private long groupId;
    private String tunnelName;
    private boolean monitorEnabled;

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    boolean internal;

    public Class<? extends TunnelTypeBase> getTunnelType() {
        return tunnelType;
    }

    public void setTunnelType(Class<? extends TunnelTypeBase> tunnelType) {
        this.tunnelType = tunnelType;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getTunnelName() {
        return tunnelName;
    }

    public void setTunnelName(String tunnelName) {
        this.tunnelName = tunnelName;
    }

    public boolean isMonitorEnabled() {
        return monitorEnabled;
    }

    public void setMonitorEnabled(boolean monitorEnabled) {
        this.monitorEnabled = monitorEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DpnTepInterfaceInfo that = (DpnTepInterfaceInfo) o;

        if (getGroupId() != that.getGroupId()) return false;
        return getTunnelName() != null ? getTunnelName().equals(that.getTunnelName()) : that.getTunnelName() == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (getGroupId() ^ (getGroupId() >>> 32));
        result = 31 * result + (getTunnelName() != null ? getTunnelName().hashCode() : 0);
        return result;
    }
}
