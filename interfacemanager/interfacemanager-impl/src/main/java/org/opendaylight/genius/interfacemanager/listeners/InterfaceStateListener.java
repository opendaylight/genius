/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceStateListener
        extends AsyncClusteredDataTreeChangeListenerBase<Interface, InterfaceStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStateListener.class);
    private final ManagedNewTransactionRunner txRunner;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public InterfaceStateListener(DataBroker dataBroker, final EntityOwnershipUtils entityOwnershipUtils,
            final JobCoordinator coordinator, final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceStateOld) {
        interfaceManagerCommonUtils.removeFromInterfaceStateCache(interfaceStateOld);
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceStateOld, Interface interfaceStateNew) {
        interfaceManagerCommonUtils.addInterfaceStateToCache(interfaceStateNew);
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceStateNew) {
        interfaceManagerCommonUtils.addInterfaceStateToCache(interfaceStateNew);
        if (!Tunnel.class.equals(interfaceStateNew.getType())
            || !entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                    IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Tunnel state add event for {}", interfaceStateNew.getName());
        coordinator.enqueueJob(interfaceStateNew.getName(), () -> {
            Interface.OperStatus bfdState = interfaceManagerCommonUtils
                    .getBfdStateFromCache(interfaceStateNew.getName());
            if (bfdState != null && bfdState != interfaceStateNew.getOperStatus()
                    && interfaceStateNew.getOperStatus() != Interface.OperStatus.Unknown) {
                // update opstate of interface if TEP has gone down/up as a
                // result of BFD monitoring
                LOG.debug("updating tunnel state for interface {}", interfaceStateNew.getName());
                return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
                    tx -> InterfaceManagerCommonUtils.updateOpState(tx, interfaceStateNew.getName(), bfdState)));
            }
            return Collections.emptyList();
        });
    }

    @Override
    protected InterfaceStateListener getDataTreeChangeListener() {
        return InterfaceStateListener.this;
    }
}
