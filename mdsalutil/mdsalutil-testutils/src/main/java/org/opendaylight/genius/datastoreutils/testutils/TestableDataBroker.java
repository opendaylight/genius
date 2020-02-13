/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import java.util.concurrent.ExecutorService;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.infrautils.utils.concurrent.LoggingFutures;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DataBroker} useful in tests of utilities.
 *
 * @author Michael Vorburger.ch
 */
public abstract class TestableDataBroker implements DataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(TestableDataBroker.class);

    public static TestableDataBroker newInstance() {
        TestableDataBroker testableDataBroker = Mockito.mock(TestableDataBroker.class, realOrException());
        testableDataBroker.executor = Executors.newSingleThreadExecutor(TestableDataBroker.class.getSimpleName(), LOG);
        return testableDataBroker;
    }

    private ExecutorService executor;

    private volatile DataTreeChangeListener<?> listener;

    @Override
    public synchronized <T extends DataObject, L extends DataTreeChangeListener<T>>
        ListenerRegistration<L> registerDataTreeChangeListener(DataTreeIdentifier<T> id, L newListener) {
        if (listener != null) {
            throw new IllegalStateException("TestableDataBroker only supports one listener registration");
        }
        this.listener = requireNonNull(newListener, "newListener");
        return new ListenerRegistration<L>() {

            @Override
            public L getInstance() {
                return newListener;
            }

            @Override
            public void close() {
                listener = null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public synchronized void fireDataTreeChangeListener() {
        if (listener != null) {
            listener.onDataTreeChanged(singletonList(Mockito.mock(DataTreeModification.class, realOrException())));
        }
    }

    /**
     * Run {@link #fireDataTreeChangeListener()} in an asynchronous background thread.
     * Exceptions thrown will be logged as errors; tests using this are therefore strongly encouraged
     * to use the org.opendaylight.infrautils.testutils.LogCaptureRule so that tests fail if there
     * were exceptions in the listener.
     */
    public void asyncFireDataTreeChangeListener() {
        LoggingFutures.addErrorLogging(executor.submit(() -> fireDataTreeChangeListener()),
                LOG, "fireDataTreeChangeListener() eventually failed");
    }

}
