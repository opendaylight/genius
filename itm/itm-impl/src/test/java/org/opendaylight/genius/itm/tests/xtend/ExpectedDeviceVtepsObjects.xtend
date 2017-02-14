/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.genius.itm.tests.ItmTestConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsBuilder;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedDeviceVtepsObjects {

    static def newDeviceVtepsObject() {
        new DeviceVtepsBuilder >> [
            ipAddress = ItmTestConstants.ipAddress3
            nodeId = ItmTestConstants.sourceDevice
            topologyId = ItmTestConstants.destinationDevice
        ]
    }

    static def newDeviceVtepsObject2() {
        new DeviceVtepsBuilder >> [
            ipAddress = ItmTestConstants.ipAddress3
            nodeId = ItmTestConstants.sourceDevice2
            topologyId = ItmTestConstants.destinationDevice
        ]
    }
}
