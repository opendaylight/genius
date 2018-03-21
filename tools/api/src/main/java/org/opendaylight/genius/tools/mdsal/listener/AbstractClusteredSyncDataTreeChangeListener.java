/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.tools.mdsal.listener;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to specific listeners. This is the clustered version of the
 * {@link AbstractSyncDataTreeChangeListener}.
 *
 * @param <T> type of the data object the listener is registered to.
 *
 * @see AbstractSyncDataTreeChangeListener
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
public abstract class AbstractClusteredSyncDataTreeChangeListener<T extends DataObject> extends
        AbstractDataTreeChangeListener<T> implements ClusteredDataTreeChangeListener<T> {

    @Inject
    public AbstractClusteredSyncDataTreeChangeListener(DataBroker dataBroker,
                                                       DataTreeIdentifier<T> dataTreeIdentifier) {
        super(dataBroker, dataTreeIdentifier);
    }

    @Inject
    public AbstractClusteredSyncDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                                       InstanceIdentifier<T> instanceIdentifier) {
        super(dataBroker, datastoreType, instanceIdentifier);
    }

    @Inject
    public AbstractClusteredSyncDataTreeChangeListener(DataBroker dataBroker,
                                                       LogicalDatastoreType datastoreType,
                                                       InstanceIdentifier<T> instanceIdentifier,
                                                       MetricProvider metricProvider) {
        super(dataBroker, datastoreType, instanceIdentifier, metricProvider);
    }

    @Override
    public final void onDataTreeChanged(@Nonnull Collection<DataTreeModification<T>> collection) {
        super.onDataTreeChanged(collection, getDataStoreMetrics());
    }
}
