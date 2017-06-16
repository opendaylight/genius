/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import java.math.BigInteger;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;

public class AbstractFlowBasedServicesConfigInstallHelper {

    protected void writeFlowsEgress(Set<BigInteger> dpnId, BoundServices boundServiceNew, WriteTransaction transaction,
                                    BoundServicesState boundServiceState, short currentServiceIndex,
                                    short nextServiceIndex, Interface iface, String ifaceName, boolean typeBased) {

        if (typeBased) {
            for (BigInteger dpId : dpnId) { //install flows per dpn
                FlowBasedServicesUtils.installTypeBasedEgressDispatcherFlows(dpId, boundServiceNew, transaction,
                        ifaceName, currentServiceIndex, nextServiceIndex);
            }
        } else {
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpnId.iterator().next(), boundServiceNew,
                    boundServiceState.getInterfaceName(), transaction, boundServiceState.getIfIndex(),
                    currentServiceIndex, nextServiceIndex, iface);
        }
    }

    protected void writeFlowsIngress(Set<BigInteger> dpnId, BoundServices boundServiceNew, WriteTransaction transaction,
                                     BoundServicesState boundServiceState, short currentServiceIndex,
                                     short nextServiceIndex, boolean typeBased) {

        if (typeBased) {
            for (BigInteger dpId : dpnId) { //install flows per dpn
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew,
                        boundServiceState.getInterfaceName(), transaction, boundServiceState.getIfIndex(),
                        currentServiceIndex, nextServiceIndex);
            }
        } else {
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpnId.iterator().next(), boundServiceNew,
                    boundServiceState.getInterfaceName(), transaction, boundServiceState.getIfIndex(),
                    currentServiceIndex, nextServiceIndex);
        }
    }

}
