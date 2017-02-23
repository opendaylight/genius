/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collection;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
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

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
@Singleton
public class CacheInterfaceStateListener implements ClusteredDataTreeChangeListener<Interface> {
    private static final Logger LOG = LoggerFactory.getLogger(CacheInterfaceStateListener.class);
    private ListenerRegistration<CacheInterfaceStateListener> registration;
    private final DataTreeIdentifier<Interface> treeId =
            new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());

    @Inject
    public CacheInterfaceStateListener(final DataBroker dataBroker) {
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = dataBroker.registerDataTreeChangeListener(treeId, CacheInterfaceStateListener.this);
        } catch (final Exception e) {
            LOG.warn("CacheInterfaceConfigListener registration failed", e);
        }

    }

    @PreDestroy
    public void close() throws Exception {
        if(registration != null) {
            registration.close();
        }
    }

    private InstanceIdentifier<Interface> getWildcardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
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