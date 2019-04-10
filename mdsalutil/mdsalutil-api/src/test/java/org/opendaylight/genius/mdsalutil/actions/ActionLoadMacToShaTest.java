/*
 * Copyright © 2016, 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Test for {@link ActionLoadMacToSha}.
 */
public class ActionLoadMacToShaTest {
    private static final String MAC_ADDRESS = "11:22:33:44:55:66";

    private XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionLoadMacToSha(new MacAddress(MAC_ADDRESS)).buildAction());
    }

    private void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof NxActionRegLoadNodesNodeTableFlowApplyActionsCase);
        NxActionRegLoadNodesNodeTableFlowApplyActionsCase actionCase =
            (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getAction();
        assertNotNull(actionCase.getNxRegLoad());
        NxRegLoad nxRegLoad = actionCase.getNxRegLoad();
        assertTrue(nxRegLoad.getDst().getDstChoice() instanceof DstNxArpShaCase);
        DstNxArpShaCase dstNxArpShaCase = (DstNxArpShaCase) nxRegLoad.getDst().getDstChoice();
        assertEquals(Empty.getInstance(),dstNxArpShaCase.getNxArpSha());
        assertEquals(0, nxRegLoad.getDst().getStart().intValue());
        assertEquals(47, nxRegLoad.getDst().getEnd().intValue());
        assertEquals(BigInteger.valueOf(NWUtil.macToLong(new MacAddress(MAC_ADDRESS))), nxRegLoad.getValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionLoadMacToSha(new MacAddress(MAC_ADDRESS));
        assertEquals("new ActionLoadMacToSha(0, new MacAddress(\"" + MAC_ADDRESS + "\"))",
                generator.getExpression(actionInfo));
    }
}
