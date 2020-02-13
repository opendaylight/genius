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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockGroupForwarder extends AbstractMockForwardingRulesManager<Group> {
    private static final Logger LOG = LoggerFactory.getLogger(MockGroupForwarder.class);

    private final AtomicInteger groupCount = new AtomicInteger(0);
    private ListenerRegistration<MockGroupForwarder> listenerRegistration;

    public MockGroupForwarder(final DataBroker db) {
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Group> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, MockGroupForwarder.this);
    }

    private InstanceIdentifier<Group> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class)
                .child(Group.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Group>> changes) {
        for (DataTreeModification<Group> change : changes) {
            final InstanceIdentifier<Group> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Group> mod = change.getRootNode();

            switch (mod.getModificationType()) {
                case DELETE:
                    groupCount.decrementAndGet();
                    break;
                case SUBTREE_MODIFIED:
                    // CHECK IF RQD
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        groupCount.incrementAndGet();
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
        Awaitility.await("MockGroupForwarder").atMost(5, TimeUnit.SECONDS).pollDelay(0, MILLISECONDS)
            .conditionEvaluationListener(condition -> LOG.info(
                "awaitDataChangeCount: Elapsed time {}s, remaining time {}s; flowCount: {}",
                    condition.getElapsedTimeInMS() / 1000, condition.getRemainingTimeInMS() / 1000,
                    condition.getValue()))
            .untilAtomic(groupCount, Matchers.equalTo(expCount));
    }
}
