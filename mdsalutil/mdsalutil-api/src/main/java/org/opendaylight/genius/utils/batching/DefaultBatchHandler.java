/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.utils.batching;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class DefaultBatchHandler implements ResourceHandler {

    private final DataBroker dataBroker;
    private final Integer batchSize;
    private final Integer batchInterval;
    private final LogicalDatastoreType datastoreType;

    public DefaultBatchHandler(DataBroker dataBroker, LogicalDatastoreType dataStoreType, Integer batchSize,
            Integer batchInterval) {
        this.dataBroker = dataBroker;
        this.batchSize = batchSize;
        this.batchInterval = batchInterval;
        this.datastoreType = dataStoreType;
    }

    @Override
    public void update(WriteTransaction tx, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier identifier, Object original, Object update, List<SubTransaction> transactionObjects) {
        if (update != null && !(update instanceof DataObject)) {
            return;
        }
        if (logicalDatastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.UPDATE);
        subTransaction.setInstance(update);
        subTransaction.setInstanceIdentifier(identifier);
        transactionObjects.add(subTransaction);

        tx.merge(logicalDatastoreType, identifier, (DataObject) update, true);
    }

    @Override
    public void create(WriteTransaction tx, final LogicalDatastoreType logicalDatastoreType,
            final InstanceIdentifier identifier, final Object data, List<SubTransaction> transactionObjects) {
        if (data != null && !(data instanceof DataObject)) {
            return;
        }
        if (logicalDatastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.CREATE);
        subTransaction.setInstance(data);
        subTransaction.setInstanceIdentifier(identifier);
        transactionObjects.add(subTransaction);

        tx.put(logicalDatastoreType, identifier, (DataObject) data, true);
    }

    @Override
    public void delete(WriteTransaction tx, final LogicalDatastoreType logicalDatastoreType,
            final InstanceIdentifier identifier, final Object data, List<SubTransaction> transactionObjects) {
        if (data != null && !(data instanceof DataObject)) {
            return;
        }
        if (logicalDatastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.DELETE);
        subTransaction.setInstanceIdentifier(identifier);
        transactionObjects.add(subTransaction);

        tx.delete(logicalDatastoreType, identifier);
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
        return datastoreType;
    }
}

