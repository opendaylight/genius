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
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesConfigUnbindHelper implements FlowBasedServicesConfigRemovable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesConfigUnbindHelper.class);
    private InterfacemgrProvider interfaceMgrProvider;

    /**
     * Create instance.
     * @param interfaceMgrProvider instance of interfaceMgrProvider
     */
    public AbstractFlowBasedServicesConfigUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    @Override
    public List<ListenableFuture<Void>> unbindService(InstanceIdentifier<BoundServices> instanceIdentifier,
                                                      BoundServices boundServiceOld) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        String interfaceName =
                InstanceIdentifier.keyOf(instanceIdentifier.firstIdentifierOf(ServicesInfo.class)).getInterfaceName();
        Class<? extends ServiceModeBase> serviceMode = InstanceIdentifier
                .keyOf(instanceIdentifier.firstIdentifierOf(ServicesInfo.class)).getServiceMode();

        // Get the Parent ServiceInfo
        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(interfaceName, serviceMode,
                dataBroker);
        if (servicesInfo == null) {
            LOG.error("services info null during bind service {}, for: {}", boundServiceOld, interfaceName);
            return futures;
        }

        Interface
                ifState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
        if (ifState == null) {
            LOG.info("Not unbinding Service since operstatus is DOWN for Interface: {}", interfaceName);
            return futures;
        }
        List<BoundServices> boundServices = servicesInfo.getBoundServices();
        if (FlowBasedServicesUtils.isInterfaceTypeBasedServiceBinding(interfaceName)) {
            unbindServiceOnInterfaceType(boundServiceOld, boundServices, dataBroker);
        } else if (L2vlan.class.equals(ifState.getType()) || Tunnel.class.equals(ifState.getType())) {
            unbindServiceOnInterface(boundServiceOld, boundServices, ifState, dataBroker);
        }
        return futures;
    }


    protected abstract List<ListenableFuture<Void>> unbindServiceOnInterface(BoundServices boundServiceNew,
                                                                             List<BoundServices> allServices,
                                                                             Interface ifState, DataBroker dataBroker);

    protected abstract List<ListenableFuture<Void>> unbindServiceOnInterfaceType(BoundServices boundServiceNew,
                                                                                 List<BoundServices> allServices,
                                                                                 DataBroker dataBroker);

}


