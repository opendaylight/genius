/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.fcapsapp.FcapsConstants;
import org.opendaylight.genius.fcapsapp.FcapsUtil;
import org.opendaylight.genius.fcapsapp.portinfo.PortNameMapping;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.openflowplugin.common.wait.SimpleTaskRetryLooper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowNodeConnectorInventoryTranslatorImpl extends NodeConnectorEventListener<FlowCapableNodeConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(FlowNodeConnectorInventoryTranslatorImpl.class);

    private static final int STARTUP_LOOP_TICK = 500;
    private static final int STARTUP_LOOP_MAX_RETRIES = 8;

    private final EntityOwnershipUtils entityOwnershipUtils;
    private final DataBroker dataBroker;
    private ListenerRegistration<FlowNodeConnectorInventoryTranslatorImpl> dataTreeChangeListenerRegistration;

    private static final String SEPARATOR = ":";
    private Map<String,Counter> counterMap = new ConcurrentHashMap<>();
    private final MetricProvider metricProvider;
    private static final InstanceIdentifier<FlowCapableNodeConnector>
            II_TO_FLOW_CAPABLE_NODE_CONNECTOR = InstanceIdentifier
            .builder(Nodes.class).child(Node.class).child(NodeConnector.class)
            .augmentation(FlowCapableNodeConnector.class).build();

    private static Multimap<Long, String> dpnToPortMultiMap = Multimaps
            .synchronizedListMultimap(ArrayListMultimap.<Long, String>create());


    @Inject
    @SuppressWarnings("checkstyle:IllegalCatch")
    public FlowNodeConnectorInventoryTranslatorImpl(final DataBroker dataBroker,
                                                    final EntityOwnershipUtils entityOwnershipUtils,
                                                    MetricProvider metricProvider) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.metricProvider = metricProvider;
        final DataTreeIdentifier<FlowCapableNodeConnector> treeId = new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, getWildCardPath());
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(STARTUP_LOOP_TICK, STARTUP_LOOP_MAX_RETRIES);
            dataTreeChangeListenerRegistration = looper.loopUntilNoException(() -> dataBroker
                    .registerDataTreeChangeListener(treeId, FlowNodeConnectorInventoryTranslatorImpl.this));
        } catch (Exception e) {
            LOG.warn(" FlowNodeConnectorInventoryTranslatorImpl listener registration fail!");
            LOG.debug("FlowNodeConnectorInventoryTranslatorImpl DataChange listener registration fail ..", e);
            throw new IllegalStateException(
                    "FlowNodeConnectorInventoryTranslatorImpl startup fail! System needs restart.", e);
        }
    }

    protected InstanceIdentifier<FlowCapableNodeConnector> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class);
    }

    @PreDestroy
    @Override
    public void close() {
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
            dataTreeChangeListenerRegistration = null;
        }
    }

    @Override
    public void remove(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector del,
            InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if (compareInstanceIdentifierTail(identifier, II_TO_FLOW_CAPABLE_NODE_CONNECTOR)) {
            String nodeConnectorIdentifier = getNodeConnectorId(
                    String.valueOf(nodeConnIdent.firstKeyOf(NodeConnector.class).getId()));
            long dataPathId = getDpIdFromPortName(nodeConnectorIdentifier);
            if (dpnToPortMultiMap.containsKey(dataPathId)) {
                LOG.debug("Node Connector {} removed", nodeConnectorIdentifier);
                dpnToPortMultiMap.remove(dataPathId, nodeConnectorIdentifier);
                String counterKey = FcapsUtil.getCounterName(String.valueOf(dataPathId),
                        FcapsConstants.COUNTERPORTPERSWITCH);
                counterMap.get(counterKey).decrement();
                PortNameMapping.updatePortMap("openflow:" + dataPathId + ":" + del.getName(), nodeConnectorIdentifier,
                        "DELETE");
            }
        }
    }

    @Override
    public void update(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector original,
            FlowCapableNodeConnector update, InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        // Don't need to do anything as we are not considering updates here
    }

    @Override
    public void add(InstanceIdentifier<FlowCapableNodeConnector> identifier, FlowCapableNodeConnector add,
            InstanceIdentifier<FlowCapableNodeConnector> nodeConnIdent) {
        if (compareInstanceIdentifierTail(identifier, II_TO_FLOW_CAPABLE_NODE_CONNECTOR)) {

            String nodeConnectorIdentifier = getNodeConnectorId(
                    String.valueOf(nodeConnIdent.firstKeyOf(NodeConnector.class).getId()));
            long dataPathId = getDpIdFromPortName(nodeConnectorIdentifier);
            if (entityOwnershipUtils.isEntityOwner(FcapsConstants.SERVICE_ENTITY_TYPE,getNodeId(dataPathId))) {
                if (!dpnToPortMultiMap.containsEntry(dataPathId, nodeConnectorIdentifier)) {
                    LOG.debug("Node Connector {} added", nodeConnectorIdentifier);
                    dpnToPortMultiMap.put(dataPathId, nodeConnectorIdentifier);
                    String counterKey = FcapsUtil.getCounterName(String.valueOf(dataPathId),
                            FcapsConstants.COUNTERPORTPERSWITCH);
                    counterMap.computeIfAbsent(counterKey,
                            (counter -> metricProvider.newCounter(this, counterKey))).increment();
                    PortNameMapping.updatePortMap("openflow:" + dataPathId + ":" + add.getName(),
                            nodeConnectorIdentifier, "ADD");
                } else {
                    LOG.error("Duplicate Event.Node Connector already added");
                }
            }
        }
    }

    private String getNodeConnectorId(String node) {
        // Uri [_value=openflow:1:1]
        String[] temp = node.split("=");
        return temp[1].substring(0, temp[1].length() - 1);
    }

    private String getNodeId(Long dpnId) {
        return "openflow:" + dpnId;
    }

    private boolean compareInstanceIdentifierTail(InstanceIdentifier<?> identifier1,
            InstanceIdentifier<?> identifier2) {
        return Iterables.getLast(identifier1.getPathArguments())
                .equals(Iterables.getLast(identifier2.getPathArguments()));
    }

    private long getDpIdFromPortName(String portName) {
        String dpId = portName.substring(portName.indexOf(SEPARATOR) + 1, portName.lastIndexOf(SEPARATOR));
        return Long.parseLong(dpId);
    }

    public void nodeRemovedNotification(String node) {
        String[] switchId = node.split(":");
        if (counterMap.containsKey(FcapsUtil.getCounterName(switchId[1],FcapsConstants.COUNTERPORTPERSWITCH))) {
            counterMap.get(FcapsUtil.getCounterName(switchId[1],FcapsConstants.COUNTERPORTPERSWITCH)).close();
            counterMap.remove(FcapsUtil.getCounterName(switchId[1],FcapsConstants.COUNTERPORTPERSWITCH));
        }
    }
}
