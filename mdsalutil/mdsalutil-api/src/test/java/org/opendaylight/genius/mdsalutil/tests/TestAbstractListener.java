package org.opendaylight.genius.mdsalutil.tests;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opendaylight.genius.mdsalutil.hwvtep.HwvtepHACacheTest.newNodeInstanceIdentifier;

public class TestAbstractListener  extends AbstractDataBrokerTest {
/*
    DataTreeListenerImpl runTest = new DataTreeListenerImpl();

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TestAbstractListener.class);

    private class ListenerTestClass {
        public DataBroker dataBroker;
        InstanceIdentifier key = null;
        DataObject newData = null;
        DataObject oldData = null;
        Long eventEnqueuedTS = 0L;
        public Long eventDequeuedTS = 0L;
        AsyncDataTreeChangeListenerBase.DeferedEvent.EventType eventType;

        ListenerTestClass(InstanceIdentifier key, DataObject newData, DataObject oldData, Long enqueuedTS,
                          AsyncDataTreeChangeListenerBase.DeferedEvent.EventType eventType){
            this.dataBroker = getDataBroker();
            this.key = key;
            this.newData = newData;
            this.oldData = oldData;
            this.eventEnqueuedTS  = enqueuedTS;
            this.eventType = eventType;
            runTest.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
        }
    }
    */
    /*
    * ***JUNIT Test Cases:
         1 - no defer events, just return
         2 - call individual listeners with 1Sec delay, wait for call back and check timeStamp
            2.1: add
            2.2: update
            2.3: delete
         3 - call individual listeners with 5Sec delay, wait for call back and check timeStamp
            3.1: add
            3.2: update
            3.3: delete
         4 - multiple call back's (multiple add's, update, remove) independent triggers
            4.1: 2-keyValues of ADD call-back with 1sec and 5sec wait time. check the timestamps.
            4.2: 2-keyValues of UPDATE call-back with 1sec and 5sec wait time. check the timestamps.
            4.3: 2-keyValues of DELETE call-back with 1sec and 5sec wait time. check the timestamps.
            4.4: 6-keyValues, key1-add-1sec, key2-update-5sec, key3-add-5sec, key4-delete-5sec, key5-delete-1sec, key6-update-1sec: check timestamps
            4.5: 6-keyValues, key1-add-1sec, key2-update-5sec, **Sleep for 1 sec** key3-add-5sec, key4-delete-5sec, key5-delete-1sec, key6-update-1sec: check timestamps
            4.6: 6-keyValues, key1-add-1sec, key2-update-5sec, key3-add-5sec, key4-delete-5sec, **Sleep for 1 sec** key5-delete-1sec, key6-update-1sec: check timestamps

         5 - multiple call back's and check supressing takes place properly.
         6 - multiple call back's and verify the TimeStamps by storing and compare.
    */
/*
    @Test
    public void  basicListenerTest() {

        runTest.setDeferAddEvent(false);
        runTest.setDeferRemoveEvent(false);
        runTest.setDeferUpdateEvent(false);

        InstanceIdentifier<Node> eventAdd = newNodeInstanceIdentifier("add1");
        ListenerTestClass add = new ListenerTestClass(eventAdd, null, null, (System.currentTimeMillis()/1000),
                AsyncDataTreeChangeListenerBase.DeferedEvent.EventType.ADD);
        runTest.add(add.key, null);
        add.eventDequeuedTS = (System.currentTimeMillis()/1000);
        if ((add.eventDequeuedTS - add.eventEnqueuedTS) > 0) {
            LOG.error("basicListenerTest: Add Failure, execution took Sec: {}", (add.eventDequeuedTS - add.eventEnqueuedTS));
        } else {
            LOG.error("basicListenerTest: Add Success");
        }

        InstanceIdentifier<Node> eventRemove = newNodeInstanceIdentifier("remove1");
        ListenerTestClass remove = new ListenerTestClass(eventRemove, null, null, (System.currentTimeMillis()/1000),
                AsyncDataTreeChangeListenerBase.DeferedEvent.EventType.REMOVE);
        runTest.remove(remove.key, null);
        remove.eventDequeuedTS = (System.currentTimeMillis()/1000);
        if ((remove.eventDequeuedTS - remove.eventEnqueuedTS) > 0) {
            LOG.error("basicListenerTest: Remove Failure, execution took Sec: {}", (remove.eventDequeuedTS - remove.eventEnqueuedTS));
        } else {
            LOG.error("basicListenerTest: Remove Success");
        }

        InstanceIdentifier<Node> eventUpdate = newNodeInstanceIdentifier("update1");
        ListenerTestClass update = new ListenerTestClass(eventUpdate, null, null, (System.currentTimeMillis()/1000),
                AsyncDataTreeChangeListenerBase.DeferedEvent.EventType.REMOVE);
        runTest.update(update.key, null, null);
        update.eventDequeuedTS = (System.currentTimeMillis()/1000);
        if ((update.eventDequeuedTS - update.eventEnqueuedTS) > 0) {
            LOG.error("basicListenerTest: Update Failure, execution took Sec: {}", (update.eventDequeuedTS - update.eventEnqueuedTS));
        } else {
            LOG.error("basicListenerTest: Update Success");
        }
    }

    @Test
    public void  queueDelayOperationalListenerTest() throws InterruptedException {
        long waitTime = 1L;
        long deltaTime = 2L;

        runTest.setDeferAddEvent(true);
        runTest.setDeferRemoveEvent(true);
        runTest.setDeferUpdateEvent(true);
        runTest.setWaitTimeAddEvent(waitTime );
        runTest.setWaitTimeRemoveEvent(waitTime );
        runTest.setWaitTimeUpdateEvent(waitTime );

        DataBroker db = getDataBroker();
        WriteTransaction wrTx = db.newWriteOnlyTransaction();
        NodeId node1ID = new NodeId("a");
        Node oldNode = new NodeBuilder().setNodeId(node1ID).build();
        Node newNode = new NodeBuilder().setNodeId(node1ID).build();

        List<InstanceIdentifier> iidList = new ArrayList<InstanceIdentifier>();
        iidList.add(newNodeInstanceIdentifier("add1"));

        InstanceIdentifier<Node> eventAdd = newNodeInstanceIdentifier("add1");
        ListenerTestClass add = new ListenerTestClass(eventAdd, null, null, (System.currentTimeMillis()/1000),
                AsyncDataTreeChangeListenerBase.DeferedEvent.EventType.ADD);
        runTest.add(add.key, newNode);

        InstanceIdentifier<Node> eventUpdate = newNodeInstanceIdentifier("update1");
        ListenerTestClass update = new ListenerTestClass(eventUpdate, null, null, (System.currentTimeMillis()/1000),
                AsyncDataTreeChangeListenerBase.DeferedEvent.EventType.REMOVE);
        runTest.update(update.key, oldNode, newNode);

        InstanceIdentifier<Node> eventRemove = newNodeInstanceIdentifier("remove1");
        ListenerTestClass remove = new ListenerTestClass(eventRemove, null, null, (System.currentTimeMillis()/1000),
                AsyncDataTreeChangeListenerBase.DeferedEvent.EventType.REMOVE);
        runTest.remove(remove.key, newNode);

        Thread.sleep(5*waitTime*1000L);

        if (((runTest.dequeuedAddEventTimeStamp - add.eventEnqueuedTS) >= waitTime) &&
           ((runTest.dequeuedAddEventTimeStamp - add.eventEnqueuedTS) <= (waitTime+deltaTime))) {
            LOG.error("queueDelayOperationalListenerTest: Add Success, execution took Sec: {}",
                    (runTest.dequeuedAddEventTimeStamp - add.eventEnqueuedTS));
        } else {
            LOG.error("queueDelayOperationalListenerTest: Add Failure, execution took Sec: {}",
                    (runTest.dequeuedAddEventTimeStamp - add.eventEnqueuedTS));
        }

        if (((runTest.dequeuedRemoveEventTimeStamp - remove.eventEnqueuedTS) >= waitTime) &&
            ((runTest.dequeuedRemoveEventTimeStamp - remove.eventEnqueuedTS) <= waitTime+deltaTime)) {
            LOG.error("queueDelayOperationalListenerTest: Remove Success, execution took Sec: {}",
                    (runTest.dequeuedRemoveEventTimeStamp - remove.eventEnqueuedTS));
        } else {
            LOG.error("queueDelayOperationalListenerTest: Remove Failure, execution took Sec: {}",
                    (runTest.dequeuedRemoveEventTimeStamp - remove.eventEnqueuedTS));
        }

        if (((runTest.dequeuedUpdateEventTimeStamp - update.eventEnqueuedTS) >= waitTime) &&
            ((runTest.dequeuedUpdateEventTimeStamp - update.eventEnqueuedTS) <= waitTime+deltaTime)) {
            LOG.error("queueDelayOperationalListenerTest: Update Success, execution took Sec: {}",
                    (runTest.dequeuedUpdateEventTimeStamp - update.eventEnqueuedTS));
        } else {
            LOG.error("queueDelayOperationalListenerTest: Update Failure, execution took Sec: {}",
                    (runTest.dequeuedUpdateEventTimeStamp - update.eventEnqueuedTS));
        }
    }
    @Test
    public void executeTestSuite(){ basicListenerTest();}
    */
}
