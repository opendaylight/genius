package org.opendaylight.genius.mdsalutil.tests;


import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.utils.hwvtep.HwvtepSouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.util.ArrayList;
import java.util.List;

import static org.opendaylight.genius.mdsalutil.hwvtep.HwvtepHACacheTest.newNodeInstanceIdentifier;

public class DataTreeListenerImpl extends AsyncDataTreeChangeListenerBase<Node, DataTreeListenerImpl>  {

    public DataTreeListenerImpl(Class<Node> clazz, Class<DataTreeListenerImpl> eventClazz) {
        super(clazz, eventClazz);
    }

    public void setDeferAddEvent(boolean deferAddEvent) {
        this.deferAddEvent = deferAddEvent;
    }

    public void setDeferUpdateEvent(boolean deferUpdateEvent) {
        this.deferUpdateEvent = deferUpdateEvent;
    }

    public void setDeferRemoveEvent(boolean deferRemoveEvent) {
        this.deferRemoveEvent = deferRemoveEvent;
    }

    private boolean deferAddEvent = true;
    private boolean deferUpdateEvent = true;
    private boolean deferRemoveEvent = true;


    public void setWaitTimeAddEvent(long waitTimeAddEvent) {
        this.waitTimeAddEvent = waitTimeAddEvent;
    }

    public void setWaitTimeUpdateEvent(long waitTimeUpdateEvent) {
        this.waitTimeUpdateEvent = waitTimeUpdateEvent;
    }

    public void setWaitTimeRemoveEvent(long waitTimeRemoveEvent) {
        this.waitTimeRemoveEvent = waitTimeRemoveEvent;
    }

    private long waitTimeAddEvent = 5L;
    private long waitTimeUpdateEvent = 5L;
    private long waitTimeRemoveEvent = 5L;

    public long dequeuedAddEventTimeStamp = 0L;
    public long dequeuedUpdateEventTimeStamp = 0L;
    public long dequeuedRemoveEventTimeStamp = 0L;

    @Override
    protected InstanceIdentifier<Node> getWildCardPath() {
        return null;
    }

    @Override
    protected void remove(InstanceIdentifier<Node> key, Node dataObjectModification) {

    }

    @Override
    protected void update(InstanceIdentifier<Node> key, Node dataObjectModificationBefore, Node dataObjectModificationAfter) {

    }

    @Override
    protected void add(InstanceIdentifier<Node> key, Node dataObjectModification) {

    }

    @Override
    protected DataTreeListenerImpl getDataTreeChangeListener() {
        return null;
    }
/*


    public DataTreeListenerImpl(Class clazz, Class eventClazz) {
        super(clazz, eventClazz);
    }

    public DataTreeListenerImpl() {
        super(Node.class, DataTreeListenerImpl.class);
    }

    @Override
    protected InstanceIdentifier getWildCardPath() {
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID));
    }

    @Override
    protected void remove(InstanceIdentifier<Node> key, Node dataObjectModification) {
        if(deferRemoveEvent){
            List<InstanceIdentifier<Node>> iidList = new ArrayList<InstanceIdentifier<Node>>();
            iidList.add(newNodeInstanceIdentifier("remove1"));

            List<DependencyData> dependentIidList = new ArrayList<DependencyData>();
            dependentIidList.add(new DependencyData(newNodeInstanceIdentifier("remove1"), true, LogicalDatastoreType.CONFIGURATION));

            if (isEventToBeProcessed(key, null, null, EventType.REMOVE, waitTimeRemoveEvent, CONFIGURATION_DS_RETRY_COUNT)) {
                //process Remove event here.
            } else {

            }
        } else {
            LOG.error("Remove event called without Queuing");
            dequeuedRemoveEventTimeStamp = (System.currentTimeMillis()/1000);
        }
    }

    @Override
    protected void update(InstanceIdentifier<Node> key, Node oldData, Node newData) {
        if(deferUpdateEvent){

            // assuming depenency needs to be resolved
            List<DependencyData> dependentIidList = new ArrayList<DependencyData>();
            dependentIidList.add(new DependencyData(newNodeInstanceIdentifier("update1"), true, LogicalDatastoreType.CONFIGURATION));


            if (isEventToBeProcessed(key, oldData, newData, EventType.UPDATE, waitTimeUpdateEvent,
                    CONFIGURATION_DS_RETRY_COUNT)) {
                //process Update event here
            } else {
                LOG.error("Event Update either Queued/Suppressed, key:{}, eventType:{}", key,"UPDATE");
            }
            deferUpdateEvent = false;
        } else {
            LOG.error("Update event called without Queuing");
            dequeuedUpdateEventTimeStamp = (System.currentTimeMillis()/1000);
        }
    }

    @Override
    protected void add(InstanceIdentifier<Node> key, Node dataObjectModification) {
        if(deferAddEvent){
            // assuming depenency needs to be resolved

            List<DependencyData> dependentIidList = new ArrayList<DependencyData>();
            dependentIidList.add(new DependencyData(newNodeInstanceIdentifier("add1"), false, LogicalDatastoreType.CONFIGURATION));

            if (isEventToBeProcessed(key, null, dataObjectModification, EventType.ADD, waitTimeAddEvent,
                    CONFIGURATION_DS_RETRY_COUNT)) {
                //process ADD event here

            } else {
                // populate dependent IID.
                WriteTransaction wrTx = broker.newWriteOnlyTransaction();
                for (DependencyData dependentIID : dependentIidList) {
                    NodeId node1ID = new NodeId(dependentIID.getIid().toString());
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey  node1IDKey=
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey(node1ID);
                    Node node1 = new NodeBuilder().setNodeId(node1ID).setKey(node1IDKey).build();
//                    wrTx.put(dependentIID.getDsType(), node1ID, node1);
                    wrTx.put(dependentIID.getDsType(), newNodeInstanceIdentifier(dependentIID.getIid().toString()), node1);
                }
                wrTx.submit();
            }
            deferAddEvent = false;
        } else {
            LOG.error("ADD event called without Queuing");
            dequeuedAddEventTimeStamp = (System.currentTimeMillis()/1000);
        }
    }

    @Override
    protected DataTreeListenerImpl getDataTreeChangeListener() {
        return this;
    }
*/

}
