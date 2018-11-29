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
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.openflowplugin.libraries.liblldp.BitBufferHelper;
import org.opendaylight.openflowplugin.libraries.liblldp.EtherTypes;
import org.opendaylight.openflowplugin.libraries.liblldp.LLDP;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;

/**
 * Class that represents the Ethernet frame objects
 * taken from opendaylight(helium) adsal bundle.
 */
public class Ethernet extends Packet {
    private static final String DMAC = "DestinationMACAddress";
    private static final String SMAC = "SourceMACAddress";
    private static final String ETHT = "EtherType";

    // TODO: This has to be outside and it should be possible for osgi
    // to add new coming packet classes
    @SuppressWarnings("checkstyle:ConstantName") // public constant is used in other projects; too late to rename easily
    public static final Map<Short, Class<? extends Packet>> etherTypeClassMap = new ConcurrentHashMap<>();

    static {
        etherTypeClassMap.put(EtherTypes.ARP.shortValue(), ARP.class);
        etherTypeClassMap.put(EtherTypes.LLDP.shortValue(), LLDP.class);
        etherTypeClassMap.put(EtherTypes.IPv4.shortValue(), IPv4.class);
        // TODO: Add support for more classes here
        etherTypeClassMap.put(EtherTypes.VLANTAGGED.shortValue(), IEEE8021Q.class);
        // etherTypeClassMap.put(EtherTypes.OLDQINQ.shortValue(), IEEE8021Q.class);
        // etherTypeClassMap.put(EtherTypes.QINQ.shortValue(), IEEE8021Q.class);
        // etherTypeClassMap.put(EtherTypes.CISCOQINQ.shortValue(), IEEE8021Q.class);
    }

    @SuppressWarnings("serial")
    private static Map<String, Pair<Integer, Integer>> fieldCoordinates
        = new LinkedHashMap<String, Pair<Integer, Integer>>() { {
                put(DMAC, new ImmutablePair<>(0, 48));
                put(SMAC, new ImmutablePair<>(48, 48));
                put(ETHT, new ImmutablePair<>(96, 16));
            }
        };
    private final Map<String, byte[]> fieldValues;

    /**
     * Default constructor that creates and sets the HashMap.
     */
    public Ethernet() {
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    /**
     * Constructor that sets the access level for the packet and
     * creates and sets the HashMap.
     * @param writeAccess boolean
     */
    public Ethernet(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }

    @Override
    public void setHeaderField(String headerField, byte[] readValue) {
        if (headerField.equals(ETHT)) {
            payloadClass = etherTypeClassMap.get(BitBufferHelper
                    .getShort(readValue));
        }
        hdrFieldsMap.put(headerField, readValue);
    }

    public byte[] getDestinationMACAddress() {
        return fieldValues.get(DMAC);
    }

    public byte[] getSourceMACAddress() {
        return fieldValues.get(SMAC);
    }

    public short getEtherType() {
        return BitBufferHelper.getShort(fieldValues.get(ETHT));
    }

    public Ethernet setDestinationMACAddress(byte[] destinationMACAddress) {
        fieldValues.put(DMAC, destinationMACAddress);
        return this;
    }

    public Ethernet setSourceMACAddress(byte[] sourceMACAddress) {
        fieldValues.put(SMAC, sourceMACAddress);
        return this;
    }

    public Ethernet setEtherType(short etherType) {
        byte[] ethType = BitBufferHelper.toByteArray(etherType);
        fieldValues.put(ETHT, ethType);
        return this;
    }

}
