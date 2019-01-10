/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.fcapsapp.alarm.AlarmAgent;
import org.opendaylight.genius.fcapsapp.performancecounter.FlowNodeConnectorInventoryTranslatorImpl;
import org.opendaylight.genius.fcapsapp.performancecounter.NodeUpdateCounter;
import org.opendaylight.genius.fcapsapp.performancecounter.PacketInCounterHandler;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NodeEventListener<D extends DataObject> implements ClusteredDataTreeChangeListener<D>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NodeEventListener.class);
    public final AlarmAgent alarmAgent;
    public final NodeUpdateCounter nodeUpdateCounter;
    public final PacketInCounterHandler packetInCounterHandler;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final EntityOwnershipService entityOwnershipService;
    private final FlowNodeConnectorInventoryTranslatorImpl nodeConnectorInventoryTranslator;

    @Inject
    public NodeEventListener(final AlarmAgent alarmAgent, final NodeUpdateCounter nodeUpdateCounter,
                             final PacketInCounterHandler packetInCounterHandler,
                             final EntityOwnershipUtils entityOwnershipUtils,
                             final EntityOwnershipService entityOwnershipService,
                             final FlowNodeConnectorInventoryTranslatorImpl nodeConnectorInventoryTranslator) {
        this.alarmAgent = alarmAgent;
        this.nodeUpdateCounter = nodeUpdateCounter;
        this.packetInCounterHandler = packetInCounterHandler;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.entityOwnershipService = entityOwnershipService;
        this.nodeConnectorInventoryTranslator = nodeConnectorInventoryTranslator;
    }

    @PostConstruct
    public void start() throws Exception {
        LOG.info("NodeEventListener started");
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        LOG.info("NodeEventListener closed");
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<D>> changes) {
        for (DataTreeModification<D> change : changes) {
            final InstanceIdentifier<D> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<D> mod = change.getRootNode();
            final InstanceIdentifier<FlowCapableNode> nodeConnIdent = key.firstIdentifierOf(FlowCapableNode.class);

            String hostName = System.getenv().get("HOSTNAME");
            if (hostName == null) {
                try {
                    hostName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    LOG.error("Retrieving hostName failed", e);
                }
            }
            String nodeId = getDpnId(String.valueOf(nodeConnIdent.firstKeyOf(Node.class).getId()));
            switch (mod.getModificationType()) {
                case DELETE:
                    LOG.debug("NodeRemoved {} notification is received on host {}", nodeId, hostName);
                    if (nodeUpdateCounter.isDpnConnectedLocal(nodeId)) {
                        alarmAgent.raiseControlPathAlarm(nodeId, hostName);
                        nodeUpdateCounter.nodeRemovedNotification(nodeId, hostName);
                        nodeConnectorInventoryTranslator.nodeRemovedNotification(nodeId);
                    }
                    packetInCounterHandler.nodeRemovedNotification(nodeId);
                    break;
                case SUBTREE_MODIFIED:
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        if (entityOwnershipUtils.isEntityOwner(FcapsConstants.SERVICE_ENTITY_TYPE,nodeId)) {
                            LOG.debug("NodeAdded {} notification is received on host {}", nodeId, hostName);
                            alarmAgent.clearControlPathAlarm(nodeId, hostName);
                            nodeUpdateCounter.nodeAddedNotification(nodeId, hostName);
                        } else {
                            LOG.debug("ADD: Node {} is not connected to host {}", nodeId, hostName);
                        }
                    }
                    break;
                default:
                    LOG.debug("Unhandled Modification type {}", mod.getModificationType());
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    @Nonnull
    private String getDpnId(@Nonnull String node) {
        // Uri [_value=openflow:1]
        String[] temp = node.split("=");
        return temp[1].substring(0, temp[1].length() - 1);
    }
}
