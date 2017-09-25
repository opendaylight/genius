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

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    @PreDestroy
    public final void close() {
        // ^^^ final to avoid @Override without @PreDestroy
        // JSR 250: "the method is called unless a subclass overrides the method without repeating the annotation"
        dataChangeListenerRegistration.close();
    }
}

