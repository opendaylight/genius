/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore.Configuration;
import org.opendaylight.mdsal.binding.util.TypedReadWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedIngressServicesStateBindHelper extends AbstractFlowBasedServicesStateBindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesStateBindHelper.class);

    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedIngressServicesStateBindHelper(@Reference final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        super(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    @Override
    public void bindServicesOnInterface(TypedReadWriteTransaction<Configuration> tx, List<BoundServices> allServices,
            Interface ifaceState) {
        LOG.debug("binding services on interface {}", ifaceState.getName());
        if (L2vlan.class.equals(ifaceState.getType())) {
            bindServiceOnVlan(tx, allServices, ifaceState);
        } else if (Tunnel.class.equals(ifaceState.getType())) {
            bindServiceOnTunnel(tx, allServices, ifaceState);
        }
    }

    private void bindServiceOnTunnel(TypedReadWriteTransaction<Configuration> tx, List<BoundServices> allServices,
            Interface ifState) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.Interface iface = interfaceManagerCommonUtils
                .getInterfaceFromConfigDS(ifState.getName());
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        Uint64 dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        List<MatchInfo> matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);
        BoundServices highestPriorityBoundService = FlowBasedServicesUtils.getHighestPriorityService(allServices);
        FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, highestPriorityBoundService,
                tx, matches, ifState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);

        for (BoundServices boundService : allServices) {
            if (!boundService.equals(highestPriorityBoundService)) {
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, boundService, ifState.getName(),
                        tx, ifState.getIfIndex(), boundService.getServicePriority().toJava(),
                        (short) (boundService.getServicePriority().toJava() + 1));
            }
        }
    }

    private void bindServiceOnVlan(TypedReadWriteTransaction<Configuration> tx, List<BoundServices> allServices,
            Interface ifState) {
        LOG.info("bind all ingress services for vlan port: {}", ifState.getName());
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        Uint64 dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        allServices.sort(Comparator.comparing(BoundServices::getServicePriority));
        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority().toJava()
                : highestPriority.getServicePriority().toJava() + 1);
        FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, highestPriority, ifState.getName(), tx,
                ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev != null) {
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, prev, ifState.getName(), tx,
                        ifState.getIfIndex(), prev.getServicePriority().toJava(),
                        boundService.getServicePriority().toJava());
            }
            prev = boundService;
        }
        if (prev != null) {
            FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, prev, ifState.getName(), tx,
                    ifState.getIfIndex(), prev.getServicePriority().toJava(),
                    (short) (prev.getServicePriority().toJava() + 1));
        }
    }

    @Override
    public void bindServicesOnInterfaceType(List<ListenableFuture<?>> futures, Uint64 dpnId, String ifaceName) {
        // FIXME: does this mean this is not finished?
        LOG.info("bindServicesOnInterfaceType Ingress - WIP");
    }
}
