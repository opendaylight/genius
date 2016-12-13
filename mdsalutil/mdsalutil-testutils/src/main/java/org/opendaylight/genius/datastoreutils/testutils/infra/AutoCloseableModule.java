/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.infra;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.InjectorCloseListener;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module which {@link AutoCloseable#close()}'s all bound {@link AutoCloseable}.
 *
 * @author Michael Vorburger.ch
 */
public class AutoCloseableModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(AutoCloseableModule.class);

    // Implementation inspired by com.mycila.guice.ext.jsr250.Jsr250Module

    // This does not yet respect dependency ordering when closing.
    // If that is ever required, have a closer look at the Jsr250Module

    @Override
    protected void configure() {
        requireBinding(CloseableInjector.class);
        bind(InjectorCloseListener.class).to(Closer.class);
    }

    @Singleton
    private static class Closer implements InjectorCloseListener {

        final Injector injector;

        @Inject
        Closer(Injector injector) {
            this.injector = injector;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void onInjectorClosing() {
            for (Key<?> key : injector.getAllBindings().keySet()) {
                if (AutoCloseable.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
                    AutoCloseable autoCloseable = (AutoCloseable) injector.getInstance(key);
                    try {
                        autoCloseable.close();
                        LOG.info("close() {}", autoCloseable);
                    } catch (Exception e) {
                        throw new AutoCloseableRuntimeException("Failed to close() " + autoCloseable.getClass(), e);
                    }
                }
            }
        }
    }

}
