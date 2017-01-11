/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.batching.ResourceHandler;
import org.opendaylight.genius.utils.batching.SubTransaction;
import org.opendaylight.genius.utils.batching.SubTransactionImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;

public class CounterBatchHandler implements ResourceHandler {
    private static DataBroker dataBroker;

    public CounterBatchHandler() {}

    public CounterBatchHandler(final DataBroker db) {
        dataBroker = db;
    }
    @Override
    public void create(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifer, Object data, List<SubTransaction> transactionObjects) {
        if ((data != null) && !(data instanceof DataObject)) {
            return;
        }
        if (datastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.CREATE);
        subTransaction.setInstance(data);
        subTransaction.setInstanceIdentifier(identifer);
        transactionObjects.add(subTransaction);

        tx.put(datastoreType, identifer, (DataObject) data, true);
    }

    @Override
    public void delete(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifer, Object data, List<SubTransaction> transactionObjects) {
        if ((data != null) && !(data instanceof DataObject)) {
            return;
        }
        if (datastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.DELETE);
        subTransaction.setInstanceIdentifier(identifer);
        transactionObjects.add(subTransaction);

        tx.delete(datastoreType, identifer);
    }

    @Override
    public void update(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier, Object original, Object update, List<SubTransaction> transactionObjects) {
        if ((update != null) && !(update instanceof DataObject)) {
            return;
        }
        if (datastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.UPDATE);
        subTransaction.setInstance(update);
        subTransaction.setInstanceIdentifier(identifier);
        transactionObjects.add(subTransaction);

        tx.merge(datastoreType, identifier, (DataObject) update, true);
    }

    @Override
    public LogicalDatastoreType getDatastoreType() {
        return LogicalDatastoreType.CONFIGURATION;
    }

    @Override
    public int getBatchSize() {
        return CounterUtil.batchSize;
    }

    @Override
    public int getBatchInterval() {
        return CounterUtil.batchInterval;
    }

    @Override
    public DataBroker getResourceBroker() {
        return dataBroker;
    }
}