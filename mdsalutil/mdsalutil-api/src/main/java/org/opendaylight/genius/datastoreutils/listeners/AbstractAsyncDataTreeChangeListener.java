/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to specific listeners. This listener launches the received
 * notifications in a different thread by using the an {@link ExecutorService}.
 *
 * @param <T> type of the data object the listener is registered to.
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
public abstract class AbstractAsyncDataTreeChangeListener<T extends DataObject> extends
        AbstractDataTreeChangeListener<T> implements DataTreeChangeListenerActions<T>, DataTreeChangeListener<T> {
    private final ExecutorService executorService;

    @Inject
    public AbstractAsyncDataTreeChangeListener(DataBroker dataBroker, DataTreeIdentifier<T> dataTreeIdentifier,
                                               ExecutorService executorService) {
        super(dataBroker, dataTreeIdentifier);
        this.executorService = executorService;
    }

    @Inject
    public AbstractAsyncDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                               InstanceIdentifier<T> instanceIdentifier,
                                               ExecutorService executorService) {
        super(dataBroker, datastoreType, instanceIdentifier);
        this.executorService = executorService;
    }

    @Override
    public final void onDataTreeChanged(@Nonnull Collection<DataTreeModification<T>> collection) {
        executorService.execute(() -> DataTreeChangeListenerActions.super.onDataTreeChanged(collection));
    }
}
