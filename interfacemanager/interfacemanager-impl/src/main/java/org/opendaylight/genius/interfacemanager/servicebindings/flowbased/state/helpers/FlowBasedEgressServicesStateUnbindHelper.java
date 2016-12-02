/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowBasedEgressServicesStateUnbindHelper implements FlowBasedServicesStateRemovable{
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateUnbindHelper.class);

    private InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

    private FlowBasedEgressServicesStateUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedEgressServicesStateUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedServicesStateRemovable == null) {
            synchronized (FlowBasedEgressServicesStateUnbindHelper.class) {
                if (flowBasedServicesStateRemovable == null) {
                    flowBasedServicesStateRemovable = new FlowBasedEgressServicesStateUnbindHelper(interfaceMgrProvider);
                }
            }
        }
    }

    public static FlowBasedServicesStateRemovable getFlowBasedEgressServicesStateRemoveHelper() {
        if (flowBasedServicesStateRemovable == null) {
            LOG.error("FlowBasedIngressBindHelper`` is not initialized");
        }
        return flowBasedServicesStateRemovable;
    }
    public List<ListenableFuture<Void>> unbindServicesFromInterface(Interface ifaceState) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if(ifaceState.getType() == null) {
            return futures;
        }
        LOG.debug("unbinding services on interface {}", ifaceState.getName());

        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(ifaceState.getName(), ServiceModeEgress.class, dataBroker);
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
            return unbindServiceOnVlan(allServices, ifaceState, ifaceState.getIfIndex(), dataBroker);
        } else if (ifaceState.getType().isAssignableFrom(Tunnel.class)){
            return unbindServiceOnTunnel(allServices, ifaceState, ifaceState.getIfIndex(), dataBroker);
        }
        // remove the default egress service bound on the interface, once all flows are removed
        FlowBasedServicesUtils.unbindDefaultEgressDispatcherService(dataBroker, ifaceState.getName());
        return futures;
    }

    private static List<ListenableFuture<Void>> unbindServiceOnTunnel(
            List<BoundServices> allServices,
            Interface iface,
            Integer ifIndex, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        //FIXME : not supported yet
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
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        Collections.sort(allServices,
                (serviceInfo1, serviceInfo2) -> serviceInfo1.getServicePriority().compareTo(serviceInfo2.getServicePriority()));
        BoundServices highestPriority = allServices.remove(0);
        FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(), highestPriority, t, NwConstants.DEFAULT_SERVICE_INDEX);
        for (BoundServices boundService : allServices) {
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(), boundService, t, boundService.getServicePriority());
        }
        futures.add(t.submit());
        return futures;

    }

}
