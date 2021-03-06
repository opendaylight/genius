/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class ActionableResource<T extends DataObject> {

    static final short CREATE = 1;
    static final short UPDATE = 2;
    static final short DELETE = 3;
    static final short READ = 4;
    // MDSAL-534 Merge,Put with no create_missing_parents flag
    static final short UPDATECONTAINER = 5;

    private final InstanceIdentifier<T> path;
    private final short action;

    // Hidden to prevent subclassing outside of this package
    ActionableResource(final InstanceIdentifier<T> path, final short action) {
        this.path = requireNonNull(path);
        this.action = action;
    }

    final short getAction() {
        return action;
    }

    final @NonNull InstanceIdentifier<T> getInstanceIdentifier() {
        return path;
    }

    abstract Object getInstance();

    abstract Object getOldInstance();

    abstract ListenableFuture<Void> getResultFuture();
}
