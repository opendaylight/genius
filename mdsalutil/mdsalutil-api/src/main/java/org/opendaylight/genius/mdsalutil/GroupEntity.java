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

    private long m_lGroupId;
    private String m_sGroupName;
    private GroupTypes m_groupType;
    private List<BucketInfo> m_listBucketInfo;

    private transient GroupBuilder m_groupBuilder;

    public GroupEntity(BigInteger dpnId) {
        super(dpnId);
    }

    public GroupEntity(long dpnId) {
        this(BigInteger.valueOf(dpnId));
    }

    @Override
    public String toString() {
        return "GroupEntity [dpnId=" + getDpnId() + ", groupId=" + m_lGroupId + ", groupName=" + m_sGroupName + ", groupType="
                + m_groupType + ", bucketInfo=" + m_listBucketInfo + "]";
    }

    public List<BucketInfo> getBucketInfoList() {
        return m_listBucketInfo;
    }

    public GroupBuilder getGroupBuilder() {
        if (m_groupBuilder == null) {
            m_groupBuilder = new GroupBuilder();

            GroupId groupId = new GroupId(getGroupId());
            m_groupBuilder.setKey(new GroupKey(groupId));
            m_groupBuilder.setGroupId(groupId);

            m_groupBuilder.setGroupName(getGroupName());
            m_groupBuilder.setGroupType(getGroupType());
            m_groupBuilder.setBuckets(MDSALUtil.buildBuckets(getBucketInfoList()));
        }

        return m_groupBuilder;
    }

    public long getGroupId() {
        return m_lGroupId;
    }

    public String getGroupName() {
        return m_sGroupName;
    }

    public GroupTypes getGroupType() {
        return m_groupType;
    }

    public void setBucketInfoList(List<BucketInfo> listBucketInfo) {
        m_listBucketInfo = listBucketInfo;
    }

    public void setGroupId(long lGroupId) {
        m_lGroupId = lGroupId;
        if (m_groupBuilder != null) {
            GroupId groupId = new GroupId(getGroupId());
            m_groupBuilder.setKey(new GroupKey(groupId));
            m_groupBuilder.setGroupId(groupId);
        }
    }

    public void setGroupName(String sGroupName) {
        m_sGroupName = sGroupName;
        m_groupBuilder = null;
    }

    public void setGroupType(GroupTypes groupType) {
        m_groupType = groupType;
        m_groupBuilder = null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDpnId(), m_lGroupId, m_sGroupName, m_groupType, m_listBucketInfo);
    }

    @Override
    public boolean equals(Object obj) {
        return EvenMoreObjects.equalsHelper(this, obj,
            (self, other) -> Objects.equals(self.getDpnId(), other.getDpnId())
                          && Objects.equals(this.m_lGroupId, other.m_lGroupId)
                          && Objects.equals(this.m_sGroupName, other.m_sGroupName)
                          && Objects.equals(this.m_groupType, other.m_groupType)
                          && Objects.equals(this.m_listBucketInfo, other.m_listBucketInfo));
    }
}
