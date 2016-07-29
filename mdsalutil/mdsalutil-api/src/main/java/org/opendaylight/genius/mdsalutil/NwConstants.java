/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;

public class NwConstants {

    // EthType Values
    public static final int ETHTYPE_802_1Q            = 0X8100;
    public static final int ETHTYPE_IPV4              = 0X0800;
    public static final int ETHTYPE_IPV6              = 0x86dd;
    public static final int ETHTYPE_ARP               = 0X0806;

    public static final int ETHTYPE_MPLS_UC           = 0X8847;
    public static final int ETHTYPE_PBB               = 0X88E7;

    //Protocol Type
    public static final int IP_PROT_ICMP = 1;
    public static final int IP_PROT_TCP = 6;
    public static final int IP_PROT_UDP = 17;
    public static final int IP_PROT_GRE = 47;

    //ARP TYPE
    public static final int ARP_REQUEST = 1;
    public static final int ARP_REPLY = 2;

    //Default Port
    public static final int UDP_DEFAULT_PORT = 4789;


    // Flow Actions
    public static final int ADD_FLOW = 0;
    public static final int DEL_FLOW = 1;
    public static final int MOD_FLOW = 2;

    // Flow Constants
    public static final String FLOWID_SEPARATOR = ".";
    public static final int TABLE_MISS_FLOW = 0;
    public static final int TABLE_MISS_PRIORITY = 0;

    public static final int DEFAULT_ARP_FLOW_PRIORITY = 100;

    public static final short DHCP_SERVICE_INDEX = 1;
    public static final short ACL_SERVICE_INDEX = 2;
    public static final short IPV6_SERVICE_INDEX = 3;
    public static final short SCF_SERVICE_INDEX = 4;
    public static final short L3VPN_SERVICE_INDEX = 5;
    public static final short ELAN_SERVICE_INDEX = 6;

    public static final BigInteger COOKIE_IPV6_TABLE = new BigInteger("4000000", 16);
    public static final BigInteger VLAN_TABLE_COOKIE = new BigInteger("8000000", 16);
    public static final BigInteger COOKIE_VM_INGRESS_TABLE = new BigInteger("8000001", 16);
    public static final BigInteger COOKIE_VM_LFIB_TABLE = new BigInteger("8000002", 16);
    static final BigInteger COOKIE_VM_FIB_TABLE =  new BigInteger("8000003", 16);
    public static final BigInteger COOKIE_DNAT_TABLE = new BigInteger("8000004", 16);
    public static final BigInteger COOKIE_TS_TABLE = new BigInteger("8000005", 16);
    public static final BigInteger COOKIE_SNAT_TABLE = new BigInteger("8000006", 16);
    public static final BigInteger COOKIE_VXLAN_TRUNK_L2_TABLE = new BigInteger("1200000", 16);
    public static final BigInteger COOKIE_GRE_TRUNK_L2_TABLE = new BigInteger("1400000", 16);
    public static final BigInteger COOKIE_ELAN_INGRESS_TABLE = new BigInteger("8040000", 16);
    public static final BigInteger TUNNEL_TABLE_COOKIE = new BigInteger("9000000", 16);

    //Table IDs
    public static final short VLAN_INTERFACE_INGRESS_TABLE = 0;
    public static final short LPORT_DISPATCHER_TABLE = 17;
    public static final short DHCP_TABLE_EXTERNAL_TUNNEL = 18;
    public static final short DHCP_TABLE = 19;
    public static final short L3_LFIB_TABLE = 20;
    public static final short L3_FIB_TABLE = 21;
    public static final short L3_SUBNET_ROUTE_TABLE=22;
    public static final short PDNAT_TABLE = 25;
    public static final short PSNAT_TABLE = 26;
    public static final short DNAT_TABLE = 27;
    public static final short SNAT_TABLE = 28;
    public static final short INTERNAL_TUNNEL_TABLE = 36;
    public static final short EXTERNAL_TUNNEL_TABLE = 38;
    public static final short EGRESS_ACL_TABLE_ID = 40;
    public static final short EGRESS_ACL_NEXT_TABLE_ID = 41;
    public static final short INBOUND_NAPT_TABLE = 44;
    public static final short IPV6_TABLE = 45;
    public static final short OUTBOUND_NAPT_TABLE = 46;
    public static final short NAPT_PFIB_TABLE = 47;
    public static final short ELAN_SMAC_TABLE = 50;
    public static final short ELAN_DMAC_TABLE = 51;
    public static final short ELAN_UNKNOWN_DMAC_TABLE = 52;
    public static final short ELAN_FILTER_EQUALS_TABLE = 55;
    public static final short L3_INTERFACE_TABLE = 80;
    public static final short INGRESS_ACL_TABLE_ID = 251;
    public static final short INGRESS_ACL_NEXT_TABLE_ID = 252;


 }
