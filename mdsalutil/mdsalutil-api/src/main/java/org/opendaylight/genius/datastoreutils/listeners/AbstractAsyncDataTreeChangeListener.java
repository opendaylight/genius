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
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.ops4j.pax.cdi.api.OsgiService;

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

    private final JobCoordinator jobCoordinator;

    @Inject
    public AbstractAsyncDataTreeChangeListener(@OsgiService JobCoordinator jobCoordinator) {
        this.jobCoordinator = jobCoordinator;
    }

    @Override
    public final void onDataTreeChanged(Collection<DataTreeModification<T>> collection) {
        jobCoordinator.enqueueJob(getClass().getName(), () -> {
            DataTreeChangeListenerActions.super.onDataTreeChanged(collection);
            return Collections.emptyList();
        });
    }
}
