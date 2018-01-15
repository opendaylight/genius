/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.test

import org.opendaylight.genius.idmanager.IdUtils
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.AvailableIdsHolderBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ChildPoolsBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntriesBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.ReleasedIdsHolderBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.released.ids.DelayedIdEntriesBuilder

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedAllocateIdFromReleasedId {

    static val idUtil = new IdUtils();
    static String localPoolName = idUtil.getLocalPoolName("test-pool");

    def static idPoolParent() {
        new IdPoolBuilder >> [
            availableIdsHolder = new AvailableIdsHolderBuilder >> [
                cursor = 101L
                end = 100L
                start = 0L
            ]
            blockSize = 10
            childPools = #[
                new ChildPoolsBuilder >> [
                    childPoolName = localPoolName
                    lastAccessTime = 0L
                ]
            ]
            idEntries = #[
                new IdEntriesBuilder >> [
                    idKey = "test-key1"
                    idValue = #[
                        1L
                    ]
                ],
                new IdEntriesBuilder >> [
                    idKey = "test-key2"
                    idValue = #[
                        0L
                    ]
                ]
            ]
            poolName = "test-pool"
            releasedIdsHolder = new ReleasedIdsHolderBuilder >> [
                availableIdCount = 0L
                delayedIdEntries = #[]
                delayedTimeSec = 0L
            ]
        ]
    }

    def static idPoolChild() {

        new IdPoolBuilder >> [
            availableIdsHolder = new AvailableIdsHolderBuilder >> [
                cursor = 10L
                end = 9L
                start = 0L
            ]
            blockSize = 10
            parentPoolName = "test-pool"
            poolName = localPoolName
            releasedIdsHolder = new ReleasedIdsHolderBuilder >> [
                availableIdCount = 2L
                delayedIdEntries = #[
                    new DelayedIdEntriesBuilder >> [
                        id = 2L
                        readyTimeSec = 0L
                    ],
                    new DelayedIdEntriesBuilder >> [
                        id = 3L
                        readyTimeSec = 0L
                    ]
                ]
                delayedTimeSec = 30L
            ]
        ]
    }
}