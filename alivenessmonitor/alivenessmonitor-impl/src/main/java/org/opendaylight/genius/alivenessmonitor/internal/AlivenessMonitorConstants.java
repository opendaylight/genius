/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.internal;

public class AlivenessMonitorConstants {
    static final String MONITOR_IDPOOL_NAME = "aliveness-monitor";
    static final long MONITOR_IDPOOL_START = 1L;
    static final long MONITOR_IDPOOL_SIZE = 65535;
    static final short L3_INTERFACE_TABLE = 80;
    static final String SEPERATOR = ".";

    // BFD parameters
    static final String BFD_PARAM_ENABLE = "enable";
    static final String BFD_PARAM_MIN_RX = "min_rx";
    static final String BFD_PARAM_MIN_TX = "min_tx";
    static final String BFD_PARAM_DECAY_MIN_RX = "decay_min_rx";
    static final String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";
    static final String BFD_PARAM_CPATH_DOWN = "cpath_down";
    static final String BFD_PARAM_CHECK_TNL_KEY = "check_tnl_key";

    // BFD Local/Remote Configuration parameters
    static final String BFD_CONFIG_BFD_DST_MAC = "bfd_dst_mac";
    static final String BFD_CONFIG_BFD_DST_IP = "bfd_dst_ip";

    static final String BFD_OP_STATE = "state";
    static final String BFD_STATE_UP = "up";

}
