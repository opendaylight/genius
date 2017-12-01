/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;

/**
 * Implementation of {@link ManagedNewTransactionRunner} with automatic transparent retries.
 *
 * <h3>Details about the threading model used by this class</h3>
 *
 * <p>This class runs the first attempt to call the delegated {@link ManagedNewTransactionRunner},
 * which typically is a {@link ManagedNewTransactionRunnerImpl} which safely invokes {@link WriteTransaction#submit()},
 * in the using application's thread (like a {@link MoreExecutors#directExecutor()} would, if this were an
 * {@link Executor}, which it's not).
 *
 * <p>Any retry attempts required, if that <code>submit()</code> (eventually) fails with an
 * {@link OptimisticLockFailedException}, are run in the calling thread of that eventual future completion by a
 * {@link MoreExecutors#directExecutor()} implicit in the constructor which does not require you to specify an
 * explicit Executor argument.  Normally that will be an internal thread from the respective DataBroker implementation,
 * not your application's thread anymore, because that meanwhile could well be off doing something else!  Normally,
 * that is not a problem, because retries "should" be relatively uncommon, and (re)issuing some DataBroker
 * <code>put()</code> or <code>delete()</code> and <code>re-submit()</code> <i>should</i> be fast.
 *
 * <p>If this default is not suitable (e.g. for particularly slow try/retry code), then you can specify
 * another {@link Executor} to be used for the retries by using the alternative constructor.
 *
 * @author Michael Vorburger.ch
 */
@Beta
// Do *NOT* mark this as @Singleton, because users choose Impl; and as long as this in API, because of https://wiki.opendaylight.org/view/BestPractices/DI_Guidelines#Nota_Bene
public class RetryingManagedNewTransactionRunner extends RetryingManagedNewTransactionRunnerImpl {

    @Inject
    public RetryingManagedNewTransactionRunner(DataBroker dataBroker) {
        super(new ManagedNewTransactionRunnerImpl(dataBroker));
    }

    public RetryingManagedNewTransactionRunner(DataBroker dataBroker, Executor executor) {
        super(new ManagedNewTransactionRunnerImpl(dataBroker), executor);
    }
}
