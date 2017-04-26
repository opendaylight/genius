/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.infra;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * {@link CompletionStage} including methods for probing completion status or
 * results, or awaiting completion of a stage.
 *
 * @author Michael Vorburger.ch
 */
public interface CompletionStage2<T> extends CompletionStage<T> {

    // TODO co-variant return types all methods return CompletionStage2 instead of CompletionStage ...

    /**
     * Returns the result value when complete, or throws an (unchecked)
     * exception if completed exceptionally. To better conform with the use of
     * common functional forms, if a computation involved in the completion of
     * this CompletableFuture threw an exception, this method throws an
     * (unchecked) {@link CompletionException} with the underlying exception as
     * its cause.
     *
     * @return the result value
     * @throws CancellationException
     *             if the computation was cancelled
     * @throws CompletionException
     *             if this future completed exceptionally or a completion
     *             computation threw an exception
     */
    T get() throws CompletionException, CancellationException;

    /**
     * Returns {@code true} if completed in any fashion: normally,
     * exceptionally, or via cancellation.
     *
     * @return {@code true} if completed
     */
    boolean isDone();

    /**
     * Returns {@code true} if this CompletableFuture completed exceptionally,
     * in any way. Possible causes include cancellation, explicit invocation of
     * {@code
     * completeExceptionally}, and abrupt termination of a CompletionStage
     * action.
     *
     * @return {@code true} if this CompletableFuture completed exceptionally
     */
    boolean isCompletedExceptionally();

    /**
     * Returns {@code true} if this CompletableFuture was cancelled before it
     * completed normally.
     *
     * @return {@code true} if this CompletableFuture was cancelled before it
     *         completed normally
     */
    boolean isCancelled();

}
