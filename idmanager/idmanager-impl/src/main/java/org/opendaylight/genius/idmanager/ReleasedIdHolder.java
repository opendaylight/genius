/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;

import com.google.common.base.Optional;

public class ReleasedIdHolder implements IdHolder, Serializable {

    private static final long serialVersionUID = 1L;
    private static final int INITIAL_INDEX = 0;
    private AtomicLong availableIdCount = new AtomicLong();

    private long timeDelaySec;
    private List<DelayedIdEntry> delayedEntries;

    public ReleasedIdHolder(long timeDelaySec) {
        this.timeDelaySec = timeDelaySec;
        this.delayedEntries = new CopyOnWriteArrayList<>();
        availableIdCount.set(0);
    }

    public static class DelayedIdEntry implements Serializable {

        private static final long serialVersionUID = 1L;

        private final long id;
        private final long readyTimeSec;

        public DelayedIdEntry(long id, long readyTimeSec) {
            this.id = id;
            this.readyTimeSec = readyTimeSec;
        }

        @Override
        public String toString() {
            return "{Id: " + id + " ReadyTimeSec: " + readyTimeSec + "}";
        }

        public long getId() {
            return id;
        }

        public long getReadyTimeSec() {
            return readyTimeSec;
        }
    }

    @Override
    public Optional<Long> allocateId() {
        if (!isIdAvailable()) {
            return Optional.absent();
        }
        availableIdCount.decrementAndGet();
        Optional<Long> id = Optional.of(delayedEntries.get(INITIAL_INDEX).id);
        delayedEntries.remove(INITIAL_INDEX);
        return id;
    }

    @Override
    public void addId(long id) {
        long curTimeSec = System.currentTimeMillis() / 1000;
        DelayedIdEntry entry = new DelayedIdEntry(id, curTimeSec + timeDelaySec);
        if (delayedEntries == null) {
            delayedEntries = new CopyOnWriteArrayList<>();
        }
        availableIdCount.incrementAndGet();
        delayedEntries.add(entry);
    }

    @Override
    public boolean isIdAvailable() {
        if (availableIdCount.get() == 0) {
            return false;
        }
        boolean isIdExists = false;
        long curTimeSec = System.currentTimeMillis() / 1000;
        if (!delayedEntries.isEmpty() && delayedEntries.get(INITIAL_INDEX).readyTimeSec <= curTimeSec) {
            isIdExists = true;
        }
        return isIdExists;
    }

    @Override
    public long getAvailableIdCount() {
        long availableDelayedEntries = availableIdCount.get();
        int index = INITIAL_INDEX;
        if (delayedEntries.isEmpty()) {
            return index;
        }
        long curTimeSec = System.currentTimeMillis() / 1000;
        while (index < availableDelayedEntries && delayedEntries.get(index).readyTimeSec <= curTimeSec) {
            index++;
        }
        return index;
    }

    public long getTimeDelaySec() {
        return timeDelaySec;
    }

    public List<DelayedIdEntry> getDelayedEntries() {
        return delayedEntries;
    }

    public void setTimeDelaySec(long timeDelaySec) {
        this.timeDelaySec = timeDelaySec;
    }

    public void setDelayedEntries(List<DelayedIdEntry> delayedEntries) {
        this.delayedEntries = delayedEntries;
    }

    public void setAvailableIdCount(long availableIdCount) {
        this.availableIdCount.set(availableIdCount);
    }

    @Override
    public String toString() {
        return "ReleasedIdHolder [availableIdCount=" + availableIdCount
                + ", timeDelaySec=" + timeDelaySec + ", delayedEntries="
                + delayedEntries + "]";
    }

    @Override
    public void refreshDataStore(IdPoolBuilder idPoolBuilder) {
        IdUtils.syncReleaseIdHolder(this, idPoolBuilder);
    }
}