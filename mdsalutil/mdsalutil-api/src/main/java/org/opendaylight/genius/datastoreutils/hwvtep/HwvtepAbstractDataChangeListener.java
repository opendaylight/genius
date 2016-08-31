/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.hwvtep;

import org.opendaylight.genius.mdsalutil.AbstractDataChangeListener;
import org.opendaylight.genius.utils.hwvtep.HwvtepHACache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public abstract class HwvtepAbstractDataChangeListener<T extends DataObject>  extends AbstractDataChangeListener<T> {
    public HwvtepAbstractDataChangeListener(Class clazz) {
        super(clazz);
    }

    @Override
    protected void remove(InstanceIdentifier<T> identifier, T del) {
        String nodeIdVal = identifier.firstKeyOf(Node.class).getNodeId().getValue();
        if (HwvtepHACache.isHAEnabledDevice(nodeIdVal)){
            return;
        }
        removed(identifier, del);
    }

    @Override
    protected void update(InstanceIdentifier<T> identifier, T original, T update) {
        String nodeIdVal = identifier.firstKeyOf(Node.class).getNodeId().getValue();
        if (HwvtepHACache.isHAEnabledDevice(nodeIdVal)){
            return;
        }
        updated(identifier,original,update);
    }

    @Override
    protected void add(InstanceIdentifier<T> identifier, T add) {
        String nodeIdVal = identifier.firstKeyOf(Node.class).getNodeId().getValue();
        if (HwvtepHACache.isHAEnabledDevice(nodeIdVal)){
            return;
        }
        added(identifier,add);
    }

    protected abstract void removed(InstanceIdentifier<T> identifier, T del);

    protected abstract void updated(InstanceIdentifier<T> identifier, T original, T update);

    protected abstract void added(InstanceIdentifier<T> identifier, T add);


}
