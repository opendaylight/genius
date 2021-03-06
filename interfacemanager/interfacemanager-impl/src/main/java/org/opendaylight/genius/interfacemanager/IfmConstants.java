/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager;

import org.opendaylight.yangtools.yang.common.Uint64;

// FIXME: rename this to IfmImplConstants
// FIXME: this should be a final utility class
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
    Uint64 COOKIE_VM_LFIB_TABLE = Uint64.valueOf("8000002", 16).intern();
    String TUNNEL_TABLE_FLOWID_PREFIX = "TUNNEL.";
    Uint64 TUNNEL_TABLE_COOKIE = Uint64.valueOf("9000000", 16).intern();
    int FLOW_HIGH_PRIORITY = 10;
    int FLOW_PRIORITY_FOR_UNTAGGED_VLAN = 4;

    int REG6_START_INDEX = 0;
    int REG6_END_INDEX = 31;

    int JOB_MAX_RETRIES = 6;
    long DELAY_TIME_IN_MILLISECOND = 10000;

    Uint64 DEAD_BEEF_MAC_PREFIX = Uint64.valueOf("DEADBEEF", 16).intern();
    String INVALID_MAC = "00:00:00:00:00:00";
    String MAC_SEPARATOR = ":";
    int MAC_STRING_LENGTH = INVALID_MAC.length();

    long INVALID_PORT_NO = -1;

    String INTERFACE_CONFIG_ENTITY = "interface_config";
    String INTERFACE_SERVICE_BINDING_ENTITY = "interface_service_binding";
    String INTERFACE_SERVICE_NAME = "IFM";
    String SERVICE_ENTITY_TYPE = "org.opendaylight.mdsal.ServiceEntityType";
}
