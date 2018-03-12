/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BigIntCacheKey<T extends DataObject> extends CacheKeyBase<T> {

    private final BigInteger key;

    public BigIntCacheKey(Class<T> dataObjType, InstanceIdentifier<T> instanceIdentifier, BigInteger key) {
        super(dataObjType, instanceIdentifier);
        this.key = key;
    }

    public BigIntCacheKey(Class<T> dataObjType, InstanceIdentifier<T> instanceIdentifier) {
        super(dataObjType, instanceIdentifier);
        this.key = null;
    }

    public BigInteger getKey() {
        return key;
    }
}
