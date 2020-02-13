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
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedEgressServicesStateBindHelper extends AbstractFlowBasedServicesStateBindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateBindHelper.class);

    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public FlowBasedEgressServicesStateBindHelper(@Reference final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        super(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    @Override
    public void bindServicesOnInterface(TypedReadWriteTransaction<Configuration> tx, List<BoundServices> allServices,
                                        Interface ifState) {
        LOG.info("bind all egress services for interface: {}", ifState.getName());
        NodeConnectorId nodeConnectorId = FlowBasedServicesUtils.getNodeConnectorIdFromInterface(ifState);
        Uint64 dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        allServices.sort(Comparator.comparing(BoundServices::getServicePriority));
        BoundServices highestPriority = allServices.remove(0);
        short nextServiceIndex = (short) (allServices.size() > 0 ? allServices.get(0).getServicePriority().toJava()
                : highestPriority.getServicePriority().toJava() + 1);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.Interface iface = interfaceManagerCommonUtils
                .getInterfaceFromConfigDS(ifState.getName());
        FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, highestPriority, ifState.getName(), tx,
                ifState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, nextServiceIndex, iface);
        BoundServices prev = null;
        for (BoundServices boundService : allServices) {
            if (prev != null) {
                FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), tx,
                        ifState.getIfIndex(), prev.getServicePriority().toJava(),
                        boundService.getServicePriority().toJava(), iface);
            }
            prev = boundService;
        }
        if (prev != null) {
            FlowBasedServicesUtils.installEgressDispatcherFlows(dpId, prev, ifState.getName(), tx,
                    ifState.getIfIndex(), prev.getServicePriority().toJava(),
                    (short) (prev.getServicePriority().toJava() + 1), iface);
        }
    }

    @Override
    public void bindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, Uint64 dpnId, String ifaceName) {
        // TODO: No-op?
    }
}
