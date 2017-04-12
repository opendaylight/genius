/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.api;

public final class ArpConstants {

    private ArpConstants() {
        // prevent instantiation
    }

    public static final String PREFIX = "/32";
    public static final String NODE_CONNECTOR_NOT_FOUND_ERROR = "Node connector id not found for interface %s";
    public static final String FAILED_TO_GET_SRC_IP_FOR_INTERFACE = "Failed to get src ip for %s";
    public static final String FAILED_TO_GET_SRC_MAC_FOR_INTERFACE = "Failed to get src mac for interface %s iid %s ";
    public static final String FAILED_TO_SEND_ARP_REQ_FOR_INTERFACE = "failed to send arp req for interface ";
    public static final String UNKNOWN_IP_ADDRESS_SUPPLIED = "unknown ip address supplied";
    public static final String DPN_NOT_FOUND_ERROR = "dpn not found for interface %s ";
    public static final short ARP_REQUEST_OP = (short) 1;
    public static final short ARP_RESPONSE_OP = (short) 2;
    public static final short ETH_TYPE_ARP = 0x0806;
    public static final String ARPJOB = "Arpcache";
    public static final long DEFAULT_ARP_LEARNED_CACHE_TIMEOUT = 120000; /* 120 seconds = 2 minutes */
    public static final String ARP_CACHE_TIMEOUT_PROP = "arp.cache.timeout";
    public static final long ARP_CACHE_TIMEOUT_MILLIS = Long.getLong(ArpConstants.ARP_CACHE_TIMEOUT_PROP,
        ArpConstants.DEFAULT_ARP_LEARNED_CACHE_TIMEOUT);
    public static final long FAILURE_THRESHOLD = 2;
    public static final long MONITORING_WINDOW = 4;

    public static long getArpCacheTimeoutMillis() {
        return ArpConstants.ARP_CACHE_TIMEOUT_MILLIS;
    }
}
