/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Service to register callbacks for individual data instance changes.
 *
 * <p>Each callback function, for a given IID, is invoked only 1 time.
 *
 * @author Michael Vorburger.ch
 */
public interface DataInstanceChangeListenerRegisterer {

    <T extends DataObject> void onAdd(DataTreeIdentifier<T> dataTreeIdentifier, Consumer<T> callback);

    <T extends DataObject> void onChange(DataTreeIdentifier<T> dataTreeIdentifier, BiConsumer<T, T> callback);

    <T extends DataObject> void onRemove(DataTreeIdentifier<T> dataTreeIdentifier, Runnable runnable);

}
