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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.*;

import java.math.BigInteger;

public interface ItmTestConstants {
    String extTunnelInterfaceName = "tunfdeecc7cb4d";
    BigInteger dpId1 = BigInteger.valueOf(1);
    BigInteger dpId2 = BigInteger.valueOf(2);
    int vlanId = 100 ;
    String portName1 = "phy0";
    String parentInterfaceName = "1:phy0:100";
    String tepIp3 = "192.168.56.101";
    String gwyIp1 = "192.168.56.105";
    String TZ_NAME = "TZA" ;
    String subnetIp = "192.168.56.100";
    String sourceDevice = "abc";
    String sourceDevice2 = "def";
    String destinationDevice = "xyz";
    IpPrefix ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
    IpAddress  ipAddress3 = IpAddressBuilder.getDefaultInstance(tepIp3);
    IpAddress gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
    Class<? extends TunnelTypeBase> TUNNEL_TYPE_VXLAN = TunnelTypeVxlan.class;
    Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
    String  DPID_STR_ONE="1";

    String tunnelInterfaceName1 = "tun030025bd04f";
    String tunnelInterfaceName2 = "tun9a55a9c38f2";
    String tunnelInterfaceName3 = "tun17c6e20c283";
    String tunnelInterfaceName4 = "tunaa109b6c8c5";
    String tunnelInterfaceName5 = "tund903ed434d5";
    String tunnelInterfaceName6 = "tunc3315b110a6";

    String tunnelInterfaceNameVxLanTwoToOne = "tund63d470c9c9";
    String tunnelInterfaceNameVxLanOneToTwo = "tun5fb197e6890";

    String tunnelInterfaceNameGreTwoToOne = "tun3bd4d241071";
    String tunnelInterfaceNameGreOneToTwo = "tunf2dbe50bb6b";

    String tunnelInterfaceNameBothTwoToOne = "tun3bd4d241071";
    String tunnelInterfaceNameBothOneToTwo = "tun5fb197e6890";

    String source = "hwvtep://192.168.101.30:6640/physicalswitch/s3" ;
    String destination = "hwvtep://192.168.101.40:6640/physicalswitch/s4" ;

    String MC_30_URL_FOR_EXTENDED = "hwvtep://192.168.101.30:6640" ;
    String MC_40_URL_FOR_EXTENDED = "hwvtep://192.168.101.40:6640" ;

    String portName2 = "phy1" ;
    String parentInterfaceName1 = "2:phy1:100" ;
    String tepIp1 = "192.168.56.30";
    String gwyIp2 = "192.168.56.106";
    String tepIp_invalid = "168.56.102";
    String subnetMask_invalid = "192.168.56/24";
    String tepIp2 = "192.168.56.40";
    String tepIp4 = "150.168.56.102";
    String TOPO_ID = "hwvtep:1";
    String subnetMask = subnetIp +"/24";
    String TUNNEL_MON_TYPE_STR= "BFD";
    String gwIpCliDef="0.0.0.0";
    String VXLAN_STR = "VXLAN";
    String PHY_SWI_STR="physicalswitch";
    String TRUNK_INT_STR = "Trunk Interface";
    String DESTINATION_DEV_IP_ADDR_STR = "IpAddress [_ipv4Address=Ipv4Address [_value=192.168.56.40]]";


    IpAddress ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
    IpAddress ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
    IpAddress gtwyIp2 = IpAddressBuilder.getDefaultInstance(gwyIp2);
    IpAddress gtwyIpDef = IpAddressBuilder.getDefaultInstance(gwIpCliDef);

    Class<? extends TunnelTypeBase> TUNNEL_TYPE_GRE = TunnelTypeGre.class;
    Class<? extends TunnelMonitoringTypeBase> TUNNEL_MON_TYPE =TunnelMonitoringTypeBfd.class;

    Boolean OPT_OF_TUNNEL = false;
    Boolean ENABLE_TUN_MON = false ;
    Boolean FALSE_BOOL = false ;

    int ZERO = 0;
    int ONE = 1;
    int MON_INTERVAL = 1000;



}
