/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
public class CacheInterfaceStateListener implements ClusteredDataTreeChangeListener<Interface> {
    private static final Logger LOG = LoggerFactory.getLogger(CacheInterfaceStateListener.class);
    private DataBroker db;
    private ListenerRegistration<CacheInterfaceStateListener> registration;

    public CacheInterfaceStateListener(DataBroker broker) {
        this.db = broker;
        registerListener(db);
    }

    private void registerListener(DataBroker db) {
        final DataTreeIdentifier<Interface> treeId =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = db.registerDataTreeChangeListener(treeId, CacheInterfaceStateListener.this);
        } catch (final Exception e) {
            LOG.warn("CacheInterfaceConfigListener registration failed", e);
        }
    }

    private InstanceIdentifier<Interface> getWildcardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    public void close() throws Exception {
        if(registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Interface>> changes) {
        for (DataTreeModification<Interface> change : changes) {
        final InstanceIdentifier<Interface> key = change.getRootPath().getRootIdentifier();
        final DataObjectModification<Interface> mod = change.getRootNode();
            switch (mod.getModificationType()) {
            case DELETE:
                InterfaceManagerCommonUtils.removeFromInterfaceStateCache(mod.getDataBefore());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                InterfaceManagerCommonUtils.addInterfaceStateToCache(mod.getDataAfter());
                break;
            default:
                throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

}