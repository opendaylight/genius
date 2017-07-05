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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedIngressServicesConfigBindHelper extends AbstractFlowBasedServicesConfigBindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesConfigBindHelper.class);
    private static volatile FlowBasedServicesConfigAddable flowBasedIngressServicesAddable;

    @Inject
    public FlowBasedIngressServicesConfigBindHelper(final DataBroker dataBroker) {
        super(dataBroker);
        flowBasedIngressServicesAddable = this;
    }

    public static void clearFlowBasedIngressServicesConfigAddHelper() {
        flowBasedIngressServicesAddable = null;
    }

    public static FlowBasedServicesConfigAddable getFlowBasedIngressServicesAddHelper() {
        if (flowBasedIngressServicesAddable == null) {
            LOG.error("{} is not initialized", FlowBasedIngressServicesConfigBindHelper.class.getSimpleName());
        }
        return flowBasedIngressServicesAddable;
    }

    @Override
    protected void bindServiceOnInterface(List<ListenableFuture<Void>> futures,BoundServices boundServiceNew,
                                          List<BoundServices> allServices, BoundServicesState boundServiceState) {

        if (L2vlan.class.equals(boundServiceState.getInterfaceType())) {
            bindServiceOnVlan(futures, boundServiceNew, allServices, boundServiceState);
        } else if (Tunnel.class.equals(boundServiceState.getInterfaceType())) {
            bindServiceOnTunnel(futures, boundServiceNew, allServices, boundServiceState);
        }
    }

    private void bindServiceOnTunnel(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                     List<BoundServices> allServices, BoundServicesState boundServiceState) {
        long portNo = boundServiceState.getPortNo();
        BigInteger dpId = boundServiceState.getDpid();
        LOG.info("binding ingress service {} for tunnel port: {}", boundServiceNew.getServiceName(),
                boundServiceState.getInterfaceName());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServiceState.getInterfaceName(),
            dataBroker);
        if (allServices.size() == 1) {
            // If only one service present, install instructions in table 0.
            List<MatchInfo> matches = null;
            matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);
            FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, boundServiceNew, transaction, matches,
                    boundServiceState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return;
        }

        boolean isCurrentServiceHighestPriority = true;
        Map<Short, BoundServices> tmpServicesMap = new ConcurrentHashMap<>();
        short highestPriority = 0xFF;
        for (BoundServices boundService : allServices) {
            if (boundService.getServicePriority() < boundServiceNew.getServicePriority()) {
                isCurrentServiceHighestPriority = false;
                break;
            }
            if (!boundService.equals(boundServiceNew)) {
                tmpServicesMap.put(boundService.getServicePriority(), boundService);
                if (boundService.getServicePriority() < highestPriority) {
                    highestPriority = boundService.getServicePriority();
                }
            }
        }

        if (!isCurrentServiceHighestPriority) {
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew,
                boundServiceState.getInterfaceName(), transaction, boundServiceState.getIfIndex(),
                boundServiceNew.getServicePriority(), (short) (boundServiceNew.getServicePriority() + 1));
        } else {
            BoundServices serviceToReplace = tmpServicesMap.get(highestPriority);
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, serviceToReplace,
                boundServiceState.getInterfaceName(), transaction, boundServiceState.getIfIndex(),
                serviceToReplace.getServicePriority(), (short) (serviceToReplace.getServicePriority() + 1));
            List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);

            if (matches != null) {
                WriteTransaction removeFlowTransaction = dataBroker.newWriteOnlyTransaction();
                FlowBasedServicesUtils.removeIngressFlow(iface.getName(), serviceToReplace, dpId,
                        removeFlowTransaction);
                futures.add(removeFlowTransaction.submit());

                WriteTransaction installFlowTransaction = dataBroker.newWriteOnlyTransaction();
                FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, boundServiceNew, installFlowTransaction,
                        matches, boundServiceState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
                futures.add(installFlowTransaction.submit());
            }
        }

        if (transaction != null) {
            futures.add(transaction.submit());
        }
    }

    private void bindServiceOnVlan(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                   List<BoundServices> allServices, BoundServicesState boundServiceState) {
        BigInteger dpId = boundServiceState.getDpid();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        LOG.info("binding ingress service {} for vlan port: {}", boundServiceNew.getServiceName(), boundServiceState
            .getInterfaceName());
        if (allServices.size() == 1) {
            // calling LportDispatcherTableForService with current service index
            // as 0 and next service index as some value since this is the only
            // service bound.
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew,
                boundServiceState.getInterfaceName(), transaction, boundServiceState.getIfIndex(),
                NwConstants.DEFAULT_SERVICE_INDEX, (short) (boundServiceNew.getServicePriority() + 1));
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return;
        }
        allServices.remove(boundServiceNew);
        BoundServices[] highLowPriorityService = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices,
                boundServiceNew);
        BoundServices low = highLowPriorityService[0];
        BoundServices high = highLowPriorityService[1];
        BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(allServices);
        short currentServiceIndex = NwConstants.DEFAULT_SERVICE_INDEX;
        short nextServiceIndex = (short) (boundServiceNew.getServicePriority() + 1); // dummy
                                                                                        // service
                                                                                        // index
        if (low != null) {
            nextServiceIndex = low.getServicePriority();
            if (low.equals(highest)) {
                // In this case the match criteria of existing service should be
                // changed.
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, low)[0];
                short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                        : low.getServicePriority() + 1);
                LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        low, low.getServicePriority(), lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, low, boundServiceState.getInterfaceName(),
                    transaction, boundServiceState.getIfIndex(), low.getServicePriority(), lowerServiceIndex);
            } else {
                currentServiceIndex = boundServiceNew.getServicePriority();
            }
        }
        if (high != null) {
            currentServiceIndex = boundServiceNew.getServicePriority();
            if (high.equals(highest)) {
                LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServiceState.getInterfaceName(),
                    transaction, boundServiceState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX,
                    currentServiceIndex);
            } else {
                LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, high.getServicePriority(), currentServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServiceState.getInterfaceName(),
                    transaction, boundServiceState.getIfIndex(), high.getServicePriority(), currentServiceIndex);
            }
        }
        LOG.trace("Installing ingress dispatcher table entry for new service match on service index {} update with "
                + "service index {}", currentServiceIndex, nextServiceIndex);
        FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew, boundServiceState.getInterfaceName(),
            transaction, boundServiceState.getIfIndex(), currentServiceIndex, nextServiceIndex);
        futures.add(transaction.submit());
    }

    @Override
    protected void bindServiceOnInterfaceType(List<ListenableFuture<Void>> futures, String ifaceName,
                                              BoundServices boundServiceNew, List<BoundServices> allServices) {

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        Set<BigInteger> dpId = InterfaceMetaUtils.getDpnIdsFromBridgeEntryCache();
        LOG.info("binding ingress service {} for interfaceType: {}", boundServiceNew.getServiceName(), ifaceName);
        if (allServices.size() == 1) {
            // calling LportDispatcherTableForService with current service index
            // as 0 and next service index as some value since this is the only
            // service bound.
            writeFlowsOnDpnsIngress(transaction, boundServiceNew, dpId, ifaceName, NwConstants.DEFAULT_SERVICE_INDEX,
                    (short) (boundServiceNew.getServicePriority() + 1));
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return;
        }
        allServices.remove(boundServiceNew);
        BoundServices[] highLowPriorityService = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices,
                boundServiceNew);
        BoundServices low = highLowPriorityService[0];
        BoundServices high = highLowPriorityService[1];
        BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(allServices);
        short currentServiceIndex = NwConstants.DEFAULT_SERVICE_INDEX;
        short nextServiceIndex = (short) (boundServiceNew.getServicePriority() + 1); // dummy
        // service
        // index
        if (low != null) {
            nextServiceIndex = low.getServicePriority();
            if (low.equals(highest)) {
                // In this case the match criteria of existing service should be
                // changed.
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, low)[0];
                short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                        : low.getServicePriority() + 1);
                LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        low, low.getServicePriority(), lowerServiceIndex);
                writeFlowsOnDpnsIngress(transaction, low, dpId, ifaceName, low.getServicePriority(),
                        lowerServiceIndex);
            } else {
                currentServiceIndex = boundServiceNew.getServicePriority();
            }
        }
        if (high != null) {
            currentServiceIndex = boundServiceNew.getServicePriority();
            if (high.equals(highest)) {
                LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
                writeFlowsOnDpnsIngress(transaction, high, dpId, ifaceName, NwConstants.DEFAULT_SERVICE_INDEX,
                        currentServiceIndex);

            } else {
                LOG.trace("Installing ingress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, high.getServicePriority(), currentServiceIndex);
                writeFlowsOnDpnsIngress(transaction, high, dpId, ifaceName, high.getServicePriority(),
                        currentServiceIndex);
            }
        }
        LOG.trace("Installing ingress dispatcher table entry for new service match on service index {} update with "
                + "service index {}", currentServiceIndex, nextServiceIndex);
        writeFlowsOnDpnsIngress(transaction, boundServiceNew, dpId, ifaceName, currentServiceIndex, nextServiceIndex);
        futures.add(transaction.submit());
    }
}
