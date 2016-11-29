package org.opendaylight.genius.mdsalutil.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;

/**
 * Test class for {@link ActionGroup}.
 */
public class ActionGroupTest {
    XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void actionInfoTestForGroupAction() {
        ActionInfo actionInfo = new ActionGroup(123);
        Action action = actionInfo.buildAction();
        assertTrue(action.getAction() instanceof GroupActionCase);
        GroupActionCase actionCase = (GroupActionCase) action.getAction();
        assertEquals(123L, (long) actionCase.getGroupAction().getGroupId());
    }

    @Test
    public void generateActionGroup() {
        ActionInfo actionGroup = new ActionGroup(123);
        actionGroup.buildAction();
        assertEquals("(new ActionGroupBuilder => [\n" +
                "    groupId = 123L\n" +
                "]).build()", generator.getExpression(actionGroup));
    }
}
