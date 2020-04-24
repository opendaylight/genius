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
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceChildCache {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChildCache.class);

    private final InstanceIdDataObjectCache<InterfaceParentEntry> dataObjectCache;

    @Inject
    public InterfaceChildCache(@Reference final DataBroker dataBroker, final @Reference CacheProvider cacheProvider) {
        dataObjectCache = new InstanceIdDataObjectCache<>(InterfaceParentEntry.class,
                dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(InterfaceChildInfo.class).child(InterfaceParentEntry.class),
                cacheProvider);
    }

    public Optional<List<InterfaceChildEntry>> getInterfaceChildEntries(String parentInterfaceName) {
        try {
            Optional<InterfaceParentEntry> interfaceParentEntry = dataObjectCache.get(
                    getInterfaceParentEntryIdentifier(parentInterfaceName));
            if (interfaceParentEntry.isPresent()) {
                List<InterfaceChildEntry> interfaceChildEntries = interfaceParentEntry.get()
                        .getInterfaceChildEntry() != null ? interfaceParentEntry.get()
                        .getInterfaceChildEntry() : Collections.emptyList();
                return Optional.of(Collections.unmodifiableList(interfaceChildEntries));
            }
        } catch (ReadFailedException ex) {
            LOG.error("ReadFailedException exception", ex);
        }
        return Optional.absent();
    }

    private InstanceIdentifier<InterfaceParentEntry> getInterfaceParentEntryIdentifier(String parentInterfaceName) {
        InstanceIdentifier.InstanceIdentifierBuilder<InterfaceParentEntry> intfIdBuilder =
                InstanceIdentifier.builder(InterfaceChildInfo.class)
                        .child(InterfaceParentEntry.class, new InterfaceParentEntryKey(parentInterfaceName));
        return intfIdBuilder.build();
    }
}
