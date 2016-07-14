/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.resourcemanager;

import com.google.common.net.InetAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

class ResourceManagerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerUtils.class);

    private static int BLADE_ID;
    static {
        try {
            BLADE_ID = InetAddresses.coerceToInteger(InetAddress.getLocalHost());
        } catch (Exception e) {
            LOGGER.error("ResourceManager - Exception - {}", e.getMessage());
        }
    }

    protected static InstanceIdentifier<IdPool> getIdPoolInstance(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idPoolBuilder = InstanceIdentifier
                .builder(IdPools.class).child(IdPool.class,
                        new IdPoolKey(poolName));
        InstanceIdentifier<IdPool> id = idPoolBuilder.build();
        return id;
    }

    protected static String getLocalPoolName(String poolName) {
        return (poolName + "." + BLADE_ID);
    }

}
