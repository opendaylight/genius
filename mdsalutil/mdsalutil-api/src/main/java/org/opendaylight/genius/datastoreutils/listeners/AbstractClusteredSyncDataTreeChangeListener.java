/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Abstract class providing some common functionality to specific listeners.
 * This listener should be used in clustered deployments.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 * @param <T>
 *            type of the data object the listener is registered to.
 */
public abstract class AbstractClusteredSyncDataTreeChangeListener<T extends DataObject>
        implements DataTreeChangeListenerActions<T>, ClusteredDataTreeChangeListener<T> {

    @Override
    public final void onDataTreeChanged(Collection<DataTreeModification<T>> collection) {
        DataTreeChangeListenerActions.super.onDataTreeChanged(collection);
    }
}
