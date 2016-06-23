/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FlowBasedServicesStateBindHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesStateBindHelper.class);

    public static List<ListenableFuture<Void>> bindServicesOnInterface(Interface ifaceState, Class<? extends ServiceModeBase> serviceMode,
                                                                       DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        LOG.debug("binding services on interface {}", ifaceState.getName());
        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(ifaceState.getName(), serviceMode, dataBroker);
        if (servicesInfo == null) {
            LOG.trace("service info is null for interface {}", ifaceState.getName());
            return futures;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices == null || allServices.isEmpty()) {
            LOG.trace("bound services is empty for interface {}", ifaceState.getName());
            return futures;
        }

        if (ifaceState.getType().isAssignableFrom(L2vlan.class)) {
            return bindServiceOnVlan(allServices, ifaceState, dataBroker);
        } else if (ifaceState.getType().isAssignableFrom(Tunnel.class)){
            return bindServiceOnTunnel(allServices, ifaceState, dataBroker);
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnTunnel(
            List<BoundServices> allServices, Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(ifState.getName(), dataBroker);
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        long portNo = Long.parseLong(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
        BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
        List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable (dpId, portNo);
        BoundServices highestPriorityBoundService = FlowBasedServicesUtils.getHighestPriorityService(allServices);
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        if (matches != null) {
            FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, highestPriorityBoundService,
                    t, matches, ifState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
        }

        for (BoundServices boundService : allServices) {
            if (!boundService.equals(highestPriorityBoundService)) {
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundService, ifState.getName(), t, ifState.getIfIndex(), boundService.getServicePriority(), (short) (boundService.getServicePriority()+1));
            }
        }

        futures.add(t.submit());
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnVlan(
            List<BoundServices> allServices,
            Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        Collections.sort(allServices, new Comparator<BoundServices>() {
            @Override
            public int compare(BoundServices serviceInfo1, BoundServices serviceInfo2) {
                return serviceInfo1.getServicePriority().compareTo(serviceInfo2.getServicePriority());
            }
        });
        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority() : highestPriority.getServicePriority() + 1);
        FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, highestPriority, ifState.getName(), t, ifState.getIfIndex(), IfmConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev!=null) {
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, prev, ifState.getName(), t, ifState.getIfIndex(), prev.getServicePriority(), boundService.getServicePriority());
            }
            prev = boundService;
        }
        if (prev!=null) {
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, prev, ifState.getName(), t, ifState.getIfIndex(), prev.getServicePriority(), (short) (prev.getServicePriority()+1));
        }
        futures.add(t.submit());
        return futures;

    }

}