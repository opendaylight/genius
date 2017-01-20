/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager.jobs;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;

public class UpdateIdEntryJob implements Callable<List<ListenableFuture<Void>>> {

    private final String parentPoolName;
    private final String localPoolName;
    private final String idKey;
    private final List<Long> newIdValues;
    private final DataBroker broker;
    private final IdUtils idUtils;

    public UpdateIdEntryJob(String parentPoolName, String localPoolName,
            String idKey, List<Long> newIdValues, DataBroker broker, IdUtils idUtils) {
        this.parentPoolName = parentPoolName;
        this.localPoolName = localPoolName;
        this.idKey = idKey;
        this.newIdValues = newIdValues;
        this.broker = broker;
        this.idUtils = idUtils;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        idUtils.updateChildPool(tx, parentPoolName, localPoolName);
        if (newIdValues != null && !newIdValues.isEmpty()) {
            IdEntries newIdEntry = idUtils.createIdEntries(idKey, newIdValues);
            tx.merge(CONFIGURATION, idUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey), newIdEntry);
        } else {
            tx.delete(CONFIGURATION, idUtils.getIdEntriesInstanceIdentifier(parentPoolName, idKey));
        }
        tx.submit().checkedGet();
        String uniqueIdKey = idUtils.getUniqueKey(parentPoolName, idKey);
        Optional.ofNullable(idUtils.releaseIdLatchMap.get(uniqueIdKey))
            .ifPresent(latch -> latch.countDown());
        // Once the id is written to DS, removing the id value from map.
        idUtils.allocatedIdMap.remove(uniqueIdKey);
        return Collections.emptyList();
    }
}
