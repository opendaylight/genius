/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedIngressServicesConfigBindHelper extends AbstractFlowBasedServicesConfigBindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesConfigBindHelper.class);

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedIngressServicesConfigBindHelper(@Reference final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    @Override
    protected void bindServiceOnInterface(List<ListenableFuture<Void>> futures,BoundServices boundServiceNew,
                                          List<BoundServices> allServices, BoundServicesState boundServiceState) {
        if (allServices.isEmpty()) {
            LOG.error("Reached Impossible part 1 in the code during bind service for: {}", boundServiceNew);
            return;
        }
        // Split based on type of interface...
        if (L2vlan.class.equals(boundServiceState.getInterfaceType())) {
            bindServiceOnVlan(futures, boundServiceNew, allServices, boundServiceState);
        } else if (Tunnel.class.equals(boundServiceState.getInterfaceType())) {
            bindServiceOnTunnel(futures, boundServiceNew, allServices, boundServiceState);
        }
    }

    private void bindServiceOnTunnel(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                     List<BoundServices> allServices, BoundServicesState boundServiceState) {
        long portNo = boundServiceState.getPortNo().toJava();
        Uint64 dpId = boundServiceState.getDpid();
        LOG.info("binding ingress service {} for tunnel port: {}", boundServiceNew.getServiceName(),
                boundServiceState.getInterfaceName());
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            Interface iface =
                    interfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServiceState.getInterfaceName());
            if (allServices.size() == 1) {
                // If only one service present, install instructions in table 0.
                List<MatchInfo> matches =  FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);
                FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, boundServiceNew, tx, matches,
                        boundServiceState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
                return;
            }

            boolean isCurrentServiceHighestPriority = true;
            Map<Short, BoundServices> tmpServicesMap = new ConcurrentHashMap<>();
            short highestPriority = 0xFF;
            for (BoundServices boundService : allServices) {
                if (boundService.getServicePriority().toJava() < boundServiceNew.getServicePriority().toJava()) {
                    isCurrentServiceHighestPriority = false;
                    break;
                }
                if (!boundService.equals(boundServiceNew)) {
                    tmpServicesMap.put(boundService.getServicePriority().toJava(), boundService);
                    if (boundService.getServicePriority().toJava() < highestPriority) {
                        highestPriority = boundService.getServicePriority().toJava();
                    }
                }
            }

            if (!isCurrentServiceHighestPriority) {
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew,
                        boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(),
                        boundServiceNew.getServicePriority().toJava(),
                        (short) (boundServiceNew.getServicePriority().toJava() + 1));
            } else {
                BoundServices serviceToReplace = tmpServicesMap.get(highestPriority);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, serviceToReplace,
                        boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(),
                        serviceToReplace.getServicePriority().toJava(),
                        (short) (serviceToReplace.getServicePriority().toJava() + 1));
                List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);

                // Separate transactions to remove and install flows
                // TODO skitt Should these be sequential?
                futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                    removeFlowTx -> FlowBasedServicesUtils.removeIngressFlow(iface.getName(), serviceToReplace,
                            dpId, removeFlowTx)));
                futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                    installFlowTransaction -> FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface,
                            boundServiceNew, installFlowTransaction, matches, boundServiceState.getIfIndex(),
                            NwConstants.VLAN_INTERFACE_INGRESS_TABLE)));
            }
        }));
    }

    private void bindServiceOnVlan(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                   List<BoundServices> allServices, BoundServicesState boundServiceState) {
        Uint64 dpId = boundServiceState.getDpid();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            LOG.info("binding ingress service {} for vlan port: {}", boundServiceNew.getServiceName(), boundServiceState
                    .getInterfaceName());
            if (allServices.size() == 1) {
                // calling LportDispatcherTableForService with current service index
                // as 0 and next service index as some value since this is the only
                // service bound.
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew,
                        boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(),
                        NwConstants.DEFAULT_SERVICE_INDEX, (short) (boundServiceNew.getServicePriority().toJava() + 1));
                return;
            }
            allServices.remove(boundServiceNew);
            BoundServices[] highLowPriorityService = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices,
                    boundServiceNew);
            BoundServices low = highLowPriorityService[0];
            BoundServices high = highLowPriorityService[1];
            BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(allServices);
            short currentServiceIndex = NwConstants.DEFAULT_SERVICE_INDEX;
            short nextServiceIndex = (short) (boundServiceNew.getServicePriority().toJava() + 1); // dummy
            // service
            // index
            if (low != null) {
                nextServiceIndex = low.getServicePriority().toJava();
                if (low.equals(highest)) {
                    // In this case the match criteria of existing service should be
                    // changed.
                    BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, low)[0];
                    short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority().toJava()
                            : low.getServicePriority().toJava() + 1);
                    LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                    + "service index {} update with service index {}",
                            low, low.getServicePriority(), lowerServiceIndex);
                    FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, low, boundServiceState.getInterfaceName(),
                            tx, boundServiceState.getIfIndex(), low.getServicePriority().toJava(), lowerServiceIndex);
                } else {
                    currentServiceIndex = boundServiceNew.getServicePriority().toJava();
                }
            }
            if (high != null) {
                currentServiceIndex = boundServiceNew.getServicePriority().toJava();
                if (high.equals(highest)) {
                    LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                    + "service index {} update with service index {}",
                            high, NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
                    FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServiceState.getInterfaceName(),
                            tx, boundServiceState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX,
                            currentServiceIndex);
                } else {
                    LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                    + "service index {} update with service index {}",
                            high, high.getServicePriority().toJava(), currentServiceIndex);
                    FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServiceState.getInterfaceName(),
                            tx, boundServiceState.getIfIndex(), high.getServicePriority().toJava(),
                            currentServiceIndex);
                }
            }
            LOG.trace("Installing ingress dispatcher table entry for new service match on service index {} update with "
                    + "service index {}", currentServiceIndex, nextServiceIndex);
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew,
                    boundServiceState.getInterfaceName(), tx, boundServiceState.getIfIndex(), currentServiceIndex,
                    nextServiceIndex);
        }));
    }

    @Override
    protected void bindServiceOnInterfaceType(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                              List<BoundServices> allServices) {
        LOG.info("Interface Type based ingress service binding - WIP");
    }
}
