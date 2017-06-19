/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Abstract class providing some common functionality to specific listeners.
 * This listener launches the received notifications in a different job by using
 * the queuing functionality of the {@link JobCoordinator} which uses different
 * threads for each job.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 * @param <T>
 *            type of the data object the listener is registered to.
 */
public abstract class AbstractAsyncDataTreeChangeListener<T extends DataObject>
        implements DataTreeChangeListenerActions<T>, DataTreeChangeListener<T> {
    private static final int DEFAULT_POOL_SIZE = 5;

    private final ExecutorService executor;

    @Inject
    public AbstractAsyncDataTreeChangeListener() {
        this.executor = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);
    }

    @Override
    public final void onDataTreeChanged(Collection<DataTreeModification<T>> collection) {
        executor.submit(() -> {
            DataTreeChangeListenerActions.super.onDataTreeChanged(collection);
            return Collections.emptyList();
        });
    }
}
