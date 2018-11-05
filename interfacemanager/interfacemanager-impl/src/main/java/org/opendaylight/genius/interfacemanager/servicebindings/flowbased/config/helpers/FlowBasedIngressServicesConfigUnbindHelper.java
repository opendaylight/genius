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
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedIngressServicesConfigUnbindHelper extends AbstractFlowBasedServicesConfigUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesConfigUnbindHelper.class);

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedIngressServicesConfigUnbindHelper(final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    @Override
    protected void unbindServiceOnInterface(List<ListenableFuture<Void>> futures, BoundServices boundServiceOld,
                                            List<BoundServices> boundServices, BoundServicesState boundServicesState) {
        // Split based on type of interface....
        if (L2vlan.class.equals(boundServicesState.getInterfaceType())) {
            unbindServiceOnVlan(futures, boundServiceOld, boundServices, boundServicesState);
        } else if (Tunnel.class.equals(boundServicesState.getInterfaceType())) {
            unbindServiceOnTunnel(futures, boundServiceOld, boundServices, boundServicesState);
        }
    }

    private void unbindServiceOnVlan(List<ListenableFuture<Void>> futures, BoundServices boundServiceOld,
                                            List<BoundServices> boundServices, BoundServicesState boundServicesState) {
        LOG.info("unbinding ingress service {} for vlan port: {}", boundServiceOld.getServiceName(),
                boundServicesState.getInterfaceName());
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            BigInteger dpId = boundServicesState.getDpid();
            if (boundServices == null || boundServices.isEmpty()) {
                // Remove default entry from Lport Dispatcher Table.
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                        boundServiceOld, tx, NwConstants.DEFAULT_SERVICE_INDEX);
                return;
            }
            BoundServices[] highLow =
                    FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, boundServiceOld);
            BoundServices low = highLow[0];
            BoundServices high = highLow[1];
            // This means the one removed was the highest priority service
            if (high == null) {
                LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}",
                        boundServiceOld, NwConstants.DEFAULT_SERVICE_INDEX);
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                        boundServiceOld, tx, NwConstants.DEFAULT_SERVICE_INDEX);
                if (low != null) {
                    // delete the lower services flow entry.
                    LOG.trace("Deleting ingress dispatcher table entry for lower service {}, match service index {}",
                            low, low.getServicePriority());
                    FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(), low,
                            tx, low.getServicePriority());
                    BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, low)[0];
                    short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                            : low.getServicePriority() + 1);
                    LOG.trace("Installing new ingress dispatcher table entry for lower service {}, match service index "
                                    + "{}, update service index {}",
                            low, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                    FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, low, boundServicesState.getInterfaceName(),
                            tx, boundServicesState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                }
            } else {
                LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}",
                        boundServiceOld, boundServiceOld.getServicePriority());
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                        boundServiceOld, tx, boundServiceOld.getServicePriority());
                short lowerServiceIndex = (short) (low != null ? low.getServicePriority()
                        : boundServiceOld.getServicePriority() + 1);
                BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(boundServices);
                if (high.equals(highest)) {
                    LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                            high, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                    FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServicesState.getInterfaceName(),
                            tx, boundServicesState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                } else {
                    LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                            high, high.getServicePriority(), lowerServiceIndex);
                    FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServicesState.getInterfaceName(),
                            tx, boundServicesState.getIfIndex(), high.getServicePriority(), lowerServiceIndex);
                }
            }
        }));
    }

    private void unbindServiceOnTunnel(List<ListenableFuture<Void>> futures, BoundServices boundServiceOld,
                                       List<BoundServices> boundServices, BoundServicesState boundServicesState) {
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            BigInteger dpId = boundServicesState.getDpid();

            LOG.info("unbinding ingress service {} for tunnel port: {}", boundServiceOld.getServiceName(),
                    boundServicesState.getInterfaceName());

            if (boundServices.isEmpty()) {
                // Remove entry from Ingress Table.
                FlowBasedServicesUtils.removeIngressFlow(boundServicesState.getInterfaceName(), boundServiceOld, dpId,
                        tx);
                return;
            }

            Map<Short, BoundServices> tmpServicesMap = new ConcurrentHashMap<>();
            short highestPriority = 0xFF;
            for (BoundServices boundService : boundServices) {
                tmpServicesMap.put(boundService.getServicePriority(), boundService);
                if (boundService.getServicePriority() < highestPriority) {
                    highestPriority = boundService.getServicePriority();
                }
            }

            if (highestPriority < boundServiceOld.getServicePriority()) {
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                        boundServiceOld, tx, boundServiceOld.getServicePriority());
                return;
            }

            List<MatchInfo> matches;
            long portNo = boundServicesState.getPortNo();
            matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);

            BoundServices toBeMoved = tmpServicesMap.get(highestPriority);
            Interface iface =
                    interfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServicesState.getInterfaceName());
            FlowBasedServicesUtils.removeIngressFlow(iface.getName(), boundServiceOld, dpId, tx);
            FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, toBeMoved, tx, matches,
                    boundServicesState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, iface.getName(), toBeMoved, tx,
                    toBeMoved.getServicePriority());
        }));
    }

    @Override
    protected void unbindServiceOnInterfaceType(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                                List<BoundServices> allServices) {
        LOG.info("unbindServiceOnInterfaceType Ingress - WIP");
    }
}
