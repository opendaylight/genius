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

    /**
     * Create instance.
     * @param dataBroker instance of interfaceMgerProvider
     */
    public AbstractFlowBasedServicesStateUnbindHelper(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public final void unbindServices(List<ListenableFuture<Void>> futures, Interface ifaceState,
                                     Class<? extends ServiceModeBase> serviceMode) {

        LOG.debug("unbinding services on interface {}", ifaceState.getName());
        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(ifaceState.getName(),
                serviceMode, dataBroker);
        if (servicesInfo == null) {
            LOG.trace("service info is null for interface {}", ifaceState.getName());
            return;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices == null || allServices.isEmpty()) {
            LOG.trace("bound services is empty for interface {}", ifaceState.getName());
            return;
        }

        if (L2vlan.class.equals(ifaceState.getType()) || Tunnel.class.equals(ifaceState.getType())) {
            unbindServicesOnInterface(futures, allServices, ifaceState, ifaceState.getIfIndex());
        }
    }

    protected abstract void unbindServicesOnInterface(List<ListenableFuture<Void>> futures,
                                                      List<BoundServices> allServices, Interface ifState,
                                                      Integer ifIndex);

    public abstract void unbindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId,
                                                       String ifaceName);
}


