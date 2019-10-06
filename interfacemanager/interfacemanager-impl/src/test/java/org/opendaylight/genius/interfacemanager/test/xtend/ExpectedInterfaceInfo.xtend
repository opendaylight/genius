/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceAdminState
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceOpState
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceType
import org.opendaylight.genius.interfacemanager.globals.VlanInterfaceInfo
import org.opendaylight.yangtools.yang.common.Uint64

class ExpectedInterfaceInfo {
    static def newInterfaceInfo(Integer lportTag, String ifaceName, String parentInterface,
    InterfaceInfo.InterfaceType ifaceType) {
        new InterfaceInfo(Uint64.ONE, parentInterface) => [
            adminState = InterfaceAdminState.ENABLED
            interfaceName = ifaceName
            interfaceTag = 1
            if (ifaceType !== null)
                interfaceType = ifaceType
            macAddress = "AA:AA:AA:AA:AA:AA"
            opState = InterfaceOpState.UP
            portNo = 2
        ]
    }

    static def newVlanInterfaceInfo() {
        new VlanInterfaceInfo(Uint64.ONE, "tap23701c04-7e", 0 as short) => [
            adminState = InterfaceAdminState.ENABLED
            interfaceName = "23701c04-7e58-4c65-9425-78a80d49a218"
            interfaceTag = 1
            interfaceType = InterfaceType.VLAN_INTERFACE
            macAddress = "AA:AA:AA:AA:AA:AA"
            opState = InterfaceOpState.UP
            portNo = 2
        ]
    }
}
