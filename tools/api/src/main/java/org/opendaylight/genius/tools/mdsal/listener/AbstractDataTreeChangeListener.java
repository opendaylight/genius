/*
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.tools.mdsal.listener;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to abstract listeners. This class is not designed to be
 * extended by the specific listeners, that's why it is package-private. It provides subclasses with access to the
 * {@link DataBroker} passed as constructor argument, listener registration/de-registration and a shutdown method to
 * be implemented if needed by the subclasses (e.g. shutting down services, closing resources, etc.)
 *
 * @param <T> type of the data object the listener is registered to.
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
abstract class AbstractDataTreeChangeListener<T extends DataObject> implements DataTreeChangeListener<T>,
        DataTreeChangeListenerActions<T>, AutoCloseable {

    private final DataBroker dataBroker;
    private final DataTreeIdentifier<T> dataTreeIdentifier;
    private ListenerRegistration<AbstractDataTreeChangeListener<T>> dataChangeListenerRegistration;
    private DataStoreMetrics dataStoreMetrics;

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker, DataTreeIdentifier<T> dataTreeIdentifier) {
        this.dataBroker = dataBroker;
        this.dataTreeIdentifier = dataTreeIdentifier;
    }

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                   InstanceIdentifier<T> instanceIdentifier) {
        this(dataBroker, new DataTreeIdentifier<>(datastoreType, instanceIdentifier));
    }

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker,
                                   LogicalDatastoreType datastoreType,
                                   InstanceIdentifier<T> instanceIdentifier,
                                   MetricProvider metricProvider) {
        this(dataBroker, new DataTreeIdentifier<>(datastoreType, instanceIdentifier));
        this.dataStoreMetrics = new DataStoreMetrics(metricProvider, getClass());
    }

    @PostConstruct
    public void register() {
        this.dataChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIdentifier, this);
    }

    protected DataBroker getDataBroker() {
        return dataBroker;
    }

    protected DataStoreMetrics getDataStoreMetrics() {
        return dataStoreMetrics;
    }

    @Override
    @PreDestroy
    public void close() {
        if (dataChangeListenerRegistration != null) {
            dataChangeListenerRegistration.close();
        }
    }

    @Override
    @Deprecated
    public void add(@Nonnull T newDataObject) {
        // TODO: to be removed after all listeners migrated to use the new methods
    }

    @Override
    @Deprecated
    public void remove(@Nonnull T removedDataObject) {
        // TODO: to be removed after all listeners migrated to use the new methods
    }

    @Override
    @Deprecated
    public void update(@Nonnull T originalDataObject, @Nonnull T updatedDataObject) {
        // TODO: to be removed after all listeners migrated to use the new methods
    }
}

