/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
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

/**
 * Class that represents the ARP packet objects
 * taken from opendaylight(helium) adsal bundle.
 */
public class ARP extends Packet {
    private static final String HWTYPE = "HardwareType";
    private static final String PTYPE = "ProtocolType";
    private static final String HWADDRLENGTH = "HardwareAddressLength";
    private static final String PADDRLENGTH = "ProtocolAddressLength";
    private static final String OPCODE = "OpCode";
    private static final String SENDERHWADDR = "SenderHardwareAddress";
    private static final String SENDERPADDR = "SenderProtocolAddress";
    private static final String TARGETHWADDR = "TargetHardwareAddress";
    private static final String TARGETPADDR = "TargetProtocolAddress";

    public static final short HW_TYPE_ETHERNET = (short) 0x1;
    public static final short REQUEST = (short) 0x1;
    public static final short REPLY = (short) 0x2;

    public static final short PROTO_TYPE_IP = 0x800;

    @SuppressWarnings("serial")
    private static Map<String, Pair<Integer, Integer>> fieldCoordinates
        = new LinkedHashMap<String, Pair<Integer, Integer>>() { {
                put(HWTYPE, new ImmutablePair<>(0, 16));
                put(PTYPE, new ImmutablePair<>(16, 16));
                put(HWADDRLENGTH, new ImmutablePair<>(32, 8));
                put(PADDRLENGTH, new ImmutablePair<>(40, 8));
                put(OPCODE, new ImmutablePair<>(48, 16));
                put(SENDERHWADDR, new ImmutablePair<>(64, 48));
                put(SENDERPADDR, new ImmutablePair<>(112, 32));
                put(TARGETHWADDR, new ImmutablePair<>(144, 48));
                put(TARGETPADDR, new ImmutablePair<>(192, 32));
            }
        };

    private final Map<String, byte[]> fieldValues;

    /**
     * Default constructor that creates and sets the HashMap.
     */
    public ARP() {
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    public ARP(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    public short getHardwareType() {
        return BitBufferHelper.getShort(fieldValues.get(HWTYPE));

    }

    public short getProtocolType() {
        return BitBufferHelper.getShort(fieldValues.get(PTYPE));
    }

    public byte getHardwareAddressLength() {
        return BitBufferHelper.getByte(fieldValues.get(HWADDRLENGTH));
    }

    public byte getProtocolAddressLength() {
        return BitBufferHelper.getByte(fieldValues.get(PADDRLENGTH));
    }

    public short getOpCode() {
        return BitBufferHelper.getShort(fieldValues.get(OPCODE));
    }

    public byte[] getSenderHardwareAddress() {
        return fieldValues.get(SENDERHWADDR);
    }

    public byte[] getSenderProtocolAddress() {
        return fieldValues.get(SENDERPADDR);
    }

    public byte[] getTargetHardwareAddress() {
        return fieldValues.get(TARGETHWADDR);
    }

    public ARP setHardwareType(short hardwareType) {
        byte[] hwType = BitBufferHelper.toByteArray(hardwareType);
        fieldValues.put(HWTYPE, hwType);
        return this;
    }

    public ARP setProtocolType(short protocolType) {
        byte[] protType = BitBufferHelper.toByteArray(protocolType);
        fieldValues.put(PTYPE, protType);
        return this;
    }

    public ARP setHardwareAddressLength(byte hardwareAddressLength) {
        byte[] hwAddressLength = BitBufferHelper
                .toByteArray(hardwareAddressLength);
        fieldValues.put(HWADDRLENGTH, hwAddressLength);
        return this;
    }

    public ARP setProtocolAddressLength(byte protocolAddressLength) {
        byte[] protocolAddrLength = BitBufferHelper
                .toByteArray(protocolAddressLength);
        fieldValues.put(PADDRLENGTH, protocolAddrLength);
        return this;
    }

    public ARP setOpCode(short opCode) {
        byte[] operationCode = BitBufferHelper.toByteArray(opCode);
        fieldValues.put(OPCODE, operationCode);
        return this;
    }

    public ARP setSenderHardwareAddress(byte[] senderHardwareAddress) {
        fieldValues.put(SENDERHWADDR, senderHardwareAddress);
        return this;
    }

    public ARP setTargetHardwareAddress(byte[] targetHardwareAddress) {
        fieldValues.put(TARGETHWADDR, targetHardwareAddress);
        return this;
    }

    public ARP setTargetProtocolAddress(byte[] targetProtocolAddress) {
        fieldValues.put(TARGETPADDR, targetProtocolAddress);
        return this;
    }

    public ARP setSenderProtocolAddress(byte[] senderIP) {
        fieldValues.put(SENDERPADDR, senderIP);
        return this;
    }

    public byte[] getTargetProtocolAddress() {
        return fieldValues.get(TARGETPADDR);
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
        ARP other = (ARP) obj;
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
