/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import com.google.inject.AbstractModule;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinatorMonitor;
import org.opendaylight.infrautils.jobcoordinator.internal.JobCoordinatorImpl;

/**
 * Guice Binding for using {@link JobCoordinator} in components tests (with
 * support for legacy deprecated {@link DataStoreJobCoordinator}.
 *
 * @author Michael Vorburger.ch
 */
public class JobCoordinatorTestModule extends AbstractModule {

    // The GuiceRule needs to include
    // CloseableModule.class, Jsr250Module (but not the AutoCloseableModule)
    // directly or via the org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module
    // for this to work.

    @Override
    protected void configure() {
        JobCoordinatorImpl jobCoordinatorImpl = new JobCoordinatorImpl();
        bind(JobCoordinator.class).toInstance(jobCoordinatorImpl);
        bind(JobCoordinatorMonitor.class).toInstance(jobCoordinatorImpl);
    }
}
