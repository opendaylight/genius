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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
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

    protected List<ListenableFuture<Void>> bindServiceOnInterface(BoundServices boundServiceNew,
                                                                  List<BoundServices> allServices,
                                                                  BoundServicesState boundServiceState,
                                                                  DataBroker dataBroker) {

        List<ListenableFuture<Void>> futures = new ArrayList<>();
        BigInteger dpId = boundServiceState.getDpid();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServiceState.getInterfaceName(),
            dataBroker);
        LOG.info("binding egress service {} for interface: {}", boundServiceNew.getServiceName(),
            boundServiceState.getInterfaceName());
        if (allServices.size() == 1) {
            // calling LportDispatcherTableForService with current service index
            // as 0 and next service index as
            // some value since this is the only service bound.
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew,
                boundServiceState.getInterfaceName(), transaction, boundServiceState.getIfIndex(),
                NwConstants.DEFAULT_SERVICE_INDEX, (short) (boundServiceNew.getServicePriority() + 1), iface);
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return futures;
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
                LOG.trace(
                        "Installing egress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        low, low.getServicePriority(), lowerServiceIndex);
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, low, boundServiceState.getInterfaceName(),
                    transaction, boundServiceState.getIfIndex(), low.getServicePriority(), lowerServiceIndex, iface);
            } else {
                currentServiceIndex = boundServiceNew.getServicePriority();
            }
        }
        if (high != null) {
            currentServiceIndex = boundServiceNew.getServicePriority();
            if (high.equals(highest)) {
                LOG.trace(
                        "Installing egress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, boundServiceState.getInterfaceName(),
                    transaction, boundServiceState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX,
                    currentServiceIndex, iface);
            } else {
                LOG.trace(
                        "Installing egress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, high.getServicePriority(), currentServiceIndex);
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, boundServiceState.getInterfaceName(),
                    transaction, boundServiceState.getIfIndex(), high.getServicePriority(), currentServiceIndex, iface);
            }
        }
        LOG.trace(
                "Installing egress dispatcher table entry "
                + "for new service match on service index {} update with service index {}",
                currentServiceIndex, nextServiceIndex);
        FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, boundServiceNew, boundServiceState.getInterfaceName(),
            transaction, boundServiceState.getIfIndex(), currentServiceIndex, nextServiceIndex, iface);
        futures.add(transaction.submit());
        return futures;
    }

    protected List<ListenableFuture<Void>> bindServiceOnInterfaceType(String ifaceName,
                                                                      BoundServices boundServiceNew,
                                                                      List<BoundServices> allServices,
                                                                      DataBroker dataBroker) {
        LOG.info("type based binding egress service {} for : {}", boundServiceNew.getServiceName(), ifaceName);
        final List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        Set<BigInteger> dpId = InterfaceMetaUtils.getDpnIdsFromBridgeEntryCache(); //get all dpnIDs
        if (allServices.size() == 1) {
            // calling LportDispatcherTableForService with current service index
            // as 0 and next service index as
            // some value since this is the only service bound.
            writeFlowsOnDpnsEgress(transaction, boundServiceNew, dpId, ifaceName, NwConstants.DEFAULT_SERVICE_INDEX,
                    (short) (boundServiceNew.getServicePriority() + 1));
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return futures;
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
                LOG.trace(
                        "Installing egress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        low, low.getServicePriority(), lowerServiceIndex);
                writeFlowsOnDpnsEgress(transaction,low, dpId, ifaceName, low.getServicePriority(),
                        lowerServiceIndex);
            } else {
                currentServiceIndex = boundServiceNew.getServicePriority();
            }
        }
        if (high != null) {
            currentServiceIndex = boundServiceNew.getServicePriority();
            if (high.equals(highest)) {
                LOG.trace(
                        "Installing egress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, NwConstants.DEFAULT_SERVICE_INDEX, currentServiceIndex);
                writeFlowsOnDpnsEgress(transaction, high, dpId, ifaceName, NwConstants.DEFAULT_SERVICE_INDEX,
                        currentServiceIndex);
            } else {
                LOG.trace(
                        "Installing egress dispatcher table entry for existing service {} service match on "
                                + "service index {} update with service index {}",
                        high, high.getServicePriority(), currentServiceIndex);
                writeFlowsOnDpnsEgress(transaction, high, dpId, ifaceName, high.getServicePriority(),
                        currentServiceIndex);
            }
        }
        LOG.trace(
                "Installing egress dispatcher table entry "
                        + "for new service match on service index {} update with service index {}",
                currentServiceIndex, nextServiceIndex);
        writeFlowsOnDpnsEgress(transaction, boundServiceNew, dpId, ifaceName, currentServiceIndex, nextServiceIndex);
        futures.add(transaction.submit());
        return futures;
    }
}
