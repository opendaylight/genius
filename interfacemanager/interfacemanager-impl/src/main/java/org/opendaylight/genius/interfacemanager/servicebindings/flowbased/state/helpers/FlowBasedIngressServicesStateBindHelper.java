/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedIngressServicesStateBindHelper implements FlowBasedServicesStateAddable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesStateBindHelper.class);

    private final InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesStateAddable flowBasedIngressServicesStateAddable;

    private FlowBasedIngressServicesStateBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedIngressServicesStateAddHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedIngressServicesStateAddable == null) {
            synchronized (FlowBasedIngressServicesStateBindHelper.class) {
                if (flowBasedIngressServicesStateAddable == null) {
                    flowBasedIngressServicesStateAddable = new FlowBasedIngressServicesStateBindHelper(
                            interfaceMgrProvider);
                }
            }
        }
    }

    public static void clearFlowBasedIngressServicesStateAddHelper() {
        flowBasedIngressServicesStateAddable = null;
    }

    public static FlowBasedServicesStateAddable getFlowBasedIngressServicesStateAddHelper() {
        if (flowBasedIngressServicesStateAddable == null) {
            LOG.error("OvsInterfaceConfigAdd Renderer is not initialized");
        }
        return flowBasedIngressServicesStateAddable;
    }

    @Override
    public void bindServicesOnInterface(List<ListenableFuture<Void>> futures,
                                        Interface ifaceState, List<BoundServices> allServices) {
        LOG.debug("binding services on interface {}", ifaceState.getName());
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        if (L2vlan.class.equals(ifaceState.getType())) {
            bindServiceOnVlan(futures, allServices, ifaceState, dataBroker);
        } else if (Tunnel.class.equals(ifaceState.getType())) {
            bindServiceOnTunnel(futures, allServices, ifaceState, dataBroker);
        }
    }

    private static void bindServiceOnTunnel(List<ListenableFuture<Void>> futures,
                                            List<BoundServices> allServices, Interface ifState,
                                            DataBroker dataBroker) {
        LOG.info("bind all ingress services on tunnel interface: {}", ifState.getName());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.Interface iface = InterfaceManagerCommonUtils
                .getInterfaceFromConfigDS(ifState.getName(), dataBroker);
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);
        BoundServices highestPriorityBoundService = FlowBasedServicesUtils.getHighestPriorityService(allServices);
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        if (matches != null) {
            FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, highestPriorityBoundService,
                    writeTransaction, matches, ifState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
        }

        for (BoundServices boundService : allServices) {
            if (!boundService.equals(highestPriorityBoundService)) {
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundService, ifState.getName(),
                        writeTransaction, ifState.getIfIndex(), boundService.getServicePriority(),
                        (short) (boundService.getServicePriority() + 1));
            }
        }

        futures.add(writeTransaction.submit());
    }

    private static void bindServiceOnVlan(List<ListenableFuture<Void>> futures,
                                          List<BoundServices> allServices, Interface ifState,
                                          DataBroker dataBroker) {
        LOG.info("bind all ingress services on vlan interface: {}", ifState.getName());
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        Collections.sort(allServices, (serviceInfo1, serviceInfo2) -> serviceInfo1.getServicePriority()
                .compareTo(serviceInfo2.getServicePriority()));
        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority()
                : highestPriority.getServicePriority() + 1);
        FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, highestPriority, ifState.getName(), writeTransaction,
                ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev != null) {
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, prev, ifState.getName(), writeTransaction,
                        ifState.getIfIndex(), prev.getServicePriority(), boundService.getServicePriority());
            }
            prev = boundService;
        }
        if (prev != null) {
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, prev, ifState.getName(), writeTransaction,
                    ifState.getIfIndex(), prev.getServicePriority(), (short) (prev.getServicePriority() + 1));
        }
        futures.add(writeTransaction.submit());
    }
}
