/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesStateUnbindHelper implements FlowBasedServicesStateRemovable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesStateUnbindHelper.class);

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;

    /**
     * Create instance.
     * @param dataBroker instance of interfaceMgerProvider
     */
    protected AbstractFlowBasedServicesStateUnbindHelper(final DataBroker dataBroker) {
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
    public final void unbindServices(List<ListenableFuture<Void>> futures, Interface ifaceState,  String ifaceName,
                                     Class<? extends ServiceModeBase> serviceMode, BigInteger dpnId) {

        LOG.debug("unbinding services on interface {}", ifaceName);
        ServicesInfo servicesInfo =
                FlowBasedServicesUtils.getServicesInfoForInterface(ifaceName, serviceMode, dataBroker);
        if (servicesInfo == null) {
            LOG.trace("service info is null for interface {}", ifaceName);
            return;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices == null || allServices.isEmpty()) {
            LOG.trace("bound services is empty for interface {}", ifaceName);
            return;
        }

        if (FlowBasedServicesUtils.isInterfaceTypeBasedServiceBinding(ifaceName)) {
            unbindServicesFromInterfaceType(futures, dpnId, ifaceName, allServices);
        } else {
            if (L2vlan.class.equals(ifaceState.getType()) || Tunnel.class.equals(ifaceState.getType())) {
                unbindServicesFromInterface(futures, ifaceState, allServices);
            }
        }
    }

    protected abstract void unbindServicesFromInterface(List<ListenableFuture<Void>> futures, Interface ifState,
                                                     List<BoundServices> allServices);

    protected abstract void unbindServicesFromInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId,
                                                         String ifaceName, List<BoundServices> allServices);
}


