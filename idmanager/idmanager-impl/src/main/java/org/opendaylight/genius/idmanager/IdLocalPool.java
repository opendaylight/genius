/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

public class IdLocalPool {

    private final String poolName;
    private final IdUtils idUtils;
    private volatile IdHolder availableIds; // List of available IDs
    private volatile IdHolder releasedIds; // List of released IDs

    public IdLocalPool(IdUtils idUtils, String poolName, long low, long high) {
        this.poolName = poolName;
        this.idUtils = idUtils;
        this.availableIds = new AvailableIdHolder(idUtils, low, high);
        this.releasedIds = new ReleasedIdHolder(idUtils, IdUtils.DEFAULT_DELAY_TIME);
    }

    public IdLocalPool(IdUtils idUtils, String poolName) {
        this.poolName = poolName;
        this.idUtils = idUtils;
        this.releasedIds = new ReleasedIdHolder(idUtils, IdUtils.DEFAULT_DELAY_TIME);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (availableIds == null ? 0 : availableIds.hashCode());
        result = prime * result + (poolName == null ? 0 : poolName.hashCode());
        result = prime * result + (releasedIds == null ? 0 : releasedIds.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "IdLocalPool [poolName=" + poolName + ", availableIds=" + availableIds + ", releasedIds=" + releasedIds
                + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IdLocalPool other = (IdLocalPool) obj;
        if (availableIds == null) {
            if (other.availableIds != null) {
                return false;
            }
        } else if (!availableIds.equals(other.availableIds)) {
            return false;
        }
        if (poolName == null) {
            if (other.poolName != null) {
                return false;
            }
        } else if (!poolName.equals(other.poolName)) {
            return false;
        }
        if (releasedIds == null) {
            if (other.releasedIds != null) {
                return false;
            }
        } else if (!releasedIds.equals(other.releasedIds)) {
            return false;
        }
        return true;
    }

    public String getPoolName() {
        return poolName;
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

    public IdLocalPool deepCopyOf() {
        AvailableIdHolder tempAvailableIdHolder = (AvailableIdHolder) getAvailableIds();
        ReleasedIdHolder tempReleaseIdHolder = (ReleasedIdHolder) getReleasedIds();
        IdLocalPool clonedIdPool = new IdLocalPool(idUtils, getPoolName());

        AvailableIdHolder newAvailableIds = new AvailableIdHolder(idUtils, tempAvailableIdHolder.getLow(),
                tempAvailableIdHolder.getHigh());
        newAvailableIds.setCur(tempAvailableIdHolder.getCur().longValue());
        clonedIdPool.setAvailableIds(newAvailableIds);

        ReleasedIdHolder newReleasedIds = new ReleasedIdHolder(idUtils, IdUtils.DEFAULT_DELAY_TIME,
                tempReleaseIdHolder.getDelayedEntries());
        newReleasedIds.setAvailableIdCount(tempReleaseIdHolder.getAvailableIdCount());
        clonedIdPool.setReleasedIds(newReleasedIds);

        return clonedIdPool;
    }
}
