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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.databrokerutils.AsyncReadTransaction;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AsyncReadTransaction}.
 *
 * @author Michael Vorburger.ch
 */
public class AsyncReadTransactionImpl implements AsyncReadTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncReadTransactionImpl.class);

    private final ReadOnlyTransaction tx;

    public AsyncReadTransactionImpl(ReadOnlyTransaction tx) {
        this.tx = tx;
    }

    @Override
    public <T extends DataObject> Elser<Optional<T>> read(LogicalDatastoreType store,
            InstanceIdentifier<T> path, Consumer<T> reader) {

        CheckedFuture<com.google.common.base.Optional<T>, ReadFailedException> guavaFuture = tx.read(store, path);

        // https://github.com/krka/java8-future-guide
        // https://github.com/spotify/futures-extra

        CompletableFuture<com.google.common.base.Optional<T>> java8guavaOptionalFuture
            = CompletableFuturesExtra.toCompletableFuture(guavaFuture);

        CompletableFuture<Optional<T>> java8Future = java8guavaOptionalFuture.thenApply(o -> Optionals.toJavaUtil(o));

        // TODO run accept() on an Executor using whenCompleteAsync ?!
        java8Future.whenComplete((optional, exception) -> {
            optional.ifPresent(dataObject -> reader.accept(dataObject));
            if (exception != null) {
                // TODO LOG with call trace how-to?
                LOG.error("", exception);
            }
        });

        return new ElserImpl<>(java8Future);
    }

    @Override
    public void close() {
        tx.close();
    }

    private static class ElserImpl<T> extends CompletionStage2Impl<T> implements Elser<T> {

        ElserImpl(CompletableFuture<T> completableFuture) {
            super(completableFuture);
        }

        @Override
        @SuppressWarnings("unchecked")
        public CompletionStage2<T> orElse(Runnable elser) {
            return new CompletionStage2Impl<>(this.completableFuture.whenComplete((optional, exception) -> {
                final Optional<T> reallyOptional = (Optional<T>) optional;
                if (!reallyOptional.isPresent()) {
                    elser.run();
                }
            }));
        }

    }

}
