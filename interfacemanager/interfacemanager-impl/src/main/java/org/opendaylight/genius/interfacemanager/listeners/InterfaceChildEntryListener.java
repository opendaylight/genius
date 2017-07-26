/*
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceChildEntryListener extends AsyncClusteredDataTreeChangeListenerBase<InterfaceChildEntry,
        InterfaceChildEntryListener> {

    private final DataBroker dataBroker;
    private final IdManagerService idManagerService;
    private final IMdsalApiManager mdsalApiManager;
    private final AlivenessMonitorService alivenessMonitorService;

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChildEntryListener.class);

    @Inject
    public InterfaceChildEntryListener(DataBroker dataBroker,
                                       IdManagerService idManagerService,
                                       IMdsalApiManager mdsalApiManager,
                                       AlivenessMonitorService alivenessMonitorService) {
        this.dataBroker = dataBroker;
        this.idManagerService = idManagerService;
        this.mdsalApiManager = mdsalApiManager;
        this.alivenessMonitorService = alivenessMonitorService;
        this.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    @Override
    protected InstanceIdentifier<InterfaceChildEntry> getWildCardPath() {
        return InstanceIdentifier.create(InterfaceChildInfo.class)
                        .child(InterfaceParentEntry.class)
                        .child(InterfaceChildEntry.class);
    }

    @Override
    protected void remove(InstanceIdentifier<InterfaceChildEntry> key, InterfaceChildEntry dataObjectModification) {
        // NOOP
    }

    @Override
    protected void update(InstanceIdentifier<InterfaceChildEntry> key,
                          InterfaceChildEntry dataObjectModificationBefore,
                          InterfaceChildEntry dataObjectModificationAfter) {
        // NOOP
    }

    @Override
    protected void add(InstanceIdentifier<InterfaceChildEntry> key, InterfaceChildEntry dataObjectModification) {
        LOG.trace("Received add event of InterfaceChildEntry {}", dataObjectModification);
        String childInterface = dataObjectModification.getChildInterface();
        String parentInterface = key.firstKeyOf(InterfaceParentEntry.class).getParentInterface();
        Interface parentInterfaceState = InterfaceManagerCommonUtils.getInterfaceStateFromCache(parentInterface);
        Interface childInterfaceState = InterfaceManagerCommonUtils.getInterfaceStateFromCache(childInterface);
        boolean isOfTunnelInterface = InterfaceManagerCommonUtils.isOfTunnelInterface(
                InterfaceManagerCommonUtils.getInterfaceFromCache(childInterface));

        if (parentInterfaceState == null || childInterfaceState != null || !isOfTunnelInterface) {
            LOG.trace("Ignore add event of InterfaceChildEntry: not a stateless OF tunnel interface with parent state");
            return;
        }

        if (parentInterfaceState.getLowerLayerIf() == null || parentInterfaceState.getLowerLayerIf().isEmpty()) {
            LOG.warn("No lower layer interface for {}", parentInterfaceState.getName());
            return;
        }

        LOG.trace("Enqueue job to add state for interface {} after parent state {}",
                childInterface,
                parentInterfaceState);
        DataStoreJobCoordinator.getInstance().enqueueJob(childInterface, () -> OvsInterfaceStateAddHelper.addState(
                dataBroker, idManagerService, mdsalApiManager, alivenessMonitorService, childInterface,
                parentInterfaceState));
    }

    @Override
    protected InterfaceChildEntryListener getDataTreeChangeListener() {
        return this;
    }
}
