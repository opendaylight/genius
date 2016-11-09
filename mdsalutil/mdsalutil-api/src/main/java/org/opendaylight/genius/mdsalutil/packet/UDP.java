/*
 * Copyright (c) 2013 - 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.packet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.Packet;

/**
 * Class that represents the UDP datagram objects
 */

public class UDP extends Packet {

    private static final String SRCPORT = "SourcePort";
    private static final String DESTPORT = "DestinationPort";
    private static final String LENGTH = "Length";
    private static final String CHECKSUM = "Checksum";

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;
        {
            put(SRCPORT, new ImmutablePair<>(0, 16));
            put(DESTPORT, new ImmutablePair<>(16, 16));
            put(LENGTH, new ImmutablePair<>(32, 16));
            put(CHECKSUM, new ImmutablePair<>(48, 16));
        }
    };

    public UDP() {
        super();
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort(0);
        setDestinationPort(0);
        setChecksum((short) 0);
    }

    public UDP(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort(0);
        setDestinationPort(0);
        setChecksum((short) 0);
    }

    private final Map<String, byte[]> fieldValues;

    /* public static Map<Short, Class<? extends Packet>> decodeMap;

      static {
          decodeMap = new HashMap<Short, Class<? extends Packet>>();
          UDP.decodeMap.put((short)67, DHCP.class);
          UDP.decodeMap.put((short)68, DHCP.class);
      }*/
    /**
     * Get the stored source port
     * @return int - the sourcePort
     */
    public int getSourcePort() {
        return (BitBufferHelper.getInt(fieldValues.get(SRCPORT)));
    }

    /**
     * Get the stored destination port
     * @return int - the destinationPort
     */
    public int getDestinationPort() {
        return (BitBufferHelper.getInt(fieldValues.get(DESTPORT)));
    }

    /**
     * Gets the stored length of UDP header
     * @return short - the length
     */
    public short getLength() {
        return (BitBufferHelper.getShort(fieldValues.get(LENGTH)));
    }

    /**
     * Get the stored checksum value of the UDP header
     * @return short - the checksum
     */
    public short getChecksum() {
        return (BitBufferHelper.getShort(fieldValues.get(CHECKSUM)));
    }

    @Override
    /**
     * Store the value read from data stream in hdrFieldMap
     */
    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Sets the sourcePort value for the current UDP object instance
     * @param udpSourcePort int source port to set
     * @return UDP
     */
    public UDP setSourcePort(int udpSourcePort) {
        byte[] sourcePort = BitBufferHelper
                .toByteArray(udpSourcePort);
        byte[] port = Arrays.copyOfRange(sourcePort, 2, 4);
        fieldValues.put(SRCPORT, port);
        return this;
    }

    /**
     * Sets the destinationPort value for the current UDP object instance
     * @param udpDestinationPort int destination port to set
     * @return UDP
     */
    public UDP setDestinationPort(int udpDestinationPort) {
        byte[] destinationPort = BitBufferHelper
                .toByteArray(udpDestinationPort);
        byte[] port = Arrays.copyOfRange(destinationPort, 2, 4);
        fieldValues.put(DESTPORT, port);
        return this;
    }

    /**
     * Set the UDP header length value for the current UDP object instance
     * @param udpLength - short - the length to set
     * @return UDP
     */
    public UDP setLength(short udpLength) {
        byte[] length = BitBufferHelper.toByteArray(udpLength);
        fieldValues.put(LENGTH, length);
        return this;
    }

    /**
     * Set the checksum for the current UDP object instance
     * @param udpChecksum - short - the checksum to set
     * @return UDP
     */
    public UDP setChecksum(short udpChecksum) {
        byte[] checksum = BitBufferHelper.toByteArray(udpChecksum);
        fieldValues.put(CHECKSUM, checksum);
        return this;
    }

}
