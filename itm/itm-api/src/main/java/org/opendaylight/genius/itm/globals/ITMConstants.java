/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.globals;

import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;

public interface ITMConstants {
    String ITM_SERVICE_NAME = "ITM";
    BigInteger COOKIE_ITM = new BigInteger("9000000", 16);
    BigInteger COOKIE_ITM_EXTERNAL = new BigInteger("9050000", 16);
    String ITM_IDPOOL_SIZE = "100000";

    long DELAY_TIME_IN_MILLISECOND = 5000;
    int REG6_START_INDEX = 0;
    int REG6_END_INDEX = 31;
    int JOB_MAX_RETRIES = 6;

    int LLDP_SERVICE_ID = 0;
    // Tunnel Monitoring
    boolean DEFAULT_MONITOR_ENABLED = false;
    int DEFAULT_MONITOR_INTERVAL = 10000;
    int BFD_DEFAULT_MONITOR_INTERVAL = 1000;
    int MIN_MONITOR_INTERVAL = 1000;
    int MAX_MONITOR_INTERVAL = 30000;
    String DUMMY_IP_ADDRESS = "0.0.0.0";
    String TUNNEL_TYPE_VXLAN = "VXLAN";
    String TUNNEL_TYPE_GRE = "GRE";
    // FIXME: the following annotation should be removed once the itm-impl
    // changes the name
    @SuppressWarnings("checkstyle:ConstantName")
    String TUNNEL_TYPE_MPLSoGRE = "MPLS_OVER_GRE";
    String TUNNEL_TYPE_LOGICAL_GROUP_VXLAN = "LOGICAL_VXLAN_GROUP";
    String TUNNEL_TYPE_INVALID = "Invalid";
    String MONITOR_TYPE_LLDP = "LLDP";
    String MONITOR_TYPE_BFD = "BFD";
    String DEFAULT_TRANSPORT_ZONE = "default-transport-zone";
    Class<? extends TunnelMonitoringTypeBase> DEFAULT_MONITOR_PROTOCOL
            = TunnelMonitoringTypeBfd.class;
    String INTERNAL_TUNNEL_CACHE_NAME = "InternalTunnelCache";
    String EXTERNAL_TUNNEL_CACHE_NAME = "ExternalTunnelCache";
    String UNPROCESSED_TUNNELS_CACHE_NAME = "ItmUnprocessedTunnelsCache";
    String TUNNEL_STATE_UP = "UP";
    String TUNNEL_STATE_DOWN = "DOWN";
    String TUNNEL_STATE_UNKNOWN = "UNKNOWN";
    String DUMMY_PREFIX = "255.255.255.255/32";
    String DUMMY_GATEWAY_IP = "0.0.0.0";
    String DUMMY_PORT = "";
    int DUMMY_VLANID = 0;
    String DEFAULT_BRIDGE_NAME = "br-int";
    String BRIDGE_URI_PREFIX = "bridge";
    String ITM_PREFIX = "ITM";
    String ITM_ALARM = "ITM_ALARM";
    // Southbound side OVSDB ExternalIds list TEP parameters
    String OTH_CFG_TEP_PARAM_KEY_LOCAL_IP = "local_ip";
    String EXT_ID_TEP_PARAM_KEY_TZNAME = "transport-zone";
    String EXT_ID_TEP_PARAM_KEY_BR_NAME = "br-name";
    String EXT_ID_TEP_PARAM_KEY_OF_TUNNEL = "of-tunnel";

    // ITM DIRECT TUNNELS RELATED CONSTANTS
    String ITM_CONFIG_ENTITY = "itm_config";
    int INVALID_PORT_NO = -1;
    int DEFAULT_FLOW_PRIORITY = 5;
    String OF_URI_SEPARATOR = ":";
    String UNPROCESSED_NODE_CONNECTOR_CACHE = "UnprocessedNCCache";
}
