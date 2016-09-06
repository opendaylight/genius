/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface SubTransaction {
    static final short CREATE = 1;
    static final short UPDATE = 2;
    static final short DELETE = 3;

    InstanceIdentifier getInstanceIdentifier();
    void setInstanceIdentifier(InstanceIdentifier identifier);
    Object getInstance();
    void setInstance(Object instance);
    short getAction();
    void setAction(short action);
}
