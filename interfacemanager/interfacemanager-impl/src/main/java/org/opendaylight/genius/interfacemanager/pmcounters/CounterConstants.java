/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

public interface CounterConstants {
    //IFM counter name strings
    String IFM_PORT_COUNTER_OFPORT_DURATION = "OFPortDuration";
    String IFM_PORT_COUNTER_OFPORT_PKT_RECVDROP = "PacketsPerOFPortReceiveDrop";
    String IFM_PORT_COUNTER_OFPORT_PKT_RECVERROR = "PacketsPerOFPortReceiveError";
    String IFM_PORT_COUNTER_OFPORT_PKT_SENT = "PacketsPerOFPortSent";
    String IFM_PORT_COUNTER_OFPORT_PKT_RECV = "PacketsPerOFPortReceive";
    String IFM_PORT_COUNTER_OFPORT_BYTE_SENT = "BytesPerOFPortSent";
    String IFM_PORT_COUNTER_OFPORT_BYTE_RECV = "BytesPerOFPortReceive";
    String IFM_FLOW_TBL_COUNTER_FLOWS_PER_TBL = "EntriesPerOFTable";

    // IFM counter metric key constants
    String CNT_TYPE_ENTITY_CNT_ID = "entitycounter";
    String LBL_KEY_ENTITY_TYPE = "entitytype";
    String LBL_VAL_ENTITY_TYPE_PORT = "port";
    String LBL_VAL_ENTITY_TYPE_FLOWTBL = "flowtable";
    String LBL_KEY_SWITCHID = "switchid";
    String LBL_KEY_PORTID = "portid";
    String LBL_KEY_FLOWTBLID = "flowtableid";
    String LBL_KEY_ALIASID = "aliasid";
    String LBL_KEY_COUNTER_NAME = "name";
}