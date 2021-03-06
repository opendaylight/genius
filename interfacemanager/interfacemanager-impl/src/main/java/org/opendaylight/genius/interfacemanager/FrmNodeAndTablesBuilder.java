/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore.Configuration;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.binding.util.TypedReadWriteTransaction;
import org.opendaylight.openflowplugin.api.openflow.device.DeviceInfo;
import org.opendaylight.openflowplugin.applications.reconciliation.NotificationRegistration;
import org.opendaylight.openflowplugin.applications.reconciliation.ReconciliationManager;
import org.opendaylight.openflowplugin.applications.reconciliation.ReconciliationNotificationListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.rf.state.rev170713.ResultState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-creates the nodes and tables in the FRM config/inventory datastore
 * whenever a switch is connected. This class implements the callback provided
 * by openflowplugin to do the node and tables pre-creation.
 */
@Singleton
public class FrmNodeAndTablesBuilder implements ReconciliationNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrmNodeAndTablesBuilder.class);

    private static final String SERVICE_NAME = "FrmNodeAndTablesBuilder";
    private static final short CANARY_TABLE_ID = 255;
    private static final int SLEEP_BETWEEN_RETRIES = 1000;
    private static final int MAX_RETRIES = 3;
    private static final int TASK_PRIORITY = 2;

    private final DataBroker dataBroker;
    private NotificationRegistration registration;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public FrmNodeAndTablesBuilder(DataBroker dataBroker,
                                   ReconciliationManager reconciliationManager) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        reconciliationManager = requireNonNull(reconciliationManager, "ReconciliationManager cannot be null!");
        reconciliationManager.registerService(this);
        LOGGER.info("FrmNodeAndTablesBuilder has started successfully.");
    }

    @Override
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

    @Override
    public ListenableFuture<Boolean> startReconciliation(DeviceInfo connectedNode) {
        return Futures.immediateFuture(preCreateInventoryNodeAndTables(connectedNode, MAX_RETRIES,
                SLEEP_BETWEEN_RETRIES));
    }

    @Override
    public ListenableFuture<Boolean> endReconciliation(DeviceInfo node) {
        return Futures.immediateFuture(true);
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public ResultState getResultState() {
        return ResultState.DONOTHING;
    }

    @Override
    public int getPriority() {
        return TASK_PRIORITY;
    }

    private boolean preCreateInventoryNodeAndTables(DeviceInfo connectedNode, int maxRetries,
                                                    int sleepBetweenRetries) {
        Uint64 dpnId = getDpnIdFromNodeIdent(connectedNode);
        Node nodeDpn = buildDpnNode(dpnId);
        LOGGER.info("Pre-creating FRM node and tables for {}", dpnId);
        while (true) {
            ListenableFuture<?> future = txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
                if (!isTableAlreadyPrecreated(tx, nodeDpn)) {
                    LOGGER.info("{} getting connected for first time,"
                            + "proceed with FRM node and table pre-create", dpnId);
                    for (short tableId = 0; tableId <= 255; tableId++) {
                        InstanceIdentifier<Table> tableIId = InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                                .child(Table.class, new TableKey(Uint8.valueOf(tableId))).build();
                        tx.mergeParentStructureMerge(tableIId,
                                new TableBuilder().withKey(new TableKey(Uint8.valueOf(tableId))).build());
                    }
                    LOGGER.info("Pre-creating FRM node and tables for {} is finished", dpnId);
                } else {
                    LOGGER.info("FRM node and tables already present for {}", dpnId);
                }
            });
            try {
                future.get();
                return true;
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.error("Pre-creating FRM node and tables for {} failed, retrying.. {}", dpnId, maxRetries);
                maxRetries--;
                if (maxRetries > 0) {
                    try {
                        Thread.sleep(sleepBetweenRetries);
                    } catch (InterruptedException e) {
                        continue;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    private boolean isTableAlreadyPrecreated(TypedReadWriteTransaction<Configuration> tx, Node nodeDpn)
            throws ExecutionException, InterruptedException {
        InstanceIdentifier<Table> tableIId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(CANARY_TABLE_ID)).build();
        return tx.exists(tableIId).get();
    }

    private Uint64 getDpnIdFromNodeIdent(final DeviceInfo nodeIdentity) {
        Uint64 nodeName = nodeIdentity.getDatapathId();
        return nodeName;
    }

    protected Node buildDpnNode(Uint64 dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).withKey(new NodeKey(nodeId)).build();
        return nodeDpn;
    }

}
