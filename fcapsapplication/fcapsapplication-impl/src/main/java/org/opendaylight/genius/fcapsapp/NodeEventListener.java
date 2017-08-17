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
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.genius.fcapsapp.alarm.AlarmAgent;
import org.opendaylight.genius.fcapsapp.performancecounter.NodeUpdateCounter;
import org.opendaylight.genius.fcapsapp.performancecounter.PacketInCounterHandler;
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
    private final EntityOwnershipService entityOwnershipService;

    @Inject
    public NodeEventListener(final AlarmAgent alarmAgent, final NodeUpdateCounter nodeUpdateCounter,
            final PacketInCounterHandler packetInCounterHandler, final EntityOwnershipService entityOwnershipService) {
        this.alarmAgent = alarmAgent;
        this.nodeUpdateCounter = nodeUpdateCounter;
        this.packetInCounterHandler = packetInCounterHandler;
        this.entityOwnershipService = entityOwnershipService;
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
    public void onDataTreeChanged(Collection<DataTreeModification<D>> changes) {
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
            LOG.debug("retrieved hostname {}", hostName);
            String nodeId = getDpnId(String.valueOf(nodeConnIdent.firstKeyOf(Node.class).getId()));
            if (nodeId != null) {
                switch (mod.getModificationType()) {
                    case DELETE:
                        LOG.debug("NodeRemoved {} notification is received on host {}", nodeId, hostName);
                        if (nodeUpdateCounter.isDpnConnectedLocal(nodeId)) {
                            alarmAgent.raiseControlPathAlarm(nodeId, hostName);
                            nodeUpdateCounter.nodeRemovedNotification(nodeId, hostName);
                        }
                        packetInCounterHandler.nodeRemovedNotification(nodeId);
                        break;
                    case SUBTREE_MODIFIED:
                        if (isNodeOwner(nodeId)) {
                            LOG.debug("NodeUpdated {} notification is received", nodeId);
                        } else {
                            LOG.debug("UPDATE: Node {} is not connected to host {}", nodeId, hostName);
                        }
                        break;
                    case WRITE:
                        if (mod.getDataBefore() == null) {
                            if (isNodeOwner(nodeId)) {
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
            } else {
                LOG.error("DpnID is null");
            }
        }
    }

    private String getDpnId(String node) {
        // Uri [_value=openflow:1]
        String[] temp = node.split("=");
        return temp[1].substring(0, temp[1].length() - 1);
    }

    /**
     * Method checks if *this* instance of controller is owner of the given
     * openflow node.
     *
     * @param nodeId
     *            DpnId
     * @return True if owner, else false
     */
    public boolean isNodeOwner(String nodeId) {
        Entity entity = new Entity("openflow", nodeId);
        return this.entityOwnershipService.getOwnershipState(entity).transform(EntityOwnershipState::isOwner).or(false);
    }
}
