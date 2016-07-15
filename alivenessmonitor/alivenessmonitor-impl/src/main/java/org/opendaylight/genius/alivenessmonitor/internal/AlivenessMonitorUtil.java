/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.internal;

import com.google.common.primitives.UnsignedBytes;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.InterfaceMonitorMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProfiles;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoridKeyMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitoringStates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411._interface.monitor.map.InterfaceMonitorEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfile;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.profiles.MonitorProfileKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitorid.key.map.MonitoridKeyEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitoring.states.MonitoringStateKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AlivenessMonitorUtil {

    static InstanceIdentifier<MonitoringState> getMonitorStateId(String keyId) {
        return InstanceIdentifier.builder(MonitoringStates.class)
                .child(MonitoringState.class, new MonitoringStateKey(keyId)).build();
    }

    static InstanceIdentifier<MonitoringInfo> getMonitoringInfoId(Long monitorId) {
        return InstanceIdentifier.builder(MonitorConfigs.class)
                .child(MonitoringInfo.class, new MonitoringInfoKey(monitorId)).build();
    }

    static InstanceIdentifier<MonitorProfile> getMonitorProfileId(Long profileId) {
        return InstanceIdentifier.builder(MonitorProfiles.class)
                .child(MonitorProfile.class, new MonitorProfileKey(profileId)).build();
    }

    static InstanceIdentifier<MonitoridKeyEntry> getMonitorMapId(Long keyId) {
        return InstanceIdentifier.builder(MonitoridKeyMap.class)
                .child(MonitoridKeyEntry.class, new MonitoridKeyEntryKey(keyId)).build();
    }

    static InstanceIdentifier<InterfaceMonitorEntry> getInterfaceMonitorMapId(String interfaceName) {
        return InstanceIdentifier.builder(InterfaceMonitorMap.class)
                .child(InterfaceMonitorEntry.class, new InterfaceMonitorEntryKey(interfaceName)).build();
    }

    public static String toStringIpAddress(byte[] ipAddress)
    {
        String ip = "";
        if (ipAddress == null) {
            return ip;
        }

        try {
            ip = InetAddress.getByAddress(ipAddress).getHostAddress();
        } catch(UnknownHostException e) {  }

        return ip;
    }

    public static String toStringMacAddress(byte[] macAddress)
    {
        if (macAddress == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(18);

        for (byte macAddres : macAddress) {
            sb.append(UnsignedBytes.toString(macAddres, 16).toUpperCase());
            sb.append(":");
        }

        sb.setLength(17);
        return sb.toString();
    }

    public static byte[] parseIpAddress(String ipAddress) {
        byte cur;

        String[] addressPart = ipAddress.split(".");
        int size = addressPart.length;

        byte[] part = new byte[size];
        for (int i = 0; i < size; i++) {
            cur = UnsignedBytes.parseUnsignedByte(addressPart[i], 16);
            part[i] = cur;
        }

        return part;
    }

    public static byte[] parseMacAddress(String macAddress) {
        byte cur;

        String[] addressPart = macAddress.split(":");
        int size = addressPart.length;

        byte[] part = new byte[size];
        for (int i = 0; i < size; i++) {
            cur = UnsignedBytes.parseUnsignedByte(addressPart[i], 16);
            part[i] = cur;
        }

        return part;
    }
}
