/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

public class IdLocalPool {
    private String poolName;
    private IdHolder availableIds; // List of available IDs
    private IdHolder releasedIds; // List of released IDs

    public IdLocalPool(String poolName, long low, long high) {
        this.poolName = poolName;
        availableIds = new AvailableIdHolder(low, high);
        releasedIds = new ReleasedIdHolder(IdUtils.DEFAULT_DELAY_TIME);
    }

    public IdLocalPool(String poolName) {
        this.poolName = poolName;
        releasedIds = new ReleasedIdHolder(IdUtils.DEFAULT_DELAY_TIME);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((availableIds == null) ? 0 : availableIds.hashCode());
        result = prime * result + ((poolName == null) ? 0 : poolName.hashCode());
        result = prime * result + ((releasedIds == null) ? 0 : releasedIds.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "IdLocalPool [poolName=" + poolName + ", availableIds="
                + availableIds + ", releasedIds=" + releasedIds + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IdLocalPool other = (IdLocalPool) obj;
        if (availableIds == null) {
            if (other.availableIds != null)
                return false;
        } else if (!availableIds.equals(other.availableIds))
            return false;
        if (poolName == null) {
            if (other.poolName != null)
                return false;
        } else if (!poolName.equals(other.poolName))
            return false;
        if (releasedIds == null) {
            if (other.releasedIds != null)
                return false;
        } else if (!releasedIds.equals(other.releasedIds))
            return false;
        return true;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public IdHolder getAvailableIds() {
        return availableIds;
    }

    public void setAvailableIds(IdHolder availableIds) {
        this.availableIds = availableIds;
    }

    public IdHolder getReleasedIds() {
        return releasedIds;
    }

    public void setReleasedIds(IdHolder releasedIds) {
        this.releasedIds = releasedIds;
    }

}
