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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedIngressServicesConfigUnbindHelper extends AbstractFlowBasedServicesConfigUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesConfigUnbindHelper.class);

    private static volatile FlowBasedServicesConfigRemovable flowBasedIngressServicesRemovable;

    private FlowBasedIngressServicesConfigUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        super(interfaceMgrProvider);

    }

    public static void intitializeFlowBasedIngressServicesConfigRemoveHelper(
            InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedIngressServicesRemovable == null) {
            synchronized (FlowBasedIngressServicesConfigUnbindHelper.class) {
                if (flowBasedIngressServicesRemovable == null) {
                    flowBasedIngressServicesRemovable = new FlowBasedIngressServicesConfigUnbindHelper(
                            interfaceMgrProvider);
                }
            }
        }
    }

    public static FlowBasedServicesConfigRemovable getFlowBasedIngressServicesRemoveHelper() {
        if (flowBasedIngressServicesRemovable == null) {
            LOG.error("FlowBasedIngressBindHelper is not initialized");
        }
        return flowBasedIngressServicesRemovable;
    }

    public static void clearFlowBasedIngressServicesConfigUnbindHelper() {
        flowBasedIngressServicesRemovable = null;
    }

    protected List<ListenableFuture<Void>> unbindServiceOnVlan(BoundServices boundServiceOld,
                                                               List<BoundServices> boundServices,
                                                               org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                                                       .ietf.interfaces.rev140508.interfaces.state
                                                                       .Interface ifaceState,
                                                               DataBroker dataBroker) {

        LOG.info("unbinding ingress service {} for vlan port: {}", boundServiceOld.getServiceName(),
                ifaceState.getName());
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        BigInteger dpId = FlowBasedServicesUtils.getDpnIdFromInterface(ifaceState);
        if (boundServices.isEmpty()) {
            // Remove default entry from Lport Dispatcher Table.
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), boundServiceOld, tx,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            if (tx != null) {
                futures.add(tx.submit());
            }
            return futures;
        }
        BoundServices[] highLow = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, boundServiceOld);
        BoundServices low = highLow[0];
        BoundServices high = highLow[1];
        // This means the one removed was the highest priority service
        if (high == null) {
            LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), boundServiceOld, tx,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            if (low != null) {
                // delete the lower services flow entry.
                LOG.trace("Deleting ingress dispatcher table entry for lower service {}, match service index {}", low,
                        low.getServicePriority());
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), low, tx,
                        low.getServicePriority());
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, low)[0];
                short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                        : low.getServicePriority() + 1);
                LOG.trace(
                        "Installing new ingress dispatcher table entry for lower service {}, match service index "
                                + "{}, update service index {}",
                        low, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, low, ifaceState.getName(), tx,
                        ifaceState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
            }
        } else {
            LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                    boundServiceOld.getServicePriority());
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), boundServiceOld, tx,
                    boundServiceOld.getServicePriority());
            short lowerServiceIndex = (short) (low != null ? low.getServicePriority()
                    : boundServiceOld.getServicePriority() + 1);
            BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(boundServices);
            if (high.equals(highest)) {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                        high, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, ifaceState.getName(), tx,
                        ifaceState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
            } else {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                        high, high.getServicePriority(), lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, ifaceState.getName(), tx,
                        ifaceState.getIfIndex(), high.getServicePriority(), lowerServiceIndex);
            }
        }
        futures.add(tx.submit());
        return futures;
    }

    protected List<ListenableFuture<Void>> unbindServiceOnTunnel(BoundServices boundServiceOld,
            List<BoundServices> boundServices,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState,
            DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);

        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);

        if (boundServices.isEmpty()) {
            // Remove entry from Ingress Table.
            FlowBasedServicesUtils.removeIngressFlow(ifState.getName(), boundServiceOld, dpId, tx);
            if (tx != null) {
                futures.add(tx.submit());
            }
            return futures;
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
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifState.getName(), boundServiceOld, tx,
                    boundServiceOld.getServicePriority());
            if (tx != null) {
                futures.add(tx.submit());
            }
            return futures;
        }

        List<MatchInfo> matches;
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);

        BoundServices toBeMoved = tmpServicesMap.get(highestPriority);
        Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(ifState.getName(), dataBroker);
        FlowBasedServicesUtils.removeIngressFlow(iface.getName(), boundServiceOld, dpId, tx);
        FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, toBeMoved, tx, matches, ifState.getIfIndex(),
                NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
        FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, iface.getName(), toBeMoved, tx,
                toBeMoved.getServicePriority());

        if (tx != null) {
            futures.add(tx.submit());
        }
        return futures;
    }

    @Override
    protected List<ListenableFuture<Void>> unbindServiceOnInterfaceType(BoundServices boundServiceNew,
                                                                        List<BoundServices> allServices,
                                                                        DataBroker dataBroker) {
        //TODO
        return null;
    }
}
