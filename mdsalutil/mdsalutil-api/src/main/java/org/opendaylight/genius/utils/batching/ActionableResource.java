/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ActionableResource {

    short CREATE = 1;
    short UPDATE = 2;
    short DELETE = 3;
    short READ = 4;

    InstanceIdentifier<?> getInstanceIdentifier();

    Object getInstance();

    Object getOldInstance();

    short getAction();

    String getKey();

    ListenableFuture<Void> getResultFuture();
}
