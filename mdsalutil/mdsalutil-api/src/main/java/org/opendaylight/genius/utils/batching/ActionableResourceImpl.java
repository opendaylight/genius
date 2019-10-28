/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class ActionableResourceImpl extends ActionableResource {
    private final SettableFuture future = SettableFuture.create();
    private final Object instance;
    private final Object oldInstance;
    private final Object key;

    ActionableResourceImpl(InstanceIdentifier<?> path, short action, Object updatedData, Object oldData) {
        super(path, action);
        this.key = null;
        this.instance = updatedData;
        this.oldInstance = oldData;
    }

    ActionableResourceImpl(Identifier key, InstanceIdentifier<?> path, short action, Object updatedData,
            Object oldData) {
        super(path, action);
        this.key = requireNonNull(key);
        this.instance = updatedData;
        this.oldInstance = oldData;
    }

    @Override
    final Object getInstance() {
        return this.instance;
    }

    @Override
    final Object getOldInstance() {
        return this.oldInstance;
    }

    @Override
    final ListenableFuture<Void> getResultFuture() {
        return future;
    }

    @Override
    public final String toString() {
        return key != null ? key.toString() : getInstanceIdentifier().toString();
    }
}
