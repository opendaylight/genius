/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CacheKeyBase<T extends DataObject> {

    protected final InstanceIdentifier<T> instanceIdentifier;

    public CacheKeyBase(Class<T> dataObjType, InstanceIdentifier<T> instanceIdentifier) {
        this.instanceIdentifier = instanceIdentifier;
    }

    protected InstanceIdentifier<T> getInstanceIdentifier() {
        return instanceIdentifier;
    }
}
