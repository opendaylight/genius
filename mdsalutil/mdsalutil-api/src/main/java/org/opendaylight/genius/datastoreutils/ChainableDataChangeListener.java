/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import java.util.EventListener;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;

/**
 * {@link DataChangeListener} which can notify another DataChangeListener.
 *
 * @deprecated Migrate your listeners from {@link DataChangeListener} to
 *             {@link DataTreeChangeListener}, and use the
 *             {@link ChainableDataTreeChangeListener}.
 *
 * @author Michael Vorburger, based on discussions with Stephen Kitt
 */
@Deprecated
public interface ChainableDataChangeListener extends EventListener {

    /**
     * See {@link ChainableDataTreeChangeListener#addAfterListener(DataTreeChangeListener)}.
     */
    void addAfterListener(DataChangeListener listener);

}
