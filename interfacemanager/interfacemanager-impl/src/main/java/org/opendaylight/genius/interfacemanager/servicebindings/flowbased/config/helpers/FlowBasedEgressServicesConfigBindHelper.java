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
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class FlowBasedEgressServicesConfigBindHelper implements FlowBasedServicesConfigAddable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesConfigBindHelper.class);

    private InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesConfigAddable flowBasedEgressServicesAddable;

    private FlowBasedEgressServicesConfigBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedEgressServicesConfigAddHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedEgressServicesAddable == null) {
            synchronized (FlowBasedEgressServicesConfigBindHelper.class) {
                if (flowBasedEgressServicesAddable == null) {
                    flowBasedEgressServicesAddable = new FlowBasedEgressServicesConfigBindHelper(interfaceMgrProvider);
                }
            }
        }
    }

    public static FlowBasedServicesConfigAddable getFlowBasedEgressServicesAddHelper() {
        if (flowBasedEgressServicesAddable == null) {
            LOG.error("OvsInterfaceConfigAdd Renderer is not initialized");
        }
        return flowBasedEgressServicesAddable;
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

        // Split based on type of interface....
        if (ifState.getType().isAssignableFrom(L2vlan.class)) {
            return bindServiceOnVlan(boundServiceNew, allServices, ifState, dataBroker);
        } else if (ifState.getType().isAssignableFrom(Tunnel.class)) {
            return bindServiceOnTunnel(boundServiceNew, allServices, ifState, dataBroker);
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnTunnel(BoundServices boundServiceNew, List<BoundServices> allServices,
                                                                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState, DataBroker dataBroker) {
        // TODO - binding egress services on tunnels is not supported currently
        return null;
    }

    private static List<ListenableFuture<Void>> bindServiceOnVlan(BoundServices boundServiceNew, List<BoundServices> allServices,
                                                                  org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        BigInteger dpId = FlowBasedServicesUtils.getDpnIdFromInterface(ifState);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        LOG.info("binding egress service for vlan port: {}", ifState.getName());
        if (allServices.size() == 1) {
            //calling LportDispatcherTableForService with current service index as 0 and next service index as
            // some value since this is the only service bound.
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew, ifState.getName(),
                    transaction, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, (short) (boundServiceNew.getServicePriority() + 1), dataBroker);
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
        short nextServiceIndex = ServiceIndex.getIndex(NwConstants.DEFAULT_EGRESS_SERVICE_NAME, NwConstants.DEFAULT_EGRESS_SERVICE_INDEX); // dummy service index
        if (low != null) {
            nextServiceIndex = low.getServicePriority();
            if (low.equals(highest)) {
                //In this case the match criteria of existing service should be changed.
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, low)[0];
                short lowerServiceIndex = (short) ((lower != null) ? lower.getServicePriority() : low.getServicePriority() + 1);
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, low, ifState.getName(), transaction, ifState.getIfIndex(),
                        low.getServicePriority(), lowerServiceIndex, dataBroker);
            } else {
                currentServiceIndex = boundServiceNew.getServicePriority();
            }
        }
        if (high != null) {
            currentServiceIndex = boundServiceNew.getServicePriority();
            if (high.equals(highest)) {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, ifState.getName(), transaction, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex, dataBroker);
            } else {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, ifState.getName(), transaction, ifState.getIfIndex(), high.getServicePriority(), currentServiceIndex, dataBroker);
            }
        }
        FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew, ifState.getName(), transaction, ifState.getIfIndex(), currentServiceIndex, nextServiceIndex, dataBroker);
        futures.add(transaction.submit());
        return futures;
    }
}
