/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager;

import java.math.BigInteger;

public interface IfmConstants {
    String IFM_IDPOOL_NAME = "interfaces";
    long IFM_ID_POOL_START = 1L;
    long IFM_ID_POOL_END = 65535;
    String IFM_IDPOOL_SIZE = "65535";
    String OF_URI_PREFIX = "openflow:";
    String OF_URI_SEPARATOR = ":";
    int DEFAULT_IFINDEX = 65536;
    int DEFAULT_FLOW_PRIORITY = 5;
    String IFM_LPORT_TAG_IDPOOL_NAME = "vlaninterfaces.lporttag";
    short VLAN_INTERFACE_INGRESS_TABLE = 0;
    //Group Prefix
    long VLAN_GROUP_START = 1000;
    long TRUNK_GROUP_START = 20000;
    long LOGICAL_GROUP_START = 100000;
    BigInteger COOKIE_VM_LFIB_TABLE = new BigInteger("8000002", 16);
    String TUNNEL_TABLE_FLOWID_PREFIX = "TUNNEL.";
    BigInteger TUNNEL_TABLE_COOKIE = new BigInteger("9000000", 16);
    int FLOW_HIGH_PRIORITY = 10;
    int FLOW_PRIORITY_FOR_UNTAGGED_VLAN = 4;

    int REG6_START_INDEX = 0;
    int REG6_END_INDEX = 31;

    int JOB_MAX_RETRIES = 6;
    long DELAY_TIME_IN_MILLISECOND = 10000;

    String DEAD_BEEF_MAC_PREFIX = "DEADBEEF";
    String INVALID_MAC = "00:00:00:00:00:00";
    String MAC_SEPARATOR = ":";
    Integer MAC_STRING_LENGTH = 17;

    long INVALID_PORT_NO = -1;

    String INTERFACE_CONFIG_ENTITY = "interface_config";
    String INTERFACE_SERVICE_BINDING_ENTITY = "interface_service_binding";
    String INTERFACE_SERVICE_NAME = "IFM";

    //IFM counter name strings
    String IFM_PORT_COUNTER_OFPORT_DURATION = "OFPortDuration";
    String IFM_PORT_COUNTER_OFPORT_PKT_RECVDROP = "PacketsPerOFPortReceiveDrop";
    String IFM_PORT_COUNTER_OFPORT_PKT_RECVERROR = "PacketsPerOFPortReceiveError";
    String IFM_PORT_COUNTER_OFPORT_PKT_SENT = "PacketsPerOFPortSent";
    String IFM_PORT_COUNTER_OFPORT_PKT_RECV = "PacketsPerOFPortReceive";
    String IFM_PORT_COUNTER_OFPORT_BYTE_SENT = "BytesPerOFPortSent";
    String IFM_PORT_COUNTER_OFPORT_BYTE_RECV = "BytesPerOFPortReceive";
    String IFM_FLOW_TBL_COUNTER_FLOWS_PER_TBL = "EntriesPerOFTable";

    /*
     * IFM counter key constants
     */
    String ENTITY_CNT_KEYWORD = "entitycounter";
    String ENTITY_TYPE_PORT_KEYWORD = "entitytype:port";
    String ENTITY_TYPE_FLOWTBL_KEYWORD = "entitytype:flowtable";
    String ENTITY_ID_KEYWORD = "entityid";
    String ENTITY_ID_SWITCHID_KEYWORD = "switchid";
    String ENTITY_ID_PORTID_KEYWORD = "portid";
    String ENTITY_ID_FLOWTBLID_KEYWORD = "flowtableid";
    String ENTITY_ID_ALIASID_KEYWORD = "aliasid";
}