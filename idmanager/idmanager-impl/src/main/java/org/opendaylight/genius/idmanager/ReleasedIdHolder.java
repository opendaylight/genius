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
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;

public class ReleasedIdHolder implements IdHolder {
    private static final int INITIAL_INDEX = 0;

    private final AtomicLong availableIdCount = new AtomicLong();

    private final long timeDelaySec;
    private final IdUtils idUtils;

    private volatile List<DelayedIdEntry> delayedEntries = new CopyOnWriteArrayList<>();

    public ReleasedIdHolder(IdUtils idUtils, long timeDelaySec) {
        this.idUtils = idUtils;
        this.timeDelaySec = timeDelaySec;
        availableIdCount.set(0);
    }

    public ReleasedIdHolder(IdUtils idUtils, long timeDelaySec, List<DelayedIdEntry> delayedEntries) {
        this(idUtils, timeDelaySec);
        this.delayedEntries.addAll(delayedEntries);
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
        long curTimeSec = System.currentTimeMillis() / 1000;
        Optional<Long> allocatedId = Optional.empty();
        if (isIdAvailable(curTimeSec)) {
            Long count = availableIdCount.decrementAndGet();
            if (count < 0L) {
                availableIdCount.incrementAndGet();
                return allocatedId;
            }
            DelayedIdEntry idEntry = delayedEntries.remove(INITIAL_INDEX);
            if (idEntry.getReadyTimeSec() <= curTimeSec) {
                allocatedId = Optional.of(idEntry.getId());
            } else {
                delayedEntries.add(INITIAL_INDEX, idEntry);
                availableIdCount.incrementAndGet();
            }
        }
        return allocatedId;
    }

    @Override
    public void addId(long id) {
        long curTimeSec = System.currentTimeMillis() / 1000;
        DelayedIdEntry entry = new DelayedIdEntry(id, curTimeSec + timeDelaySec);
        availableIdCount.incrementAndGet();
        delayedEntries.add(entry);
    }

    @Override
    public boolean isIdAvailable(long curTimeSec) {
        if (availableIdCount.get() <= 0) {
            return false;
        }
        boolean isIdExists = false;
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

    @NonNull
    public List<DelayedIdEntry> getDelayedEntries() {
        return delayedEntries;
    }

    public void replaceDelayedEntries(@NonNull List<DelayedIdEntry> newDelayedEntries) {
        this.delayedEntries = new CopyOnWriteArrayList<>(newDelayedEntries);
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
        idUtils.syncReleaseIdHolder(this, idPoolBuilder);
    }
}
