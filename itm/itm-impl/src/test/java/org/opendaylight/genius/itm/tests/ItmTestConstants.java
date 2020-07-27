/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;

public interface ItmTestConstants {

    String EXT_TUNNEL_INTERFACE_NAME = "tunfdeecc7cb4d";
    Uint64 DP_ID_1 = Uint64.ONE;
    Uint64 DP_ID_2 = Uint64.valueOf(2);
    int VLAN_ID = 100 ;
    String PORT_NAME_1 = "phy0";
    String PARENT_INTERFACE_NAME = "1:phy0:100";
    String TEP_IP_3 = "192.168.56.101";
    String GWY_IP_1 = "0.0.0.0";
    String TZ_NAME = "TZA" ;
    String SUBNET_IP = "10.1.1.24";
    String SOURCE_DEVICE = "abc";
    String SOURCE_DEVICE_2 = "def";
    String DESTINATION_DEVICE = "xyz";
    IpPrefix IP_PREFIX_TEST = IpPrefixBuilder.getDefaultInstance(SUBNET_IP + "/24");
    IpAddress IP_ADDRESS_3 = IpAddressBuilder.getDefaultInstance(TEP_IP_3);
    IpAddress GTWY_IP_1 = IpAddressBuilder.getDefaultInstance(GWY_IP_1);
    Class<? extends TunnelTypeBase> TUNNEL_TYPE_VXLAN = TunnelTypeVxlan.class;
    Class<? extends TunnelTypeBase> TUNNEL_TYPE_MPLS_OVER_GRE = TunnelTypeMplsOverGre.class;
    Class<? extends TunnelMonitoringTypeBase> MONITOR_PROTOCOL = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
    String  DPID_STR_ONE = "1";
    String OTHER_CFG_TEP_IP_KEY = "local_ip";
    String EXTERNAL_ID_TZNAME_KEY = "transport-zone";
    String EXTERNAL_ID_BR_NAME_KEY = "br-name";

    String LOCALHOST_IP = "127.0.0.1";
    Uint16 OVSDB_CONN_PORT = Uint16.valueOf(6640);

    String DEF_TZ_TEP_IP = "192.168.56.30";
    String NB_TZ_TEP_IP = "192.168.56.40";

    String DEF_BR_NAME = "br-int";
    String DEF_BR_DPID = "00:00:00:00:00:00:00:01";
    Uint64 INT_DEF_BR_DPID = Uint64.ONE;

    String BR2_NAME = "br2";
    String BR2_DPID = "00:00:00:00:00:00:00:02";
    Uint64 INT_BR2_DPID = Uint64.valueOf(2);

    //not hosted tz constants
    String NOT_HOSTED_TZ_TEP_IP = "192.168.10.20";
    String NOT_HOSTED_TZ_TEPDPN_ID = "0";
    Uint64 NOT_HOSTED_INT_TZ_TEPDPN_ID = Uint64.ZERO;
    String NOT_HOSTED_TZ_NAME = "NotHostedTZ";
    Boolean OF_TUNNEL = false;
    String NOT_HOSTED_DEF_BR_DPID = "00:00:00:00:00:00:00:00";
}
