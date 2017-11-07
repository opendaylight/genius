/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.lockmanager.impl;

import java.util.concurrent.TimeUnit;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.Locks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.TimeUnits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.Lock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.LockBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.locks.LockKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LockManagerUtils {

    private LockManagerUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static TimeUnit convertToTimeUnit(TimeUnits timeUnit) {
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

    public static InstanceIdentifier<Lock> getLockInstanceIdentifier(String lockName) {
        return InstanceIdentifier.builder(Locks.class).child(Lock.class, new LockKey(lockName)).build();
    }

    public static Lock buildLockData(String lockName) {
        return new LockBuilder().setKey(new LockKey(lockName)).setLockName(lockName)
                .setLockOwner(Thread.currentThread().getName()).build();
    }
}
