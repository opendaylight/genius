/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.fcapsapp.performancecounter.PacketInCounterHandler;
import org.opendaylight.genius.fcapsapp.portinfo.PortNameMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FcapsProvider implements AutoCloseable {

    public static Logger s_logger = LoggerFactory.getLogger(FcapsProvider.class);
    private final DataBroker dataService;
    private final NotificationService notificationProviderService;
    private final EntityOwnershipService entityOwnershipService;
    private PacketInCounterHandler packetInCounterHandler;
    private NodeEventListener<FlowCapableNode> nodeEventListener;

    /**
     * Contructor sets the services
     * @param dataBroker instance of databroker
     * @param notificationService instance of notificationservice
     * @param entityOwnershipService instance of EntityOwnershipService
     */
    @Inject
    public FcapsProvider(DataBroker dataBroker,
                         NotificationService notificationService,
                         final EntityOwnershipService entityOwnershipService,
                         PacketInCounterHandler packetInCounterHandler){
        this.dataService = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
        s_logger.info("FcapsProvider dataBroker is set");

        this.notificationProviderService = Preconditions.checkNotNull(notificationService, "notificationService can not be null!");
        s_logger.info("FcapsProvider notificationProviderService is set");

        this.entityOwnershipService = Preconditions.checkNotNull(entityOwnershipService, "EntityOwnership service can not be null");
        s_logger.info("FcapsProvider entityOwnershipService is set");

        this.packetInCounterHandler = packetInCounterHandler;
    }

    @PostConstruct
    public void start() throws Exception {
        nodeEventListener = new NodeEventListener<>(entityOwnershipService);
        PortNameMapping.registerPortMappingBean();
        registerListener(dataService);
        notificationProviderService.registerNotificationListener(packetInCounterHandler);
        s_logger.info("FcapsProvider started");
    }

    private void registerListener(DataBroker dataBroker) {
        final DataTreeIdentifier<FlowCapableNode> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildCardPath());
        try {
            dataBroker.registerDataTreeChangeListener(treeId, nodeEventListener);
        } catch (Exception e) {
            s_logger.error("Registeration failed on DataTreeChangeListener {}",e);
        }
    }

    private InstanceIdentifier<FlowCapableNode> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class)
                .child(Node.class)
                .augmentation(FlowCapableNode.class);
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        s_logger.info("FcapsProvider closed");
    }
}
