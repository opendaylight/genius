/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.hwvtep;

import java.util.concurrent.ExecutorService;
import org.opendaylight.genius.utils.hwvtep.HwvtepNodeHACache;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.serviceutils.tools.listener.AbstractAsyncDataTreeChangeListener;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class HwvtepAbstractDataTreeChangeListener<T extends DataObject,K extends DataTreeChangeListener<T>>
        extends AbstractAsyncDataTreeChangeListener<T> {

    private final HwvtepNodeHACache hwvtepNodeHACache;

    public HwvtepAbstractDataTreeChangeListener(DataBroker dataBroker,DataTreeIdentifier dataTreeIdentifier,
                                                ExecutorService executorService, HwvtepNodeHACache hwvtepNodeHACache) {
        super(dataBroker, dataTreeIdentifier, executorService);
        this.hwvtepNodeHACache = hwvtepNodeHACache;
    }

    @Override
    public void remove(InstanceIdentifier<T> identifier, T del) {
        if (hwvtepNodeHACache.isHAEnabledDevice(identifier)) {
            return;
        }
        removed(identifier, del);
    }

    @Override
    public void update(InstanceIdentifier<T> identifier, T original, T update) {
        if (hwvtepNodeHACache.isHAEnabledDevice(identifier)) {
            return;
        }
        updated(identifier,original,update);
    }

    @Override
    public void add(InstanceIdentifier<T> identifier, T add) {
        if (hwvtepNodeHACache.isHAEnabledDevice(identifier)) {
            return;
        }
        added(identifier,add);
    }

    protected abstract void removed(InstanceIdentifier<T> identifier, T del);

    protected abstract void updated(InstanceIdentifier<T> identifier, T original, T update);

    protected abstract void added(InstanceIdentifier<T> identifier, T add);


}
