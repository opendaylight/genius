/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowBasedIngressServicesConfigUnbindHelper extends AbstractFlowBasedServicesConfigUnbindHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedIngressServicesConfigUnbindHelper.class);
    private static volatile FlowBasedServicesConfigRemovable flowBasedIngressServicesRemovable;

    @Inject
    public FlowBasedIngressServicesConfigUnbindHelper(final DataBroker dataBroker) {
        super(dataBroker);
        flowBasedIngressServicesRemovable = this;
    }

    public static FlowBasedServicesConfigRemovable getFlowBasedIngressServicesRemoveHelper() {
        if (flowBasedIngressServicesRemovable == null) {
            LOG.error("{} is not initialized", FlowBasedIngressServicesConfigUnbindHelper.class.getSimpleName());
        }
        return flowBasedIngressServicesRemovable;
    }

    public static void clearFlowBasedIngressServicesConfigUnbindHelper() {
        flowBasedIngressServicesRemovable = null;
    }

    @Override
    protected void unbindServiceFromInterface(List<ListenableFuture<Void>> futures, BoundServices boundServiceOld,
                                            List<BoundServices> boundServices, BoundServicesState boundServicesState) {
        // Split based on type of interface....
        if (L2vlan.class.equals(boundServicesState.getInterfaceType())) {
            unbindServiceFromVlan(futures, boundServiceOld, boundServices, boundServicesState);
        } else if (Tunnel.class.equals(boundServicesState.getInterfaceType())) {
            unbindServiceFromTunnel(futures, boundServiceOld, boundServices, boundServicesState);
        }
    }

    private void unbindServiceFromVlan(List<ListenableFuture<Void>> futures, BoundServices boundServiceOld,
                                       List<BoundServices> boundServices, BoundServicesState boundServicesState) {

        LOG.info("unbinding ingress service {} for vlan port: {}", boundServiceOld.getServiceName(),
                boundServicesState.getInterfaceName());
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        BigInteger dpId = boundServicesState.getDpid();
        if (boundServices.isEmpty()) {
            // Remove default entry from Lport Dispatcher Table.
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                boundServiceOld, tx, NwConstants.DEFAULT_SERVICE_INDEX);
            if (tx != null) {
                futures.add(tx.submit());
            }
            return;
        }
        BoundServices[] highLow = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, boundServiceOld);
        BoundServices low = highLow[0];
        BoundServices high = highLow[1];
        // This means the one removed was the highest priority service
        if (high == null) {
            LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                boundServiceOld, tx, NwConstants.DEFAULT_SERVICE_INDEX);
            if (low != null) {
                // delete the lower services flow entry.
                LOG.trace("Deleting ingress dispatcher table entry for lower service {}, match service index {}", low,
                        low.getServicePriority());
                FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(), low, tx,
                        low.getServicePriority());
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(boundServices, low)[0];
                short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                        : low.getServicePriority() + 1);
                LOG.trace("Installing new ingress dispatcher table entry for lower service {}, match service index "
                                + "{}, update service index {}",
                        low, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, low, boundServicesState.getInterfaceName(), tx,
                        boundServicesState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
            }
        } else {
            LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                    boundServiceOld.getServicePriority());
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                boundServiceOld, tx, boundServiceOld.getServicePriority());
            short lowerServiceIndex = (short) (low != null ? low.getServicePriority()
                    : boundServiceOld.getServicePriority() + 1);
            BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(boundServices);
            if (high.equals(highest)) {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                        high, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServicesState.getInterfaceName(), tx,
                        boundServicesState.getIfIndex(), NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
            } else {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                        high, high.getServicePriority(), lowerServiceIndex);
                FlowBasedServicesUtils.installLPortDispatcherFlow(dpId, high, boundServicesState.getInterfaceName(), tx,
                        boundServicesState.getIfIndex(), high.getServicePriority(), lowerServiceIndex);
            }
        }
        futures.add(tx.submit());
    }

    private void unbindServiceFromTunnel(List<ListenableFuture<Void>> futures, BoundServices boundServiceOld,
                                         List<BoundServices> boundServices, BoundServicesState boundServicesState) {

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        BigInteger dpId = boundServicesState.getDpid();

        LOG.info("unbinding ingress service {} for tunnel port: {}", boundServiceOld.getServiceName(),
                boundServicesState.getInterfaceName());

        if (boundServices.isEmpty()) {
            // Remove entry from Ingress Table.
            FlowBasedServicesUtils.removeIngressFlow(boundServicesState.getInterfaceName(), boundServiceOld, dpId, tx);
            if (tx != null) {
                futures.add(tx.submit());
            }
            return;
        }

        Map<Short, BoundServices> tmpServicesMap = new ConcurrentHashMap<>();
        short highestPriority = 0xFF;
        for (BoundServices boundService : boundServices) {
            tmpServicesMap.put(boundService.getServicePriority(), boundService);
            if (boundService.getServicePriority() < highestPriority) {
                highestPriority = boundService.getServicePriority();
            }
        }

        if (highestPriority < boundServiceOld.getServicePriority()) {
            FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, boundServicesState.getInterfaceName(),
                boundServiceOld, tx, boundServiceOld.getServicePriority());
            if (tx != null) {
                futures.add(tx.submit());
            }
            return;
        }

        List<MatchInfo> matches;
        long portNo = boundServicesState.getPortNo();
        matches = FlowBasedServicesUtils.getMatchInfoForTunnelPortAtIngressTable(dpId, portNo);

        BoundServices toBeMoved = tmpServicesMap.get(highestPriority);
        Interface iface =
                InterfaceManagerCommonUtils.getInterfaceFromConfigDS(boundServicesState.getInterfaceName(), dataBroker);
        FlowBasedServicesUtils.removeIngressFlow(iface.getName(), boundServiceOld, dpId, tx);
        FlowBasedServicesUtils.installInterfaceIngressFlow(dpId, iface, toBeMoved, tx, matches,
            boundServicesState.getIfIndex(), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
        FlowBasedServicesUtils.removeLPortDispatcherFlow(dpId, iface.getName(), toBeMoved, tx,
                toBeMoved.getServicePriority());

        if (tx != null) {
            futures.add(tx.submit());
        }
    }

    @Override
    protected void unbindServiceFromInterfaceType(List<ListenableFuture<Void>> futures, String interfaceType,
                                                  BoundServices boundServiceOld, List<BoundServices> allServices) {

        LOG.info("unbinding ingress service {} for type {}", boundServiceOld.getServiceName(), interfaceType);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        Set<BigInteger> dpId = InterfaceMetaUtils.getDpnIdsFromBridgeEntryCache();
        if (allServices.isEmpty()) {
            // Remove default entry from Lport Dispatcher Table.
            deleteFlowsOnDpnsIngress(dpId, boundServiceOld, transaction, interfaceType,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            if (transaction != null) {
                futures.add(transaction.submit());
            }
            return;
        }
        BoundServices[] highLow = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, boundServiceOld);
        BoundServices low = highLow[0];
        BoundServices high = highLow[1];
        // This means the one removed was the highest priority service
        if (high == null) {
            LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            deleteFlowsOnDpnsIngress(dpId, boundServiceOld, transaction, interfaceType,
                    NwConstants.DEFAULT_SERVICE_INDEX);
            if (low != null) {
                // delete the lower services flow entry.
                LOG.trace("Deleting ingress dispatcher table entry for lower service {}, match service index {}", low,
                        low.getServicePriority());
                deleteFlowsOnDpnsIngress(dpId, low, transaction, interfaceType, low.getServicePriority());
                BoundServices lower = FlowBasedServicesUtils.getHighAndLowPriorityService(allServices, low)[0];
                short lowerServiceIndex = (short) (lower != null ? lower.getServicePriority()
                        : low.getServicePriority() + 1);
                LOG.trace("Installing new ingress dispatcher table entry for lower service {}, match service index "
                                + "{}, update service index {}",
                        low, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                writeFlowsOnDpnsIngress(transaction, low, dpId, interfaceType, NwConstants.DEFAULT_SERVICE_INDEX,
                        lowerServiceIndex);
            }
        } else {
            LOG.trace("Deleting ingress dispatcher table entry for service {}, match service index {}", boundServiceOld,
                    boundServiceOld.getServicePriority());
            deleteFlowsOnDpnsIngress(dpId, boundServiceOld, transaction, interfaceType,
                    boundServiceOld.getServicePriority());
            short lowerServiceIndex = (short) (low != null ? low.getServicePriority()
                    : boundServiceOld.getServicePriority() + 1);
            BoundServices highest = FlowBasedServicesUtils.getHighestPriorityService(allServices);
            if (high.equals(highest)) {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                        high, NwConstants.DEFAULT_SERVICE_INDEX, lowerServiceIndex);
                writeFlowsOnDpnsIngress(transaction,high, dpId, interfaceType, NwConstants.DEFAULT_SERVICE_INDEX,
                        lowerServiceIndex);
            } else {
                LOG.trace("Update the existing higher service {}, match service index {}, update service index {}",
                        high, high.getServicePriority(), lowerServiceIndex);
                writeFlowsOnDpnsIngress(transaction, high, dpId, interfaceType, high.getServicePriority(),
                        lowerServiceIndex);
            }
        }
        futures.add(transaction.submit());
    }
}
