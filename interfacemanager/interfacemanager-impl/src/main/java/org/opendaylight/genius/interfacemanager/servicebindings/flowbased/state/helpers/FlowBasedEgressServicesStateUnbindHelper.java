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
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedEgressServicesStateUnbindHelper extends AbstractFlowBasedServicesStateUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateUnbindHelper.class);

    private final JobCoordinator coordinator;

    @Inject
    public FlowBasedEgressServicesStateUnbindHelper(final DataBroker dataBroker, final JobCoordinator coordinator) {
        super(dataBroker);
        this.coordinator = coordinator;
    }

    @Override
    protected void unbindServicesOnInterface(List<ListenableFuture<Void>> futures, List<BoundServices> allServices,
                                             Interface ifaceState) {
        /*futures.add(getTxRunner().callWithNewWriteOnlyTransactionAndSubmit(tx -> {
            List<String> ofportIds = ifaceState.getLowerLayerIf();
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
            BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            allServices.sort(Comparator.comparing(BoundServices::getServicePriority));
            allServices.remove(0);
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(),
                    tx, NwConstants.DEFAULT_SERVICE_INDEX);
            for (BoundServices boundService : allServices) {
                FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(),
                        tx, boundService.getServicePriority());
            }
        }));*/
        // remove the default egress service bound on the interface, once all
        // flows are removed
        IfmUtil.unbindService(getTxRunner(), coordinator, ifaceState.getName(),
                FlowBasedServicesUtils.buildDefaultServiceId(ifaceState.getName()));
    }

    @Override
    public void unbindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId,
                                              String ifaceName) {
        LOG.info("unbindServicesOnInterfaceType Egress - WIP");
    }
}
