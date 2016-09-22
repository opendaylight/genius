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
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
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
import java.util.Comparator;
import java.util.List;

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
            return bindServiceOnVlan(allServices, ifaceState, dataBroker);
        } else if (ifaceState.getType().isAssignableFrom(Tunnel.class)){
            return bindServiceOnTunnel(allServices, ifaceState, dataBroker);
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnTunnel(
            List<BoundServices> allServices, Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        // FIXME : not supported yet
        return futures;
    }

    private static List<ListenableFuture<Void>> bindServiceOnVlan(
            List<BoundServices> allServices,
            Interface ifState, DataBroker dataBroker) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        Collections.sort(allServices, new Comparator<BoundServices>() {
            @Override
            public int compare(BoundServices serviceInfo1, BoundServices serviceInfo2) {
                return serviceInfo1.getServicePriority().compareTo(serviceInfo2.getServicePriority());
            }
        });
        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority() : highestPriority.getServicePriority() + 1);
        FlowBasedServicesUtils.installEgressDispatcherFlow(dpId, highestPriority, ifState.getName(), t, ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev!=null) {
                FlowBasedServicesUtils.installEgressDispatcherFlow(dpId, prev, ifState.getName(), t, ifState.getIfIndex(), prev.getServicePriority(), boundService.getServicePriority());
            }
            prev = boundService;
        }
        if (prev!=null) {
            FlowBasedServicesUtils.installEgressDispatcherFlow(dpId, prev, ifState.getName(), t, ifState.getIfIndex(), prev.getServicePriority(), (short) (prev.getServicePriority()+1));
        }
        futures.add(t.submit());
        return futures;

    }

}