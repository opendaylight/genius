/*
 * Copyright (c) 2016, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.interfaces.testutils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.ComparisonFailure;
import org.mockito.Mockito;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fake IMdsalApiManager useful for tests.
 *
 * <p>Read e.g.
 * http://googletesting.blogspot.ch/2013/07/testing-on-toilet-know-your-test-doubles.html
 * and http://martinfowler.com/articles/mocksArentStubs.html for more background.
 *
 * <p>This class is abstract just to save reading lines and typing keystrokes to
 * manually implement a bunch of methods we're not yet interested in.  Create instances
 * of it using it's static {@link #newInstance()} method.
 *
 * @author Michael Vorburger
 * @author Faseela K
 */
public abstract class TestIMdsalApiManager implements IMdsalApiManager {

    private static final Logger LOG = LoggerFactory.getLogger(TestIMdsalApiManager.class);

    private Map<InternalFlowKey, FlowEntity> flows;
    private Map<InternalGroupKey, Group> groups;
    private Map<InternalBucketKey, Bucket> buckets;

    public static TestIMdsalApiManager newInstance() {
        TestIMdsalApiManager instance = Mockito.mock(TestIMdsalApiManager.class, realOrException());
        instance.flows = new HashMap<>();
        instance.groups = new HashMap<>();
        instance.buckets = new HashMap<>();
        return instance;
    }

    /**
     * Get list of installed flows.
     * Prefer the {@link #assertFlows(Iterable)} instead of using this and checking yourself.
     * @return immutable copy of list of flows
     */
    public List<FlowEntity> getFlows() {
        return retrieveFlows();
    }

    public void assertFlows(Iterable<FlowEntity> expectedFlows) {
        checkNonEmptyFlows(expectedFlows);
        Collection<FlowEntity> nonNullFlows = retrieveFlows();
        if (!Iterables.isEmpty(expectedFlows)) {
            assertTrue("No Flows created (bean wiring may be broken?)", !nonNullFlows.isEmpty());
        }
        // TODO Support Iterable <-> List directly within XtendBeanGenerator
        List<FlowEntity> expectedFlowsAsNewArrayList = Lists.newArrayList(expectedFlows);
        assertEqualBeans(expectedFlowsAsNewArrayList, new ArrayList<>(nonNullFlows));
    }


    private void checkNonEmptyFlows(Iterable<FlowEntity> expectedFlows) {
        if (!Iterables.isEmpty(expectedFlows)) {
            assertTrue("No Flows created (bean wiring may be broken?)", !retrieveFlows().isEmpty());
        }
    }

    // ComparisonException doesn’t allow us to keep the cause (which we don’t care about anyway)
    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    public void assertFlowsInAnyOrder(Iterable<FlowEntity> expectedFlows) {
        checkNonEmptyFlows(expectedFlows);
        // TODO Support Iterable <-> List directly within XtendBeanGenerator

        List<FlowEntity> sortedFlows = sortFlows(retrieveFlows());
        Map<InternalFlowKey, FlowEntity> keyedExpectedFlows = new HashMap<>();
        for (FlowEntity expectedFlow : expectedFlows) {
            keyedExpectedFlows.put(
                new InternalFlowKey(expectedFlow.getDpnId(), expectedFlow.getFlowId(), expectedFlow.getTableId()),
                expectedFlow);
        }
        List<FlowEntity> sortedExpectedFlows = sortFlows(keyedExpectedFlows.values());

        // FYI: This containsExactlyElementsIn() assumes that FlowEntity, and everything in it,
        // has correctly working equals() implementations.  assertEqualBeans() does not assume
        // that, and would work even without equals, because it only uses property reflection.
        // Normally this will lead to the same result, but if one day it doesn't (because of
        // a bug in an equals() implementation somewhere), then it's worth to keep this diff
        // in mind.

        // FTR: This use of G Truth and then catch AssertionError and using assertEqualBeans iff NOK
        // (thus discarding the message from G Truth) is a bit of a hack, but it works well...
        // If you're tempted to improve this, please remember that correctly re-implementing
        // containsExactlyElementsIn (or Hamcrest's similar containsInAnyOrder) isn't a 1 line
        // trivia... e.g. a.containsAll(b) && b.containsAll(a) isn't sufficient, because it
        // won't work for duplicates (which we frequently have here); and ordering before is
        // not viable because FlowEntity is not Comparable, and Comparator based on hashCode
        // is not a good idea (different instances can have same hashCode), and e.g. on
        // System#identityHashCode even less so.
        try {
            assertThat(sortedFlows).containsExactlyElementsIn(sortedExpectedFlows);
        } catch (AssertionError e) {
            // We LOG the AssertionError just for clarity why containsExactlyElementsIn() failed
            LOG.warn("assert containsExactlyElementsIn() failed", e);
            // We LOG the expected and actual flow in case of a failed assertion
            // because, even though that is typically just a HUGE String that's
            // hard to read (the diff printed subsequently by assertEqualBeans
            // is, much, more readable), there are cases when looking more closely
            // at the full toString() output of the flows is still useful, so:
            // TIP: Use e.g. 'wdiff -n expected.txt actual.txt | colordiff' to compare these!
            LOG.warn("assert failed [order ignored!]; expected flows ({}): {}", sortedExpectedFlows.size(),
                sortedExpectedFlows);
            LOG.warn("assert failed [order ignored!]; actual flows   ({}): {}", sortedFlows.size(), sortedFlows);
            for (int i = 0; i < sortedExpectedFlows.size() && i < sortedFlows.size(); i++) {
                if (!sortedExpectedFlows.get(i).equals(sortedFlows.get(i))) {
                    LOG.warn("First mismatch at index {};", i);
                    LOG.warn("               expected {}", sortedExpectedFlows.get(i));
                    LOG.warn("                    got {}", sortedFlows.get(i));
                    break;
                }
            }
            // The point of now also doing assertEqualBeans() is just that its output,
            // in case of a comparison failure, is *A LOT* more clearly readable
            // than what G Truth (or Hamcrest) can do based on toString.
            assertEqualBeans(sortedExpectedFlows, sortedFlows);
            if (sortedExpectedFlows.toString().equals(sortedFlows.toString())
                    && !sortedExpectedFlows.equals(sortedFlows)) {
                fail("Suspected toString, missing getter, equals (hashCode) bug in FlowEntity related class!!! :-(");
            }
            throw new ComparisonFailure(
                    "assertEqualBeans() MUST fail - given that the assertThat.containsExactlyElementsIn() just failed!"
                    // Beware, we're using XtendBeanGenerator instead of XtendYangBeanGenerator like in
                    // AssertDataObjects, but for FlowEntity it's the same... it only makes a difference for DataObjects
                    + " What is missing in: " + new XtendBeanGenerator().getExpression(sortedFlows),
                    sortedExpectedFlows.toString(), sortedFlows.toString());
            // If this ^^^ occurs, then there is probably a bug in ch.vorburger.xtendbeans
        }
    }

    private List<FlowEntity> sortFlows(Iterable<FlowEntity> flowsToSort) {
        List<FlowEntity> sortedFlows = Lists.newArrayList(flowsToSort);
        sortedFlows.sort((flow1, flow2) -> ComparisonChain.start()
                .compare(flow1.getTableId(), flow2.getTableId())
                .compare(flow1.getPriority(), flow2.getPriority())
                .compare(flow1.getFlowId(), flow2.getFlowId())
                .result());
        return sortedFlows;
    }

    private synchronized void storeFlow(FlowEntity flowEntity) {
        flows.put(new InternalFlowKey(flowEntity.getDpnId(), flowEntity.getFlowId(), flowEntity.getTableId()),
            flowEntity);
    }

    private synchronized List<FlowEntity> retrieveFlows() {
        return ImmutableList.copyOf(flows.values());
    }

    private synchronized void deleteFlow(Uint64 dpId, String flowId, short tableId) {
        flows.remove(new InternalFlowKey(dpId, flowId, tableId));
    }

    private synchronized void storeGroup(Uint64 dpnId, Group group) {
        groups.put(new InternalGroupKey(dpnId, group.key().getGroupId().getValue().toJava()), group);
    }

    private synchronized void deleteGroup(Uint64 dpnId, long groupId) {
        groups.remove(new InternalGroupKey(dpnId, groupId));
    }

    private synchronized void storeBucket(Uint64 dpnId, long groupId, Bucket bucket) {
        buckets.put(new InternalBucketKey(dpnId, groupId, bucket.getBucketId().getValue().toJava()), bucket);
    }

    private synchronized void deleteBucket(Uint64 dpnId, long groupId, long bucketId) {
        buckets.remove(new InternalBucketKey(dpnId, groupId, bucketId));
    }

    @Override
    public void addFlow(TypedWriteTransaction<Configuration> tx, FlowEntity flowEntity) {
        storeFlow(flowEntity);
    }

    @Override
    public void addFlow(TypedWriteTransaction<Configuration> tx, Uint64 dpId, Flow flow) {
        throw new UnsupportedOperationException("addFlow(..., BigInteger, Flow) isn't supported yet");
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, Flow flow) {
        removeFlow(tx, dpId, flow.key(), flow.getTableId().toJava());
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, FlowEntity flowEntity) {
        deleteFlow(flowEntity.getDpnId(), flowEntity.getFlowId(), flowEntity.getTableId());
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, FlowKey flowKey,
            short tableId) {
        deleteFlow(dpId, flowKey.getId().getValue(), tableId);
    }

    @Override
    public void removeFlow(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, String flowId,
            short tableId) {
        deleteFlow(dpId, flowId, tableId);
    }

    @Override
    public void addGroup(TypedWriteTransaction<Configuration> tx, GroupEntity groupEntity) {
        storeGroup(groupEntity.getDpnId(), groupEntity.getGroupBuilder().build());
    }

    @Override
    public void addGroup(TypedWriteTransaction<Configuration> tx, Uint64 dpId, Group group) {
        storeGroup(dpId, group);
    }

    @Override
    public void removeGroup(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, Group group) {
        deleteGroup(dpId, group.getGroupId().getValue().toJava());
    }

    @Override
    public void removeGroup(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId) {
        deleteGroup(dpId, groupId);
    }

    @Override
    public void addBucket(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId,
            Bucket bucket) {
        storeBucket(dpId, groupId, bucket);
    }

    @Override
    public void removeBucket(TypedReadWriteTransaction<Configuration> tx, Uint64 dpId, long groupId,
            long bucketId) {
        deleteBucket(dpId, groupId, bucketId);
    }

    public FluentFuture<Void> addCallBackForInstallFlowAndReturn(FlowEntity flowEntity) {
        storeFlow(flowEntity);
        return null;
    }

    public FluentFuture<Void> addCallBackForInstallFlowAndReturn(Uint64 dpId,
            FlowEntity flowEntity) {
        // TODO should dpId be considered here? how? Copy clone FlowEntity and change its dpId?
        return (FluentFuture<Void>) installFlow(flowEntity);
    }

    private static final class InternalFlowKey {
        private final Uint64 dpnId;
        private final String flowId;
        private final short tableId;

        private InternalFlowKey(Uint64 dpnId, String flowId, short tableId) {
            this.dpnId = dpnId;
            this.flowId = flowId;
            this.tableId = tableId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            InternalFlowKey that = (InternalFlowKey) obj;
            return tableId == that.tableId && Objects.equals(dpnId, that.dpnId) && Objects.equals(flowId, that.flowId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dpnId, flowId, tableId);
        }
    }

    private static final class InternalGroupKey {
        private final Uint64 dpnId;
        private final long groupId;

        private InternalGroupKey(Uint64 dpnId, long groupId) {
            this.dpnId = dpnId;
            this.groupId = groupId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            InternalGroupKey that = (InternalGroupKey) obj;
            return groupId == that.groupId && Objects.equals(dpnId, that.dpnId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dpnId, groupId);
        }
    }

    private static final class InternalBucketKey {
        private final Uint64 dpnId;
        private final long groupId;
        private final long bucketId;

        private InternalBucketKey(Uint64 dpnId, long groupId, long bucketId) {
            this.dpnId = dpnId;
            this.groupId = groupId;
            this.bucketId = bucketId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            InternalBucketKey that = (InternalBucketKey) obj;
            return groupId == that.groupId && bucketId == that.bucketId && Objects.equals(dpnId, that.dpnId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dpnId, groupId, bucketId);
        }
    }
}
