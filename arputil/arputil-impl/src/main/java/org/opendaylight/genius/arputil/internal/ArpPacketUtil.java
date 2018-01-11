/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.arputil.internal;

import org.opendaylight.genius.mdsalutil.packet.ARP;
import org.opendaylight.genius.mdsalutil.packet.Ethernet;
import org.opendaylight.openflowplugin.libraries.liblldp.EtherTypes;
import org.opendaylight.openflowplugin.libraries.liblldp.PacketException;

public final class ArpPacketUtil {

    private ArpPacketUtil() {
    }

    static final byte[] ETHERNET_BROADCAST_DESTINATION
            = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    static byte[] getPayload(short opCode, byte[] senderMacAddress, byte[] senderIP, byte[] targetMacAddress,
                                     byte[] targetIP) throws PacketException {
        ARP arp = createARPPacket(opCode, senderMacAddress, senderIP, targetMacAddress, targetIP);
        Ethernet ethernet = createEthernetPacket(senderMacAddress, targetMacAddress, arp);
        return ethernet.serialize();
    }

    private static ARP createARPPacket(short opCode, byte[] senderMacAddress, byte[] senderIP, byte[] targetMacAddress,
                                       byte[] targetIP) {
        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
        arp.setProtocolType(EtherTypes.IPv4.shortValue());
        arp.setHardwareAddressLength((byte) 6);
        arp.setProtocolAddressLength((byte) 4);
        arp.setOpCode(opCode);
        arp.setSenderHardwareAddress(senderMacAddress);
        arp.setSenderProtocolAddress(senderIP);
        arp.setTargetHardwareAddress(targetMacAddress);
        arp.setTargetProtocolAddress(targetIP);
        return arp;
    }

    private static Ethernet createEthernetPacket(byte[] sourceMAC, byte[] targetMAC, ARP arp) throws PacketException {
        Ethernet ethernet = new Ethernet();
        ethernet.setSourceMACAddress(sourceMAC);
        ethernet.setDestinationMACAddress(targetMAC);
        ethernet.setEtherType(EtherTypes.ARP.shortValue());
        ethernet.setPayload(arp);
        return ethernet;
    }
}
