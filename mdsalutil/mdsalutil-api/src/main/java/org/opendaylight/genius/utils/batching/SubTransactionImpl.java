/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubTransactionImpl implements SubTransaction {
    private Object instance;
    private InstanceIdentifier identifier;
    private short action;

    public SubTransactionImpl() { }

    @Override
    public void setInstance(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object getInstance() {
        return this.instance;
    }

    @Override
    public void setInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        this.identifier = instanceIdentifier;
    }

    @Override
    public InstanceIdentifier getInstanceIdentifier() {
        return this.identifier;
    }

    @Override
    public void setAction(short action) {
        this.action = action;
    }

    @Override
    public short getAction() {
        return action;
    }
}
