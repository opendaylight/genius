/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
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

public class FlowBasedServicesStateUnbindHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedServicesStateUnbindHelper.class);

    public static List<ListenableFuture<Void>> unbindServicesFromInterface(Interface ifaceState,
                                                                           DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(ifaceState.getName(), dataBroker);
        if (servicesInfo == null) {
            return futures;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices == null || allServices.isEmpty()) {
            return futures;
        }

        if (ifaceState.getType().isAssignableFrom(L2vlan.class)) {
            return unbindServiceOnVlan(allServices, ifaceState, ifaceState.getIfIndex(), dataBroker);
        } else if (ifaceState.getType().isAssignableFrom(Tunnel.class)){
             return unbindServiceOnTunnel(allServices, ifaceState, ifaceState.getIfIndex(), dataBroker);
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> unbindServiceOnTunnel(
            List<BoundServices> allServices,
            Interface iface,
            Integer ifIndex, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();

        List<String> ofportIds = iface.getLowerLayerIf();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
        if(nodeConnectorId == null){
            return futures;
        }
        BoundServices highestPriorityBoundService = FlowBasedServicesUtils.getHighestPriorityService(allServices);

        BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
        FlowBasedServicesUtils.removeIngressFlow(iface.getName(), highestPriorityBoundService, dpId, t);

        for (BoundServices boundService : allServices) {
            if (!boundService.equals(highestPriorityBoundService)) {
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, iface.getName(), boundService, t, boundService.getServicePriority());
            }
        }

        futures.add(t.submit());
        return futures;
    }

    private static List<ListenableFuture<Void>> unbindServiceOnVlan(
            List<BoundServices> allServices, Interface ifaceState,
            Integer ifIndex, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        List<String> ofportIds = ifaceState.getLowerLayerIf();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
        if(nodeConnectorId == null){
            return futures;
        }
        BigInteger dpId = new BigInteger(IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId));
        Collections.sort(allServices, new Comparator<BoundServices>() {
            @Override
            public int compare(BoundServices serviceInfo1, BoundServices serviceInfo2) {
                return serviceInfo1.getServicePriority().compareTo(serviceInfo2.getServicePriority());
            }
        });
        BoundServices highestPriority = allServices.remove(0);
        FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), highestPriority, t, IfmConstants.DEFAULT_SERVICE_INDEX);
        for (BoundServices boundService : allServices) {
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), boundService, t, boundService.getServicePriority());
        }
        futures.add(t.submit());
        return futures;

    }

}
