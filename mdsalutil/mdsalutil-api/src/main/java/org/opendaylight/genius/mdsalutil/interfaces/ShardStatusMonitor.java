/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces;

import java.util.List;

public interface ShardStatusMonitor {

    String JMX_OBJECT_NAME_LIST_OF_CONFIG_SHARDS =
            "org.opendaylight.controller:type=DistributedConfigDatastore,"
                    + "Category=ShardManager,name=shard-manager-config";
    String JMX_OBJECT_NAME_LIST_OF_OPER_SHARDS =
            "org.opendaylight.controller:type=DistributedOperationalDatastore,"
                    + "Category=ShardManager,name=shard-manager-operational";

    boolean getShardStatus(List<String> shards);
}