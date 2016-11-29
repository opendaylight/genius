/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.hwvtep;

import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.utils.hwvtep.HwvtepHACache;

public abstract class HwvtepAbstractDataTreeChangeListener<T extends DataObject,K extends DataTreeChangeListener>
        extends AsyncDataTreeChangeListenerBase<T,K> {

    public HwvtepAbstractDataTreeChangeListener(Class<T> clazz, Class<K> eventClazz) {
        super(clazz,eventClazz);
    }

    @Override
    protected void remove(InstanceIdentifier<T> identifier, T del) {
        if (HwvtepHACache.getInstance().isHAEnabledDevice(identifier)){
            return;
        }
        removed(identifier, del);
    }

    @Override
    protected void update(InstanceIdentifier<T> identifier, T original, T update) {
        if (HwvtepHACache.getInstance().isHAEnabledDevice(identifier)){
            return;
        }
        updated(identifier,original,update);
    }

    @Override
    protected void add(InstanceIdentifier<T> identifier, T add) {
        if (HwvtepHACache.getInstance().isHAEnabledDevice(identifier)){
            return;
        }
        added(identifier,add);
    }

    protected abstract void removed(InstanceIdentifier<T> identifier, T del);

    protected abstract void updated(InstanceIdentifier<T> identifier, T original, T update);

    protected abstract void added(InstanceIdentifier<T> identifier, T add);


}
