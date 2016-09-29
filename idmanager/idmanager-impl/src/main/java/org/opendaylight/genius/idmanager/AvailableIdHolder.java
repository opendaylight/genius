/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;

import com.google.common.base.Optional;

public class AvailableIdHolder implements IdHolder {
    private long low = 0;
    private long high = 0;
    private AtomicLong cur = new AtomicLong();

    public AvailableIdHolder(long low, long high) {
        addIdBlock(low, high);
    }

    private void addIdBlock(long low, long high) {
        this.low = low;
        this.high = high;
        cur.set(low - 1);
    }

    @Override
    public Optional<Long> allocateId() {
        if (!isIdAvailable()) {
            return Optional.absent();
        }
        return Optional.of(cur.incrementAndGet());
    }

    @Override
    public void addId(long id) {
        throw new RuntimeException(new UnsupportedOperationException("addId is not supported"));
    }

    @Override
    public boolean isIdAvailable() {
        return high > cur.get();
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
        IdUtils.syncAvailableIdHolder(this, idPoolBuilder);
    }
}
