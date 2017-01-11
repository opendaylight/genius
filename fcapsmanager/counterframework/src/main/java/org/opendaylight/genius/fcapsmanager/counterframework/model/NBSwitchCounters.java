/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.math.BigInteger;
import java.util.List;

public class NBSwitchCounters {
    private BigInteger flow_datapath_id ;
    private BigInteger ports;
    private BigInteger packet_in_messages_received;
    private BigInteger packet_out_messages_sent;
    private List<NBSwitchPortCounters> switch_port_counters;
    private List<NBTableCounters> table_counters;

    public NBSwitchCounters() {
        setPacket_in_messages_received(BigInteger.ZERO);
        setPacket_out_messages_sent(BigInteger.ZERO);
        setPorts(BigInteger.ZERO);
    }

    public BigInteger getFlow_datapath_id() {
        return flow_datapath_id;
    }

    public void setFlow_datapath_id(BigInteger flow_datapath_id) {
        this.flow_datapath_id = flow_datapath_id;
    }

    public BigInteger getPorts() {
        return ports;
    }

    public void setPorts(BigInteger ports) {
        this.ports = ports;
    }

    public BigInteger getPacket_in_messages_received() {
        return packet_in_messages_received;
    }

    public void setPacket_in_messages_received(BigInteger packet_in_messages_received) {
        this.packet_in_messages_received = packet_in_messages_received;
    }

    public BigInteger getPacket_out_messages_sent() {
        return packet_out_messages_sent;
    }

    public void setPacket_out_messages_sent(BigInteger packet_out_messages_sent) {
        this.packet_out_messages_sent = packet_out_messages_sent;
    }

    public List<NBSwitchPortCounters> getSwitch_port_counters() {
        return switch_port_counters;
    }

    public void setSwitch_port_counters(List<NBSwitchPortCounters> switch_port_counters) {
        this.switch_port_counters = switch_port_counters;
    }

    public List<NBTableCounters> getTable_counters() {
        return table_counters;
    }

    public void setTable_counters(List<NBTableCounters> table_counters) {
        this.table_counters = table_counters;
    }
}