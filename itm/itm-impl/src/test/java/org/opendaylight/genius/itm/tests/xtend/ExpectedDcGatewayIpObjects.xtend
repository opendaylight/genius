/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.genius.itm.tests.ItmTestConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIpBuilder;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedDcGatewayIp {

    static def newDcGatewayIpForRpcTest() {
        new DcGatewayIpBuilder >> [
              ipAddress = ItmTestConstants.IP_ADDRESS_3
              tunnnelType = ItmTestConstants.TUNNEL_TYPE_VXLAN
        ]
    }
}