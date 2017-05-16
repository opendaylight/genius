/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ExecutorService which, if shutdown, refuses executing new tasks, but logs
 * a WARN level message instead of throwing an exception (like e.g.
 * {@link AbortPolicy} does) or just completely silently ignores the execute
 * (like e.g. {@link DiscardPolicy} does). Intentionally not an
 * {@link RejectedExecutionHandler}, to clearly separate rejections due to
 * shutdown from rejections due to other reasons (such as Queue is full and core
 * threads are busy).
 *
 * @author Michael Vorburger.ch, based on discussions with Periyasamy Palanisamy
 */
public class ShutdownLoggingExecutorService implements ExecutorService {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownLoggingExecutorService.class);

    private final ExecutorService delegate;

    public ShutdownLoggingExecutorService(ExecutorService delegate) {
        super();
        this.delegate = delegate;
    }

    protected boolean isShutdownWithLog() {
        if (this.isShutdown()) {
            LOG.warn("ExecutorService execute/submit/invokeAll ignored, because isShutdown");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void execute(Runnable command) {
        if (!isShutdownWithLog()) {
            delegate.execute(command);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (!isShutdownWithLog()) {
            return delegate.submit(task);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (!isShutdownWithLog()) {
            return delegate.submit(task, result);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        if (!isShutdownWithLog()) {
            return delegate.submit(task);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (!isShutdownWithLog()) {
            return delegate.invokeAny(tasks);
        } else {
            return null;
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (!isShutdownWithLog()) {
            return delegate.invokeAny(tasks, timeout, unit);
        } else {
            return null;
        }
    }

    protected <T> List<Future<T>> newListOfNullCompletedFutures(int size) {
        List<Future<T>> futures = new ArrayList<>(size);
        for (int i = 0; i < futures.size(); i++) {
            futures.set(i, CompletableFuture.completedFuture(null));
        }
        return futures;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (!isShutdownWithLog()) {
            return delegate.invokeAll(tasks);
        } else {
            return newListOfNullCompletedFutures(tasks.size());
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (!isShutdownWithLog()) {
            return delegate.invokeAll(tasks, timeout, unit);
        } else {
            return newListOfNullCompletedFutures(tasks.size());
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

}
