/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.ipv6util.api.decoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.genius.ipv6util.api.Icmpv6Type;
import org.opendaylight.openflowplugin.libraries.liblldp.BufferException;
import org.opendaylight.openflowplugin.libraries.liblldp.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.packet.rev160620.NeighborAdvertisePacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.packet.rev160620.NeighborAdvertisePacketBuilder;

public class Ipv6NaDecoderTest {

    @Test
    public void testIpv6NaDecoder() throws UnknownHostException, BufferException {
        byte[] inputPayload = buildPacket(
                "33 33 00 00 00 02",                               // Destination MAC
                "08 00 27 FE 8F 95",                               // Source MAC
                "86 DD",                                           // Ethertype - IPv6
                "60 00 00 00",                                     // Version 6, traffic class 0, no flowlabel
                "00 20",                                           // Payload length
                "3A",                                              // Next header is ICMPv6
                "FF",                                              // Hop limit
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 01", // Source IP
                "FF 02 00 00 00 00 00 00 00 00 00 01 FF 00 00 02", // Destination IP
                "88",                                              // ICMPv6 neighbor advertisement.
                "00",                                              // Code
                "5E 94",                                           // Checksum
                "00 00 00 00",                                     // Flags
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 02", // Target Address
                "02",                                              // Type: Target Link-Layer Option
                "01",                                              // Option length
                "08 00 27 FE 8F 95"                                // Target Link layer address
        );
        NeighborAdvertisePacket expectedNa = new NeighborAdvertisePacketBuilder()
                .setDestinationMac(MacAddress.getDefaultInstance("33:33:00:00:00:02"))
                .setSourceMac(MacAddress.getDefaultInstance("08:00:27:fe:8f:95"))
                .setEthertype(EtherTypes.IPv6.intValue()).setVersion((short) 6).setFlowLabel(0L).setIpv6Length(0x0020)
                .setNextHeader((short) 0x3A).setHopLimit((short) 0xFF)
                .setSourceIpv6(Ipv6Address.getDefaultInstance("2001:db8:0:0:0:0:0:1"))
                .setDestinationIpv6(Ipv6Address.getDefaultInstance("ff02:0:0:0:0:1:ff00:2"))
                .setIcmp6Type(Icmpv6Type.NEIGHBOR_ADVERTISEMENT.getValue()).setIcmp6Code((short) 0)
                .setIcmp6Chksum(0x5E94).setFlags(0L)
                .setTargetAddress(Ipv6Address.getDefaultInstance("2001:db8:0:0:0:0:0:2")).setOptionType((short) 2)
                .setTargetAddrLength((short) 1).setTargetLlAddress(new MacAddress("08:00:27:fe:8f:95")).build();
        assertEquals(expectedNa, new Ipv6NaDecoder(inputPayload).decode());

        // Negative cases - invalid checksum
        inputPayload = buildPacket(
                "33 33 00 00 00 02",                               // Destination MAC
                "08 00 27 FE 8F 95",                               // Source MAC
                "86 DD",                                           // Ethertype - IPv6
                "60 00 00 00",                                     // Version 6, traffic class 0, no flowlabel
                "00 20",                                           // Payload length
                "3A",                                              // Next header is ICMPv6
                "FF",                                              // Hop limit
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 01", // Source IP
                "FF 02 00 00 00 00 00 00 00 00 00 01 FF 00 00 02", // Destination IP
                "88",                                              // ICMPv6 neighbor advertisement.
                "00",                                              // Code
                "5E 95",                                           // Checksum (invalid)
                "00 00 00 00",                                     // Flags
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 02", // Target Address
                "02",                                              // Type: Target Link-Layer Option
                "01",                                              // Option length
                "08 00 27 FE 8F 95"                                // Target Link layer address
        );
        assertNotEquals(expectedNa, new Ipv6NaDecoder(inputPayload).decode());

        // Invalid ICMP type
        inputPayload = buildPacket(
                "33 33 00 00 00 02",                               // Destination MAC
                "08 00 27 FE 8F 95",                               // Source MAC
                "86 DD",                                           // Ethertype - IPv6
                "60 00 00 00",                                     // Version 6, traffic class 0, no flowlabel
                "00 20",                                           // Payload length
                "3A",                                              // Next header is ICMPv6
                "FF",                                              // Hop limit
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 01", // Source IP
                "FF 02 00 00 00 00 00 00 00 00 00 01 FF 00 00 02", // Destination IP
                "87",                                              // ICMPv6 neighbor solicitation. (invalid)
                "00",                                              // Code
                "5E 94",                                           // Checksum
                "00 00 00 00",                                     // Flags
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 02", // Target Address
                "02",                                              // Type: Target Link-Layer Option
                "01",                                              // Option length
                "08 00 27 FE 8F 95"                                // Target Link layer address
        );
        assertNotEquals(expectedNa, new Ipv6NaDecoder(inputPayload).decode());

        // Invalid payload
        final byte[] invalidInputPayload = buildPacket(
                "33 33 00 00 00 02",                               // Destination MAC
                "08 00 27 FE 8F 95",                               // Source MAC
                "86 DD",                                           // Ethertype - IPv6
                "60 00 00 00",                                     // Version 6, traffic class 0, no flowlabel
                "00 20",                                           // Payload length
                "3A"                                               // Next header is ICMPv6
        );
        assertThrows(BufferException.class, () -> {
            new Ipv6NaDecoder(invalidInputPayload).decode();
        });
    }

    public byte[] buildPacket(String... contents) {
        List<String[]> splitContents = new ArrayList<>();
        int packetLength = 0;
        for (String content : contents) {
            String[] split = content.split(" ");
            packetLength += split.length;
            splitContents.add(split);
        }
        byte[] packet = new byte[packetLength];
        int index = 0;
        for (String[] split : splitContents) {
            for (String component : split) {
                // We can't use Byte.parseByte() here, it refuses anything > 7F
                packet[index] = (byte) Integer.parseInt(component, 16);
                index++;
            }
        }
        return packet;
    }
}
