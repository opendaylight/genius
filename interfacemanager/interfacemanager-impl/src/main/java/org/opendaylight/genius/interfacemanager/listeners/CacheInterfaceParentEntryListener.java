/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CacheInterfaceParentEntryListener implements ClusteredDataTreeChangeListener<InterfaceParentEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(CacheInterfaceParentEntryListener.class);

    private final ConcurrentHashMap<String,List<InterfaceChildEntry>> interfaceChildInfoMap = new ConcurrentHashMap<>();
    private final ListenerRegistration<CacheInterfaceParentEntryListener> registration;
    private final DataTreeIdentifier<InterfaceParentEntry> treeId = new DataTreeIdentifier<>(LogicalDatastoreType
            .CONFIGURATION, InstanceIdentifier.create(InterfaceChildInfo.class).child(InterfaceParentEntry.class));

    @Inject
    public CacheInterfaceParentEntryListener(final DataBroker dataBroker) {
        LOG.trace("Registering on path: {}", treeId);
        registration = dataBroker.registerDataTreeChangeListener(treeId, CacheInterfaceParentEntryListener.this);
    }

    @PreDestroy
    public void close() {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<InterfaceParentEntry>> changes) {
        for (DataTreeModification<InterfaceParentEntry> change : changes) {
            final DataObjectModification<InterfaceParentEntry> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    removeInterfaceChildInfoCache(mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    addInterfaceChildInfoCache(mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    private void addInterfaceChildInfoCache(InterfaceParentEntry interfaceParentEntry) {
        if (interfaceParentEntry != null) {
            if (interfaceParentEntry.getParentInterface() != null
                    && interfaceParentEntry.getInterfaceChildEntry() != null) {
                LOG.trace("Adding entry for portName {} portUuid in cache ", interfaceParentEntry.getParentInterface());
                interfaceChildInfoMap.put(interfaceParentEntry.getParentInterface(),
                        interfaceParentEntry.getInterfaceChildEntry());
            }
        }
    }

    private void removeInterfaceChildInfoCache(InterfaceParentEntry interfaceParentEntry) {
        if (interfaceParentEntry != null && interfaceParentEntry.getParentInterface() != null) {
            LOG.trace("Removing entry for portName {} in cache ", interfaceParentEntry.getParentInterface());
            interfaceChildInfoMap.remove(interfaceParentEntry.getParentInterface());
        }
    }

    public List<InterfaceChildEntry> getInterfaceChildEntries(String parentInterfaceName) {
        return interfaceChildInfoMap.get(parentInterfaceName);
    }
}