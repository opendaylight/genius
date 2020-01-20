/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.testutils.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.genius.datastoreutils.testutils.TestableDataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Unit test for the {@link TestableDataBroker}.
 *
 * @author Michael Vorburger.ch
 */
public class TestableDataBrokerTest {

    @Test
    public void testListener() {
        AtomicBoolean hasChanged = new AtomicBoolean(false);
        TestableDataBroker tdb = TestableDataBroker.newInstance();
        tdb.registerDataTreeChangeListener(null, (DataTreeChangeListener<DataObject>) changes -> hasChanged.set(true));
        assertThat(hasChanged.get()).isFalse();
        tdb.fireDataTreeChangeListener();
        assertThat(hasChanged.get()).isTrue();
    }

    @Test
    public void testListenerRegistration() {
        AtomicBoolean hasChanged = new AtomicBoolean(false);
        TestableDataBroker tdb = TestableDataBroker.newInstance();
        tdb.registerDataTreeChangeListener(null, (DataTreeChangeListener<DataObject>) changes -> hasChanged.set(true))
                .close();
        tdb.fireDataTreeChangeListener();
        assertThat(hasChanged.get()).isFalse();
        tdb.registerDataTreeChangeListener(null, (DataTreeChangeListener<DataObject>) changes -> hasChanged.set(true));
        tdb.fireDataTreeChangeListener();
        assertThat(hasChanged.get()).isTrue();
    }

    @Test
    public void testSingleListenerRegistration() {
        TestableDataBroker tdb = TestableDataBroker.newInstance();
        tdb.registerDataTreeChangeListener(null, changes -> { });
        assertThrows(IllegalStateException.class, () -> tdb.registerDataTreeChangeListener(null, changes -> { }));
    }
}
