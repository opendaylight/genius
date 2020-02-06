package org.opendaylight.genius.interfacemanager;


import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;


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

    @Inject
    public FrmNodeAndTablesBuilder(DataBroker dataBroker,
                                   ReconciliationManager reconciliationManager) {
        this.dataBroker = dataBroker;
        reconciliationManager = Preconditions.checkNotNull(reconciliationManager,
                "ReconciliationManager cannot be null!");
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
        Node nodeDpn = buildDpnNode(dpnId.toJava());
        LOGGER.info("Pre-creating FRM node and tables for {}", dpnId);
        while (true) {
            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
            try {
                if (!isTableAlreadyPrecreated(tx, nodeDpn)) {
                    LOGGER.info("{} getting connected for first time,"
                            + "proceed with FRM node and table pre-create", dpnId);
                    for (short tableId = 0; tableId <= 255; tableId++) {
                        InstanceIdentifier<Table> tableIId = InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                                .child(Table.class, new TableKey(tableId)).build();
                        tx.put(LogicalDatastoreType.CONFIGURATION, tableIId, new TableBuilder().withKey(
                                new TableKey(tableId)).build(), true);
                    }
                    tx.submit().checkedGet();
                    LOGGER.info("Pre-creating FRM node and tables for {} is finished", dpnId);
                    return true;
                } else {
                    LOGGER.info("FRM node and tables already present for {}", dpnId);
                    break;
                }
            } catch (TransactionCommitFailedException | ReadFailedException ex) {
                tx.cancel();
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
        return true;
    }

    private boolean isTableAlreadyPrecreated(ReadWriteTransaction tx, Node nodeDpn) throws ReadFailedException {
        InstanceIdentifier<Table> tableIId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeDpn.key()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(CANARY_TABLE_ID)).build();
        Optional<Table> canaryTable = tx.read(LogicalDatastoreType.CONFIGURATION, tableIId).checkedGet();
        return canaryTable.isPresent();
    }

    private Uint64 getDpnIdFromNodeIdent(final DeviceInfo nodeIdentity) {
        Uint64 nodeName = nodeIdentity.getDatapathId();
        return nodeName;
    }

    protected Node buildDpnNode(BigInteger dpnId) {
        NodeId nodeId = new NodeId("openflow:" + dpnId);
        Node nodeDpn = new NodeBuilder().setId(nodeId).withKey(new NodeKey(nodeId)).build();
        return nodeDpn;
    }
}
