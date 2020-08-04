/*
 * Copyright © 2017 Red Hat, Inc. and others.
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;

/**
 * Test class for {@link ActionSetSourceIpv6}.
 */
public class ActionSetSourceIpv6Test {
    private static final String IP_ADDRESS = "2001:db8::1";
    private static final String IP_MASK = "128";

    private final XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionSetSourceIpv6(IP_ADDRESS).buildAction());
        verifyAction(new ActionSetSourceIpv6(IP_ADDRESS, IP_MASK).buildAction());
        verifyAction(new ActionSetSourceIpv6(new Ipv6Prefix(IP_ADDRESS + "/" + IP_MASK)).buildAction());
    }

    private static void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof SetFieldCase);
        SetFieldCase actionCase = (SetFieldCase) action.getAction();
        assertNotNull(actionCase.getSetField().getLayer3Match());
        assertTrue(actionCase.getSetField().getLayer3Match() instanceof Ipv6Match);
        Ipv6Match ipv6Match = (Ipv6Match) actionCase.getSetField().getLayer3Match();
        assertEquals(new Ipv6Prefix(IP_ADDRESS + "/" + IP_MASK), ipv6Match.getIpv6Source());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionSetSourceIpv6(IP_ADDRESS);
        assertEquals("Ipv6Prefix{_value=" + IP_ADDRESS + "/" + IP_MASK + "}",
                ((ActionSetSourceIpv6) actionInfo).getSource().toString());
    }
}
