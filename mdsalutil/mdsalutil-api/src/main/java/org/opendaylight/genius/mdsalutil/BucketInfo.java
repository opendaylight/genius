/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.opendaylight.genius.utils.MoreObjects2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

public class BucketInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<ActionInfo> m_listActionInfo;

    private Integer weight = 0;
    private Long watchPort = 0xffffffffL;
    private Long watchGroup = 0xffffffffL;

    public BucketInfo(List<ActionInfo> listActions) {
        m_listActionInfo = listActions;
    }

    public BucketInfo(List<ActionInfo> m_listActionInfo, Integer weight, Long watchPort, Long watchGroup) {
        super();
        this.m_listActionInfo = m_listActionInfo;
        this.weight = weight;
        this.watchPort = watchPort;
        this.watchGroup = watchGroup;
    }

    public void buildAndAddActions(List<Action> listActionOut) {
        int key = 0;
        if (m_listActionInfo != null) {
            for (ActionInfo actionInfo : m_listActionInfo) {
                actionInfo.setActionKey(key++);
                listActionOut.add(actionInfo.buildAction());
            }
        }
    }

    public void setWeight(Integer bucketWeight) {
        weight = bucketWeight;
    }

    public Integer getWeight() {
        return weight;
    }

    public List<ActionInfo> getActionInfoList() {
        return m_listActionInfo;
    }

    public Long getWatchPort() {
        return watchPort;
    }

    public void setWatchPort(Long watchPort) {
        this.watchPort = watchPort;
    }

    public Long getWatchGroup() {
        return watchGroup;
    }

    public void setWatchGroup(Long watchGroup) {
        this.watchGroup = watchGroup;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("actionInfoList", m_listActionInfo).add("weight", weight)
                .add("watchPort", watchPort).add("watchGroup", watchGroup).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_listActionInfo, weight, watchPort, watchGroup);
    }

    @Override
    public boolean equals(Object obj) {
        return MoreObjects2.equalsHelper(this, obj,
                (self, other) -> Objects.equals(self.m_listActionInfo, other.m_listActionInfo)
                              && Objects.equals(self.weight, other.weight)
                              && Objects.equals(self.watchPort, other.watchPort)
                              && Objects.equals(self.watchGroup, other.watchGroup));
    }
}
