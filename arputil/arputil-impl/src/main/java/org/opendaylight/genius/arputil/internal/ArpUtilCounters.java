/*
 * Copyright (c) 2016 Hewlett-Packard Enterprise and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.arputil.internal;

import org.opendaylight.infrautils.counters.api.OccurenceCounter;

public enum ArpUtilCounters {
    arp_res_rcv,
    arp_res_rcv_notification,
    arp_res_rcv_notification_rejected,
    arp_req_rcv,
    arp_req_rcv_notification,
    arp_req_rcv_notification_rejected;

    private OccurenceCounter counter;

    ArpUtilCounters() {
        counter = new OccurenceCounter(getClass().getSimpleName(), name(), name());
    }

    public void inc() {
        counter.inc();
    }
}
