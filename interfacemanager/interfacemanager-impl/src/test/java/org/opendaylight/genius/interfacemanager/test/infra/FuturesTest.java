/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.infra;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FuturesTest {

    // TODO Test if this also works if the Future is set in another Thread, not already set; see Futures's JavaDoc..

    // TODO Use CheckedFuture instead so that it fails with an AssertionError instead of an ExecutionException
    @Test(expected = AssertionError.class)
    // TODO should be able to remove both throws again whe using a CheckedFuture?
    public void a_transform() throws InterruptedException, ExecutionException {
        SettableFuture<String> future = SettableFuture.create();
        future.set("hello, world");
        // This syntax is a big heavy - transform(), return null, get() ..
        Futures.transform(future, (AsyncFunction<String, Void>) input -> {
            Assert.assertEquals("Hello World", input);
            return null;
        // we *MUST* get() here - if we forget, the Assert is in-effective:
        }).get();
    }

    @Test(expected = IllegalStateException.class)
    public void a_transformWithFailedFuture() throws InterruptedException, ExecutionException {
        SettableFuture<String> future = SettableFuture.create();
        future.setException(new IllegalStateException());
        // This syntax is a big heavy - transform(), return null, get() ..
        Futures.transform(future, (AsyncFunction<String, Void>) input -> {
            Assert.assertEquals("Hello World", input);
            return null;
        // we *MUST* get() here:
        }).get();
    }

    @Test(expected = AssertionError.class)
    public void b_addListener() throws InterruptedException, ExecutionException {
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

    @Test(expected = IllegalStateException.class)
    public void b_addListenerWithFailedFuture() throws InterruptedException, ExecutionException {
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
    public void c_addCallback() throws InterruptedException, ExecutionException {
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

    @Test(expected = IllegalStateException.class)
    public void c_addCallbackWithFailedFuture() throws InterruptedException, ExecutionException {
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
