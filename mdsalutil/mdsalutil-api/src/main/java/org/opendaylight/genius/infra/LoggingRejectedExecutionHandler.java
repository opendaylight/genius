/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RejectedExecutionHandler} which logs
 * instead of throwing an exception (like e.g. {@link ThreadPoolExecutor}'s default
 * {@link AbortPolicy} does) or just completely silently ignores the execute
 * (like e.g. {@link DiscardPolicy} does).
 *
 * <p>This logs an ERROR level message (because typically that's a real problem),
 * unless the {@link ExecutorService#isShutdown()} - then it logs only an INFO level message
 * (because typically that's "just" a shutdown ordering issue, and not a real problem).
 *
 * @author Michael Vorburger.ch
 */
public class LoggingRejectedExecutionHandler implements RejectedExecutionHandler {

    // TODO This utility could eventually be moved to org.opendaylight.infrautils.utils.concurrent

    private static final Logger LOG = LoggerFactory.getLogger(LoggingRejectedExecutionHandler.class);

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown() || executor.isTerminating() || executor.isTerminated()) {
            LOG.info("rejectedExecution, but OK as ExecutorService is terminating or shutdown; "
                    + "executor: {}, runnable: {}", executor, runnable);
        } else {
            LOG.error("rejectedExecution (BUT ExecutorService is NOT terminating or shutdown, so that's a PROBLEM); "
                    + "executor: {}, runnable: {}", executor, runnable);
        }
    }

}
