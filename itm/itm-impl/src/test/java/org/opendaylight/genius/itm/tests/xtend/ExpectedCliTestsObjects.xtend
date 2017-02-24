/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.genius.itm.tests.ItmTestConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;


import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedCliTestsObjects {

    static def newVxlanTransportZone() {
        new TransportZoneBuilder >> [
            tunnelType = ItmTestConstants.TUNNEL_TYPE_VXLAN
            zoneName = ItmTestConstants.TZ_NAME
        ]
    }

    static def newTunnelMonitorInterval() {
       new TunnelMonitorIntervalBuilder >> [
           interval = ItmTestConstants.MON_INTERVAL
       ]
    }

    static def newTunnelMonitorParams() {
       new TunnelMonitorParamsBuilder >> [
           enabled = ItmTestConstants.ENABLE_TUN_MON
           monitorProtocol = ItmTestConstants.TUNNEL_MON_TYPE
       ]
    }

    static def newVtep() {
       new VtepsBuilder >> [
           dpnId = ItmTestConstants.dpId1
           ipAddress = ItmTestConstants.ipAddress1
           portname = ItmTestConstants.portName1
           optionOfTunnel = ItmTestConstants.OPT_OF_TUNNEL
       ]
    }
}
