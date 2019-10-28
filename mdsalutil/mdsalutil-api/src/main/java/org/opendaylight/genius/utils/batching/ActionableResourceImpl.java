/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ActionableResourceImpl implements ActionableResource {
    private Object instance;
    private Object oldInstance;
    private String key;
    private InstanceIdentifier identifier;
    private short action;
    private final SettableFuture future = SettableFuture.create();

    public ActionableResourceImpl(String key) {
        this.key = key;
    }

    public ActionableResourceImpl(String key, InstanceIdentifier identifier, short action, Object updatedData,
            Object oldData) {
        this.instance = updatedData;
        this.oldInstance = oldData;
        this.key = key;
        this.identifier = identifier;
        this.action = action;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object getInstance() {
        return this.instance;
    }

    public void setOldInstance(Object oldInstance) {
        this.oldInstance = oldInstance;
    }

    @Override
    public Object getOldInstance() {
        return this.oldInstance;
    }

    public void setInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        this.identifier = instanceIdentifier;
    }

    @Override
    public InstanceIdentifier getInstanceIdentifier() {
        return this.identifier;
    }

    public void setAction(short action) {
        this.action = action;
    }

    @Override
    public short getAction() {
        return action;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public ListenableFuture<Void> getResultFuture() {
        return future;
    }
}
