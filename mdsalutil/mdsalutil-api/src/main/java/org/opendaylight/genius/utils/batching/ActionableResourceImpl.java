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

class ActionableResourceImpl implements ActionableResource {
    private final Object instance;
    private final Object oldInstance;
    private final Object key;
    private final InstanceIdentifier identifier;
    private final short action;
    private final SettableFuture future = SettableFuture.create();

    ActionableResourceImpl(InstanceIdentifier identifier, short action, Object updatedData, Object oldData) {
        this.key = null;
        this.action = action;
        this.identifier = requireNonNull(identifier);
        this.instance = updatedData;
        this.oldInstance = oldData;
    }

    ActionableResourceImpl(Identifier key, InstanceIdentifier identifier, short action, Object updatedData,
            Object oldData) {
        this.key = requireNonNull(key);
        this.action = action;
        this.identifier = requireNonNull(identifier);
        this.instance = updatedData;
        this.oldInstance = oldData;
    }

    @Override
    public Object getInstance() {
        return this.instance;
    }

    @Override
    public Object getOldInstance() {
        return this.oldInstance;
    }

    @Override
    public InstanceIdentifier getInstanceIdentifier() {
        return this.identifier;
    }

    @Override
    public short getAction() {
        return action;
    }

    @Override
    public ListenableFuture<Void> getResultFuture() {
        return future;
    }

    @Override
    public String toString() {
        return key != null ? key.toString() : identifier.toString();
    }
}
