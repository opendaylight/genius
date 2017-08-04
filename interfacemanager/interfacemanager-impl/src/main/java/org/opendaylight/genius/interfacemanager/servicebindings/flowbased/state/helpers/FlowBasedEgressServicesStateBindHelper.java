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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedEgressServicesStateBindHelper implements FlowBasedServicesStateAddable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateBindHelper.class);

    private final InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesStateAddable flowBasedServicesStateAddable;

    private FlowBasedEgressServicesStateBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedEgressServicesStateBindHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedServicesStateAddable == null) {
            synchronized (FlowBasedEgressServicesStateBindHelper.class) {
                if (flowBasedServicesStateAddable == null) {
                    flowBasedServicesStateAddable = new FlowBasedEgressServicesStateBindHelper(interfaceMgrProvider);
                }
            }
        }
    }

    public static FlowBasedServicesStateAddable getFlowBasedEgressServicesStateBindHelper() {
        if (flowBasedServicesStateAddable == null) {
            LOG.error("OvsInterfaceConfigAdd Renderer is not initialized");
        }
        return flowBasedServicesStateAddable;
    }

    @Override
    public List<ListenableFuture<Void>> bindServicesOnInterface(List<ListenableFuture<Void>> futures,
                                                                Interface ifaceState, List<BoundServices> allServices) {
        LOG.debug("binding services on interface {}", ifaceState.getName());
        if (L2vlan.class.equals(ifaceState.getType()) || Tunnel.class.equals(ifaceState.getType())) {
            return bindServices(futures, allServices, ifaceState, interfaceMgrProvider.getDataBroker());
        }
        return null;
    }

    private static List<ListenableFuture<Void>> bindServices(List<ListenableFuture<Void>> futures,
                                                             List<BoundServices> allServices, Interface ifState,
                                                             DataBroker dataBroker) {
        LOG.info("bind all egress services for interface: {}", ifState.getName());

        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        Collections.sort(allServices, (serviceInfo1, serviceInfo2) -> serviceInfo1.getServicePriority()
                .compareTo(serviceInfo2.getServicePriority()));
        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority()
                : highestPriority.getServicePriority() + 1);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.Interface iface = InterfaceManagerCommonUtils
                .getInterfaceFromConfigDS(ifState.getName(), dataBroker);
        FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, highestPriority, ifState.getName(), writeTransaction,
                ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex, iface);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev != null) {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), writeTransaction,
                        ifState.getIfIndex(), prev.getServicePriority(), boundService.getServicePriority(), iface);
            }
            prev = boundService;
        }
        if (prev != null) {
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), writeTransaction,
                    ifState.getIfIndex(),
                    prev.getServicePriority(), (short) (prev.getServicePriority() + 1), iface);
        }
        futures.add(writeTransaction.submit());
        return futures;
    }
}
