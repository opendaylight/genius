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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;

/**
 * Test class for {@link ActionSetFieldEthernetSource}.
 */
public class ActionSetFieldEthernetSourceTest {
    private static final String MAC_ADDRESS = "11:22:33:44:55:66";

    private XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTest() {
        verifyAction(new ActionSetFieldEthernetSource(new MacAddress(MAC_ADDRESS)).buildAction());
    }

    private void verifyAction(Action action) {
        assertTrue(action.getAction() instanceof SetFieldCase);
        SetFieldCase actionCase = (SetFieldCase) action.getAction();
        assertNotNull(actionCase.getSetField().getEthernetMatch());
        assertNotNull(actionCase.getSetField().getEthernetMatch().getEthernetSource());
        assertEquals(MAC_ADDRESS,
            actionCase.getSetField().getEthernetMatch().getEthernetSource().getAddress().getValue());
    }

    @Test
    public void generateAction() {
        ActionInfo actionInfo = new ActionSetFieldEthernetSource(new MacAddress(MAC_ADDRESS));
        actionInfo.buildAction();
        assertEquals(( "MacAddress{_value=" + MAC_ADDRESS + "}"),
                ((ActionSetFieldEthernetSource) actionInfo).getSource().toString());
    }
}
