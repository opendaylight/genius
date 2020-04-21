/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesStateBindHelper implements FlowBasedServicesStateAddable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesStateBindHelper.class);

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;

    /**
     * Create instance.
     * @param dataBroker instance of interfaceMgrProvider
     */
    protected AbstractFlowBasedServicesStateBindHelper(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    protected DataBroker getDataBroker() {
        return dataBroker;
    }

    protected ManagedNewTransactionRunner getTxRunner() {
        return txRunner;
    }


    @Override
    public final void bindServices(List<ListenableFuture<Void>> futures, Interface ifaceState,
                                   List<BoundServices> allServices, Class<? extends ServiceModeBase> serviceMode) {
        futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
            if (L2vlan.class.equals(ifaceState.getType()) || Tunnel.class.equals(ifaceState.getType())) {
                LOG.debug("binding services on interface {}", ifaceState.getName());
                bindServicesOnInterface(tx, allServices, ifaceState);
            }
        }));
    }

    protected abstract void bindServicesOnInterface(TypedReadWriteTransaction<Configuration> tx,
                                                    List<BoundServices> allServices, Interface ifState);

    @Override
    public abstract void bindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, Uint64 dpnId,
                                                     String ifaceName);
}


