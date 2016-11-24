/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager.jobs;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalPoolDeleteJob implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalPoolDeleteJob.class);

    private final String poolName;
    private final DataBroker broker;

    public LocalPoolDeleteJob(String poolName, DataBroker broker) {
        super();
        this.poolName = poolName;
        this.broker = broker;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        ArrayList<ListenableFuture<Void>> futures = new ArrayList<>();
        InstanceIdentifier<IdPool> idPoolToBeDeleted = IdUtils.getIdPoolInstance(poolName);
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.CONFIGURATION, idPoolToBeDeleted);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted local pool {}", poolName);
        }
        futures.add(tx.submit());
        return futures;
    }
}
