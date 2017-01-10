/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.infra;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FuturesTest {

    private final ListeningExecutorService executorService
        = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    @Test(expected = AssertionError.class)
    public void a_get() throws InterruptedException, ExecutionException {
        ListenableFuture<String> future = executorService.submit(() -> {
            Thread.sleep(500);
            return "hello, world";
        });
        Assert.assertEquals("Hello World", future.get());
    }

    @Test(expected = ExecutionException.class) // TODO you'd want IllegalStateException here!
    // TODO should be able to remove both throws again when using a CheckedFuture?
    public void a_getWithFailedFuture() throws InterruptedException, ExecutionException {
        ListenableFuture<String> future = executorService.submit(() -> {
            Thread.sleep(500);
            throw new IllegalStateException();
        });
        Assert.assertEquals("Hello World", future.get());
    }

    @Test(expected = IllegalStateException.class)
    public void a_getWithFailedCheckedFuture() throws Exception {
        // TODO Is there an ExecutorService which directly returns CheckedFuture on submit() ?
        CheckedFuture<String, Exception> future = Futures.makeChecked(executorService.submit(() -> {
            Thread.sleep(500);
            throw new IllegalStateException();
        }), MAPPER);
        Assert.assertEquals("Hello World", future.checkedGet());
    }

    private static final Function<Exception, Exception> MAPPER = new ExceptionMappingFunction();

    private static class ExceptionMappingFunction implements Function<Exception, Exception> {
        @Override
        public Exception apply(Exception exception) {
            if (exception instanceof ExecutionException) {
                Throwable cause = exception.getCause();
                if (cause instanceof Exception) {
                    return (Exception) cause;
                } else {
                    return exception; // orig. ExecutionException
                }
            } else {
                return exception;
            }
        }
    }

    // NB: Use of Futures's transform() is *WRONG* - it's JavaDoc says:
    // "transform will schedule the function.apply to be run by the thread that completes the input Future"
    // ... this means that if the Future is set in another Thread executorService below, this may not work.

    // TODO Use CheckedFuture instead so that it fails with an AssertionError instead of an ExecutionException
    @Test(expected = ExecutionException.class) // TODO you'd want AssertionError here!
    // TODO should be able to remove both throws again when using a CheckedFuture?
    public void b_transform() throws InterruptedException, ExecutionException {
        ListenableFuture<String> future = executorService.submit(() -> {
            Thread.sleep(500);
            return "hello, world";
        });
        // This syntax is a big heavy - transform(), return null, get() ..
        Futures.transform(future, (AsyncFunction<String, Void>) input -> {
            Assert.assertEquals("Hello World", input);
            return null;
        // we *MUST* get() here - if we forget, the Assert is ineffective:
        }).get();
    }

    @Test(expected = ExecutionException.class) // TODO you'd want IllegalStateException here!
    public void b_transformWithFailedFuture() throws InterruptedException, ExecutionException {
        ListenableFuture<String> future = executorService.submit(() -> {
            Thread.sleep(500);
            throw new IllegalStateException();
        });
        // This syntax is a big heavy - transform(), return null, get() ..
        Futures.transform(future, (AsyncFunction<String, Void>) input -> {
            Assert.assertEquals("Hello World", input);
            return null;
        // we *MUST* get() here:
        }).get();
    }

    // TODO use ExecutorService as above instead of SettableFuture below

    @Test(expected = AssertionError.class)
    public void c_addListener() throws InterruptedException, ExecutionException {
        SettableFuture<String> future = SettableFuture.create();
        future.set("hello, world");
        // TODO This syntax is super ugly.. have a MoreFutures kinda helper?
        future.addListener(() -> {
            try {
                Assert.assertEquals("Hello World", future.get());
            } catch (InterruptedException | ExecutionException e) {
                // TODO This does not / cannot work?
                Throwables.propagate(e);
            }
        }, MoreExecutors.directExecutor());
    }

    @Ignore // This cannot work..
    @Test(expected = IllegalStateException.class)
    public void c_addListenerWithFailedFuture() throws InterruptedException, ExecutionException {
        SettableFuture<String> future = SettableFuture.create();
        future.setException(new IllegalStateException());
        future.addListener(() -> {
            try {
                Assert.assertEquals("Hello World", future.get());
            } catch (InterruptedException | ExecutionException e) {
                // TODO This does not / cannot work?
                Throwables.propagate(e);
            }
        }, MoreExecutors.directExecutor());
    }

    @Test(expected = AssertionError.class)
    public void d_addCallback() throws InterruptedException, ExecutionException {
        SettableFuture<String> future = SettableFuture.create();
        future.set("hello, world");
        Futures.addCallback(future, new FutureCallback<String>() {

            @Override
            public void onSuccess(String result) {
                Assert.assertEquals("Hello World", result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // TODO What would we do here?? (Have an abstract FutureCallback with it.)
            }
        });
    }

    @Ignore // This cannot work..
    @Test(expected = IllegalStateException.class)
    public void d_addCallbackWithFailedFuture() throws InterruptedException, ExecutionException {
        SettableFuture<String> future = SettableFuture.create();
        future.setException(new IllegalStateException());
        Futures.addCallback(future, new FutureCallback<String>() {

            @Override
            public void onSuccess(String result) {
                Assert.assertEquals("Hello World", result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // TODO This does not / cannot work?
                Throwables.propagate(throwable);
            }
        });
    }

}
