/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.util.List;
import org.immutables.value.Value.Immutable;
import org.opendaylight.genius.infra.OpenDaylightImmutableStyle;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;

@Immutable
@OpenDaylightImmutableStyle
public abstract class GroupEntity extends AbstractSwitchEntity {

    // This is required as it will cause the code generation by @Immutable.org to implement Builder,
    // which is required Xtend sources can use the XtendBuilderExtensions.operator_doubleGreaterThan
    public abstract static class Builder implements org.opendaylight.yangtools.concepts.Builder<GroupEntity> {}

    private transient GroupBuilder groupBuilder;

    public abstract List<BucketInfo> getBucketInfoList();

    public GroupBuilder getGroupBuilder() {
        if (groupBuilder == null) {
            groupBuilder = new GroupBuilder();

            GroupId groupId = new GroupId(getGroupId());
            groupBuilder.setKey(new GroupKey(groupId));
            groupBuilder.setGroupId(groupId);

            groupBuilder.setGroupName(getGroupName());
            groupBuilder.setGroupType(getGroupType());
            groupBuilder.setBuckets(MDSALUtil.buildBuckets(getBucketInfoList()));
        }

        return groupBuilder;
    }

    public abstract long getGroupId();

    public abstract String getGroupName();

    public abstract GroupTypes getGroupType();

}
