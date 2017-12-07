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

public class ITMConstants {
    public static final BigInteger COOKIE_ITM = new BigInteger("9000000", 16);
    public static final BigInteger COOKIE_ITM_EXTERNAL = new BigInteger("9050000", 16);

    public static final String ITM_IDPOOL_NAME = "Itmservices";
    public static final long ITM_IDPOOL_START = 1L;
    public static final String ITM_IDPOOL_SIZE = "100000";

    public static final long DELAY_TIME_IN_MILLISECOND = 5000;
    public static final int REG6_START_INDEX = 0;
    public static final int REG6_END_INDEX = 31;

    public static final int LLDP_SERVICE_ID = 0;
    // Tunnel Monitoring
    public static final boolean DEFAULT_MONITOR_ENABLED = true;
    public static final int DEFAULT_MONITOR_INTERVAL = 10000;
    public static final int BFD_DEFAULT_MONITOR_INTERVAL = 1000;
    public static final int MIN_MONITOR_INTERVAL = 1000;
    public static final int MAX_MONITOR_INTERVAL = 30000;
    public static final String DUMMY_IP_ADDRESS = "0.0.0.0";
    public static final String TUNNEL_TYPE_VXLAN = "VXLAN";
    public static final String TUNNEL_TYPE_GRE = "GRE";
    // FIXME: the following annotation should be removed once the itm-impl
    // changes the name
    @SuppressWarnings("checkstyle:ConstantName")
    public static final String TUNNEL_TYPE_MPLSoGRE = "MPLS_OVER_GRE";
    public static final String TUNNEL_TYPE_LOGICAL_GROUP_VXLAN = "LOGICAL_VXLAN_GROUP";
    public static final String TUNNEL_TYPE_INVALID = "Invalid";
    public static final String MONITOR_TYPE_LLDP = "LLDP";
    public static final String MONITOR_TYPE_BFD = "BFD";
    public static final String DEFAULT_TRANSPORT_ZONE = "default-transport-zone";
    public static final Class<? extends TunnelMonitoringTypeBase> DEFAULT_MONITOR_PROTOCOL
            = TunnelMonitoringTypeBfd.class;
    public static final String ITM_MONIRORING_PARAMS_CACHE_NAME = "ItmMonitoringParamsCache";
    public static final String TUNNEL_STATE_CACHE_NAME = "ItmTunnelStateCache";
    // FIXME: the following annotation should be removed once the itm-impl
    // changes the name
    @SuppressWarnings("checkstyle:ConstantName")
    public static final String DPN_TEPs_Info_CACHE_NAME = "ItmDpnTepsInfoCache";
    public static final String INTERNAL_TUNNEL_CACHE_NAME = "InternalTunnelCache";
    public static final String EXTERNAL_TUNNEL_CACHE_NAME = "ExternalTunnelCache";
    public static final String TUNNEL_STATE_UP = "UP";
    public static final String TUNNEL_STATE_DOWN = "DOWN";
    public static final String TUNNEL_STATE_UNKNOWN = "UNKNOWN";
    public static final String DUMMY_PREFIX = "255.255.255.255/32";
    public static final String DUMMY_GATEWAY_IP = "0.0.0.0";
    public static final String DUMMY_PORT = "";
    public static final int DUMMY_VLANID = 0;
    public static final String DEFAULT_BRIDGE_NAME = "br-int";
    public static final String BRIDGE_URI_PREFIX = "bridge";
    public static final String ITM_PREFIX = "ITM";
    public static final String ITM_ALARM = "ITM_ALARM";
    // Southbound side OVSDB OtherConfigs list TEP parameters
    public static final String OTH_CFG_TEP_PARAM_KEY_LOCAL_IP = "local_ip";
    // Southbound side OVSDB ExternalIds list TEP parameters
    public static final String EXT_ID_TEP_PARAM_KEY_TZNAME = "tzname";
    public static final String EXT_ID_TEP_PARAM_KEY_BR_NAME = "br-name";
    public static final String EXT_ID_TEP_PARAM_KEY_OF_TUNNEL = "of-tunnel";
}
