/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import com.google.common.collect.Lists;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.mdsal.binding.testutils.AssertDataObjects;

/**
 * Test for {@link ActionNxResubmit}.
 */
public class ActionNxResubmitTest {

    @Test
    public void testAssertEqualsBeanPositive() {
        AssertDataObjects.assertEqualBeans(new ActionNxResubmit(1, (short) 2), new ActionNxResubmit(1, (short) 2));
    }

    @Test(expected = ComparisonFailure.class)
    public void testAssertEqualsBeanNegative1() {
        AssertDataObjects.assertEqualBeans(new ActionNxResubmit(1, (short) 2), new ActionNxResubmit(1, (short) 3));
    }

    @Test(expected = ComparisonFailure.class)
    public void testAssertEqualsBeanNegative2() {
        AssertDataObjects.assertEqualBeans(new ActionNxResubmit(1, (short) 2), new ActionNxResubmit(2, (short) 2));
    }

    @Test(expected = ComparisonFailure.class)
    public void testAssertEqualsBeanNegative3() {
        AssertDataObjects.assertEqualBeans(new ActionNxResubmit(0, (short) 0), new ActionNxResubmit(0, (short) 17));
    }

    @Test(expected = ComparisonFailure.class)
    public void testInLists() {
        AssertDataObjects.assertEqualBeans(
                new InstructionApplyActions(Lists.newArrayList(new ActionNxResubmit(0, (short) 0))),
                new InstructionApplyActions(Lists.newArrayList(new ActionNxResubmit(0, (short) 17))));
    }

}
