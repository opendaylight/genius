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
              destinationDevice = "IpAddress{_ipv4Address=Ipv4Address{_value=192.168.56.101}}"
              sourceDevice = ItmTestConstants.DPID_STR_ONE
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.EXT_TUNNEL_INTERFACE_NAME
        ]
    }

    static def newExternalTunnel2ForRpcTest() {
         new ExternalTunnelBuilder >> [
              destinationDevice = ItmTestConstants.DESTINATION_DEVICE
              sourceDevice = ItmTestConstants.SOURCE_DEVICE
              transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
              tunnelInterfaceName = ItmTestConstants.PARENT_INTERFACE_NAME
         ]
    }
}
