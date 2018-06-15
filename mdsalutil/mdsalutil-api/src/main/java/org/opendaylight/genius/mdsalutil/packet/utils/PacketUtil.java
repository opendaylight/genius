/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.packet.utils;

import com.google.common.base.Preconditions;
import org.opendaylight.genius.ipv6util.api.Icmpv6Type;
import org.opendaylight.genius.ipv6util.api.Ipv6Constants;
import org.opendaylight.genius.mdsalutil.packet.IPProtocols;
import org.opendaylight.openflowplugin.libraries.liblldp.BitBufferHelper;
import org.opendaylight.openflowplugin.libraries.liblldp.BufferException;
import org.opendaylight.openflowplugin.libraries.liblldp.EtherTypes;

public final class PacketUtil {

    private PacketUtil() {

    }

    public static Integer getEtherTypeFromPacket(byte[] packetData) {
        Preconditions.checkArgument(packetData != null && packetData.length > 0, "packetData is null or empty");
        try {
            return BitBufferHelper
                    .getInt(BitBufferHelper.getBits(packetData, Ipv6Constants.ETHTYPE_START, Ipv6Constants.TWO_BYTES));
        } catch (BufferException e) {
            throw new IllegalArgumentException("Failed to decode packet", e);
        }
    }

    public static boolean isIpv4Packet(byte[] packetData) {
        Preconditions.checkArgument(packetData != null && packetData.length > 0, "packetData is null or empty");

        Integer ethType = getEtherTypeFromPacket(packetData);
        return ethType == EtherTypes.IPv4.intValue() ? true : false;
    }

    public static boolean isIpv6Packet(byte[] packetData) {
        Preconditions.checkArgument(packetData != null && packetData.length > 0, "packetData is null or empty");

        Integer ethType = getEtherTypeFromPacket(packetData);
        return ethType == EtherTypes.IPv6.intValue() ? true : false;
    }

    public static boolean isIpv6NaPacket(byte[] packetData) {
        Preconditions.checkArgument(packetData != null && packetData.length > 0, "packetData is null or empty");
        try {
            if (isIpv6Packet(packetData)) {
                int v6NxtHdr = BitBufferHelper.getByte(BitBufferHelper.getBits(packetData,
                        Ipv6Constants.IP_V6_HDR_START + Ipv6Constants.IP_V6_NEXT_HDR, Ipv6Constants.ONE_BYTE));
                if (v6NxtHdr == IPProtocols.IPV6ICMP.intValue()) {
                    int icmpv6Type = BitBufferHelper.getInt(BitBufferHelper.getBits(packetData,
                            Ipv6Constants.ICMPV6_HDR_START, Ipv6Constants.ONE_BYTE));
                    if (icmpv6Type == Icmpv6Type.NEIGHBOR_ADVERTISEMENT.getValue()) {
                        return true;
                    }
                }
            }
        } catch (BufferException e) {
            throw new IllegalArgumentException("Failed to decode packet", e);
        }
        return false;
    }
}
