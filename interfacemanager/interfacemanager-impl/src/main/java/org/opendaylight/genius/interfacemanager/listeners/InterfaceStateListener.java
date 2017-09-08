/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.IfmClusterUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceStateListener
        extends AsyncClusteredDataTreeChangeListenerBase<Interface, InterfaceStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStateListener.class);
    private final DataBroker dataBroker;

    @Inject
    public InterfaceStateListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceStateOld) {
        InterfaceManagerCommonUtils.removeFromInterfaceStateCache(interfaceStateOld);
        LOG.debug("Received interface state remove event for {}, ignoring", interfaceStateOld.getName());
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceStateOld, Interface interfaceStateNew) {
        InterfaceManagerCommonUtils.addInterfaceStateToCache(interfaceStateNew);
        LOG.debug("Received interface state update event for {},ignoring...", interfaceStateOld.getName());
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceStateNew) {
        InterfaceManagerCommonUtils.addInterfaceStateToCache(interfaceStateNew);
        if (!Tunnel.class.equals(interfaceStateNew.getType())
            || !IfmClusterUtils.isEntityOwner(IfmClusterUtils.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Tunnel state add event for {}", interfaceStateNew.getName());
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        coordinator.enqueueJob(interfaceStateNew.getName(), () -> {
            Interface.OperStatus bfdState = InterfaceManagerCommonUtils
                    .getBfdStateFromCache(interfaceStateNew.getName());
            if (bfdState != null && bfdState != interfaceStateNew.getOperStatus()
                    && interfaceStateNew.getOperStatus() != Interface.OperStatus.Unknown) {
                // update opstate of interface if TEP has gone down/up as a
                // result of BFD monitoring
                LOG.debug("updating tunnel state for interface {}", interfaceStateNew.getName());
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                InterfaceManagerCommonUtils.updateOpState(transaction, interfaceStateNew.getName(), bfdState);
                return Collections.singletonList(transaction.submit());
            }
            return Collections.emptyList();
        });
    }

    @Override
    protected InterfaceStateListener getDataTreeChangeListener() {
        return InterfaceStateListener.this;
    }
}
