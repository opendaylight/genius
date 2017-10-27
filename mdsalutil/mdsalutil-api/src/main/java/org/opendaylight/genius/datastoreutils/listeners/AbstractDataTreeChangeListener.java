/*
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to abstract listeners. This is class is not designed to be
 * extended by the specific listeners, that's why it is package-private. It provides subclasses with access to the
 * {@link DataBroker} passed as constructor argument, listener registration/de-registration and a shutdown method to
 * be implemented if needed by the subclasses (e.g. shutting down services, closing resources, etc.)
 *
 * @param <T> type of the data object the listener is registered to.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
abstract class AbstractDataTreeChangeListener<T extends DataObject> implements DataTreeChangeListener<T>,
        AutoCloseable {
    private final DataBroker dataBroker;
    private final ListenerRegistration<AbstractDataTreeChangeListener<T>> dataChangeListenerRegistration;

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker, DataTreeIdentifier<T> dataTreeIdentifier) {
        this.dataBroker = dataBroker;
        dataChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIdentifier, this);
    }

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                   InstanceIdentifier<T> instanceIdentifier) {
        this(dataBroker, new DataTreeIdentifier<>(datastoreType, instanceIdentifier));
    }

    protected DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    @PreDestroy
    public final void close() {
        // ^^^ final to avoid @Override without @PreDestroy
        // JSR 250: "the method is called unless a subclass overrides the method without repeating the annotation"
        shutdown();
        if (dataChangeListenerRegistration != null) {
            dataChangeListenerRegistration.close();
        }
    }

    /**
     * Sub-classes can override this method to add their own close behaviours.
     */
    protected void shutdown() {
        // Nothing by default
    }
}

