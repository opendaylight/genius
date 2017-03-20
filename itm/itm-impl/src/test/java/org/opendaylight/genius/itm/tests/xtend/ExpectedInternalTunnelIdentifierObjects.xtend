/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelBuilder;
import org.opendaylight.genius.itm.tests.ItmTestConstants;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan
import java.util.Collections

class ExpectedInternalTunnelIdentifierObjects {

    static def newInternalTunnelObjVxLanOneToTwo() {
        new InternalTunnelBuilder >> [
            destinationDPN = ItmTestConstants.DP_ID_2
            sourceDPN = ItmTestConstants.DP_ID_1
            transportType = ItmTestConstants.TUNNEL_TYPE_VXLAN
            tunnelInterfaceNames = Collections.singletonList(ItmTestConstants.PARENT_INTERFACE_NAME)
        ]
    }
}
