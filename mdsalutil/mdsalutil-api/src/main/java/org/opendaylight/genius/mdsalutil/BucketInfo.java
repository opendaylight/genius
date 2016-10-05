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
import org.opendaylight.yangtools.util.EvenMoreObjects;

public class BucketInfo extends AbstractActionInfoList implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer weight = 0;
    private Long watchPort = 0xffffffffL;
    private Long watchGroup = 0xffffffffL;

    public BucketInfo(List<ActionInfo> listActions) {
        super(listActions);
    }

    public BucketInfo(List<ActionInfo> actionInfos, Integer weight, Long watchPort, Long watchGroup) {
        super(actionInfos);
        this.weight = weight;
        this.watchPort = watchPort;
        this.watchGroup = watchGroup;
    }

    public void setWeight(Integer bucketWeight) {
        weight = bucketWeight;
    }

    public Integer getWeight() {
        return weight;
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
        return MoreObjects.toStringHelper(this).add("actionInfos", getActionInfos()).add("weight", weight)
                .add("watchPort", watchPort).add("watchGroup", watchGroup).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getActionInfos(), weight, watchPort, watchGroup);
    }

    @Override
    public boolean equals(Object obj) {
        return EvenMoreObjects.equalsHelper(this, obj,
                (self, other) -> Objects.equals(self.getActionInfos(), other.getActionInfos())
                              && Objects.equals(self.weight, other.weight)
                              && Objects.equals(self.watchPort, other.watchPort)
                              && Objects.equals(self.watchGroup, other.watchGroup));
    }
}
