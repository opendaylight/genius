/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.common.wait.SimpleTaskRetryLooper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.PerformanceCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info.Switch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info.SwitchKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info._switch.SwitchPortsCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info._switch.SwitchPortsCountersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.performance.counters.SwitchCounters;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.Callable;

public class NodeConnectorInventoryImpl extends NodeConnectorEvent<FlowCapableNodeConnector> {
    public static final int STARTUP_LOOP_TICK = 500;
    public static final int STARTUP_LOOP_MAX_RETRIES = 8;
    private static final Logger LOG = LoggerFactory.getLogger(NodeConnectorInventoryImpl.class);

    private ListenerRegistration<NodeConnectorInventoryImpl> dataTreeChangeListenerRegistration;

    public static final String SEPARATOR = ":";
    private DataBroker dataBroker;

    private static final InstanceIdentifier<FlowCapableNodeConnector> II_TO_FLOW_CAPABLE_NODE_CONNECTOR
            = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class)
            .child(NodeConnector.class)
            .augmentation(FlowCapableNodeConnector.class)
            .build();

    public NodeConnectorInventoryImpl(final DataBroker dataBroker) {
        super( FlowCapableNodeConnector.class);
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");

        final DataTreeIdentifier<FlowCapableNodeConnector> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildCardPath());
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(STARTUP_LOOP_TICK,
                    STARTUP_LOOP_MAX_RETRIES);
            dataTreeChangeListenerRegistration = looper.loopUntilNoException(new Callable<ListenerRegistration<NodeConnectorInventoryImpl>>() {
                @Override
                public ListenerRegistration<NodeConnectorInventoryImpl> call() throws Exception {
                    return dataBroker.registerDataTreeChangeListener(treeId, NodeConnectorInventoryImpl.this);
                }
            });
        } catch (final Exception e) {
            LOG.warn(" NodeConnectorInventoryImpl listener registration fail!");
            LOG.debug("NodeConnectorInventoryImpl DataChange listener registration fail ..", e);
            throw new IllegalStateException("NodeConnectorInventoryImpl startup fail! System needs restart.", e);
        }
    }


    protected InstanceIdentifier<FlowCapableNodeConnector> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class)
                .child(Node.class)
                .child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class);
    }

    @Override
    public void close() {
        if (dataTreeChangeListenerRegistration != null) {
            try {
                dataTreeChangeListenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FRM NodeConnectorInventoryImpl: {}", e.getMessage());
                LOG.debug("Error by stop FRM NodeConnectorInventoryImpl..", e);
            }
            dataTreeChangeListenerRegistration = null;
        }
    }
    @Override
    public void remove(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector del, InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if(compareInstanceIdentifierTail(identifier,II_TO_FLOW_CAPABLE_NODE_CONNECTOR)) {
            String sNodeConnectorIdentifier = getNodeConnectorId(String.valueOf(nodeConnIdent.firstKeyOf(NodeConnector.class).getId()));
            BigInteger switchId = getDpIdFromPortName(sNodeConnectorIdentifier);
            BigInteger portId = getPortIdFromPortName(sNodeConnectorIdentifier);

            if (getSwitchStatus(switchId)) {
                LOG.info("Portname {} with PortId {} removed for dpn {} ", sNodeConnectorIdentifier, portId, switchId);

                try {
                    InstanceIdentifier switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                            .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                    CounterUtil.batchDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                    LOG.info("Deleted port id : {} for switch with id : {} for pm counter", portId, switchId);
                } catch (Exception ex) {
                    LOG.error("Exception while deleting port {} from pm Ds", sNodeConnectorIdentifier);
                }
            }
        }
    }

    @Override
    public void update(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector original, FlowCapableNodeConnector update, InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if(compareInstanceIdentifierTail(identifier,II_TO_FLOW_CAPABLE_NODE_CONNECTOR)) {
            //donot need to do anything as we are not considering updates here
        }
    }

    @Override
    public void add(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector add, InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if (compareInstanceIdentifierTail(identifier,II_TO_FLOW_CAPABLE_NODE_CONNECTOR)){

            String sNodeConnectorIdentifier = getNodeConnectorId(String.valueOf(nodeConnIdent.firstKeyOf(NodeConnector.class).getId()));
            LOG.info("Node Connector {} added", sNodeConnectorIdentifier);

        }
    }
    private String getNodeConnectorId(String node) {
        //Uri [_value=openflow:1:1]
        String temp[] = node.split("=");
        String dpnId = temp[1].substring(0,temp[1].length() - 1);
        return dpnId;
    }

    private boolean compareInstanceIdentifierTail(InstanceIdentifier<?> identifier1,
                                                  InstanceIdentifier<?> identifier2) {
        return Iterables.getLast(identifier1.getPathArguments()).equals(Iterables.getLast(identifier2.getPathArguments()));
    }

    private BigInteger getDpIdFromPortName(String portName) {
        String dpId = portName.substring(portName.indexOf(SEPARATOR) + 1, portName.lastIndexOf(SEPARATOR));
        return new BigInteger(dpId);
    }

    private BigInteger getPortIdFromPortName(String sNodeConnectorIdentifier) {
        //openflow:1:1
        String ports[] = sNodeConnectorIdentifier.split(SEPARATOR);
        return new BigInteger(ports[2]);
    }

    public boolean getSwitchStatus(BigInteger switchId){
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