/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yangtools.util.EvenMoreObjects;

public class GroupEntity extends AbstractSwitchEntity {
    private static final long serialVersionUID = 1L;

    private long groupIdAsLong;
    private String groupName;
    private GroupTypes groupType;
    private List<BucketInfo> bucketInfos;

    private transient GroupBuilder groupBuilder;

    public GroupEntity(BigInteger dpnId) {
        super(dpnId);
    }

    public GroupEntity(long dpnId) {
        this(BigInteger.valueOf(dpnId));
    }

    @Override
    public String toString() {
        return "GroupEntity [dpnId=" + getDpnId() + ", groupId=" + groupIdAsLong + ", groupName=" + groupName
                + ", groupType=" + groupType + ", bucketInfo=" + bucketInfos + "]";
    }

    public List<BucketInfo> getBucketInfoList() {
        return bucketInfos;
    }

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

    public long getGroupId() {
        return groupIdAsLong;
    }

    public String getGroupName() {
        return groupName;
    }

    public GroupTypes getGroupType() {
        return groupType;
    }

    public void setBucketInfoList(List<BucketInfo> listBucketInfo) {
        this.bucketInfos = listBucketInfo;
    }

    public void setGroupId(long groupIdAsLong) {
        this.groupIdAsLong = groupIdAsLong;
        if (this.groupBuilder != null) {
            GroupId groupId = new GroupId(getGroupId());
            this.groupBuilder.setKey(new GroupKey(groupId));
            this.groupBuilder.setGroupId(groupId);
        }
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
        this.groupBuilder = null;
    }

    public void setGroupType(GroupTypes groupType) {
        this.groupType = groupType;
        this.groupBuilder = null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDpnId(), groupIdAsLong, groupName, groupType, bucketInfos);
    }

    @Override
    public boolean equals(Object obj) {
        return EvenMoreObjects.equalsHelper(this, obj,
            (self, other) -> Objects.equals(self.getDpnId(), other.getDpnId())
                          && Objects.equals(this.groupIdAsLong, other.groupIdAsLong)
                          && Objects.equals(this.groupName, other.groupName)
                          && Objects.equals(this.groupType, other.groupType)
                          && Objects.equals(this.bucketInfos, other.bucketInfos));
    }
}
