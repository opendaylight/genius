/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesConfigBindHelper implements FlowBasedServicesConfigAddable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesConfigBindHelper.class);
    private InterfacemgrProvider interfaceMgrProvider;

    /**
     * Create instance.
     * @param interfaceMgrProvider instance of interfaceMgrProvider
     */
    public AbstractFlowBasedServicesConfigBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public List<ListenableFuture<Void>> bindService(InstanceIdentifier<BoundServices> instanceIdentifier,
                                                    BoundServices boundServiceNew) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        String interfaceName =
                InstanceIdentifier.keyOf(instanceIdentifier.firstIdentifierOf(ServicesInfo.class)).getInterfaceName();
        Class<? extends ServiceModeBase> serviceMode =
                InstanceIdentifier.keyOf(instanceIdentifier.firstIdentifierOf(ServicesInfo.class)).getServiceMode();
        // Get the Parent ServiceInfo
        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(interfaceName, serviceMode,
                dataBroker);
        if (servicesInfo == null) {
            LOG.error("services info null during bind service {}, for: {}", boundServiceNew, interfaceName);
            return futures;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices.isEmpty()) {
            LOG.error("empty bound service list during bind service {}, for: {}", boundServiceNew, interfaceName);
            return futures;
        }

        if (FlowBasedServicesUtils.isInterfaceTypeBasedServiceBinding(interfaceName)) {
            bindServiceOnInterfaceType(boundServiceNew, allServices, dataBroker);
        } else {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
                    .Interface ifState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName,
                    dataBroker);
            if (ifState == null) {
                LOG.warn("Interface not operational, not Binding Service for Interface: {}", interfaceName);
                return futures;
            }
            if (L2vlan.class.equals(ifState.getType()) || Tunnel.class.equals(ifState.getType())) {
                bindServiceOnInterface(boundServiceNew, allServices, ifState, dataBroker);
            }
        }
        return futures;
    }

    protected abstract List<ListenableFuture<Void>> bindServiceOnInterface(BoundServices boundServiceNew,
                                                                           List<BoundServices> allServices,
                                                                           org.opendaylight.yang.gen.v1.urn.ietf.params
                                                                                   .xml.ns.yang.ietf.interfaces
                                                                                   .rev140508.interfaces
                                                                                   .state.Interface ifState,
                                                                           DataBroker dataBroker);

    protected abstract List<ListenableFuture<Void>> bindServiceOnInterfaceType(BoundServices boundServiceNew,
                                                                               List<BoundServices> allServices,
                                                                               DataBroker dataBroker);
}


