/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.cache;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A DataObjectCache keyed by InstanceIdentifier.
 *
 * @author Thomas Pantelis
 */
public class InstanceIdDataObjectCache<V extends DataObject> extends DataObjectCache<InstanceIdentifier<V>, V> {
    public InstanceIdDataObjectCache(Class<V> dataObjectClass, DataBroker dataBroker,
            LogicalDatastoreType datastoreType, InstanceIdentifier<V> listenerRegistrationPath,
            CacheProvider cacheProvider) {
        super(dataObjectClass, dataBroker, datastoreType, listenerRegistrationPath, cacheProvider,
            (iid, value) -> iid, iid -> iid);
    }
}
