/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowBasedEgressServicesStateBindHelper implements FlowBasedServicesStateAddable{
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateBindHelper.class);

    private InterfacemgrProvider interfaceMgrProvider;
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

    public List<ListenableFuture<Void>> bindServicesOnInterface(Interface ifaceState) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if(ifaceState.getType() == null) {
            return futures;
        }

        LOG.debug("binding services on interface {}", ifaceState.getName());
        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();

        // bind the default egress dispatcher service for this interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface
            = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(ifaceState.getName(), dataBroker);
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifaceState);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);

        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(ifaceState.getName(),
            ServiceModeEgress.class, dataBroker);
        if (servicesInfo != null) {
            List<BoundServices> allServices = servicesInfo.getBoundServices();
            if (allServices != null && !allServices.isEmpty()) {
                if (L2vlan.class.equals(ifaceState.getType())) {
                    futures = bindServiceOnVlan(dpId, allServices, ifaceState, dataBroker, iface);
                } else if (Tunnel.class.equals(ifaceState.getType())) {
                    futures = bindServiceOnTunnel(allServices, ifaceState, dataBroker);
                }
            }else{
                LOG.trace("bound services is empty for interface {}", ifaceState.getName());
            }
        }else{
            LOG.trace("service info is null for interface {}", ifaceState.getName());
        }
        // Bind default Egress Dispatcher Service for vlan interfaces
        if (L2vlan.class.equals(ifaceState.getType())) {
            Long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
            FlowBasedServicesUtils.bindDefaultEgressDispatcherService(futures, dataBroker, iface, Long.toString(portNo), ifaceState.getName(), ifaceState.getIfIndex());
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnTunnel(
            List<BoundServices> allServices, Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        // FIXME : not supported yet
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnVlan(BigInteger dpId, List<BoundServices> allServices,
            Interface ifState, DataBroker dataBroker, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                                      .interfaces.rev140508.interfaces.Interface
                                                                      iface) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        Collections.sort(allServices,
                (serviceInfo1, serviceInfo2) -> serviceInfo1.getServicePriority().compareTo(serviceInfo2.getServicePriority()));

        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority() : highestPriority.getServicePriority() + 1);
        FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, highestPriority, ifState.getName(), t, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex, iface);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev!=null) {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), t, ifState.getIfIndex(), prev.getServicePriority(), boundService.getServicePriority(), iface);
            }
            prev = boundService;
        }
        if (prev!=null) {
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), t, ifState.getIfIndex(), prev.getServicePriority(), (short) (prev.getServicePriority()+1), iface);
        }
        futures.add(t.submit());
        return futures;

    }

}