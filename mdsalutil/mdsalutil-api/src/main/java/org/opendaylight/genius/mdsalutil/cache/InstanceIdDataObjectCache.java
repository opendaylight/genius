/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.cache;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
            LogicalDatastoreType datastoreType, InstanceIdentifier<V> listetenerRegistrationPath,
            CacheProvider cacheProvider) {
        super(dataObjectClass, dataBroker, datastoreType, listetenerRegistrationPath, cacheProvider,
            (iid, value) -> iid, iid -> iid);
    }
}
