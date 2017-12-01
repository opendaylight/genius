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
import java.util.Comparator;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
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

public class FlowBasedEgressServicesStateUnbindHelper implements FlowBasedServicesStateRemovable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateUnbindHelper.class);

    private final InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

    private FlowBasedEgressServicesStateUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedEgressServicesStateUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedServicesStateRemovable == null) {
            synchronized (FlowBasedEgressServicesStateUnbindHelper.class) {
                if (flowBasedServicesStateRemovable == null) {
                    flowBasedServicesStateRemovable = new FlowBasedEgressServicesStateUnbindHelper(
                            interfaceMgrProvider);
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

    @Override
    public List<ListenableFuture<Void>> unbindServicesFromInterface(Interface ifaceState) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        LOG.debug("unbinding services on interface {}", ifaceState.getName());

        DataBroker dataBroker = interfaceMgrProvider.getDataBroker();
        ManagedNewTransactionRunner txRunner = interfaceMgrProvider.getTransactionRunner();
        ServicesInfo servicesInfo = FlowBasedServicesUtils.getServicesInfoForInterface(ifaceState.getName(),
                ServiceModeEgress.class, dataBroker);
        if (servicesInfo == null) {
            LOG.trace("service info is null for interface {}", ifaceState.getName());
            return futures;
        }

        List<BoundServices> allServices = servicesInfo.getBoundServices();
        if (allServices == null || allServices.isEmpty()) {
            LOG.trace("bound services is empty for interface {}", ifaceState.getName());
            return futures;
        }

        if (L2vlan.class.equals(ifaceState.getType()) || Tunnel.class.equals(ifaceState.getType())) {
            return unbindServices(allServices, ifaceState, txRunner);
        }
        return futures;
    }

    private static List<ListenableFuture<Void>> unbindServices(List<BoundServices> allServices, Interface ifaceState,
            ManagedNewTransactionRunner txRunner) {
        LOG.info("unbinding all egress services on interface: {}", ifaceState.getName());
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            List<String> ofportIds = ifaceState.getLowerLayerIf();
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
            BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            allServices.sort(Comparator.comparing(BoundServices::getServicePriority));
            BoundServices highestPriority = allServices.remove(0);
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(), highestPriority,
                    tx, NwConstants.DEFAULT_SERVICE_INDEX);
            for (BoundServices boundService : allServices) {
                FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(), boundService,
                        tx, boundService.getServicePriority());
            }
            // remove the default egress service bound on the interface, once all
            // flows are removed
            FlowBasedServicesUtils.unbindDefaultEgressDispatcherService(txRunner, ifaceState.getName());
        }));
        return futures;

    }
}
