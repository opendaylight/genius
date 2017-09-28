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
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to specific listeners. This listener launches the received
 * notifications in a different thread by using an {@link ExecutorService}.
 *
 * <p>The {@link ExecutorService} passed to the constructor could e.g. be obtained from the {@link SpecialExecutors},
 * typically its <code>newBoundedFastThreadPool()</code> variant if the AsyncDTCL implementation subclass can handle
 * notifications concurrently (and therefore possibly out of order), or otherwise otherwise typically an
 * {@link org.opendaylight.infrautils.utils.concurrent.Executors#newSingleThreadExecutor(String, org.slf4j.Logger)}.
 * In both cases, the thread name prefix argument should by convention be set to the class name of the listener
 * subclass of this abstract class, and the Logger to it's own <code>LOG</code>.
 *
 * <p>Subclasses are also encouraged to, in addition to passing the ExecutorService for use in
 * production (by Blueprint wiring) based on above via super(), expose a public constructor letting tests specify
 * an alternative ExecutorService; this is useful e.g. to inject infrautils' AwaitableExecutorService for testing.
 *
 * @param <T> type of the data object the listener is registered to.
 *
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
