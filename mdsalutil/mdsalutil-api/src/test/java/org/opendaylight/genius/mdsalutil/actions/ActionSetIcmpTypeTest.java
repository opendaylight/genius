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
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;

/**
 * Test class for {@link ActionSetIcmpType}.
 */
public class ActionSetIcmpTypeTest {
    private static final short TYPE = (short) 2;

    private XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionSetIcmpType(TYPE).buildAction());
    }

    @Test
    public void backwardsCompatibleAction() {
        verifyAction(new ActionInfo(ActionType.set_icmp_type, new String[] {Short.toString(TYPE)}).buildAction());
    }

    private void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof SetFieldCase);
        SetFieldCase actionCase = (SetFieldCase) action.getAction();
        assertNotNull(actionCase.getSetField().getIcmpv4Match());
        Icmpv4Match icmpv4Match = actionCase.getSetField().getIcmpv4Match();
        assertEquals(TYPE, icmpv4Match.getIcmpv4Type().shortValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionSetIcmpType(TYPE);
        assertEquals("new ActionSetIcmpType(0, 2 as short)", generator.getExpression(actionInfo));
    }
}
