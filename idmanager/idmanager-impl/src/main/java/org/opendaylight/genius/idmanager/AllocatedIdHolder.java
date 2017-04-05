/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import com.google.common.base.Optional;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.allocated.ids.AllocatedIdEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;

public class AllocatedIdHolder implements IdHolder, Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Long, AllocatedEntry> allocatedEntries;

    private final IdUtils idUtils;

    public AllocatedIdHolder(IdUtils idUtils) {
        this.idUtils = idUtils;
        this.allocatedEntries = new HashMap<>();
    }

    public static class AllocatedEntry implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Long id;
        private Long expiredTimeSec;
        private String idKey;

        public AllocatedEntry(long id, long expiredTimeSec, String idKey) {
            this.id = id;
            this.expiredTimeSec = expiredTimeSec;
            this.idKey = idKey;
        }

        public long getId() {
            return id;
        }

        public void setExpiredTimeSec(long expiredTimeSec) {
            this.expiredTimeSec = expiredTimeSec;
        }

        public void setIdKey(String idKey) {
            this.idKey = idKey;
        }

        public long getExpiredTimeSec() {
            return expiredTimeSec;
        }

        public String getIdKey() {
            return idKey;
        }

        public boolean isExpired(long curTimeSec) {
            if (expiredTimeSec > 0 && expiredTimeSec <= curTimeSec) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "{Id: " + id + " ExpiredTimeSec: " + expiredTimeSec + " IdKey: " + idKey + "}";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AllocatedEntry entry2 = (AllocatedEntry) obj;
            if (id == entry2.getId() && idKey == entry2.getIdKey()) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (id == null ? 0 : id.hashCode());
            result = prime * result + (expiredTimeSec == null ? 0 : expiredTimeSec.hashCode());
            result = prime * result + (idKey == null ? 0 : idKey.hashCode());
            return result;
        }
    }

    @Override
    public Optional<Long> allocateId() {
        // get an expired id
        Optional<AllocatedEntry> optionalAllocatedId = getExpiredId();
        if (!optionalAllocatedId.isPresent()) {
            // no expired ids found
            return Optional.absent();
        }
        return Optional.of(Long.parseLong(optionalAllocatedId.get().getIdKey()));
    }

    public Optional<String> allocateId(Long id, Long expirationTimeSec, String idKey) {
        // Look for specific id
        Optional<AllocatedEntry> optionalAllocatedId = getAllocatedId(id);
        if (!optionalAllocatedId.isPresent()) {
            // id is free to use
            return Optional.absent();
        }
        AllocatedEntry allocatedId = optionalAllocatedId.get();
        long curTimeSec = System.currentTimeMillis() / 1000;
        if (idKey == allocatedId.getIdKey()) {
            // renew expiration
            reUseAllocatedId(expirationTimeSec, idKey, allocatedId);
            return Optional.absent();
        } else if (allocatedId.isExpired(curTimeSec)) {
            // return key for release
            return Optional.of(allocatedId.getIdKey());
        }
        // return request key - nothing to do
        return Optional.of(idKey);
    }

    private void reUseAllocatedId(Long expirationTimeSec, String idKey, AllocatedEntry allocatedId) {
        long expiredTimeSec = (expirationTimeSec != null) ? expirationTimeSec + System.currentTimeMillis() / 1000 : 0;
        allocatedId.setExpiredTimeSec(expiredTimeSec);
    }

    private Optional<AllocatedEntry> getExpiredId() {
        long curTimeSec = System.currentTimeMillis() / 1000;
        if (isIdAvailable(curTimeSec)) { // confusing
            for (AllocatedEntry allocatedIdEntry : allocatedEntries.values()) {
                if (allocatedIdEntry.isExpired(curTimeSec)) {
                    // found expired id
                    return Optional.of(allocatedIdEntry);
                }
            }
        }
        return Optional.absent();
    }

    private Optional<AllocatedEntry> getAllocatedId(long id) {
        AllocatedEntry allocatedEnry = allocatedEntries.get(id);
        if (allocatedEnry != null) {
            return Optional.of(allocatedEnry);
        }
        return Optional.absent();
    }

    @Override
    public void addId(long id) {
    }

    @Override
    public void addId(Long id, Long expirationTimeSec, String idKey) {
        // add/ update ? if equal - update expired, if doesn't exist create
        long curTimeSec = System.currentTimeMillis() / 1000;
        AllocatedEntry newEntry = new AllocatedEntry(id, curTimeSec + expirationTimeSec, idKey);
        if (allocatedEntries == null) {
            allocatedEntries = new HashMap<>();
        } else {
            AllocatedEntry existentEntry = allocatedEntries.get(id);
            if (newEntry.equals(existentEntry)) {
                reUseAllocatedId(expirationTimeSec, idKey, existentEntry);
                return;
            }
        }
        allocatedEntries.put(id, newEntry);
    }

    public void removeId(long id) {
        // send idKey as well and remove only if key is the same
        if (allocatedEntries != null) {
            AllocatedIdEntriesKey entryKey = new AllocatedIdEntriesKey(id);
            allocatedEntries.remove(entryKey);
        }
    }

    @Override
    public boolean isIdAvailable(long curTimeSec) {
        return !allocatedEntries.isEmpty();
    }

    @Override
    public boolean isIdAvailable(Long id) {
        // Look for specific id - free if not existent, re-use if expired or key is the same
        Optional<AllocatedEntry> optionalAllocatedId = getAllocatedId(id);
        if (!optionalAllocatedId.isPresent()) {
            // id is free to use
            return true;
        }
        AllocatedEntry allocatedId = optionalAllocatedId.get();
        long curTimeSec = System.currentTimeMillis() / 1000;
        return allocatedId.isExpired(curTimeSec);
    }

    public Map<Long, AllocatedEntry> getAllocatedEntries() {
        return allocatedEntries;
    }

    @Override
    public long getAvailableIdCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return "AllocatedIdHolder [allocatedEntries=" + allocatedEntries + "]";
    }

    @Override
    public void refreshDataStore(IdPoolBuilder idPoolBuilder) {
        idUtils.syncAllocatedIdHolder(this, idPoolBuilder);
    }
}
