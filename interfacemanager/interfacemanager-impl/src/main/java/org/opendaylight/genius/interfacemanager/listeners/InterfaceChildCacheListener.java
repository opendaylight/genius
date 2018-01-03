/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceChildCacheListener extends AbstractClusteredSyncDataTreeChangeListener<InterfaceParentEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChildCacheListener.class);
    private final ConcurrentHashMap<String,List<InterfaceChildEntry>> interfaceChildInfoMap = new ConcurrentHashMap<>();

    @Inject
    public InterfaceChildCacheListener(final DataBroker dataBroker) {
        super(dataBroker, new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(InterfaceChildInfo.class).child(InterfaceParentEntry.class)));
    }

    @Override
    public void add(@Nonnull InterfaceParentEntry interfaceParentEntry) {
        List<InterfaceChildEntry> interfaceChildEntries = interfaceParentEntry.getInterfaceChildEntry();
        if (interfaceChildEntries == null) {
            interfaceChildEntries = Collections.emptyList();
        }
        LOG.trace("Adding entry for portName {} portUuid in cache ", interfaceParentEntry.getParentInterface());
        interfaceChildInfoMap.put(interfaceParentEntry.getParentInterface(), interfaceChildEntries);
    }

    @Override
    public void remove(@Nonnull InterfaceParentEntry interfaceParentEntry) {
        LOG.trace("Removing entry for portName {} in cache ", interfaceParentEntry.getParentInterface());
        interfaceChildInfoMap.remove(interfaceParentEntry.getParentInterface());
    }

    @Override
    public void update(@Nonnull InterfaceParentEntry originalDataObject,
                       @Nonnull InterfaceParentEntry updatedDataObject) {
    }

    public Optional<List<InterfaceChildEntry>> getInterfaceChildEntries(String parentInterfaceName) {
        List<InterfaceChildEntry> interfaceChildEntries = (interfaceChildInfoMap.get(parentInterfaceName) != null)
                ? interfaceChildInfoMap.get(parentInterfaceName) : Collections.EMPTY_LIST;
        return Optional.of(Collections.unmodifiableList(interfaceChildEntries));
    }
}