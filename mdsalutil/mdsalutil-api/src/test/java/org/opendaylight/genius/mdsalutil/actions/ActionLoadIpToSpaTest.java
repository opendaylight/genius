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
import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yangtools.yang.common.Empty;


/**
 * Test for {@link ActionLoadIpToSpa}.
 */
public class ActionLoadIpToSpaTest {
    private static final String IP_ADDRESS = "1.2.3.4";

    private XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionLoadIpToSpa(IP_ADDRESS).buildAction());
    }

    private void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof NxActionRegLoadNodesNodeTableFlowApplyActionsCase);
        NxActionRegLoadNodesNodeTableFlowApplyActionsCase actionCase =
            (NxActionRegLoadNodesNodeTableFlowApplyActionsCase) action.getAction();
        assertNotNull(actionCase.getNxRegLoad());
        NxRegLoad nxRegLoad = actionCase.getNxRegLoad();
        assertTrue(nxRegLoad.getDst().getDstChoice() instanceof DstOfArpSpaCase);
        DstOfArpSpaCase dstOfArpSpaCase = (DstOfArpSpaCase) nxRegLoad.getDst().getDstChoice();
        assertEquals(Empty.getInstance(),dstOfArpSpaCase.isOfArpSpa());
        assertEquals(0, nxRegLoad.getDst().getStart().intValue());
        assertEquals(31, nxRegLoad.getDst().getEnd().intValue());
        assertEquals(
            BigInteger.valueOf(InetAddresses.coerceToInteger(InetAddresses.forString(IP_ADDRESS)) & 0xffffffffL),
            nxRegLoad.getValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionLoadIpToSpa(IP_ADDRESS);
        assertEquals("new ActionLoadIpToSpa(0, \"" + IP_ADDRESS + "\")", generator.getExpression(actionInfo));
    }
}
