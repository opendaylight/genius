/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.globals;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.yang.common.Uint64;


public final class  IfmConstants {

    private IfmConstants() {
    }

    public static final String OF_URI_PREFIX = "openflow:";
    public static final String OF_URI_SEPARATOR = ":";
    public static final int DEFAULT_IFINDEX = 65536;
    public static final int FLOW_HIGH_PRIORITY = 10;
    public static final int FLOW_PRIORITY_FOR_UNTAGGED_VLAN = 4;
    public static final int FLOW_TABLE_MISS_PRIORITY = 0;
    public static final int DEFAULT_ARP_FLOW_PRIORITY = 100;
    public static final int INVALID_PORT_NO = -1;

    // Id pool
    public static final String IFM_IDPOOL_NAME = "interfaces";
    public static final long IFM_ID_POOL_START = 1L;
    public static final long IFM_ID_POOL_END = 65535;

    public static final long VXLAN_GROUPID_MIN = 300000L;
    // Group Prefix
    public static final long VLAN_GROUP_START = 1000;
    public static final long TRUNK_GROUP_START = 20000;
    public static final long LOGICAL_GROUP_START = 100000;
    // Table
    public static final long DELAY_TIME_IN_MILLISECOND = 10000;
    // Cookies
    public static final Uint64 COOKIE_L3_BASE = Uint64.valueOf("8000000", 16).intern();
    public static final Uint64 COOKIE_EGRESS_DISPATCHER_TABLE = Uint64.valueOf("1300000", 16).intern();
    // Tunnel Monitoring
    public static final int DEFAULT_MONITOR_INTERVAL = 10000;

    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    // TUNNEL TYPE KEYWORDS
    // These are the reserved keywords to be used for service-binding on tunnel-type
    public static final String ALL_VXLAN_INTERNAL = "all_vxlan_internal";
    public static final String ALL_VXLAN_EXTERNAL = "all_vxlan_external";
    public static final String ALL_MPLS_OVER_GRE = "all_mpls_over_gre";
}
