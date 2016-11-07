/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * DataChangeListener useful for testing in asynchronous scenarios.
 *
 * @deprecated Migrate your listeners from {@link DataChangeListener} to
 *             {@link DataTreeChangeListener}, and use the {@link TestableDataTreeChangeListener}.
 *
 * @author Michael Vorburger
 */
@Deprecated
public class TestableDataChangeListener
        extends AbstractTestableListener
        implements DataChangeListener {

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        consumedEvents();
    }

}
