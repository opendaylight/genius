/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.IfIndexesInterfaceMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Singleton
public class IfIndexInterfaceCache extends DataObjectCache<Integer, IfIndexInterface> {

    @Inject
    public IfIndexInterfaceCache(@Reference DataBroker dataBroker, @Reference CacheProvider cacheProvider) {
        super(IfIndexInterface.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(IfIndexesInterfaceMap.class).child(IfIndexInterface.class).build(),
                cacheProvider, (iid, ifIndexInterface) -> ifIndexInterface.key().getIfIndex(),
            ifIndex -> InstanceIdentifier.builder(IfIndexesInterfaceMap.class)
                .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build());
    }
}
