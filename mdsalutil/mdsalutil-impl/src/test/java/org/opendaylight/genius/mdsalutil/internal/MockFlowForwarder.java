/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockFlowForwarder extends AbstractMockForwardingRulesManager<Flow> {
    private static final Logger LOG = LoggerFactory.getLogger(MockFlowForwarder.class);

    private final AtomicInteger flowCount = new AtomicInteger(0);

    private ListenerRegistration<MockFlowForwarder> listenerRegistration;

    public MockFlowForwarder(final DataBroker db) {
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Flow> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, MockFlowForwarder.this);
    }

    private InstanceIdentifier<Flow> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class)
                .child(Table.class).child(Flow.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Flow>> changes) {
        for (DataTreeModification<Flow> change : changes) {
            final InstanceIdentifier<Flow> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Flow> mod = change.getRootNode();

            switch (mod.getModificationType()) {
                case DELETE:
                    flowCount.decrementAndGet();
                    break;
                case SUBTREE_MODIFIED:
                    // CHECK IF RQD
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        flowCount.incrementAndGet();
                    } else {
                        // UPDATE COUNT UNCHANGED
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    public void awaitDataChangeCount(int expCount) {
        Awaitility.await("MockFlowForwarder").atMost(5, TimeUnit.SECONDS).pollDelay(0, MILLISECONDS)
            .conditionEvaluationListener(condition -> LOG.info(
                "awaitDataChangeCount: Elapsed time {}s, remaining time {}s; flowCount: {}",
                    condition.getElapsedTimeInMS() / 1000, condition.getRemainingTimeInMS() / 1000,
                    condition.getValue()))
            .untilAtomic(flowCount, Matchers.equalTo(expCount));
    }
}
