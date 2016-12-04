/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.globals;

import java.math.BigInteger;

public class IfmConstants {
    public static final String OF_URI_PREFIX = "openflow:";
    public static final String OF_URI_SEPARATOR = ":";
    public static final int DEFAULT_IFINDEX = 65536;
    public static final int FLOW_HIGH_PRIORITY = 10;
    public static final int FLOW_PRIORITY_FOR_UNTAGGED_VLAN = 4;
    public static final int FLOW_TABLE_MISS_PRIORITY = 0;
    public static final int DEFAULT_ARP_FLOW_PRIORITY = 100;
    public static final int INVALID_PORT_NO = -1;
    public static final BigInteger INVALID_DPID = new BigInteger("-1");
    //Id pool
    public static final String IFM_IDPOOL_NAME = "interfaces";
    public static final long IFM_ID_POOL_START = 1L;
    public static final long IFM_ID_POOL_END = 65535;
    //Group Prefix
    public static final long VLAN_GROUP_START = 1000;
    public static final long TRUNK_GROUP_START = 20000;
    public static final long LOGICAL_GROUP_START = 100000;
    //Table
    public static final long DELAY_TIME_IN_MILLISECOND = 10000;
    //Cookies
    public static final BigInteger COOKIE_L3_BASE = new BigInteger("8000000", 16);
    public static final BigInteger COOKIE_EGRESS_DISPATCHER_TABLE = new BigInteger("1300000", 16);
    //Tunnel Monitoring
    public static final int DEFAULT_MONITOR_INTERVAL = 10000;

    public static final String PORT_LBAAS_OCTAVIA_HM_INTERFACE = "o-hm0"; // FIXME is this truly static? should be configurable.
}
