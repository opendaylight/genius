/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesStateBuilder
import org.opendaylight.yangtools.yang.common.Uint32
import org.opendaylight.yangtools.yang.common.Uint64

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedBoundServiceState {
    static def newBoundServiceState() {
        new BoundServicesStateBuilder >> [
            dpid = Uint64.ONE
            ifIndex = 1
            interfaceName = "23701c04-7e58-4c65-9425-78a80d49a218"
            interfaceType = L2vlan
            portNo = Uint32.TWO
            serviceMode = ServiceModeIngress
        ]
    }
}
