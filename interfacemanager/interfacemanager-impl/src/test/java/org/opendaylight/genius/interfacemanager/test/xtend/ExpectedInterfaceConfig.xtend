/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfExternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan.L2vlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedInterfaceConfig {
    static def newInterfaceConfig(String interfaceName) {
        new InterfaceBuilder >> [
            description = interfaceName
            name = interfaceName
            type = L2vlan
            addAugmentation(ParentRefs, new ParentRefsBuilder >> [
                parentInterface = "tap23701c04-7e"
            ])addAugmentation(IfL2vlan, new IfL2vlanBuilder >> [
                l2vlanMode = L2vlanMode.Trunk
                vlanId = new VlanId(0)
            ])
        ]
    }

    static def newVlanInterfaceConfig(String interfaceName, String parentRef) {
        new InterfaceBuilder >> [
            description = interfaceName
            name = interfaceName
            type = L2vlan
            addAugmentation(IfL2vlan, new IfL2vlanBuilder >> [
                l2vlanMode = L2vlanMode.Trunk
            ])addAugmentation(ParentRefs, new ParentRefsBuilder >> [
                parentInterface = parentRef
            ])
        ]
    }

    static def newExternalInterface(String interfaceName, String parentRefs) {
        new InterfaceBuilder >> [
            description = interfaceName
            name = interfaceName
            type = L2vlan
            addAugmentation(IfExternal, new IfExternalBuilder >> [
                external = true
            ])
            addAugmentation(IfL2vlan, new IfL2vlanBuilder >> [
                l2vlanMode = L2vlanMode.Trunk
            ])addAugmentation(ParentRefs, new ParentRefsBuilder >> [
                parentInterface = parentRefs
            ])
        ]
    }

    static def newChildInterfaceList(String interfaceName, String parentRefs){
        #[
            new InterfaceBuilder >> [
                description = interfaceName
                name = interfaceName
                type = L2vlan
                addAugmentation(IfL2vlan, new IfL2vlanBuilder >> [
                    l2vlanMode = L2vlanMode.Trunk
                    vlanId = new VlanId(0)
                ])addAugmentation(ParentRefs, new ParentRefsBuilder >> [
                    parentInterface = parentRefs
                ])
            ]
        ]
    }
}
