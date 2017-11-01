/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.internal;

import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessMonitorAndProtocolsConstants;

/**
 * Constants private to alivenessmonitor.internal.
 */
public interface AlivenessMonitorConstants {
    String MONITOR_IDPOOL_NAME = "aliveness-monitor";
    long MONITOR_IDPOOL_START = 1L;
    long MONITOR_IDPOOL_SIZE = 65535;
    String SEPERATOR = AlivenessMonitorAndProtocolsConstants.SEPERATOR;

    // BFD parameters
    String BFD_PARAM_ENABLE = "enable";
    String BFD_PARAM_MIN_RX = "min_rx";
    String BFD_PARAM_MIN_TX = "min_tx";
    String BFD_PARAM_DECAY_MIN_RX = "decay_min_rx";
    String BFD_PARAM_FORWARDING_IF_RX = "forwarding_if_rx";
    String BFD_PARAM_CPATH_DOWN = "cpath_down";
    String BFD_PARAM_CHECK_TNL_KEY = "check_tnl_key";

    // BFD Local/Remote Configuration parameters
    String BFD_CONFIG_BFD_DST_MAC = "bfd_dst_mac";
    String BFD_CONFIG_BFD_DST_IP = "bfd_dst_ip";

    String BFD_OP_STATE = "state";
    String BFD_STATE_UP = "up";

}
