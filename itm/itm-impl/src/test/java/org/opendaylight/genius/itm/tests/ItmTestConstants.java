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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;

import java.math.BigInteger;

public interface ItmTestConstants {
    String extTunnelInterfaceName = "tunfdeecc7cb4d";
    BigInteger dpId1 = BigInteger.valueOf(1);
    BigInteger dpId2 = BigInteger.valueOf(2);
    int vlanId = 100 ;
    String portName1 = "phy0";
    String parentInterfaceName = "1:phy0:100";
    String tepIp3 = "192.168.56.101";
    String gwyIp1 = "0.0.0.0";
    String TZ_NAME = "TZA" ;
    String subnetIp = "10.1.1.24";
    String sourceDevice = "abc";
    String sourceDevice2 = "def";
    String destinationDevice = "xyz";
    IpPrefix ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
    IpAddress  ipAddress3 = IpAddressBuilder.getDefaultInstance(tepIp3);
    IpAddress gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
    Class<? extends TunnelTypeBase> TUNNEL_TYPE_VXLAN = TunnelTypeVxlan.class;
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
    String  DPID_STR_ONE="1";

    String EXTERNAL_ID_TEP_IP_KEY = "tep-ip";
    String EXTERNAL_ID_TZNAME_KEY = "tzname";
    String EXTERNAL_ID_DPN_BR_NAME_KEY = "dpn-br-name";

    String LOCALHOST_IP = "127.0.0.1";
    int OVSDB_CONN_PORT = 6640;

    String DEF_TZ_TEP_IP = "192.168.56.30";
    String NB_TZ_TEP_IP = "192.168.56.40";

    String DEF_BR_NAME = "br-int";
    String DEF_BR_DPID = "00:00:00:00:00:00:00:01";
    BigInteger INT_DEF_BR_DPID = BigInteger.valueOf(1);

    String BR2_NAME = "br2";
    String BR2_DPID = "00:00:00:00:00:00:00:02";
    BigInteger INT_BR2_DPID = BigInteger.valueOf(2);

    //not hosted tz constants
    String NOT_HOSTED_TZ_TEP_IP = "192.168.10.20";
    String NOT_HOSTED_TZ_TEPDPN_ID = "0";
    BigInteger NOT_HOSTED_INT_TZ_TEPDPN_ID = BigInteger.valueOf(0);
    String NOT_HOSTED_TZ_NAME = "NotHostedTZ";
    Boolean OF_TUNNEL = false;
    String NOT_HOSTED_DEF_BR_DPID = "00:00:00:00:00:00:00:00";
}
