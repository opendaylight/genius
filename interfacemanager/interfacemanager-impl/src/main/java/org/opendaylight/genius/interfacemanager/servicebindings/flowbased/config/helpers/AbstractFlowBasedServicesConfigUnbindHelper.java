/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesConfigUnbindHelper extends FlowInstallHelper implements
        FlowBasedServicesConfigRemovable {

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
    public List<ListenableFuture<Void>> unbindService(String interfaceName, BoundServices boundServiceOld,
                                                      List<BoundServices> boundServices,
                                                      Class<? extends ServiceModeBase> serviceMode) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        if (FlowBasedServicesUtils.isInterfaceTypeBasedServiceBinding(interfaceName)) {
            return unbindServiceOnInterfaceType(interfaceName, boundServiceOld, boundServices, dataBroker);
        } else {
            BoundServicesState boundServicesState = FlowBasedServicesUtils.getBoundServicesState(
                    dataBroker, interfaceName, serviceMode);
            if (boundServices.isEmpty()) {
                FlowBasedServicesUtils.removeBoundServicesState(dataBroker, interfaceName, serviceMode);
            }
            if (L2vlan.class.equals(boundServicesState.getInterfaceType())
                    || Tunnel.class.equals(boundServicesState.getInterfaceType())) {
                return unbindServiceOnInterface(interfaceName, boundServiceOld, boundServices, boundServicesState,
                        dataBroker);
            }
        }
        return futures;
    }


    protected abstract List<ListenableFuture<Void>> unbindServiceOnInterface(String interfaceName,
                                                                             BoundServices boundServiceOld,
                                                                             List<BoundServices> allServices,
                                                                             BoundServicesState boundServicesState,
                                                                             DataBroker dataBroker);

    protected abstract List<ListenableFuture<Void>> unbindServiceOnInterfaceType(String ifaceType,
                                                                                 BoundServices boundServiceOld,
                                                                                 List<BoundServices> allServices,
                                                                                 DataBroker dataBroker);

    protected void deleteFlowsOnDpnsEgress(Set<BigInteger> allDpId, BoundServices boundServices,
                                           WriteTransaction transaction, String interfaceType,
                                           short serviceIndex) {

        for (BigInteger dpId : allDpId) {
            FlowBasedServicesUtils.removeTypeBasedEgressDispatcherFlows(dpId, boundServices, transaction, interfaceType,
                    serviceIndex);
        }
    }

    protected void deleteFlowsOnDpnsIngress(Set<BigInteger> allDpId, BoundServices boundServices,
                                            WriteTransaction transaction, String interfaceType,
                                            short serviceIndex) {

        for (BigInteger dpId : allDpId) {
            FlowBasedServicesUtils.removeTypeBasedLPortDispatcherFlow(dpId, boundServices, transaction, interfaceType,
                    serviceIndex);
        }
    }
}


