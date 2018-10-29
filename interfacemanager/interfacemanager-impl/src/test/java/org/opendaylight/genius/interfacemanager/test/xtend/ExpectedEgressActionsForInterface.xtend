/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.xtend

import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.^extension.nicira.action.rev140714.add.group.input.buckets.bucket.action.action.NxActionResubmitRpcAddGroupCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.^extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.^extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.^extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.^extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.^extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmitBuilder

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class EgressActionsForInterfaceOutput {

    static def newEgressActionsForInterfaceOutput() {
        new GetEgressActionsForInterfaceOutputBuilder >> [
            action = #[
                new ActionBuilder >> [
                    action = new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder >> [
                        nxRegLoad = new NxRegLoadBuilder >> [
                            dst = new DstBuilder >> [
                                dstChoice = new DstNxRegCaseBuilder >> [
                                    nxReg = NxmNxReg6
                                ]
                                end = 31
                                start = 0
                            ]
                            value = 256bi
                        ]
                    ]
                    order = 0
                ],
                new ActionBuilder >> [
                    action = new NxActionResubmitRpcAddGroupCaseBuilder >> [
                        nxResubmit = new NxResubmitBuilder >> [
                            table = 220 as short
                        ]
                    ]
                    order = 1
                ]
            ]
        ]
    }
}