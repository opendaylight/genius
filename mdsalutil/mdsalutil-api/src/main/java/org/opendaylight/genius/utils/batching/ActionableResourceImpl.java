/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ActionableResourceImpl implements ActionableResource {
    private Object instance;
    private Object oldInstance;
    private String key;
    private InstanceIdentifier identifier;
    private short action;

    public ActionableResourceImpl(String key) {
        this.key = key;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Object getInstance() {
        return this.instance;
    }

    public void setOldInstance(Object oldInstance) {
        this.oldInstance = oldInstance;
    }

    public Object getOldInstance() {
        return this.oldInstance;
    }

    public void setInstanceIdentifier(InstanceIdentifier identifier) {
        this.identifier = identifier;
    }

    public InstanceIdentifier getInstanceIdentifier() {
        return this.identifier;
    }

    public void setAction(short action) {
        this.action = action;
    }

    public short getAction(){
        return action;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
