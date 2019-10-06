/*
 * Copyright (c) 2016, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertEquals;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionDrop;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionLearn;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Test to check that we no longer need a builder for
 * {@link ActionInfo} to be able to be used with the {@link XtendBeanGenerator}.
 *
 * <p>Add tests here for new ActionInfo
 * (sub)types; ensure (manually) that coverage on ActionInfo and its subtypes
 * constructors stays 100%.
 *
 * @author Michael Vorburger
 */
public class ActionInfoBuilderTest {

    XtendBeanGenerator generator = new UintXtendBeanGenerator();

    @Test
    public void noActionValues() {
        ActionInfo actionInfo = new ActionDrop();
        assertEquals("new ActionDrop", generator.getExpression(actionInfo));
    }

    @Test
    public void groupActionWithSingleIntegerInStringValue() {
        ActionInfo actionInfo = new ActionGroup(123);
        assertEquals("(new ActionGroupBuilder => [" + System.lineSeparator()
                + "    groupId = 123L" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void groupActionWithSingleIntegerInStringValueWithActionKey() {
        ActionInfo actionInfo = new ActionGroup(69, 123);
        assertEquals("(new ActionGroupBuilder => [" + System.lineSeparator()
                + "    groupId = 123L" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

    @Test
    public void set_field_tunnel_idActionWithBigActionValues() {
        ActionInfo actionInfo = new ActionSetFieldTunnelId(Uint64.valueOf(123));
        assertEquals("new ActionSetFieldTunnelId(0, (u64)123)", generator.getExpression(actionInfo));
    }

    @Test
    public void set_field_tunnel_idActionWithBigActionValuesWithActionKey() {
        ActionInfo actionInfo = new ActionSetFieldTunnelId(69, Uint64.valueOf(123));
        assertEquals("new ActionSetFieldTunnelId(69, (u64)123)", generator.getExpression(actionInfo));
    }

    @Test
    @Ignore("Needs to be rewritten")
    public void learnActionValuesMatrix() {
        ActionInfo actionInfo = new ActionLearn(1, 2, 3, Uint64.valueOf(4), 5, (short) 6, 7, 8,
                Arrays.asList(
                        new ActionLearn.CopyFromValue(2, 3, 4),
                        new ActionLearn.OutputToPort(4, 5)));
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
                + "            \"COPY_FROM_VALUE\"," + System.lineSeparator()
                + "            \"2\"," + System.lineSeparator()
                + "            \"3\"," + System.lineSeparator()
                + "            \"4\"" + System.lineSeparator()
                + "        ]," + System.lineSeparator()
                + "        #[" + System.lineSeparator()
                + "            \"OUTPUT_TO_PORT\"," + System.lineSeparator()
                + "            \"4\"," + System.lineSeparator()
                + "            \"5\"" + System.lineSeparator()
                + "        ]" + System.lineSeparator()
                + "    ]" + System.lineSeparator()
                + "]).build()", generator.getExpression(actionInfo));
    }

}
