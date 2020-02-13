/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ResourceHandler {

    void create(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifer, Object vrfEntry,
            List<SubTransaction> transactionObjects);

    void delete(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifer, Object vrfEntry,
            List<SubTransaction> transactionObjects);

    void update(WriteTransaction tx, LogicalDatastoreType datastoreType, InstanceIdentifier identifier, Object original,
            Object update, List<SubTransaction> transactionObjects);

    LogicalDatastoreType getDatastoreType();

    int getBatchSize();

    int getBatchInterval();

    DataBroker getResourceBroker();
}
