/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend


import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.get.dpn._interface.list.output.InterfacesBuilder;
import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class DpnInterfaceListOutput {

    static def newDpnInterfaceListOutput() {
        new GetDpnInterfaceListOutputBuilder >> [
            interfaces = #[
                new InterfacesBuilder >> [

                ]
            ]
        ]
    }
}
