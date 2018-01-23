/*
 * Copyright (c) 2013 - 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
 * Class that represents the TCP segment objects.
 */
public class TCP extends Packet {

    public static final String SRCPORT = "SourcePort";
    public static final String DESTPORT = "DestinationPort";
    public static final String SEQNUMBER = "SequenceNumber";
    public static final String ACKNUMBER = "AcknowledgementNumber";
    public static final String DATAOFFSET = "DataOffset";
    public static final String RESERVED = "Reserved";
    public static final String HEADERLENFLAGS = "HeaderLenFlags";
    public static final String WINDOWSIZE = "WindowSize";
    public static final String CHECKSUM = "Checksum";
    public static final String URGENTPOINTER = "UrgentPointer";

    @SuppressWarnings("serial")
    private static Map<String, Pair<Integer, Integer>> fieldCoordinates
        = new LinkedHashMap<String, Pair<Integer, Integer>>() { {
                put(SRCPORT, new ImmutablePair<>(0, 16));
                put(DESTPORT, new ImmutablePair<>(16, 16));
                put(SEQNUMBER, new ImmutablePair<>(32, 32));
                put(ACKNUMBER, new ImmutablePair<>(64, 32));
                put(DATAOFFSET, new ImmutablePair<>(96, 4));
                put(RESERVED, new ImmutablePair<>(100, 3));
                put(HEADERLENFLAGS, new ImmutablePair<>(103, 9));
                put(WINDOWSIZE, new ImmutablePair<>(112, 16));
                put(CHECKSUM, new ImmutablePair<>(128, 16));
                put(URGENTPOINTER, new ImmutablePair<>(144, 16));
            }
        };

    private final Map<String, byte[]> fieldValues;

    /**
     * Default constructor that sets all the header fields to zero.
     */
    public TCP() {
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort((short)0);
        setDestinationPort((short)0);
        setSequenceNumber(0);
        setAckNumber(0);
        setDataOffset((byte) 0);
        setReserved((byte) 0);
        setWindowSize((short) 0);
        setUrgentPointer((short) 0);
        setChecksum((short) 0);
    }

    /**
     * Constructor that sets the access level for the packet and
     * sets all the header fields to zero.
     * @param writeAccess - boolean
     */
    public TCP(boolean writeAccess) {
        super(writeAccess);
        fieldValues = new HashMap<>();
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
        /* Setting all remaining header field values to
         * default value of 0.  These maybe changed as needed
         */
        setSourcePort((short) 0);
        setDestinationPort((short) 0);
        setSequenceNumber(0);
        setAckNumber(0);
        setDataOffset((byte) 0);
        setReserved((byte) 0);
        setWindowSize((short) 0);
        setUrgentPointer((short) 0);
        setChecksum((short) 0);
    }

    @Override
    /**
     * Stores the value read from data stream
     * @param headerField - String
     * @param readValue - byte[]
     */
    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    /**
     * Sets the TCP source port for the current TCP object instance.
     * @param tcpSourcePort short
     * @return TCP
     */
    public TCP setSourcePort(short tcpSourcePort) {
        byte[] sourcePort = BitBufferHelper.toByteArray(tcpSourcePort);
        fieldValues.put(SRCPORT, sourcePort);
        return this;
    }

    /**
     * Sets the TCP destination port for the current TCP object instance.
     * @param tcpDestinationPort short
     * @return TCP
     */
    public TCP setDestinationPort(short tcpDestinationPort) {
        byte[] destinationPort = BitBufferHelper
                .toByteArray(tcpDestinationPort);
        fieldValues.put(DESTPORT, destinationPort);
        return this;
    }

    /**
     * Sets the TCP sequence number for the current TCP object instance.
     * @param tcpSequenceNumber - int
     * @return TCP
     */
    public TCP setSequenceNumber(int tcpSequenceNumber) {
        byte[] sequenceNumber = BitBufferHelper.toByteArray(tcpSequenceNumber);
        fieldValues.put(SEQNUMBER, sequenceNumber);
        return this;
    }

    /**
     * Sets the TCP data offset for the current TCP object instance.
     * @param tcpDataOffset - byte
     * @return TCP
     */
    public TCP setDataOffset(byte tcpDataOffset) {
        byte[] offset = BitBufferHelper.toByteArray(tcpDataOffset);
        fieldValues.put("DataOffset", offset);
        return this;
    }

    /**
     * Sets the TCP reserved bits for the current TCP object instance.
     * @param tcpReserved byte
     * @return TCP
     */
    public TCP setReserved(byte tcpReserved) {
        byte[] reserved = BitBufferHelper.toByteArray(tcpReserved);
        fieldValues.put("Reserved", reserved);
        return this;
    }

    /**
     * Sets the TCP Ack number for the current TCP object instance.
     * @param tcpAckNumber int
     * @return TCP
     */
    public TCP setAckNumber(int tcpAckNumber) {
        byte[] ackNumber = BitBufferHelper.toByteArray(tcpAckNumber);
        fieldValues.put(ACKNUMBER, ackNumber);
        return this;
    }

    /**
     * Sets the TCP flags for the current TCP object instance.
     * @param tcpFlags short
     * @return TCP
     */
    public TCP setHeaderLenFlags(short tcpFlags) {
        byte[] headerLenFlags = BitBufferHelper.toByteArray(tcpFlags);
        fieldValues.put(HEADERLENFLAGS, headerLenFlags);
        return this;
    }

    /**
     * Sets the TCP window size for the current TCP object instance.
     * @param tcpWsize short
     * @return TCP
     */
    public TCP setWindowSize(short tcpWsize) {
        byte[] wsize = BitBufferHelper.toByteArray(tcpWsize);
        fieldValues.put(WINDOWSIZE, wsize);
        return this;
    }

    /**
     * Sets the TCP checksum for the current TCP object instance.
     * @param tcpChecksum short
     * @return TCP
     */
    public TCP setChecksum(short tcpChecksum) {
        byte[] checksum = BitBufferHelper.toByteArray(tcpChecksum);
        fieldValues.put(CHECKSUM, checksum);
        return this;
    }

    /**
     * Sets the TCP Urgent Pointer for the current TCP object instance.
     * @param tcpUrgentPointer short
     * @return TCP
     */
    public TCP setUrgentPointer(short tcpUrgentPointer) {
        byte[] urgentPointer = BitBufferHelper.toByteArray(tcpUrgentPointer);
        fieldValues.put(URGENTPOINTER, urgentPointer);
        return this;
    }

    /**
     * Gets the stored source port value of TCP header.
     * @return the sourcePort
     */
    public short getSourcePort() {
        return BitBufferHelper.getShort(fieldValues.get(SRCPORT));
    }

    /**
     * Gets the stored destination port value of TCP header.
     * @return the destinationPort
     */
    public short getDestinationPort() {
        return BitBufferHelper.getShort(fieldValues.get(DESTPORT));
    }

    /**
     * Get the stored checksum value of the TCP header.
     * @return short - the checksum
     */
    public short getChecksum() {
        return BitBufferHelper.getShort(fieldValues.get(CHECKSUM));
    }

}
