/*
 * Copyright (c) 2016, 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Abstract class providing some common functionality to specific listeners.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 *
 * @param <T>
 *            type of the data object the listener is registered to.
 */
public interface AbstractClusteredDataTreeChangeListener<T extends DataObject>
        extends DataTreeChangeListenerActions<T>, ClusteredDataTreeChangeListener<T>, AutoCloseable {

    /* (non-Javadoc)
     * @see org.opendaylight.genius.datastoreutils.DataTreeChangeListenerActions#onDataTreeChanged(java.util.Collection)
     */
    @Override
    default void onDataTreeChanged(Collection<DataTreeModification<T>> collection) {
        DataTreeChangeListenerActions.super.onDataTreeChanged(collection);
    }
}
