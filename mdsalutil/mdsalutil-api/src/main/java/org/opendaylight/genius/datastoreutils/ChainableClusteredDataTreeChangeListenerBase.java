/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.serviceutils.tools.mdsal.listener.ChainableDataTreeChangeListener;
import org.opendaylight.serviceutils.tools.mdsal.listener.ChainableDataTreeChangeListenerImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * {@link ClusteredDataTreeChangeListener} which is chain-able ({@link ChainableDataTreeChangeListener}).
 *
 * <p>Do *NOT* use this just in order to be able to wait for change event processing of listeners extending this
 * in tests with the AsyncEventsWaiter.  That is not necessary - because this listener is not asynchronous.
 * This is just intended to be used for any other cases where you would like a CDTL to be chainable, not
 * for tests and the AsyncEventsWaiter.
 *
 * @author Michael Vorburger
 */
public abstract class ChainableClusteredDataTreeChangeListenerBase<T extends DataObject>
        implements ClusteredDataTreeChangeListener<T>, ChainableDataTreeChangeListener<T> {

    private final ChainableDataTreeChangeListenerImpl<T> chainingDelegate = new ChainableDataTreeChangeListenerImpl<>();

    @Override
    public void addBeforeListener(DataTreeChangeListener<T> listener) {
        chainingDelegate.addBeforeListener(listener);
    }

    @Override
    public void addAfterListener(DataTreeChangeListener<T> listener) {
        chainingDelegate.addAfterListener(listener);
    }

    @Override
    public final void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        chainingDelegate.notifyBeforeOnDataTreeChanged(changes);
        onDataTreeChanged2(changes);
        chainingDelegate.notifyAfterOnDataTreeChanged(changes);
    }

    protected abstract void onDataTreeChanged2(Collection<DataTreeModification<T>> changes);
}
