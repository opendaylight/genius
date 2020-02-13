/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.internal;

import java.util.List;
import org.opendaylight.genius.utils.batching.ResourceHandler;
import org.opendaylight.genius.utils.batching.SubTransaction;
import org.opendaylight.genius.utils.batching.SubTransactionImpl;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class MdSalUtilBatchHandler implements ResourceHandler {
    private final DataBroker dataBroker;
    private final int batchSize;
    private final int batchInterval;

    MdSalUtilBatchHandler(DataBroker dataBroker, int batchSize, int batchInterval) {
        this.dataBroker = dataBroker;
        this.batchSize = batchSize;
        this.batchInterval = batchInterval;
    }

    @Override
    public void update(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier,
            Object original, Object update, List<SubTransaction> transactionObjects) {
        if (update != null && !(update instanceof DataObject)) {
            return;
        }
        if (datastoreType != this.getDatastoreType()) {
            return;
        }
        tx.merge(datastoreType, identifier, (DataObject) update, true);

        buildSubTransactions(transactionObjects, identifier, update, SubTransaction.UPDATE);
    }

    @Override
    public void create(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier,
            Object data, List<SubTransaction> transactionObjects) {
        if (data != null && !(data instanceof DataObject)) {
            return;
        }
        if (datastoreType != this.getDatastoreType()) {
            return;
        }
        tx.put(datastoreType, identifier, (DataObject) data, true);

        buildSubTransactions(transactionObjects, identifier, data, SubTransaction.CREATE);
    }

    @Override
    public void delete(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier,
            Object data, List<SubTransaction> transactionObjects) {
        if (data != null && !(data instanceof DataObject)) {
            return;
        }
        if (datastoreType != this.getDatastoreType()) {
            return;
        }
        tx.delete(datastoreType, identifier);

        buildSubTransactions(transactionObjects, identifier, data, SubTransaction.DELETE);
    }

    @Override
    public DataBroker getResourceBroker() {
        return dataBroker;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public int getBatchInterval() {
        return batchInterval;
    }

    @Override
    public LogicalDatastoreType getDatastoreType() {
        return LogicalDatastoreType.CONFIGURATION;
    }

    private void buildSubTransactions(List<SubTransaction> transactionObjects, InstanceIdentifier identifier,
            Object data, short subTransactionType) {
        // enable retries
        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setInstanceIdentifier(identifier);
        subTransaction.setInstance(data);
        subTransaction.setAction(subTransactionType);
        transactionObjects.add(subTransaction);
    }
}
