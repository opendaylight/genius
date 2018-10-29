/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceBfdBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedTerminationPoint {

    static def newTerminationPoint() {
        new TerminationPointBuilder >> [
            tpId = new TpId("tun414a856a7a4")
            addAugmentation(OvsdbTerminationPointAugmentation, new OvsdbTerminationPointAugmentationBuilder >> [
                interfaceType = InterfaceTypeVxlan
                name = "tun414a856a7a4"
                options = #[
                    new OptionsBuilder >> [
                        option = "key"
                        value = "flow"
                    ],
                    new OptionsBuilder >> [
                        option = "local_ip"
                        value = "2.2.2.2"
                    ],
                    new OptionsBuilder >> [
                        option = "remote_ip"
                        value = "1.1.1.1"
                    ]
                ]
            ])
        ]

    }

    static def newBfdEnabledTerminationPoint() {
    new TerminationPointBuilder >> [
        tpId = new TpId("tun414a856a7a4")
        addAugmentation(OvsdbTerminationPointAugmentation, new OvsdbTerminationPointAugmentationBuilder >> [
            interfaceBfd = #[
                new InterfaceBfdBuilder >> [
                    bfdKey = "forwarding_if_rx"
                    bfdValue = "true"
                ],
                new InterfaceBfdBuilder >> [
                    bfdKey = "enable"
                    bfdValue = "true"
                ],
                new InterfaceBfdBuilder >> [
                    bfdKey = "min_tx"
                    bfdValue = "10000"
                ]
            ]
            interfaceType = InterfaceTypeVxlan
            name = "tun414a856a7a4"
            options = #[
                new OptionsBuilder >> [
                    option = "key"
                    value = "flow"
                ],
                new OptionsBuilder >> [
                    option = "local_ip"
                    value = "2.2.2.2"
                ],
                new OptionsBuilder >> [
                    option = "remote_ip"
                    value = "1.1.1.1"
                ]
            ]
        ])
    ]
    }

    static def newOvsdbTerminationPointAugmentation() {
        new OvsdbTerminationPointAugmentationBuilder >> [
            interfaceExternalIds = #[
                new InterfaceExternalIdsBuilder >> [
                    externalIdKey = "iface-id"
                    externalIdValue = "23701c04-7e58-4c65-9425-78a80d49a218"
                ]
            ]
            name = "tap23701c04-7e"
        ]
    }

    static def newTerminationPointList() {
        #[
            new OvsdbTerminationPointAugmentationBuilder >> [
                interfaceType = InterfaceTypeVxlan
                name = "tun414a856a7a4"
            ]
        ]
    }
}
