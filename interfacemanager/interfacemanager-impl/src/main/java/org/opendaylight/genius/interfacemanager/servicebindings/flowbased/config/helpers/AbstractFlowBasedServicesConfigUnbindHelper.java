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
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesConfigUnbindHelper extends AbstractFlowBasedServicesConfigInstallHelper
        implements FlowBasedServicesConfigRemovable {

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
                                                      BoundServicesState boundServicesState) {

        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        return unbindServiceOnInterface(interfaceName, boundServiceOld, boundServices, boundServicesState, dataBroker);
    }

    protected abstract List<ListenableFuture<Void>> unbindServiceOnInterface(String interfaceName,
                                                                             BoundServices boundServiceOld,
                                                                             List<BoundServices> allServices,
                                                                             BoundServicesState boundServicesState,
                                                                             DataBroker dataBroker);


    protected void removeFlowsEgress(Set<BigInteger> dpnId, BoundServices boundServicesOld,
                                     WriteTransaction transaction, short currentServiceIndex,
                                     String ifaceName, boolean typeBased) {

        if (typeBased) {
            for (BigInteger dpId : dpnId) { //install flows per dpn
                FlowBasedServicesUtils.removeTypeBasedEgressDispatcherFlows(dpId, ifaceName, transaction,
                        currentServiceIndex);
            }
        } else {
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpnId.iterator().next(), ifaceName, boundServicesOld,
                    transaction, currentServiceIndex);
        }
    }

    protected void removeFlowsIngress(Set<BigInteger> dpnId, BoundServices boundServicesOld,
                                      WriteTransaction transaction, short currentServiceIndex,
                                      String ifaceName, boolean typeBased) {

        if (typeBased) {
            for (BigInteger dpId : dpnId) { //install flows per dpn
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceName, boundServicesOld, transaction,
                        currentServiceIndex);
            }
        } else {
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpnId.iterator().next(), ifaceName, boundServicesOld,
                    transaction,
                    currentServiceIndex);
        }
    }
}


