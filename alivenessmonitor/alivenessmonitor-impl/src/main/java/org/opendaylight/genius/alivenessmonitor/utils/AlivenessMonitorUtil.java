/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.utils;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.genius.ipv6util.api.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.InterfaceMonitorMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfiles;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoridKeyMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoringStates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfileKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringStateKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;

public final class AlivenessMonitorUtil {

    private AlivenessMonitorUtil() {}

    public static InstanceIdentifier<MonitoringState> getMonitorStateId(String keyId) {
        return InstanceIdentifier.builder(MonitoringStates.class)
                .child(MonitoringState.class, new MonitoringStateKey(keyId)).build();
    }

    public static InstanceIdentifier<MonitoringInfo> getMonitoringInfoId(Long monitorId) {
        return InstanceIdentifier.builder(MonitorConfigs.class)
                .child(MonitoringInfo.class, new MonitoringInfoKey(monitorId)).build();
    }

    public static InstanceIdentifier<MonitorProfile> getMonitorProfileId(Long profileId) {
        return InstanceIdentifier.builder(MonitorProfiles.class)
                .child(MonitorProfile.class, new MonitorProfileKey(profileId)).build();
    }

    public  static InstanceIdentifier<MonitoridKeyEntry> getMonitorMapId(Long keyId) {
        return InstanceIdentifier.builder(MonitoridKeyMap.class)
                .child(MonitoridKeyEntry.class, new MonitoridKeyEntryKey(keyId)).build();
    }

    public static InstanceIdentifier<InterfaceMonitorEntry> getInterfaceMonitorMapId(String interfaceName) {
        return InstanceIdentifier.builder(InterfaceMonitorMap.class)
                .child(InterfaceMonitorEntry.class, new InterfaceMonitorEntryKey(interfaceName)).build();
    }

    public static String getIpAddress(EndpointType endpoint) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress ipAddress = null;
        if (endpoint instanceof IpAddress) {
            ipAddress = ((IpAddress) endpoint).getIpAddress();
        } else if (endpoint instanceof Interface) {
            ipAddress = ((Interface) endpoint).getInterfaceIp();
        }
        return ipAddress != null ? Ipv6Util.getFormattedIpAddress(ipAddress) : StringUtils.EMPTY;
    }

    public static PhysAddress getMacAddress(EndpointType source) {
        PhysAddress macAddress = null;
        if (source instanceof Interface) {
            macAddress = ((Interface) source).getMacAddress();
        }
        return macAddress;
    }

    public static String getInterfaceName(EndpointType endpoint) {
        String interfaceName = null;
        if (endpoint instanceof Interface) {
            interfaceName = ((Interface) endpoint).getInterfaceName();
        }
        return interfaceName;
    }

    public static String getErrorText(Collection<RpcError> errors) {
        StringBuilder errorText = new StringBuilder();
        for (RpcError error : errors) {
            errorText.append(",").append(error.getErrorType()).append("-").append(error.getMessage());
        }
        return errorText.toString();
    }

    // TODO Replace this with mdsal's DataObjectUtils.nullToEmpty when upgrading to mdsal 3.0.2
    @Nonnull
    public static <T> List<T> nullToEmpty(final @Nullable List<T> input) {
        return input != null ? input : emptyList();
    }
}
