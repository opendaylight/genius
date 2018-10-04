/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedFlowEntries {

    static def newIngressFlow() {
        new FlowBuilder >> [
            cookie = new FlowCookie(134217728bi)
            flowName = "0:1:23701c04-7e58-4c65-9425-78a80d49a218"
            hardTimeout = 0
            id = new FlowId("0:1:23701c04-7e58-4c65-9425-78a80d49a218")
            idleTimeout = 0
            instructions = new InstructionsBuilder >> [
                instruction = #[
                    new InstructionBuilder >> [
                        instruction = new GoToTableCaseBuilder >> [
                            goToTable = new GoToTableBuilder >> [
                                tableId = 17 as short
                            ]
                        ]
                        order = 1
                    ],
                    new InstructionBuilder >> [
                        instruction = new WriteMetadataCaseBuilder >> [
                            writeMetadata = new WriteMetadataBuilder >> [
                                metadata = 1099511627776bi
                                metadataMask = 18446742974197923841bi
                            ]
                        ]
                        order = 0
                    ]
                ]
            ]
            match = new MatchBuilder >> [
                inPort = new NodeConnectorId("openflow:1:2")
                vlanMatch = new VlanMatchBuilder >> [
                    vlanId = new VlanIdBuilder >> [
                        vlanId = new VlanId(0)
                    ]
                ]
            ]
            priority = 4
            tableId = 0 as short
        ]
    }

    static def newLportDispatcherFlow(){
        new FlowBuilder >> [
            cookie = new FlowCookie(134479872bi)
            flowName = "ELAN"
            hardTimeout = 0
            id = new FlowId("1.17.23701c04-7e58-4c65-9425-78a80d49a218.0")
            idleTimeout = 0
            instructions = new InstructionsBuilder >> [
                instruction = #[
                    new InstructionBuilder >> [
                        instruction = new GoToTableCaseBuilder >> [
                            goToTable = new GoToTableBuilder >> [
                                tableId = 48 as short
                            ]
                        ]
                        order = 3
                    ],
                    new InstructionBuilder >> [
                        instruction = new WriteMetadataCaseBuilder >> [
                            writeMetadata = new WriteMetadataBuilder >> [
                                metadata = 12682137650203721728bi
                                metadataMask = 18446744073709551614bi
                            ]
                        ]
                        order = 4
                    ],
                    new InstructionBuilder >> [
                        instruction = new ApplyActionsCaseBuilder >> [
                            applyActions = new ApplyActionsBuilder >> [
                                action = #[
                                ]
                            ]
                        ]
                        order = 0
                    ]
                ]
            ]
            match = new MatchBuilder >> [
                metadata = new MetadataBuilder >> [
                    metadata = 1099511627776bi
                    metadataMask = 18446742974197923840bi
                ]
            ]
            priority = 10
            tableId = 17 as short
        ]
    }
}