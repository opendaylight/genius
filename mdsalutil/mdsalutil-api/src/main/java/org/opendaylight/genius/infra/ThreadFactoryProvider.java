/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import org.immutables.value.Value;
import org.slf4j.Logger;

/**
 * Builder for {@link ThreadFactory}. Easier to use than the
 * {@link com.google.common.util.concurrent.ThreadFactoryBuilder}, because it
 * enforces settings properties.
 *
 * @author Michael Vorburger.ch
 */
@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract class ThreadFactoryProvider {

    public static ImmutableThreadFactoryProvider.NamePrefixBuildStage builder() {
        return ImmutableThreadFactoryProvider.builder();
    }

    /**
     * Prefix for threads from this factory. For example, "rpc-pool". Note that
     * this is a prefix, not a format, so you pass just "rpc-pool" instead of
     * e.g. "rpc-pool-%d".
     */
    @Value.Parameter public abstract String namePrefix();

    @Value.Parameter public abstract Logger logger();

    @Value.Default public UncaughtExceptionHandler uncaughtExceptionHandler() {
        return LoggingThreadUncaughtExceptionHandler.toLOG(logger());
    }

    @Value.Parameter public abstract Optional<Integer> priority();

    public ThreadFactory get() {
        ThreadFactoryBuilder guavaBuilder = new ThreadFactoryBuilder();
        guavaBuilder.setNameFormat(namePrefix() + "-%d");
        guavaBuilder.setUncaughtExceptionHandler(uncaughtExceptionHandler());
        guavaBuilder.setDaemon(true);
        priority().ifPresent(priority -> guavaBuilder.setPriority(priority));
        logger().info("ThreadFactory created: " + namePrefix());
        return guavaBuilder.build();
    }
}
