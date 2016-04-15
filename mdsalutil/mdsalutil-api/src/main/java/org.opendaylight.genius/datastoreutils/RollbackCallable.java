/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.datastoreutils;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Callable;

public abstract class RollbackCallable implements Callable<List<ListenableFuture<Void>>> {

    private List<ListenableFuture<Void>> futures;

    public RollbackCallable() {
    }

    public List<ListenableFuture<Void>> getFutures() {
        return futures;
    }

    public void setFutures(List<ListenableFuture<Void>> futures) {
        this.futures = futures;
    }
}
