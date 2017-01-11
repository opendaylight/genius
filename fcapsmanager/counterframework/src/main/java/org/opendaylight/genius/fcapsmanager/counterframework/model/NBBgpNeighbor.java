/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.math.BigInteger;

public class NBBgpNeighbor {
    private BigInteger autonomous_system_number;
    private String neighbor_ip;
    private BigInteger packets_received;
    private BigInteger packets_sent;

    public BigInteger getAutonomous_system_number() {
        return autonomous_system_number;
    }

    public void setAutonomous_system_number(BigInteger autonomous_system_number) {
        this.autonomous_system_number = autonomous_system_number;
    }

    public String getNeighbor_ip() {
        return neighbor_ip;
    }

    public void setNeighbor_ip(String neighbor_ip) {
        this.neighbor_ip = neighbor_ip;
    }

    public BigInteger getPackets_received() {
        return packets_received;
    }

    public void setPackets_received(BigInteger packets_received) {
        this.packets_received = packets_received;
    }

    public BigInteger getPackets_sent() {
        return packets_sent;
    }

    public void setPackets_sent(BigInteger packets_sent) {
        this.packets_sent = packets_sent;
    }
}