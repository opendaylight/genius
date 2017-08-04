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
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedIngressServicesStateUnbindHelper extends AbstractFlowBasedServicesStateUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesStateUnbindHelper.class);
    private static volatile FlowBasedServicesStateRemovable flowBasedIngressServicesStateRemovable;

    @Inject
    public FlowBasedIngressServicesStateUnbindHelper(final DataBroker dataBroker) {
        super(dataBroker);
        flowBasedIngressServicesStateRemovable = this;
    }

    public static FlowBasedServicesStateRemovable getFlowBasedIngressServicesStateRemoveHelper() {
        if (flowBasedIngressServicesStateRemovable == null) {
            LOG.error("{} is not initialized", FlowBasedIngressServicesStateUnbindHelper.class.getSimpleName());
        }
        return flowBasedIngressServicesStateRemovable;
    }

    public static void clearFlowBasedIngressServicesStateUnbindHelper() {
        flowBasedIngressServicesStateRemovable = null;
    }

    @Override
    public void unbindServicesFromInterface(List<ListenableFuture<Void>> futures, Interface ifState,
                                            ServicesInfo servicesInfo, List<BoundServices> allServices) {

        LOG.info("unbinding all ingress services for interface type: {}", ifState.getName());
        if (!validate(ifState.getName(), servicesInfo, allServices)) {
            return;
        }
        if (L2vlan.class.equals(ifState.getType())) {
            unbindServicesFromVlan(futures, ifState, allServices);
        } else if (Tunnel.class.equals(ifState.getType())) {
            unbindServicesFromTunnel(futures, ifState, allServices);
        }
    }


    protected void unbindServicesFromTunnel(List<ListenableFuture<Void>> futures, Interface iface,
                                            List<BoundServices> allServices) {

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        LOG.info("unbinding all services on tunnel interface {}", iface.getName());
        List<String> ofportIds = iface.getLowerLayerIf();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
        if (nodeConnectorId == null) {
            return;
        }
        BoundServices highestPriorityBoundService = FlowBasedServicesUtils.getHighestPriorityService(allServices);

        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        FlowBasedServicesUtils.removeIngressFlow(iface.getName(), highestPriorityBoundService, dpId, writeTransaction);

        for (BoundServices boundService : allServices) {
            if (!boundService.equals(highestPriorityBoundService)) {
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, iface.getName(), boundService, writeTransaction,
                        boundService.getServicePriority());
            }
        }
        futures.add(writeTransaction.submit());
    }

    protected void unbindServicesFromVlan(List<ListenableFuture<Void>> futures, Interface ifaceState,
                                          List<BoundServices> allServices) {

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        List<String> ofportIds = ifaceState.getLowerLayerIf();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
        if (nodeConnectorId == null) {
            return;
        }
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        Collections.sort(allServices, Comparator.comparing(BoundServices::getServicePriority));
        BoundServices highestPriority = allServices.remove(0);
        FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), highestPriority, writeTransaction,
                NwConstants.DEFAULT_SERVICE_INDEX);
        for (BoundServices boundService : allServices) {
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), boundService, writeTransaction,
                    boundService.getServicePriority());
        }
        futures.add(writeTransaction.submit());
    }

    @Override
    public void unbindServicesFromInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId,
                                                ServicesInfo servicesInfo, List<BoundServices> allServices) {

        LOG.info("unbinding all ingress services for interface type: {}", servicesInfo.getInterfaceName());
        if (!validate(servicesInfo.getInterfaceName(), servicesInfo, allServices)) {
            return;
        }
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        Collections.sort(allServices, (serviceInfo1, serviceInfo2) -> serviceInfo1.getServicePriority()
                .compareTo(serviceInfo2.getServicePriority()));
        BoundServices highestPriority = allServices.remove(0);
        FlowBasedServicesUtils.removeTypeBasedLPortDispatcherFlow(dpnId, highestPriority, writeTransaction,
                servicesInfo.getInterfaceName(), NwConstants.DEFAULT_SERVICE_INDEX);
        for (BoundServices boundService : allServices) {
            FlowBasedServicesUtils.removeTypeBasedLPortDispatcherFlow(dpnId, boundService, writeTransaction,
                    servicesInfo.getInterfaceName(), boundService.getServicePriority());
        }
        futures.add(writeTransaction.submit());
    }
}
