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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedEgressServicesStateBindHelper extends AbstractFlowBasedServicesStateBindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateBindHelper.class);

    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedEgressServicesStateBindHelper(final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        super(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    @Override
    public void bindServicesOnInterface(WriteTransaction tx, List<BoundServices> allServices,
                                        Interface ifState) {
        LOG.info("bind all egress services for interface: {}", ifState.getName());
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        allServices.sort(Comparator.comparing(BoundServices::getServicePriority));
        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority()
                : highestPriority.getServicePriority() + 1);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.Interface iface = interfaceManagerCommonUtils
                .getInterfaceFromConfigDS(ifState.getName());
        FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, highestPriority, ifState.getName(), tx,
                ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex, iface);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev != null) {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), tx,
                        ifState.getIfIndex(), prev.getServicePriority(), boundService.getServicePriority(), iface);
            }
            prev = boundService;
        }
        if (prev != null) {
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), tx,
                    ifState.getIfIndex(), prev.getServicePriority(), (short) (prev.getServicePriority() + 1),
                    iface);
        }
    }

    @Override
    public void bindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId, String ifaceName) {
        LOG.info("bindServicesOnInterfaceType Egress - WIP");
    }
}
