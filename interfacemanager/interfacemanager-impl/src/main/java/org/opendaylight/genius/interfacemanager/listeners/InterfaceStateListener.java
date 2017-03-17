/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
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

import java.util.ArrayList;
import java.util.List;

public class InterfaceStateListener extends AsyncClusteredDataTreeChangeListenerBase<Interface, InterfaceStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStateListener.class);
    private DataBroker dataBroker;

    public InterfaceStateListener(DataBroker dataBroker) {
        super(Interface.class, InterfaceStateListener.class);
        this.dataBroker = dataBroker;
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceStateOld) {
        LOG.debug("Received interface state remove event for {}, ignoring", interfaceStateOld.getName());
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceStateOld, Interface interfaceStateNew) {
        LOG.debug("Received interface state update event for {},ignoring...", interfaceStateOld.getName());
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceStateNew) {
        if(!Tunnel.class.equals(interfaceStateNew.getType())
            || !IfmClusterUtils.isEntityOwner(IfmClusterUtils.INTERFACE_CONFIG_ENTITY)){
            return;
        }

        LOG.debug("Received Tunnel state add event for {}", interfaceStateNew.getName());
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        coordinator.enqueueJob(interfaceStateNew.getName(), () -> {
            final List<ListenableFuture<Void>> futures = new ArrayList<>();
            Interface.OperStatus bfdState = InterfaceManagerCommonUtils.getBfdStateFromCache(interfaceStateNew.getName());
            if (bfdState != null && bfdState != interfaceStateNew.getOperStatus() &&
                interfaceStateNew.getOperStatus() != Interface.OperStatus.Unknown) {
                // update opstate of interface if TEP has gone down/up as a result of BFD monitoring
                LOG.debug("updating tunnel state for interface {}", interfaceStateNew.getName());
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                InterfaceManagerCommonUtils.updateOpState(transaction, interfaceStateNew.getName(), bfdState);
                futures.add(transaction.submit());
            }
            return futures;
        });
    }

    @Override
    protected InterfaceStateListener getDataTreeChangeListener() {
        return InterfaceStateListener.this;
    }
}