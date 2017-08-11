/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lockmanager;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.Locks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TimeUnits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.LockBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.LockKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class LockManagerUtils {

    private static final String SEPARATOR = ":";

    private final AtomicInteger counter;
    private final int bladeId;

    public LockManagerUtils() throws UnknownHostException {
        counter = new AtomicInteger(0);
        bladeId = InetAddresses.coerceToInteger(InetAddress.getLocalHost());
    }

    public TimeUnit convertToTimeUnit(TimeUnits timeUnit) {
        switch (timeUnit) {
            case Days:
                return TimeUnit.DAYS;
            case Microseconds:
                return TimeUnit.MICROSECONDS;
            case Hours:
                return TimeUnit.HOURS;
            case Minutes:
                return TimeUnit.MINUTES;
            case Nanoseconds:
                return TimeUnit.NANOSECONDS;
            case Seconds:
                return TimeUnit.SECONDS;
            case Milliseconds:
            default:
                return TimeUnit.MILLISECONDS;
        }
    }

    public InstanceIdentifier<Lock> getLockInstanceIdentifier(String lockName) {
        return InstanceIdentifier.builder(Locks.class).child(Lock.class, new LockKey(lockName)).build();
    }

    public Lock buildLock(String lockName, String owner) {
        return new LockBuilder().setKey(new LockKey(lockName)).setLockName(lockName)
                .setLockOwner(owner).build();
    }

    public String getUniqueID() {
        int lockId = counter.incrementAndGet();
        return bladeId + SEPARATOR + lockId;
    }
}
