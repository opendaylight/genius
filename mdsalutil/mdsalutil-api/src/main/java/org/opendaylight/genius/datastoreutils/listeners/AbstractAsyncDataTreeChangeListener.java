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
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Abstract class providing some common functionality to specific listeners.
 * This listener launches the received notifications in a different thread by
 * using the queuing functionality of the {@link ExecutorService}.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 * @param <T>
 *            type of the data object the listener is registered to.
 */
public abstract class AbstractAsyncDataTreeChangeListener<T extends DataObject>
        implements DataTreeChangeListenerActions<T>, DataTreeChangeListener<T> {
    private final ExecutorService executorService;

    @Inject
    public AbstractAsyncDataTreeChangeListener(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public final void onDataTreeChanged(Collection<DataTreeModification<T>> collection) {
        executorService.submit(() -> DataTreeChangeListenerActions.super.onDataTreeChanged(collection));
    }
}
