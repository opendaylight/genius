/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.math.BigInteger;

public class NBSwitchPortCounters {

    private BigInteger port_id;
    private BigInteger packets_received_drop;
    private BigInteger packets_received_error;
    private BigInteger duration;
    private BigInteger packets_sent;
    private BigInteger packets_received;
    private BigInteger bytes_sent;
    private BigInteger bytes_received;
    private BigInteger packets_sent_on_tunnel;
    private BigInteger packets_received_on_tunnel;

    public NBSwitchPortCounters() {
        setPackets_sent(BigInteger.ZERO);
        setPackets_received(BigInteger.ZERO);
        setPackets_sent_on_tunnel(BigInteger.ZERO);
        setPackets_received_on_tunnel(BigInteger.ZERO);
        setPackets_received_drop(BigInteger.ZERO);
        setPackets_received_error(BigInteger.ZERO);
        setDuration(BigInteger.ZERO);
        setBytes_received(BigInteger.ZERO);
        setBytes_sent(BigInteger.ZERO);
    }

    public BigInteger getPort_id() {
        return port_id;
    }

    public void setPort_id(BigInteger port_id) {
        this.port_id = port_id;
    }

    public BigInteger getPackets_received_drop() {
        return packets_received_drop;
    }

    public void setPackets_received_drop(BigInteger packets_received_drop) {
        this.packets_received_drop = packets_received_drop;
    }

    public BigInteger getPackets_received_error() {
        return packets_received_error;
    }

    public void setPackets_received_error(BigInteger packets_received_error) {
        this.packets_received_error = packets_received_error;
    }

    public BigInteger getDuration() {
        return duration;
    }

    public void setDuration(BigInteger duration) {
        this.duration = duration;
    }

    public BigInteger getPackets_sent() {
        return packets_sent;
    }

    public void setPackets_sent(BigInteger packets_sent) {
        this.packets_sent = packets_sent;
    }

    public BigInteger getPackets_received() {
        return packets_received;
    }

    public void setPackets_received(BigInteger packets_received) {
        this.packets_received = packets_received;
    }

    public BigInteger getBytes_sent() {
        return bytes_sent;
    }

    public void setBytes_sent(BigInteger bytes_sent) {
        this.bytes_sent = bytes_sent;
    }

    public BigInteger getBytes_received() {
        return bytes_received;
    }

    public void setBytes_received(BigInteger bytes_received) {
        this.bytes_received = bytes_received;
    }

    public BigInteger getPackets_sent_on_tunnel() {
        return packets_sent_on_tunnel;
    }

    public void setPackets_sent_on_tunnel(BigInteger packets_sent_on_tunnel) {
        this.packets_sent_on_tunnel = packets_sent_on_tunnel;
    }

    public BigInteger getPackets_received_on_tunnel() {
        return packets_received_on_tunnel;
    }

    public void setPackets_received_on_tunnel(BigInteger packets_received_on_tunnel) {
        this.packets_received_on_tunnel = packets_received_on_tunnel;
    }
}