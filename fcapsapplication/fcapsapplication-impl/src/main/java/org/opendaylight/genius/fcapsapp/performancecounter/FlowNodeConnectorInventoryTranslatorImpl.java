/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.HashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.fcapsapp.portinfo.PortNameMapping;
import org.opendaylight.openflowplugin.common.wait.SimpleTaskRetryLooper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

@Singleton
public class FlowNodeConnectorInventoryTranslatorImpl extends NodeConnectorEventListener<FlowCapableNodeConnector>  {
    public static final int STARTUP_LOOP_TICK = 500;
    public static final int STARTUP_LOOP_MAX_RETRIES = 8;
    private static final Logger LOG = LoggerFactory.getLogger(FlowNodeConnectorInventoryTranslatorImpl.class);
    private final EntityOwnershipService entityOwnershipService;
    private DataBroker dataBroker;
    private ListenerRegistration<FlowNodeConnectorInventoryTranslatorImpl> dataTreeChangeListenerRegistration;

    public static final String SEPARATOR = ":";
    private static final PMAgent pmAgent = new PMAgent();

    private static final InstanceIdentifier<FlowCapableNodeConnector> II_TO_FLOW_CAPABLE_NODE_CONNECTOR
            = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class)
            .child(NodeConnector.class)
            .augmentation(FlowCapableNodeConnector.class)
            .build();

    private static Multimap<Long,String> dpnToPortMultiMap = Multimaps.synchronizedListMultimap(ArrayListMultimap.<Long,String>create());

    private static HashMap<String, String> nodeConnectorCountermap = new HashMap<>();

    public FlowNodeConnectorInventoryTranslatorImpl(final DataBroker dataBroker,final EntityOwnershipService entityOwnershipService) {
        super( FlowCapableNodeConnector.class);
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
        this.entityOwnershipService = entityOwnershipService;
        final DataTreeIdentifier<FlowCapableNodeConnector> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildCardPath());
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(STARTUP_LOOP_TICK,
                    STARTUP_LOOP_MAX_RETRIES);
            dataTreeChangeListenerRegistration = looper.loopUntilNoException(
                    () -> dataBroker.registerDataTreeChangeListener(treeId, FlowNodeConnectorInventoryTranslatorImpl.this));
        } catch (final Exception e) {
            LOG.warn(" FlowNodeConnectorInventoryTranslatorImpl listener registration fail!");
            LOG.debug("FlowNodeConnectorInventoryTranslatorImpl DataChange listener registration fail ..", e);
            throw new IllegalStateException("FlowNodeConnectorInventoryTranslatorImpl startup fail! System needs restart.", e);
        }
    }


    protected InstanceIdentifier<FlowCapableNodeConnector> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class)
                .child(Node.class)
                .child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class);
    }

    @PreDestroy
    @Override
    public void close() {
        if (dataTreeChangeListenerRegistration != null) {
            try {
                dataTreeChangeListenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FRM FlowNodeConnectorInventoryTranslatorImpl: {}", e.getMessage());
                LOG.debug("Error by stop FRM FlowNodeConnectorInventoryTranslatorImpl..", e);
            }
            dataTreeChangeListenerRegistration = null;
        }
    }
    @Override
    public void remove(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector del, InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if(compareInstanceIdentifierTail(identifier,II_TO_FLOW_CAPABLE_NODE_CONNECTOR)) {
            String sNodeConnectorIdentifier = getNodeConnectorId(String.valueOf(nodeConnIdent.firstKeyOf(NodeConnector.class).getId()));
            long nDpId = getDpIdFromPortName(sNodeConnectorIdentifier);
            if (dpnToPortMultiMap.containsKey(nDpId)) {
                LOG.debug("Node Connector {} removal request", sNodeConnectorIdentifier);
                if(getSwitchStatus(nDpId)) {
                    dpnToPortMultiMap.remove(nDpId, sNodeConnectorIdentifier);
                    sendNodeConnectorUpdation(nDpId);
                    LOG.debug("Node Connector {} removed", sNodeConnectorIdentifier);
                    PortNameMapping.updatePortMap("openflow:" + nDpId + ":" + del.getName(), sNodeConnectorIdentifier, "DELETE");
                }
            }
        }
    }

    @Override
    public void update(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector original, FlowCapableNodeConnector update, InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if(compareInstanceIdentifierTail(identifier,II_TO_FLOW_CAPABLE_NODE_CONNECTOR)) {

            //donot need to do anything as we are not considering updates here
            String sNodeConnectorIdentifier = getNodeConnectorId(String.valueOf(nodeConnIdent.firstKeyOf(NodeConnector.class).getId()));
            long nDpId = getDpIdFromPortName(sNodeConnectorIdentifier);
            if (isNodeOwner(getNodeId(nDpId))) {
                boolean original_portstatus = original.getConfiguration().isPORTDOWN();
                boolean update_portstatus = update.getConfiguration().isPORTDOWN();

                if (update_portstatus) {
                    //port has gone down
                    LOG.debug("Node Connector {} updated port is down", sNodeConnectorIdentifier);
                } else if (original_portstatus) {
                    //port has come up
                    LOG.debug("Node Connector {} updated port is up", sNodeConnectorIdentifier);
                }
            }
        }
    }

    @Override
    public void add(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector add, InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if (compareInstanceIdentifierTail(identifier,II_TO_FLOW_CAPABLE_NODE_CONNECTOR)){

            String sNodeConnectorIdentifier = getNodeConnectorId(String.valueOf(nodeConnIdent.firstKeyOf(NodeConnector.class).getId()));
            long nDpId = getDpIdFromPortName(sNodeConnectorIdentifier);
            if (isNodeOwner(getNodeId(nDpId))) {
                if (!dpnToPortMultiMap.containsEntry(nDpId, sNodeConnectorIdentifier)) {
                    LOG.debug("Node Connector {} added", sNodeConnectorIdentifier);
                    dpnToPortMultiMap.put(nDpId, sNodeConnectorIdentifier);
                    sendNodeConnectorUpdation(nDpId);
                    PortNameMapping.updatePortMap("openflow:" + nDpId + ":" + add.getName(), sNodeConnectorIdentifier, "ADD");
                } else {
                    LOG.debug("Node Connector {} already added for dpn {}",sNodeConnectorIdentifier,nDpId);
                }
            }
        }
    }
    private String getNodeConnectorId(String node) {
        //Uri [_value=openflow:1:1]
        String temp[] = node.split("=");
        String dpnId = temp[1].substring(0,temp[1].length() - 1);
        return dpnId;
    }

    private String getNodeId(Long dpnId){
        return "openflow:" + dpnId;
    }
    /**
     * Method checks if *this* instance of controller is owner of
     * the given openflow node.
     * @param nodeId openflow node Id
     * @return True if owner, else false
     */
    public boolean isNodeOwner(String nodeId) {
        Entity entity = new Entity("openflow", nodeId);
        return this.entityOwnershipService.getOwnershipState(entity).transform(EntityOwnershipState::isOwner).or(false);
    }

    private boolean compareInstanceIdentifierTail(InstanceIdentifier<?> identifier1,
                                                  InstanceIdentifier<?> identifier2) {
        return Iterables.getLast(identifier1.getPathArguments()).equals(Iterables.getLast(identifier2.getPathArguments()));
    }

    private long getDpIdFromPortName(String portName) {
        String dpId = portName.substring(portName.indexOf(SEPARATOR) + 1, portName.lastIndexOf(SEPARATOR));
        return Long.parseLong(dpId);
    }

    private void sendNodeConnectorUpdation(Long dpnId) {
        Collection<String> portname = dpnToPortMultiMap.get(dpnId);
        String nodeListPortsCountStr,counterkey;
        nodeListPortsCountStr = "dpnId_" + dpnId + "_NumberOfOFPorts";
        counterkey = "NumberOfOFPorts:" + nodeListPortsCountStr;

        if (portname.size()!=0) {
            nodeConnectorCountermap.put(counterkey, "" + portname.size());
        } else {
            nodeConnectorCountermap.remove(counterkey);
        }
        LOG.debug("NumberOfOFPorts:" + nodeListPortsCountStr + " portlistsize " + portname.size());
        pmAgent.connectToPMAgentForNOOfPorts(nodeConnectorCountermap);
    }

    public boolean getSwitchStatus(long switchId){
        NodeId nodeId = new NodeId("openflow:" + switchId);
        LOG.debug("Querying switch with dpnId {} is up/down", nodeId);
        InstanceIdentifier<Node> nodeInstanceId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).build();
        Optional<Node> nodeOptional = read(dataBroker,LogicalDatastoreType.OPERATIONAL,nodeInstanceId);
        if (nodeOptional.isPresent()) {
            LOG.debug("Switch {} is up", nodeId);
            return true;
        }
        LOG.debug("Switch {} is down", nodeId);
        return false;
    }

    public static <T extends DataObject> Optional<T> read(DataBroker broker, LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path) {

        ReadOnlyTransaction tx = broker.newReadOnlyTransaction();

        Optional<T> result = Optional.absent();
        try {
            result = tx.read(datastoreType, path).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
