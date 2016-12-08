/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlowBasedIngressServicesConfigBindHelper implements FlowBasedServicesConfigAddable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesConfigBindHelper.class);

    private InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesConfigAddable flowBasedIngressServicesAddable;

    private FlowBasedIngressServicesConfigBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedIngressServicesConfigAddHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedIngressServicesAddable == null) {
            synchronized (FlowBasedIngressServicesConfigBindHelper.class) {
                if (flowBasedIngressServicesAddable == null) {
                    flowBasedIngressServicesAddable = new FlowBasedIngressServicesConfigBindHelper(interfaceMgrProvider);
                }
            }
        }
    }

    public static void clearFlowBasedIngressServicesConfigAddHelper() {
        flowBasedIngressServicesAddable = null;
    }

    public static FlowBasedServicesConfigAddable getFlowBasedIngressServicesAddHelper() {
        if (flowBasedIngressServicesAddable == null) {
            LOG.error("OvsInterfaceConfigAdd Renderer is not initialized");
        }
        return flowBasedIngressServicesAddable;
    }

    public List<ListenableFuture<Void>> bindService(InstanceIdentifier<BoundServices> instanceIdentifier,
                                                           BoundServices boundServiceNew) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        String interfaceName =
                InstanceIdentifier.keyOf(instanceIdentifier.firstIdentifierOf(ServicesInfo.class)).getInterfaceName();
        Class<? extends ServiceModeBase> serviceMode = InstanceIdentifier.keyOf(instanceIdentifier.firstIdentifierOf(ServicesInfo.class)).getServiceMode();

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
        if (ifState == null || ifState.getOperStatus() == OperStatus.Down) {
            LOG.warn("Interface not up, not Binding Service for Interface: {}", interfaceName);
            return futures;
        }

        // Get the Parent ServiceInfo

        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(interfaceName, serviceMode, dataBroker);
        if (servicesInfo == null) {
            LOG.error("Reached Impossible part 1 in the code during bind service for: {}", boundServiceNew);
            return futures;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices.isEmpty()) {
            LOG.error("Reached Impossible part 2 in the code during bind service for: {}", boundServiceNew);
            return futures;
        }
        if(ifState.getType() == null) {
            return futures;
        }
        // Split based on type of interface...
        if (ifState.getType().isAssignableFrom(L2vlan.class)) {
            return bindServiceOnVlan(boundServiceNew, allServices, ifState, dataBroker);
        } else if (ifState.getType().isAssignableFrom(Tunnel.class)) {
            return bindServiceOnTunnel(boundServiceNew, allServices, ifState, dataBroker);
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnTunnel(BoundServices boundServiceNew, List<BoundServices> allServices,
                                                                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(ifState.getName(), dataBroker);
        if (allServices.size() == 1) {
            // If only one service present, install instructions in table 0.
            List<MatchInfo> matches = null;
            matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable (dpId, portNo);
            FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, boundServiceNew,
                    transaction, matches, ifState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return futures;
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
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew, ifState.getName(), transaction,
                    ifState.getIfIndex(), boundServiceNew.getServicePriority(), (short) (boundServiceNew.getServicePriority()+1));
        } else {
            BoundServices serviceToReplace = tmpServicesMap.get(highestPriority);
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, serviceToReplace, ifState.getName(), transaction,
                    ifState.getIfIndex(), serviceToReplace.getServicePriority(), (short) (serviceToReplace.getServicePriority()+1));
            List<MatchInfo> matches = null;
            matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable (dpId, portNo);

            if (matches != null) {

                WriteTransaction removeFlowTransaction = dataBroker.newWriteOnlyTransaction();
                FlowBasedServicesUtils.removeIngressFlow(iface.getName(), serviceToReplace, dpId, removeFlowTransaction);
                futures.add(removeFlowTransaction.submit());

                WriteTransaction installFlowTransaction = dataBroker.newWriteOnlyTransaction();
                FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, boundServiceNew, installFlowTransaction,
                        matches, ifState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
                futures.add(installFlowTransaction.submit());
            }
        }

        if (transaction != null) {
            futures.add(transaction.submit());
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnVlan(BoundServices boundServiceNew, List<BoundServices> allServices,
                                                                  org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        BigInteger dpId = FlowBasedServicesUtils.getDpnIdFromInterface(ifState);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        LOG.info("binding service for vlan port: {}", ifState.getName());
        if (allServices.size() == 1) {
            //calling LportDispatcherTableForService with current service index as 0 and next service index as some value since this is the only service bound.
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew, ifState.getName(),
                    transaction, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX,(short) (boundServiceNew.getServicePriority() + 1));
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return futures;
        }
        allServices.remove(boundServiceNew);
        BoundServices[] highLowPriorityService = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, boundServiceNew);
        BoundServices low = highLowPriorityService[0];
        BoundServices high = highLowPriorityService[1];
        BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(allServices);
        short currentServiceIndex = NwConstants.DEFAULT_SERVICE_INDEX;
        short nextServiceIndex = (short) (boundServiceNew.getServicePriority() + 1); // dummy service index
        if (low != null) {
            nextServiceIndex = low.getServicePriority();
            if (low.equals(highest)) {
                //In this case the match criteria of existing service should be changed.
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, low)[0];
                short lowerServiceIndex = (short) ((lower!=null) ? lower.getServicePriority() : low.getServicePriority() + 1);
                LOG.trace("Installing table 17 entry for existing service {} service match on service index {} update with service index {}", low, low.getServicePriority(), lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId,low, ifState.getName(), transaction, ifState.getIfIndex(),low.getServicePriority(), lowerServiceIndex);
            } else {
                currentServiceIndex = boundServiceNew.getServicePriority();
            }
        }
        if (high != null) {
            currentServiceIndex = boundServiceNew.getServicePriority();
            if (high.equals(highest)) {
                LOG.trace("Installing table 17 entry for existing service {} service match on service index {} update with service index {}", high, NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, ifState.getName(), transaction, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
            } else {
                LOG.trace("Installing table 17 entry for existing service {} service match on service index {} update with service index {}", high, high.getServicePriority(), currentServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, ifState.getName(), transaction, ifState.getIfIndex(), high.getServicePriority(), currentServiceIndex);
            }
        }
        LOG.trace("Installing table 17 entry for new service match on service index {} update with service index {}", currentServiceIndex, nextServiceIndex);
        FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundServiceNew, ifState.getName(), transaction, ifState.getIfIndex(), currentServiceIndex, nextServiceIndex);
        futures.add(transaction.submit());
        return futures;
    }
}