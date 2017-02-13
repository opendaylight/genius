/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedEgressServicesConfigBindHelper extends AbstractFlowBasedServicesConfigBindHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesConfigBindHelper.class);
    private static volatile FlowBasedServicesConfigAddable flowBasedEgressServicesAddable;

    private FlowBasedEgressServicesConfigBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        super(interfaceMgrProvider);
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

    protected List<ListenableFuture<Void>> bindServiceOnTunnel(BoundServices boundServiceNew, List<BoundServices> allServices,
                                                             org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState, DataBroker dataBroker) {
        LOG.info("bind service on egress tunnel interface - WIP");
        return null;
    }

    protected List<ListenableFuture<Void>> bindServiceOnVlan(BoundServices boundServiceNew, List<BoundServices> allServices,
                                                           org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        BigInteger dpId = FlowBasedServicesUtils.getDpnIdFromInterface(ifState);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(ifState.getName(), dataBroker);
        LOG.info("binding egress service for vlan port: {}", ifState.getName());
        if (allServices.size() == 1) {
            //calling LportDispatcherTableForService with current service index as 0 and next service index as
            // some value since this is the only service bound.
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew, ifState.getName(),
                    transaction, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, (short) (boundServiceNew.getServicePriority() + 1), iface);
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
                        low.getServicePriority(), lowerServiceIndex, iface);
            } else {
                currentServiceIndex = boundServiceNew.getServicePriority();
            }
        }
        if (high != null) {
            currentServiceIndex = boundServiceNew.getServicePriority();
            if (high.equals(highest)) {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, ifState.getName(), transaction, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex, iface);
            } else {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, ifState.getName(), transaction, ifState.getIfIndex(), high.getServicePriority(), currentServiceIndex, iface);
            }
        }
        FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew, ifState.getName(), transaction, ifState.getIfIndex(), currentServiceIndex, nextServiceIndex, iface);
        futures.add(transaction.submit());
        return futures;
    }

    protected List<ListenableFuture<Void>> bindServiceOnInterfaceType(BoundServices boundServiceNew,
                                                                 List<BoundServices> allServices,DataBroker dataBroker) {
        LOG.info("Tunnel Type based egress service binding - WIP");
        return null;
    }
}
