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
import org.opendaylight.genius.mdsalutil.NwConstants;

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
        assertEquals("(new ActionInfoBuilder => [\n"
                + "    actionType = ActionType.drop_action\n"
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void groupActionWithSingleIntegerInStringValue() {
        ActionInfo actionInfo = new ActionInfo(ActionType.group, new String[] { "123" });
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [\n"
                + "    actionType = ActionType.group\n"
                + "    actionValues = #[\n"
                + "        \"123\"\n"
                + "    ]\n"
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void groupActionWithSingleIntegerInStringValueWithActionKey() {
        ActionInfo actionInfo = new ActionInfo(ActionType.group, new String[] { "123" }, 69);
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [\n"
                + "    actionKey = 69\n"
                + "    actionType = ActionType.group\n"
                + "    actionValues = #[\n"
                + "        \"123\"\n"
                + "    ]\n"
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void set_field_tunnel_idActionWithBigActionValues() {
        ActionInfo actionInfo = new ActionInfo(ActionType.set_field_tunnel_id,
                new BigInteger[] { BigInteger.valueOf(123) });
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [\n"
                + "    actionType = ActionType.set_field_tunnel_id\n"
                + "    bigActionValues = #[\n"
                + "        123bi\n"
                + "    ]\n"
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void set_field_tunnel_idActionWithBigActionValuesWithActionKey() {
        ActionInfo actionInfo = new ActionInfo(ActionType.set_field_tunnel_id,
                new BigInteger[] { BigInteger.valueOf(123) }, 69);
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [\n"
                + "    actionKey = 69\n"
                + "    actionType = ActionType.set_field_tunnel_id\n"
                + "    bigActionValues = #[\n"
                + "        123bi\n"
                + "    ]\n"
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void learnActionValuesMatrix() {
        ActionInfo actionInfo = new ActionInfo(ActionType.learn,
            new String[] {"1", "2", "3", "4", "5", "6", "7", "8"},
            new String[][] {
                {NwConstants.LearnFlowModsType.COPY_FROM_VALUE.name(), "2", "3", "4"},
                {NwConstants.LearnFlowModsType.OUTPUT_TO_PORT.name(), "4", "5"}
            });
        actionInfo.buildAction();
        assertEquals("(new ActionInfoBuilder => [\n"
            + "    actionType = ActionType.learn\n"
            + "    actionValues = #[\n"
            + "        \"1\",\n"
            + "        \"2\",\n"
            + "        \"3\",\n"
            + "        \"4\",\n"
            + "        \"5\",\n"
            + "        \"6\",\n"
            + "        \"7\",\n"
            + "        \"8\"\n"
            + "    ]\n"
            + "    actionValuesMatrix = #[\n"
            + "        #[\n"
            + "            \"COPY_FROM_VALUE\",\n"
            + "            \"2\",\n"
            + "            \"3\",\n"
            + "            \"4\"\n"
            + "        ],\n"
            + "        #[\n"
            + "            \"OUTPUT_TO_PORT\",\n"
            + "            \"4\",\n"
            + "            \"5\"\n"
            + "        ]\n"
            + "    ]\n"
            + "]).build()", generator.getExpression(actionInfo));
    }

}
