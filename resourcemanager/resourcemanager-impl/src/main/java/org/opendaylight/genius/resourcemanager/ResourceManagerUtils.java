/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.resourcemanager;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResourceManagerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceManagerUtils.class);

    private static Integer localHostAddress;

    private ResourceManagerUtils() {
    }

    protected static InstanceIdentifier<IdPool> getIdPoolInstance(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idPoolBuilder = InstanceIdentifier.builder(IdPools.class)
                .child(IdPool.class, new IdPoolKey(poolName));
        return idPoolBuilder.build();
    }

    protected static String getLocalPoolName(String poolName) {
        if (localHostAddress == null) {
            try {
                localHostAddress = InetAddresses.coerceToInteger(InetAddress.getLocalHost());
            } catch (UnknownHostException e) {
                LOG.error("Cannot build the local pool name: ", e);
            }
        }
        return poolName + "." + localHostAddress;
    }
}
