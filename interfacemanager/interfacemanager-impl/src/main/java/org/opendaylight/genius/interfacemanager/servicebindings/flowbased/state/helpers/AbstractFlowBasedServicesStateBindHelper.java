/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding
 * for interfaces
 */
public abstract class AbstractFlowBasedServicesStateBindHelper implements FlowBasedServicesStateAddable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesStateBindHelper.class);
    private InterfacemgrProvider interfaceMgrProvider;

    /**
     * Create instance.
     * @param interfaceMgrProvider
     */
    public AbstractFlowBasedServicesStateBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    @Override
    public List<ListenableFuture<Void>> bindServicesOnInterface(Interface ifaceState) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if(ifaceState.getType() == null) {
            return futures;
        }
        LOG.debug("binding services on interface {}", ifaceState.getName());
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(ifaceState.getName(), ServiceModeEgress.class, dataBroker);
        if (servicesInfo == null) {
            LOG.trace("service info is null for interface {}", ifaceState.getName());
            return futures;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices == null || allServices.isEmpty()) {
            LOG.trace("bound services is empty for interface {}", ifaceState.getName());
            return futures;
        }

        if (ifaceState.getType().isAssignableFrom(L2vlan.class)) {
            return bindServiceOnVlan(allServices, ifaceState, dataBroker);
        } else if (ifaceState.getType().isAssignableFrom(Tunnel.class)){
            return bindServiceOnTunnelInterface(allServices, ifaceState, dataBroker);
        }
        return futures;
    }

    @Override
    public List<ListenableFuture<Void>> bindServicesOnInterfaceType(BigInteger dpnId, String ifaceName) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        LOG.info("binding services on tunnel type - WIP");
        return futures;
    }

    protected abstract List<ListenableFuture<Void>> bindServiceOnVlan(List<BoundServices> allServices, Interface ifState, DataBroker dataBroker);

    protected abstract List<ListenableFuture<Void>> bindServiceOnTunnelInterface(List<BoundServices> allServices, Interface ifState, DataBroker dataBroker);
}


