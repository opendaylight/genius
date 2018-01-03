/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.base.Optional;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.baseimpl.internal.CacheManagersRegistryImpl;
import org.opendaylight.infrautils.caches.guava.internal.GuavaCacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceChildCache {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChildCache.class);

    private DataObjectCache<InterfaceParentEntry> dataObjectCache;
    private InstanceIdentifier<InterfaceParentEntry> instanceIdentifierPath;

    @Inject
    public InterfaceChildCache(final DataBroker dataBroker) {
        instanceIdentifierPath = InstanceIdentifier.create(InterfaceChildInfo.class).child(InterfaceParentEntry.class);
        dataObjectCache = new DataObjectCache<>(InterfaceParentEntry.class,
                dataBroker, LogicalDatastoreType.CONFIGURATION,
                instanceIdentifierPath,
                new GuavaCacheProvider(new CacheManagersRegistryImpl()));
    }

    public Optional<List<InterfaceChildEntry>> getInterfaceChildEntries(String parentInterfaceName) {
        LOG.info("hitting");
        try {
            Optional<InterfaceParentEntry> interfaceParentEntry = dataObjectCache.get(instanceIdentifierPath);
            if (interfaceParentEntry.isPresent()) {
                if (interfaceParentEntry.get().getParentInterface().equals(parentInterfaceName)) {
                    List<InterfaceChildEntry> interfaceChildEntries =
                            (interfaceParentEntry.get().getInterfaceChildEntry() != null)
                                    ? interfaceParentEntry.get().getInterfaceChildEntry() : Collections.emptyList();
                    return Optional.of(Collections.unmodifiableList(interfaceChildEntries));
                }
            }
        } catch (ReadFailedException ex) {
            LOG.error("ReadFailedException exception");
        }
        return Optional.absent();
    }
}
