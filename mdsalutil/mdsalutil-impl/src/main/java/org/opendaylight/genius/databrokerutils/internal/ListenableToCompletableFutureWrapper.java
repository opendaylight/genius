/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CompletableFuture;

class ListenableToCompletableFutureWrapper<V> extends CompletableFuture<V> implements FutureCallback<V> {

    private final ListenableFuture<V> future;

    ListenableToCompletableFutureWrapper(final ListenableFuture<V> future) {
        this.future = checkNotNull(future, "future");
        Futures.addCallback(future, this);
    }

    public ListenableFuture<V> unwrap() {
        return future;
    }

    @Override
    public void onSuccess(final V result) {
        complete(result);
    }

    @Override
    public void onFailure(final Throwable throwable) {
        completeExceptionally(throwable);
    }
}
