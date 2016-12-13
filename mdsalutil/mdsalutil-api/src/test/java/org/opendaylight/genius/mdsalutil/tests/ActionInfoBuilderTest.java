/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertEquals;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionInfoBuilder;
import org.opendaylight.genius.mdsalutil.ActionType;

/**
 * Test to illustrate why {@link ActionInfoBuilder} is required for
 * {@link ActionInfo} to be able to be used with the {@link XtendBeanGenerator}.
 *
 * <p>Delete the ActionInfoBuilder class, and see that this test fails, even though
 * there is no compile time dependency. Add tests here for new ActionInfo
 * (sub)types; ensure (manually) that coverage on ActionInfo and its subtypes
 * constructors stays 100%.
 *
 * @author Michael Vorburger
 */
public class ActionInfoBuilderTest {

    XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void noActionValues() {
        ActionInfo actionInfo = new ActionInfo(ActionType.drop_action, (String[]) null);
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [" + System.lineSeparator()
                + "    actionType = ActionType.drop_action" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void groupActionWithSingleIntegerInStringValue() {
        ActionInfo actionInfo = new ActionInfo(ActionType.group, new String[] { "123" });
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [" + System.lineSeparator()
                + "    actionType = ActionType.group" + System.lineSeparator()
                + "    actionValues = #[" + System.lineSeparator()
                + "        \"123\"" + System.lineSeparator()
                + "    ]" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void groupActionWithSingleIntegerInStringValueWithActionKey() {
        ActionInfo actionInfo = new ActionInfo(ActionType.group, new String[] { "123" }, 69);
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [" + System.lineSeparator()
                + "    actionKey = 69" + System.lineSeparator()
                + "    actionType = ActionType.group" + System.lineSeparator()
                + "    actionValues = #[" + System.lineSeparator()
                + "        \"123\"" + System.lineSeparator()
                + "    ]" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void set_field_tunnel_idActionWithBigActionValues() {
        ActionInfo actionInfo = new ActionInfo(ActionType.set_field_tunnel_id,
                new BigInteger[] { BigInteger.valueOf(123) });
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [" + System.lineSeparator()
                + "    actionType = ActionType.set_field_tunnel_id" + System.lineSeparator()
                + "    bigActionValues = #[" + System.lineSeparator()
                + "        123bi" + System.lineSeparator()
                + "    ]" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void set_field_tunnel_idActionWithBigActionValuesWithActionKey() {
        ActionInfo actionInfo = new ActionInfo(ActionType.set_field_tunnel_id,
                new BigInteger[] { BigInteger.valueOf(123) }, 69);
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [" + System.lineSeparator()
                + "    actionKey = 69" + System.lineSeparator()
                + "    actionType = ActionType.set_field_tunnel_id" + System.lineSeparator()
                + "    bigActionValues = #[" + System.lineSeparator()
                + "        123bi" + System.lineSeparator()
                + "    ]" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void learnActionValuesMatrix() {
        ActionInfo actionInfo = new ActionInfo(ActionType.learn,
                new String[] { "1", "2", "3", "4", "5", "6", "7", "8" },
                new String[][] { { "2", "3" }, { "4", "5" } });
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [" + System.lineSeparator()
                + "    actionType = ActionType.learn" + System.lineSeparator()
                + "    actionValues = #[" + System.lineSeparator()
                + "        \"1\"," + System.lineSeparator()
                + "        \"2\"," + System.lineSeparator()
                + "        \"3\"," + System.lineSeparator()
                + "        \"4\"," + System.lineSeparator()
                + "        \"5\"," + System.lineSeparator()
                + "        \"6\"," + System.lineSeparator()
                + "        \"7\"," + System.lineSeparator()
                + "        \"8\"" + System.lineSeparator()
                + "    ]" + System.lineSeparator()
                + "    actionValuesMatrix = #[" + System.lineSeparator()
                + "        #[" + System.lineSeparator()
                + "            \"2\"," + System.lineSeparator()
                + "            \"3\"" + System.lineSeparator()
                + "        ]," + System.lineSeparator()
                + "        #[" + System.lineSeparator()
                + "            \"4\"," + System.lineSeparator()
                + "            \"5\"" + System.lineSeparator()
                + "        ]" + System.lineSeparator()
                + "    ]" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

}
