/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Default implementation of ChainableDataTreeChangeListener.
 *
 * <p>Suitable as a delegate for listeners implementing ChainableDataTreeChangeListener.
 *
 * @author Michael Vorburger
 */
public final class ChainableDataTreeChangeListenerImpl<T extends DataObject>
        implements ChainableDataTreeChangeListener<T> {

    private final List<DataTreeChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addAfterListener(DataTreeChangeListener<T> listener) {
        listeners.add(listener);
    }

    public void notifyAfterOnDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        for (DataTreeChangeListener<T> listener : listeners) {
            listener.onDataTreeChanged(changes);
        }
    }

}
