package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.yangtools.concepts.Builder;

/**
 * Builder for {@link ActionGroup}.
 */
public class ActionGroupBuilder implements Builder<ActionGroup> {
    private long groupId;

    @Override
    public ActionGroup build() {
        return new ActionGroup(groupId);
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
}
