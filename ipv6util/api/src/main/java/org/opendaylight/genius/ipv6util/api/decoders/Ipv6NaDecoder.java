/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.ipv6util.api.decoders;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.opendaylight.genius.ipv6util.api.Ipv6Constants;
import org.opendaylight.genius.ipv6util.api.Ipv6Util;
import org.opendaylight.openflowplugin.libraries.liblldp.BitBufferHelper;
import org.opendaylight.openflowplugin.libraries.liblldp.BufferException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.packet.rev160620.NeighborAdvertisePacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.packet.rev160620.NeighborAdvertisePacketBuilder;

public class Ipv6NaDecoder {

    private byte[] data;

    public Ipv6NaDecoder(final byte[] packetData) {
        this.data = Arrays.copyOf(packetData, packetData.length);
    }

    public NeighborAdvertisePacket decode() throws BufferException, UnknownHostException {
        NeighborAdvertisePacketBuilder naPdu = new NeighborAdvertisePacketBuilder();
        int bitOffset = 0;

        naPdu.setDestinationMac(
                new MacAddress(Ipv6Util.bytesToHexString(BitBufferHelper.getBits(data, bitOffset, 48))));
        bitOffset = bitOffset + 48;
        naPdu.setSourceMac(
                new MacAddress(Ipv6Util.bytesToHexString(BitBufferHelper.getBits(data, bitOffset, 48))));
        bitOffset = bitOffset + 48;
        naPdu.setEthertype(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));

        bitOffset = Ipv6Constants.IP_V6_HDR_START;
        naPdu.setVersion(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 4)));
        bitOffset = bitOffset + 4;
        naPdu.setFlowLabel(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset, 28)));
        bitOffset = bitOffset + 28;
        naPdu.setIpv6Length(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
        bitOffset = bitOffset + 16;
        naPdu.setNextHeader(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
        bitOffset = bitOffset + 8;
        naPdu.setHopLimit(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
        bitOffset = bitOffset + 8;
        naPdu.setSourceIpv6(Ipv6Address.getDefaultInstance(
                InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset, 128)).getHostAddress()));
        bitOffset = bitOffset + 128;
        naPdu.setDestinationIpv6(Ipv6Address.getDefaultInstance(
                InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset, 128)).getHostAddress()));
        bitOffset = bitOffset + 128;

        naPdu.setIcmp6Type(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
        bitOffset = bitOffset + 8;
        naPdu.setIcmp6Code(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
        bitOffset = bitOffset + 8;
        naPdu.setIcmp6Chksum(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
        bitOffset = bitOffset + 16;
        naPdu.setFlags(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset, 32)));
        bitOffset = bitOffset + 32;
        naPdu.setTargetAddress(Ipv6Address.getDefaultInstance(
                InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset, 128)).getHostAddress()));

        if (naPdu.getIpv6Length() > Ipv6Constants.ICMPV6_NA_LENGTH_WO_OPTIONS) {
            bitOffset = bitOffset + 128;
            naPdu.setOptionType(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
            bitOffset = bitOffset + 8;
            naPdu.setTargetAddrLength(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
            bitOffset = bitOffset + 8;
            if (naPdu.getOptionType() == Ipv6Constants.ICMP_V6_OPTION_TARGET_LLA) {
                naPdu.setTargetLlAddress(new MacAddress(
                        Ipv6Util.bytesToHexString(BitBufferHelper.getBits(data, bitOffset, 48))));
            }
        }
        return naPdu.build();
    }
}
