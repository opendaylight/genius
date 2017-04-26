/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.internal;

import com.google.common.util.concurrent.CheckedFuture;
import com.spotify.futures.CompletableFuturesExtra;
import java.util.concurrent.CompletableFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.databrokerutils.AsyncReadWriteTransaction;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AsyncReadWriteTransaction}.
 *
 * @author Michael Vorburger.ch
 */
public class AsyncReadWriteTransactionImpl extends AsyncReadTransactionImpl implements AsyncReadWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncReadWriteTransactionImpl.class);

    @SuppressWarnings("hiding")
    protected final ReadWriteTransaction tx;

    public AsyncReadWriteTransactionImpl(ReadWriteTransaction tx) {
        super(tx);
        this.tx = tx;
    }

    @Override
    public CompletionStage2<Void> submit() {
        CheckedFuture<Void, TransactionCommitFailedException> guavaFuture = tx.submit();
        CompletableFuture<Void> java8Future = CompletableFuturesExtra.toCompletableFuture(guavaFuture);
        return new CompletionStage2Impl<>(java8Future.exceptionally(exception -> {
            // TODO LOG with original caller call trace how-to?
            LOG.error("submit() failed", exception);
            return null;
        }));
    }


    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        tx.put(store, path, data);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        tx.put(store, path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        tx.merge(store, path, data);
    }

    @Override
    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        tx.merge(store, path, data, createMissingParents);
    }

    @Override
    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        tx.delete(store, path);
    }

    @Override
    public boolean cancel() {
        return tx.cancel();
    }

}
