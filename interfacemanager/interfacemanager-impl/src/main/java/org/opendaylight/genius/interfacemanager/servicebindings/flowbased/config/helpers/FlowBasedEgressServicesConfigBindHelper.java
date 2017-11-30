/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedEgressServicesConfigBindHelper extends AbstractFlowBasedServicesConfigBindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesConfigBindHelper.class);

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedEgressServicesConfigBindHelper(final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    @Override
    protected void bindServiceOnInterface(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                          List<BoundServices> allServices, BoundServicesState boundServiceState) {
        BigInteger dpId = boundServiceState.getDpid();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            Interface iface =
                    interfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServiceState.getInterfaceName());
            LOG.info("binding egress service {} for interface: {}", boundServiceNew.getServiceName(),
                    boundServiceState.getInterfaceName());
            if (allServices.size() == 1) {
                // calling LportDispatcherTableForService with current service index
                // as 0 and next service index as
                // some value since this is the only service bound.
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew,
                        boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(),
                        NwConstants.DEFAULT_SERVICE_INDEX, (short) (boundServiceNew.getServicePriority() + 1), iface);
                return;
            }
            allServices.remove(boundServiceNew);
            BoundServices[] highLowPriorityService = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices,
                    boundServiceNew);
            BoundServices low = highLowPriorityService[0];
            BoundServices high = highLowPriorityService[1];
            BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(allServices);
            short currentServiceIndex = NwConstants.DEFAULT_SERVICE_INDEX;
            short nextServiceIndex = ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME,
                    NwConstants.DEFAULT_EGRESS_SERVICE_INDEX); // dummy service
            // index
            if (low != null) {
                nextServiceIndex = low.getServicePriority();
                if (low.equals(highest)) {
                    // In this case the match criteria of existing service should be
                    // changed.
                    BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, low)[0];
                    short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                            : low.getServicePriority() + 1);
                    LOG.trace("Installing egress dispatcher table entry for existing service {} service match on "
                                    + "service index {} update with service index {}",
                            low, low.getServicePriority(), lowerServiceIndex);
                    FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, low, boundServiceState.getInterfaceName(),
                            tx, boundServiceState.getIfIndex(), low.getServicePriority(), lowerServiceIndex, iface);
                } else {
                    currentServiceIndex = boundServiceNew.getServicePriority();
                }
            }
            if (high != null) {
                currentServiceIndex = boundServiceNew.getServicePriority();
                if (high.equals(highest)) {
                    LOG.trace("Installing egress dispatcher table entry for existing service {} service match on "
                                    + "service index {} update with service index {}",
                            high, NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
                    FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high,
                            boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(),
                            NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex, iface);
                } else {
                    LOG.trace("Installing egress dispatcher table entry for existing service {} service match on "
                                    + "service index {} update with service index {}",
                            high, high.getServicePriority(), currentServiceIndex);
                    FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high,
                            boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(),
                            high.getServicePriority(), currentServiceIndex, iface);
                }
            }
            LOG.trace("Installing egress dispatcher table entry "
                            + "for new service match on service index {} update with service index {}",
                    currentServiceIndex, nextServiceIndex);
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew,
                    boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(), currentServiceIndex,
                    nextServiceIndex, iface);
        }));
    }

    @Override
    protected void bindServiceOnInterfaceType(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                              List<BoundServices> allServices) {
        LOG.info("Tunnel Type based egress service binding - WIP");
    }
}
