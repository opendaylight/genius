/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.tools.mdsal.listener;

import java.util.EventListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * {@link DataTreeChangeListener} which can notify another DataTreeChangeListener.
 *
 * @author Michael Vorburger, based on discussions with Stephen Kitt
 */
public interface ChainableDataTreeChangeListener<T extends DataObject> extends EventListener {

    /**
     * Adds a "chained" DataTreeChangeListener, to which
     * {@link DataTreeChangeListener#onDataTreeChanged(java.util.Collection)}
     * calls are forwarded BEFORE having been processed by this DataTreeChangeListener.
     *
     * <p>If an asychronous DataTreeChangeListener supports chaining, it must forward
     * the onDataTreeChanged() call BEFORE event are submitted to its async executor for processing.
     *
     * @param listener the chained DataTreeChangeListener to invoke after this one
     */
    void addBeforeListener(DataTreeChangeListener<T> listener);

    /**
     * Adds a "chained" DataTreeChangeListener, to which
     * {@link DataTreeChangeListener#onDataTreeChanged(java.util.Collection)}
     * calls are forwarded AFTER having been processed by this DataTreeChangeListener.
     *
     * <p>If an asychronous DataTreeChangeListener supports chaining, it must forward
     * the onDataTreeChanged() call ONLY AFTER its async executor processed the event.
     *
     * @param listener the chained DataTreeChangeListener to invoke after this one
     */
    void addAfterListener(DataTreeChangeListener<T> listener);

}
