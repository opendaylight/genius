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
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedEgressServicesConfigUnbindHelper implements FlowBasedServicesConfigRemovable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesConfigUnbindHelper.class);

    private final InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesConfigRemovable flowBasedEgressServicesRemovable;

    private FlowBasedEgressServicesConfigUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedEgressServicesConfigRemoveHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedEgressServicesRemovable == null) {
            synchronized (FlowBasedEgressServicesConfigUnbindHelper.class) {
                if (flowBasedEgressServicesRemovable == null) {
                    flowBasedEgressServicesRemovable = new FlowBasedEgressServicesConfigUnbindHelper(
                            interfaceMgrProvider);
                }
            }
        }
    }

    public static FlowBasedServicesConfigRemovable getFlowBasedEgressServicesRemoveHelper() {
        if (flowBasedEgressServicesRemovable == null) {
            LOG.error("FlowBasedIngressBindHelper`` is not initialized");
        }
        return flowBasedEgressServicesRemovable;
    }

    @Override
    public void unbindService(List<ListenableFuture<Void>> futures,
                              String interfaceName, BoundServices boundServiceOld,
                              List<BoundServices> boundServices,
                              BoundServicesState boundServicesState) {
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        if (L2vlan.class.equals(boundServicesState.getInterfaceType())
            || Tunnel.class.equals(boundServicesState.getInterfaceType())) {
            unbindService(futures, boundServiceOld, boundServices, boundServicesState, dataBroker);
        }
    }

    private static void unbindService(List<ListenableFuture<Void>> futures,
                                      BoundServices boundServiceOld,
                                      List<BoundServices> boundServices,
                                      BoundServicesState boundServicesState,
                                      DataBroker dataBroker) {
        LOG.info("unbinding egress service {} for interface: {}", boundServiceOld.getServiceName(), boundServicesState
            .getInterfaceName());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServicesState.getInterfaceName(),
            dataBroker);
        BigInteger dpId = boundServicesState.getDpid();
        if (boundServices.isEmpty()) {
            // Remove default entry from Lport Dispatcher Table.
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(),
                boundServiceOld, tx, NwConstants.DEFAULT_SERVICE_INDEX);
            if (tx != null) {
                futures.add(tx.submit());
            }
            return;
        }
        BoundServices[] highLow = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, boundServiceOld);
        BoundServices low = highLow[0];
        BoundServices high = highLow[1];
        // This means the one removed was the highest priority service
        if (high == null) {
            LOG.trace("Deleting egress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                NwConstants.DEFAULT_SERVICE_INDEX);
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(),
                boundServiceOld, tx, NwConstants.DEFAULT_SERVICE_INDEX);
            if (low != null) {
                //delete the lower services flow entry.
                LOG.trace("Deleting egress dispatcher table entry for lower service {}, match service index {}", low,
                    low.getServicePriority());
                FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(), low,
                    tx, low.getServicePriority());
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, low)[0];
                short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                        : low.getServicePriority() + 1);
                LOG.trace(
                        "Installing new egress dispatcher table entry for lower service {}, match service index {},"
                                + " update service index {}",
                        low, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, low, boundServicesState.getInterfaceName(),
                    tx, boundServicesState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex, iface);
            }
        } else {
            LOG.trace("Deleting egress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                boundServiceOld.getServicePriority());
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, boundServicesState.getInterfaceName(),
                boundServiceOld, tx, boundServiceOld.getServicePriority());
            short lowerServiceIndex = (short) (low != null ? low.getServicePriority()
                    : boundServiceOld.getServicePriority() + 1);
            BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(boundServices);
            if (high.equals(highest)) {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                    high, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, boundServicesState.getInterfaceName(),
                    tx, boundServicesState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex, iface);
            } else {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                    high, high.getServicePriority(), lowerServiceIndex);
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, high, boundServicesState.getInterfaceName(),
                    tx, boundServicesState.getIfIndex(), high.getServicePriority(), lowerServiceIndex, iface);
            }
        }
        futures.add(tx.submit());
    }

    private static List<ListenableFuture<Void>> unbindServiceOnTunnel(BoundServices boundServiceOld,
            List<BoundServices> boundServices,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
            .Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        // FIXME : not yet supported
        return futures;
    }
}
