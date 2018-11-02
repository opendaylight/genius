/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfaceType
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface.AdminStatus
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface.OperStatus
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state._interface.StatisticsBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedInterfaceState {
    static def newInterfaceState(Integer lportTag, String interfaceName, OperStatus opState,
        Class<? extends InterfaceType> ifType, String dpnId, DateAndTime dateAndTime) {
        new InterfaceBuilder >> [
            adminStatus = AdminStatus.Up
            ifIndex = lportTag
            lowerLayerIf = #[
                "openflow:"+dpnId+":2"
            ]
            name = interfaceName
            operStatus = opState
            physAddress = new PhysAddress("AA:AA:AA:AA:AA:AA")
            type = ifType
            statistics = new StatisticsBuilder() >> [
                discontinuityTime = dateAndTime
            ]
        ]
    }

}
