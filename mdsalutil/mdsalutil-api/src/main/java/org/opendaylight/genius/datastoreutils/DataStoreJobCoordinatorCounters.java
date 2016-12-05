/*
 * Copyright (c) 2016 Hewlett-Packard Enterprise and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.datastoreutils;

import org.opendaylight.infrautils.counters.api.OccurenceCounter;

public enum DataStoreJobCoordinatorCounters {
    jobs_remove_entry,
    jobs_cleared,
    jobs_pending(true);

    private OccurenceCounter counter;

    DataStoreJobCoordinatorCounters() {
        counter = new OccurenceCounter(getClass().getSimpleName(), name(), name());
    }

    DataStoreJobCoordinatorCounters(boolean isState) {
        counter = new OccurenceCounter(getClass().getSimpleName(), "dsjcc", name(), name(), false, null, true, true);
    }

    public void inc() {
        counter.inc();
    }

    public void dec() {
        counter.dec();
    }
}
