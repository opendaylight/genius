/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.genius.itm.tests.ItmTestConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelBuilder;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedExternalTunnelObjects {
    static def newExternalTunnelForRpcTest() {
        new ExternalTunnelBuilder >> [
              destinationDevice = "IpAddress [_ipv4Address=Ipv4Address [_value=192.168.56.101]]"
              sourceDevice = ItmTestConstants.DPID_STR_ONE
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.extTunnelInterfaceName
        ]
    }

    static def newExternalTunnel2ForRpcTest() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.destinationDevice
              sourceDevice = ItmTestConstants.sourceDevice
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.parentInterfaceName
         ]
    }

    static def newExternalTunnel() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.DESTINATION_DEV_IP_ADDR_STR
              sourceDevice = ItmTestConstants.DPID_STR_ONE
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.tunnelInterfaceName3
         ]
    }

    static def newExternalTunnel1() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.MC_30_URL_FOR_EXTENDED
              sourceDevice = ItmTestConstants.DPID_STR_ONE
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.tunnelInterfaceName1
         ]
    }

    static def newExternalTunnel2() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.DPID_STR_ONE
              sourceDevice = ItmTestConstants.MC_30_URL_FOR_EXTENDED
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.tunnelInterfaceName2
         ]
    }

    static def newExternalTunnel3() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.MC_40_URL_FOR_EXTENDED
              sourceDevice = ItmTestConstants.DPID_STR_ONE
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.tunnelInterfaceName3
         ]
    }

    static def newExternalTunnel4() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.DPID_STR_ONE
              sourceDevice = ItmTestConstants.MC_40_URL_FOR_EXTENDED
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.tunnelInterfaceName4
         ]
    }

    static def newExternalTunnel5() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.MC_40_URL_FOR_EXTENDED
              sourceDevice = ItmTestConstants.MC_30_URL_FOR_EXTENDED
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.tunnelInterfaceName5
         ]
    }

    static def newExternalTunnel6() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.MC_30_URL_FOR_EXTENDED
              sourceDevice = ItmTestConstants.MC_40_URL_FOR_EXTENDED
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.tunnelInterfaceName6
         ]
    }
}
