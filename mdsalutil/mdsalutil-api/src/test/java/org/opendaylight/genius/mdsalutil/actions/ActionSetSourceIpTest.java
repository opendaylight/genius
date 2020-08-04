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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;

/**
 * Test class for {@link ActionSetSourceIp}.
 */
public class ActionSetSourceIpTest {
    private static final String IP_ADDRESS = "1.2.3.4";
    private static final String IP_MASK = "32";

    private final XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionSetSourceIp(IP_ADDRESS).buildAction());
        verifyAction(new ActionSetSourceIp(IP_ADDRESS, IP_MASK).buildAction());
        verifyAction(new ActionSetSourceIp(new Ipv4Prefix(IP_ADDRESS + "/" + IP_MASK)).buildAction());
    }

    private static void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof SetFieldCase);
        SetFieldCase actionCase = (SetFieldCase) action.getAction();
        assertNotNull(actionCase.getSetField().getLayer3Match());
        assertTrue(actionCase.getSetField().getLayer3Match() instanceof Ipv4Match);
        Ipv4Match ipv4Match = (Ipv4Match) actionCase.getSetField().getLayer3Match();
        assertEquals(new Ipv4Prefix(IP_ADDRESS + "/" + IP_MASK), ipv4Match.getIpv4Source());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionSetSourceIp(IP_ADDRESS);
        assertEquals("Ipv4Prefix{_value=" + IP_ADDRESS + "/" + IP_MASK + "}",
               ((ActionSetSourceIp) actionInfo).getSource().toString());
    }
}
