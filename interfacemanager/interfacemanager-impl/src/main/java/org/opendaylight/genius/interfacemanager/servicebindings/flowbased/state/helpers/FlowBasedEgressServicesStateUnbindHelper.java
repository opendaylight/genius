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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedEgressServicesStateUnbindHelper extends AbstractFlowBasedServicesStateUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesStateUnbindHelper.class);
    private static volatile FlowBasedServicesStateRemovable flowBasedServicesStateRemovable;

    @Inject
    private FlowBasedEgressServicesStateUnbindHelper(final DataBroker dataBroker) {
        super(dataBroker);
        flowBasedServicesStateRemovable = this;
    }

    public static FlowBasedServicesStateRemovable getFlowBasedEgressServicesStateRemoveHelper() {
        if (flowBasedServicesStateRemovable == null) {
            LOG.error("FlowBasedIngressBindHelper`` is not initialized");
        }
        return flowBasedServicesStateRemovable;
    }

    @Override
    protected void unbindServicesOnInterface(List<ListenableFuture<Void>> futures, List<BoundServices> allServices,
                                             Interface ifaceState, Integer ifIndex) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        List<String> ofportIds = ifaceState.getLowerLayerIf();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ofportIds.get(0));
        if (nodeConnectorId == null) {
            return;
        }
        BigInteger dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        Collections.sort(allServices, Comparator.comparing(BoundServices::getServicePriority));
        BoundServices highestPriority = allServices.remove(0);
        FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(), highestPriority,
                writeTransaction, NwConstants.DEFAULT_SERVICE_INDEX);
        for (BoundServices boundService : allServices) {
            FlowBasedServicesUtils.removeEgressDispatcherFlows(dpId, ifaceState.getName(), boundService,
                    writeTransaction, boundService.getServicePriority());
        }
        futures.add(writeTransaction.submit());
        // remove the default egress service bound on the interface, once all
        // flows are removed
        FlowBasedServicesUtils.unbindDefaultEgressDispatcherService(dataBroker, ifaceState.getName());
    }

    @Override
    public void unbindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId,
                                              String ifaceName) {
        LOG.info("unbindServicesOnInterfaceType Egress - WIP");
    }
}
