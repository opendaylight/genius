/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.recovery.listeners;

import java.util.Collection;
import javax.inject.Inject;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.recovery.impl.ServiceRecoveryManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.ServiceOps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.service.ops.Services;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.service.ops.ServicesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.service.ops.services.Operations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusIfm;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class ServiceRecoveryListener implements ClusteredDataTreeChangeListener<Operations>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRecoveryListener.class);
    private ListenerRegistration<ServiceRecoveryListener> listenerRegistration;

    @Inject
    public ServiceRecoveryListener(DataBroker dataBroker) {
        registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    protected InstanceIdentifier<Operations> getWildCardPath() {
        return InstanceIdentifier.create(ServiceOps.class)
            .child(Services.class, new ServicesKey(GeniusIfm.class)).child(Operations.class);
    }

    public void registerListener(LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier<Operations> treeId = new DataTreeIdentifier<>(dsType, getWildCardPath());
        listenerRegistration = db.registerDataTreeChangeListener(treeId, ServiceRecoveryListener.this);
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } finally {
                listenerRegistration = null;
            }
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Operations>> changes) {
        for (DataTreeModification<Operations> change : changes) {
            final InstanceIdentifier<Operations> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Operations> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        add(key, mod.getDataAfter());
                    }
                    break;
                default:
                    // FIXME: May be not a good idea to throw.
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }


    protected void add(InstanceIdentifier<Operations> key, Operations operations) {
        LOG.info("Service Recovery operation triggered for service: {}", operations);
        ServiceRecoveryManager.recoverService(operations.getEntityType(), operations.getEntityName(),
                operations.getEntityId());
    }
}