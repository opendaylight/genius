/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * {@link ClusteredDataTreeChangeListener} which is chain-able ({@link ChainableDataTreeChangeListener}).
 *
 * <p>This is required e.g. in order to be able to wait for event processing in
 * such listeners in tests with the AsyncEventsWaiter.
 *
 * @author Michael Vorburger
 */
public abstract class ChainableClusteredDataTreeChangeListenerBase<T extends DataObject>
        implements ClusteredDataTreeChangeListener<T>, ChainableDataTreeChangeListener<T> {

    private final ChainableDataTreeChangeListenerImpl<T> chainingDelegate = new ChainableDataTreeChangeListenerImpl<>();

    @Override
    public void addAfterListener(DataTreeChangeListener<T> listener) {
        chainingDelegate.addAfterListener(listener);
    }

    @Override
    public final void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        onDataTreeChanged2(changes);
        chainingDelegate.notifyAfterOnDataTreeChanged(changes);
    }

    protected abstract void onDataTreeChanged2(Collection<DataTreeModification<T>> changes);
}
