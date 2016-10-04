/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.renderer.ovs.utilities;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.utils.batching.ResourceHandler;
import org.opendaylight.genius.utils.batching.SubTransaction;
import org.opendaylight.genius.utils.batching.SubTransactionImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;

public class InterfaceBatchHandler implements ResourceHandler {
    public void update(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier, Object original, Object update,List<SubTransaction> transactionObjects) {
        if (update != null && !(update instanceof DataObject)) {
            return;
        }
        if (datastoreType != this.getDatastoreType()) {
            return;
        }
        tx.merge(datastoreType, identifier, (DataObject)update, true);

        buildSubTransactions(transactionObjects, identifier, update, SubTransaction.UPDATE);
    }

    public void create(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier, Object data,List<SubTransaction> transactionObjects) {
        if (data != null && !(data instanceof DataObject)) {
            return;
        }
        if (datastoreType != this.getDatastoreType()) {
            return;
        }
        tx.put(datastoreType, identifier, (DataObject)data, true);

        buildSubTransactions(transactionObjects, identifier, data, SubTransaction.CREATE);
    }

    public void delete(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier, Object data,List<SubTransaction> transactionObjects) {
        if (data != null && !(data instanceof DataObject)) {
            return;
        }
        if (datastoreType != this.getDatastoreType()) {
            return;
        }
        tx.delete(datastoreType, identifier);

        buildSubTransactions(transactionObjects, identifier, data, SubTransaction.DELETE);
    }

    public DataBroker getResourceBroker() {
        return BatchingUtils.getBroker();
    }

    public int getBatchSize() {
        return BatchingUtils.batchSize;
    }

    public int getBatchInterval() {
        return BatchingUtils.batchInterval;
    }

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
