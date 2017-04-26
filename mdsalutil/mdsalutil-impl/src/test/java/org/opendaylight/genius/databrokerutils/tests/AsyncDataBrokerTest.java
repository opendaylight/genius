/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.databrokerutils.AsyncDataBroker;
import org.opendaylight.genius.databrokerutils.internal.AsyncDataBrokerImpl;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link AsyncDataBroker} &amp; Co.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("deprecation")
public class AsyncDataBrokerTest extends AbstractConcurrentDataBrokerTest {

    // TODO write tests for AsyncReadWriteTransaction

    @Test
    public void testReadPresent() throws TransactionCommitFailedException {
        Nodes nodes = new NodesBuilder().setNode(Collections.emptyList()).build();
        InstanceIdentifier<Nodes> nodesII = InstanceIdentifier.create(Nodes.class);
        new SingleTransactionDataBroker(getDataBroker()).syncWrite(OPERATIONAL, nodesII, nodes);

        final AtomicBoolean isRead = new AtomicBoolean();

        AsyncDataBroker adb = new AsyncDataBrokerImpl(getDataBroker());
        adb.withNewSingleAsyncReadOnlyTransaction(tx -> {
            return tx.read(OPERATIONAL, nodesII, readNodes ->
                isRead.set(true)); })
            .get();

        assertThat(isRead.get()).isTrue();
    }

    @Test
    public void testReadAbsent() throws TransactionCommitFailedException {
        InstanceIdentifier<Nodes> nodesII = InstanceIdentifier.create(Nodes.class);

        final AtomicBoolean isRead = new AtomicBoolean();
        final AtomicBoolean isElse = new AtomicBoolean();

        AsyncDataBroker adb = new AsyncDataBrokerImpl(getDataBroker());
        adb.withNewSingleAsyncReadOnlyTransaction(tx -> {
            return tx.read(OPERATIONAL, nodesII, readNodes ->
                isRead.set(true))
            .orElse(() ->
                isElse.set(true)); })
            .get();

        assertThat(isRead.get()).isFalse();
        assertThat(isElse.get()).isTrue();
    }

}
