/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedEgressServicesConfigUnbindHelper extends AbstractFlowBasedServicesConfigUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesConfigUnbindHelper.class);

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedEgressServicesConfigUnbindHelper(@Reference final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    @Override
    protected void unbindServiceOnInterface(List<ListenableFuture<Void>> futures, BoundServices boundServiceOld,
                                           List<BoundServices> boundServices, BoundServicesState boundServicesState) {
        LOG.info("unbinding egress service {} for interface: {}", boundServiceOld.getServiceName(), boundServicesState
            .getInterfaceName());
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            Interface iface =
                    interfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServicesState.getInterfaceName());
            Uint64 dpId = boundServicesState.getDpid();
            if (boundServices.isEmpty()) {
                // Remove default entry from Lport Dispatcher Table.
                FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(),
                        tx, NwConstants.DEFAULT_SERVICE_INDEX);
                return;
            }
            BoundServices[] highLow =
                    FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, boundServiceOld);
            BoundServices low = highLow[0];
            BoundServices high = highLow[1];
            // This means the one removed was the highest priority service
            if (high == null) {
                LOG.trace("Deleting egress dispatcher table entry for service {}, match service index {}",
                        boundServiceOld, NwConstants.DEFAULT_SERVICE_INDEX);
                FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(),
                        tx, NwConstants.DEFAULT_SERVICE_INDEX);
                if (low != null) {
                    //delete the lower services flow entry.
                    LOG.trace("Deleting egress dispatcher table entry for lower service {}, match service index {}",
                            low, low.getServicePriority());
                    FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(),
                            tx, low.getServicePriority().toJava());
                    BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, low)[0];
                    short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority().toJava()
                            : low.getServicePriority().toJava() + 1);
                    LOG.trace("Installing new egress dispatcher table entry for lower service {}, match service index "
                                    + "{}, update service index {}",
                            low, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                    FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, low,
                            boundServicesState.getInterfaceName(), tx, boundServicesState.getIfIndex(),
                            NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex, iface);
                }
            } else {
                LOG.trace("Deleting egress dispatcher table entry for service {}, match service index {}",
                        boundServiceOld, boundServiceOld.getServicePriority().toJava());
                FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(),
                        tx, boundServiceOld.getServicePriority().toJava());
                short lowerServiceIndex = (short) (low != null ? low.getServicePriority().toJava()
                        : boundServiceOld.getServicePriority().toJava() + 1);
                BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(boundServices);
                if (high.equals(highest)) {
                    LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                            high, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                    FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high,
                            boundServicesState.getInterfaceName(), tx, boundServicesState.getIfIndex(),
                            NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex, iface);
                } else {
                    LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                            high, high.getServicePriority().toJava(), lowerServiceIndex);
                    FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high,
                            boundServicesState.getInterfaceName(), tx, boundServicesState.getIfIndex(),
                            high.getServicePriority().toJava(), lowerServiceIndex, iface);
                }
            }
        }));
    }

    @Override
    protected void unbindServiceOnInterfaceType(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                                List<BoundServices> allServices) {
        LOG.info("Tunnel Type based egress service unbinding - WIP");
    }
}
