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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ChainableDataTreeChangeListener.
 *
 * <p>Suitable as a delegate for listeners implementing ChainableDataTreeChangeListener.
 *
 * @author Michael Vorburger
 */
public final class ChainableDataTreeChangeListenerImpl<T extends DataObject>
        implements ChainableDataTreeChangeListener<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ChainableDataTreeChangeListenerImpl.class);

    private final List<DataTreeChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addAfterListener(DataTreeChangeListener<T> listener) {
        listeners.add(listener);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void notifyAfterOnDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        for (DataTreeChangeListener<T> listener : listeners) {
            try {
                listener.onDataTreeChanged(changes);
            } catch (Exception e) {
                LOG.error("Caught Exception from an after listener's onDataChanged(); "
                        + "nevertheless proceeding with others, if any", e);
            }
        }
    }

}
