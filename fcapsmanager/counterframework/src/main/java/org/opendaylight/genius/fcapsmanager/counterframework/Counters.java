/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework;

public enum Counters {
    NumberOfOFPorts,
    NumberOfEFS,
    InjectedOFMessagesSent,
    InjectedOFMessagesReceive,

    PacketsPerOFPortReceiveDrop,
    PacketsPerOFPortReceiveError,
    OFPortDuration,
    EntriesPerOFTable,
    PacketsPerOFPortSent,
    PacketsPerOFPortReceive,
    BytesPerOFPortSent,
    BytesPerOFPortReceive,

    PacketsPerInternalPortSent,
    PacketsPerInternalPortReceive,

    BgpNeighborPacketsReceived,
    BgpNeighborPacketsSent,
    BgpTotalPrefixes,
    BgpRdRouteCount,
}