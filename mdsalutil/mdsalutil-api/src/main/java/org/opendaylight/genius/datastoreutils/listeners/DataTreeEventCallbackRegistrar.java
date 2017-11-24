/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Service to register callbacks for data tree changes for a fixed instance.
 *
 * @author Michael Vorburger.ch, based on an idea and with design feedback from Josh
 */
public interface DataTreeEventCallbackRegistrar {

    enum UnregisterOrCallAgain { unregister, call_again }

    <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, UnregisterOrCallAgain> callback);

    <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, UnregisterOrCallAgain> callback);

    <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, UnregisterOrCallAgain> callback);

}
