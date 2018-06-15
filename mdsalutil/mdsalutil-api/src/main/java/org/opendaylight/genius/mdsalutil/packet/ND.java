/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.packet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.openflowplugin.libraries.liblldp.BitBufferHelper;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;

public class ND extends Packet {
    private static final String DEST_MAC_ADDR = "DestinationMacAddress";
    private static final String SRC_MAC_ADDR = "SourceMacAddress";
    private static final String ETHER_TYPE = "EthernetType";
    private static final String VERSION = "Version";
    private static final String FLOW_LABEL = "FlowLabel";
    private static final String PAY_LOAD_LEN = "Ipv6PayLoadLength";
    private static final String NEXT_HEADER_LEN = "NextAddressLength";
    private static final String HOP_LIMIT = "HopLimit";
    private static final String SRC_IP = "SourceIpv6Address";
    private static final String DEST_IP = "DestinationIpv6Address";
    private static final String ICMP6_TYPE = "Icmpv6Type";
    private static final String ICMP6_CODE = "Icmpv6Code";
    private static final String ICMP6_CHKSUM = "Icmpv6Checksum";
    private static final String FLAGS = "Flags";
    private static final String TARGET_ADDR = "TargetAddress";
    //to be removed below NITHI
    public static final short HW_TYPE_ETHERNET = (short) 0x1;
    public static final short NEIGHBOR_SOLICITATION = 135;
    public static final short NEIGHBOR_ADVERTISEMENT = 136;

    public static final short PROTO_TYPE_IP = 0x800;
    //to be removed above NITHI

    @SuppressWarnings("serial")
    private static Map<String, Pair<Integer, Integer>> fieldCoordinates
            = new LinkedHashMap<String, Pair<Integer, Integer>>() { {
        put(DEST_MAC_ADDR, new ImmutablePair<>(0, 48));
        put(SRC_MAC_ADDR, new ImmutablePair<>(48, 48));
        put(ETHER_TYPE, new ImmutablePair<>(96, 16));
        put(VERSION, new ImmutablePair<>(112, 4));
        put(FLOW_LABEL, new ImmutablePair<>(116, 28));
        put(PAY_LOAD_LEN, new ImmutablePair<>(144, 16));
        put(NEXT_HEADER_LEN, new ImmutablePair<>(160, 8));
        put(HOP_LIMIT, new ImmutablePair<>(168, 8));
        put(SRC_IP, new ImmutablePair<>(176, 128));
        put(DEST_IP, new ImmutablePair<>(304, 128));
        put(ICMP6_TYPE, new ImmutablePair<>(432, 8));
        put(ICMP6_CODE, new ImmutablePair<>(440, 8));
        put(ICMP6_CHKSUM, new ImmutablePair<>(448, 16));
        put(FLAGS, new ImmutablePair<>(464, 32));
        put(TARGET_ADDR, new ImmutablePair<>(496, 128));
        //TODO OPTIONS NITHI

    }
    };

    private final Map<String, byte[]> fieldValues;

    /**
     * Default constructor that creates and sets the HashMap.
     */
    public ND() {
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    public ND(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    public byte[] getDestMacAddress() { return fieldValues.get(DEST_MAC_ADDR); }

    public byte[] getSrcMacAddress() { return fieldValues.get(SRC_MAC_ADDR); }

    public short getEtherType() {
        return BitBufferHelper.getShort(fieldValues.get(ETHER_TYPE));
    }

    public byte getVersion() {
        return BitBufferHelper.getByte(fieldValues.get(VERSION));
    }

    public byte[] getFlowLabel() { return fieldValues.get(FLOW_LABEL); }

    public short getPayloadLen() {
        return BitBufferHelper.getShort(fieldValues.get(PAY_LOAD_LEN));
    }

    public byte getNextHeaderLen() {
        return BitBufferHelper.getByte(fieldValues.get(NEXT_HEADER_LEN));
    }

    public byte getHopLimit() {
        return BitBufferHelper.getByte(fieldValues.get(HOP_LIMIT));
    }

    public byte[] getSrcIpAddress() {
        return fieldValues.get(SRC_IP);
    }

    public byte[] getDestIpAddress() {
        return fieldValues.get(DEST_IP);
    }

    public byte getIcmp6Type() {
        return BitBufferHelper.getByte(fieldValues.get(ICMP6_TYPE));
    }

    public byte getIcmp6Code() {
        return BitBufferHelper.getByte(fieldValues.get(ICMP6_CODE));
    }

    public short getIcmp6Chksum() {
        return BitBufferHelper.getShort(fieldValues.get(ICMP6_CHKSUM));
    }

    public byte[] getFlags() {
        return fieldValues.get(FLAGS);
    }

    public byte[] getTargetAddress() {
        return fieldValues.get(TARGET_ADDR);
    }


    public ND setDestMacAddress(byte[] destinationMacAddress) {
        fieldValues.put(DEST_MAC_ADDR, destinationMacAddress);
        return this;
    }

    public ND setSrcMacAddress(byte[] srcMacAddressAddress) {
        fieldValues.put(SRC_MAC_ADDR, srcMacAddressAddress);
        return this;
    }

    public ND setEtherType(short etherType) {
        byte[] ethernetType = BitBufferHelper.toByteArray(etherType);
        fieldValues.put(ETHER_TYPE, ethernetType);
        return this;
    }

    public ND setVersion(byte version) {
        byte[] Version = BitBufferHelper
                .toByteArray(version);
        fieldValues.put(VERSION, Version);
        return this;
    }

    public ND setFlowLabel(byte[] srcFlowLabel) {
        fieldValues.put(FLOW_LABEL, srcFlowLabel);
        return this;
    }

    public ND setPayloadLen(short payloadLen) {
        byte[] payloadLength = BitBufferHelper.toByteArray(payloadLen);
        fieldValues.put(PAY_LOAD_LEN, payloadLength);
        return this;
    }

    public ND setNextHeaderLen(byte nextHeaderLen) {
        byte[] nextHeaderLength = BitBufferHelper
                .toByteArray(nextHeaderLen);
        fieldValues.put(NEXT_HEADER_LEN, nextHeaderLength);
        return this;
    }

    public ND setHopLimit(byte hopLimit) {
        byte[] HopLimit = BitBufferHelper
                .toByteArray(hopLimit);
        fieldValues.put(HOP_LIMIT, HopLimit);
        return this;
    }

    public ND setSrcIpAddress(byte[] srcIpAddress) {
        fieldValues.put(SRC_IP, srcIpAddress);
        return this;
    }

    public ND setDestIpAddress(byte[] destIpAddress) {
        fieldValues.put(DEST_IP, destIpAddress);
        return this;
    }

    public ND setIcmp6Type(byte icmp6Type) {
        byte[] Icmpv6Type = BitBufferHelper
                .toByteArray(icmp6Type);
        fieldValues.put(ICMP6_TYPE, Icmpv6Type);
        return this;
    }

    public ND setIcmp6Code(byte icmp6Code) {
        byte[] Icmpv6Code = BitBufferHelper
                .toByteArray(icmp6Code);
        fieldValues.put(ICMP6_CODE, Icmpv6Code);
        return this;
    }

    public ND setIcmp6Checksum(short icmp6Checksum) {
        byte[] Icmpv6Chksum = BitBufferHelper.toByteArray(icmp6Checksum);
        fieldValues.put(ICMP6_CHKSUM, Icmpv6Chksum);
        return this;
    }

    public ND setFlags(byte[] flags) {
        fieldValues.put(FLAGS, flags);
        return this;
    }

    public ND setTargetAddress(byte[] targetAddress) {
        fieldValues.put(TARGET_ADDR, targetAddress);
        return this;
    }



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + (fieldValues == null ? 0 : fieldValues.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ND other = (ND) obj;
        if (fieldValues == null) {
            if (other.fieldValues != null) {
                return false;
            }
        } else if (!fieldValues.equals(other.fieldValues)) {
            return false;
        }
        return true;
    }
}
