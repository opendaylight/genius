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
}
