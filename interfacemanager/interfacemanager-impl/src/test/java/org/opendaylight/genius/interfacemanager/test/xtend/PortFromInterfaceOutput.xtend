/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutputBuilder
import org.opendaylight.yangtools.yang.common.Uint32
import org.opendaylight.yangtools.yang.common.Uint64

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class PortFromInterfaceOutput {

    static def newPortFromInterfaceOutput() {
        new GetPortFromInterfaceOutputBuilder >> [
            dpid = Uint64.ONE
            phyAddress = "AA:AA:AA:AA:AA:AA"
            portname = "23701c04-7e58-4c65-9425-78a80d49a218"
            portno = Uint32.TWO
        ]
    }
}
