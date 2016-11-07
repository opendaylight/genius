/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ChainableDataChangeListener.
 *
 * <p>Suitable as a delegate for listeners implementing ChainableDataChangeListener.
 *
 * @deprecated Migrate your listeners from {@link DataChangeListener} to
 *             {@link DataTreeChangeListener}, and use the
 *             {@link ChainableDataTreeChangeListenerImpl}.
 *
 * @author Michael Vorburger
 */
@Deprecated
public final class ChainableDataChangeListenerImpl implements ChainableDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ChainableDataChangeListenerImpl.class);

    private final List<DataChangeListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addAfterListener(DataChangeListener listener) {
        listeners.add(listener);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void notifyAfterOnDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (DataChangeListener listener : listeners) {
            try {
                listener.onDataChanged(change);
            } catch (Exception e) {
                LOG.error("Caught Exception from an after listener's onDataChanged(); "
                        + "nevertheless proceeding with others, if any", e);
            }
        }
    }

}
