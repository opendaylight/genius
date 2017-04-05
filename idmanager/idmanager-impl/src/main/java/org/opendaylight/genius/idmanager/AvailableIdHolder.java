/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import com.google.common.base.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;

public class AvailableIdHolder implements IdHolder {

    private final long low;
    private final long high;
    private final AtomicLong cur = new AtomicLong();

    private final IdUtils idUtils;

    public AvailableIdHolder(IdUtils idUtils, long low, long high) {
        this.idUtils = idUtils;
        this.low = low;
        this.high = high;
        cur.set(low - 1);
    }

    @Override
    public Optional<Long> allocateId() {
        if (isIdAvailable(0L /*currentTime*/)) { //currentTime parameter not used for fetching Ids from AvailableIds
            long id = cur.incrementAndGet();
            if (id <= high) {
                return Optional.of(id);
            }
        }
        return Optional.absent();
    }

    @Override
    public Optional<String> allocateId(Long id, Long expirationTimeSec, String idKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addId(long id) {
        throw new UnsupportedOperationException("addId is not supported");
    }

    @Override
    public void addId(Long id, Long expirationTimeSec, String idKey) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isIdAvailable(long curTimeSec) {
        return high > cur.get();
    }

    @Override
    public boolean isIdAvailable(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeId(long id) {
        // TODO Auto-generated method stub
    }

    public long getLow() {
        return low;
    }

    public long getHigh() {
        return high;
    }

    public AtomicLong getCur() {
        return cur;
    }

    @Override
    public long getAvailableIdCount() {
        return high - cur.get();
    }

    public void setCur(long cur) {
        this.cur.set(cur);
    }

    @Override
    public String toString() {
        return "AvailableIdHolder [low=" + low + ", high=" + high + ", cur="
                + cur + "]";
    }

    @Override
    public void refreshDataStore(IdPoolBuilder idPoolBuilder) {
        idUtils.syncAvailableIdHolder(this, idPoolBuilder);
    }

    public boolean isIdInRange(long id) {
        return (low <= id && id <= high);
    }
}
