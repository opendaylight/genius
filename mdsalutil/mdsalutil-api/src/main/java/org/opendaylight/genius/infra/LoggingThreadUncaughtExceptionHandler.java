/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;

/**
 * Thread's UncaughtExceptionHandler which logs to slf4j.
 *
 * @author Michael Vorburger.ch
 */
public class LoggingThreadUncaughtExceptionHandler implements UncaughtExceptionHandler {

    public static UncaughtExceptionHandler toLOG(Logger logger) {
        return new LoggingThreadUncaughtExceptionHandler(logger);
    }

    private final Logger logger;

    private LoggingThreadUncaughtExceptionHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logger.error("Thread terminated due to uncaught exception: {}", thread.getName(), throwable);
    }

}
