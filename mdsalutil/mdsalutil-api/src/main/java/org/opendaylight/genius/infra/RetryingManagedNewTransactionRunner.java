/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.annotations.Beta;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

/**
 * Implementation of {@link ManagedNewTransactionRunner} with automatic transparent retries.
 * This is just a convenience implementation which correctly combines the {@link ManagedNewTransactionRunnerImpl}
 * with the {@link RetryingManagedNewTransactionRunnerImpl} (which is typical, if you want retries).  If you don't
 * want retries, just use the {@link ManagedNewTransactionRunnerImpl} directly.  If you need to customize for
 * a special case, (only) then use the {@link RetryingManagedNewTransactionRunnerImpl} directly.
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

    public RetryingManagedNewTransactionRunner(DataBroker dataBroker, int maxRetries) {
        super(new ManagedNewTransactionRunnerImpl(dataBroker), maxRetries);
    }

}
