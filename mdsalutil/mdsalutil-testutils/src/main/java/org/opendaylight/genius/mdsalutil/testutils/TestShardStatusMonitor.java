/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.testutils;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import java.util.List;
import org.mockito.Mockito;
import org.opendaylight.genius.mdsalutil.interfaces.ShardStatusMonitor;

public abstract class TestShardStatusMonitor implements ShardStatusMonitor {

    public static TestShardStatusMonitor newInstance() {
        return Mockito.mock(TestShardStatusMonitor.class, realOrException());
    }

    @Override
    public  boolean getShardStatus(List<String> shards) {
        return true;
    }
}