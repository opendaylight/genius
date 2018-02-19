/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.itm.recovery.impl.ItmServiceRecoveryManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.ServiceOps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.service.ops.Services;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.service.ops.ServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.service.ops.services.Operations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItm;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmServiceRecoveryListener extends AbstractSyncDataTreeChangeListener<Operations> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmServiceRecoveryListener.class);

    private final ItmServiceRecoveryManager itmServiceRecoveryManager;

    @Inject
    public ItmServiceRecoveryListener(DataBroker dataBroker, ItmServiceRecoveryManager itmServiceRecoveryManager) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(ServiceOps.class)
                .child(Services.class, new ServicesKey(GeniusItm.class)).child(Operations.class));
        this.itmServiceRecoveryManager = itmServiceRecoveryManager;
    }

    @Override
    public void add(@Nonnull Operations operations)  {
        LOG.info("Service Recovery operation triggered for service: {}", operations);
        try {
            itmServiceRecoveryManager.recoverService(operations.getEntityType(), operations.getEntityName(),
                    operations.getEntityId());
        } catch (InterruptedException e) {
            LOG.trace("Service Recovery Operation not triggered.");
        }
    }

    @Override
    public void remove(@Nonnull Operations removedDataObject) {
    }

    @Override
    public void update(@Nonnull Operations originalDataObject, @Nonnull Operations updatedDataObject)  {
        add(updatedDataObject);
    }
}
