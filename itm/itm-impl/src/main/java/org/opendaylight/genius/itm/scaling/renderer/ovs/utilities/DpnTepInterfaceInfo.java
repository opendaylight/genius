/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;

public final class DpnTepInterfaceInfo {
    private final Class<? extends TunnelTypeBase> tunnelType;
    private final long groupId;
    private final String tunnelName;
    private final boolean monitoringEnabled;
    private final boolean isInternal;

    private DpnTepInterfaceInfo(Builder builder) {
        Objects.requireNonNull(builder.tunnelType, "tunnel type not specified for DpnTepInterfaceInfo");
        this.tunnelType = builder.tunnelType;
        this.groupId = builder.groupId;
        Objects.requireNonNull(builder.tunnelName, "tunnel name not specified for DpnTepInterfaceInfo");
        this.tunnelName = builder.tunnelName;
        this.monitoringEnabled = builder.monitoringEnabled;
        this.isInternal = builder.isInternal;
    }

    public boolean isInternal() {
        return isInternal;
    }

    @Nonnull public Class<? extends TunnelTypeBase> getTunnelType() {
        return tunnelType;
    }

    public long getGroupId() {
        return groupId;
    }

    @Nonnull public String getTunnelName() {
        return tunnelName;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DpnTepInterfaceInfo that = (DpnTepInterfaceInfo) obj;
        return getGroupId()!= that.getGroupId() && getTunnelName().equals(that.getTunnelName());
    }

    @Override
    public int hashCode() {
        int result = (int) (getGroupId() ^ (getGroupId() >>> 32));
        result = 31 * result + getTunnelName().hashCode();
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(DpnTepInterfaceInfo from) {
        return new Builder(from);
    }

    public static final class Builder {
        private Class<? extends TunnelTypeBase> tunnelType;
        private long groupId;
        private String tunnelName;
        private boolean monitoringEnabled;
        private boolean isInternal;

        private Builder() {
        }

        private Builder(DpnTepInterfaceInfo from) {
            this.tunnelType = from.tunnelType;
            this.groupId = from.groupId;
            this.tunnelName = from.tunnelName;
            this.monitoringEnabled = from.monitoringEnabled;
            this.isInternal = from.isInternal;
        }

        public Builder tunnelType(Class<? extends TunnelTypeBase> value) {
            this.tunnelType = value;
            return this;
        }

        public Builder groupId(long value) {
            this.groupId = value;
            return this;
        }

        public Builder tunnelName(String value) {
            this.tunnelName = value;
            return this;
        }

        public Builder monitoringEnabled(Boolean value) {
            this.monitoringEnabled = value;
            return this;
        }

        public Builder isInternal(boolean value) {
            this.isInternal = value;
            return this;
        }

        public DpnTepInterfaceInfo build() {
            return new DpnTepInterfaceInfo(this);
        }
    }
}
