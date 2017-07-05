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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;

/**
 * Created by N Edwin Anthony on 6/19/2017.
 */
public class FlowInstallHelper {

    protected void writeFlowsOnDpnsEgress(WriteTransaction transaction, BoundServices boundServiceNew,
                                          Set<BigInteger> allDpId, String ifaceName, short currentServiceIndex,
                                          short nextServiceIndex) {

        for (BigInteger dpId : allDpId) { //install flows per dpn
            FlowBasedServicesUtils.installTypeBasedEgressDispatcherFlows(dpId, boundServiceNew, transaction,
                    ifaceName, currentServiceIndex, nextServiceIndex);
        }
    }


    protected void writeFlowsOnDpnsIngress(WriteTransaction transaction, BoundServices boundServiceNew,
                                           Set<BigInteger> allDpId, String ifaceName, short currentServiceIndex,
                                           short nextServiceIndex) {

        for (BigInteger dpId : allDpId) { //install flows per dpn
            FlowBasedServicesUtils.installTypeBasedLPortDispatcherFlow(dpId, boundServiceNew, transaction,
                    ifaceName, currentServiceIndex, nextServiceIndex);
        }
    }

    protected void deleteFlowsOnDpnsEgress(Set<BigInteger> allDpId, WriteTransaction transaction, String interfaceType,
                                           short serviceIndex) {

        for (BigInteger dpId : allDpId) {
            FlowBasedServicesUtils.removeTypeBasedEgressDispatcherFlows(dpId, transaction, interfaceType,
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
