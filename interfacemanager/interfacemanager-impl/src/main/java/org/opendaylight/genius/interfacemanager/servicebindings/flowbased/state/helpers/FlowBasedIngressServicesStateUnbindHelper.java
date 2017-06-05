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
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedIngressServicesStateUnbindHelper extends AbstractFlowBasedServicesStateUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesStateUnbindHelper.class);

    @Inject
    public FlowBasedIngressServicesStateUnbindHelper(final DataBroker dataBroker) {
        super(dataBroker);
    }

    @Override
    protected void unbindServicesOnInterface(List<ListenableFuture<Void>> futures, List<BoundServices> allServices,
                                             Interface ifState) {
        /*if (L2vlan.class.equals(ifState.getType())) {
            unbindServicesOnVlan(futures, allServices, ifState);
        } else if (Tunnel.class.equals(ifState.getType())) {
            unbindServicesOnTunnel(futures, allServices, ifState);
        }*/
    }

    protected void unbindServicesOnTunnel(List<ListenableFuture<Void>> futures, List<BoundServices> allServices,
                                          Interface iface) {
        futures.add(getTxRunner().callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            LOG.info("unbinding all services on tunnel interface {}", iface.getName());
            List<String> ofportIds = iface.getLowerLayerIf();
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
            BoundServices highestPriorityBoundService = FlowBasedServicesUtils.getHighestPriorityService(allServices);

            BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            FlowBasedServicesUtils.removeIngressFlow(iface.getName(), highestPriorityBoundService, dpId, tx);

            for (BoundServices boundService : allServices) {
                if (!boundService.equals(highestPriorityBoundService)) {
                    FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, iface.getName(), boundService, tx,
                            boundService.getServicePriority());
                }
            }
        }));
    }

    protected void unbindServicesOnVlan(List<ListenableFuture<Void>> futures,
            List<BoundServices> allServices, Interface ifaceState) {
        futures.add(getTxRunner().callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            List<String> ofportIds = ifaceState.getLowerLayerIf();
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
            BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            allServices.sort(Comparator.comparing(BoundServices::getServicePriority));
            BoundServices highestPriority = allServices.remove(0);
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), highestPriority, tx,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            for (BoundServices boundService : allServices) {
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, ifaceState.getName(), boundService, tx,
                        boundService.getServicePriority());
            }
        }));
    }

    @Override
    public void unbindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId,
                                              String ifaceName) {
        LOG.info("unbindServicesFromInterfaceType Ingree - WIP");
    }
}
