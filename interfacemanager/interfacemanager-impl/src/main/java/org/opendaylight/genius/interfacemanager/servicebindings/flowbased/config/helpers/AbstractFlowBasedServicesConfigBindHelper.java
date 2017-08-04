/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesConfigBindHelper extends FlowInstallHelper implements
        FlowBasedServicesConfigAddable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesConfigBindHelper.class);

    protected final DataBroker dataBroker;

    /**
     * Create instance.
     * @param dataBroker instance of interfaceMgrProvider
     */
    public AbstractFlowBasedServicesConfigBindHelper(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public final void bindService(List<ListenableFuture<Void>> futures, String interfaceName,
                                  BoundServices boundServiceNew, List<BoundServices> allServices,
                                  Class<? extends ServiceModeBase> serviceMode) {

        if (allServices.isEmpty()) {
            LOG.error("empty bound service list during bind service {}, for: {}", boundServiceNew, interfaceName);
            return;
        }

        if (FlowBasedServicesUtils.isInterfaceTypeBasedServiceBinding(interfaceName)) {
            bindServiceOnInterfaceType(futures, interfaceName, boundServiceNew, allServices);
        } else {
            BoundServicesState boundServicesState = FlowBasedServicesUtils
                    .getBoundServicesState(dataBroker, interfaceName, serviceMode);
            // if service-binding state is not present, construct the same using ifstate
            if (boundServicesState == null) {
                Interface ifState = InterfaceManagerCommonUtils.getInterfaceState(interfaceName, dataBroker);
                if (ifState == null) {
                    LOG.debug("Interface not operational, will bind service whenever interface comes up: {}",
                            interfaceName);
                    return;
                }
                boundServicesState = FlowBasedServicesUtils.buildBoundServicesState(ifState, serviceMode);
                FlowBasedServicesUtils.addBoundServicesState(futures, dataBroker, interfaceName,boundServicesState);
            }
            if (L2vlan.class.equals(boundServicesState.getInterfaceType())
                    || Tunnel.class.equals(boundServicesState.getInterfaceType())) {
                bindServiceOnInterface(futures, boundServiceNew, allServices, boundServicesState);
            }
        }
    }

    protected abstract void bindServiceOnInterface(List<ListenableFuture<Void>> futures, BoundServices boundServiceNew,
                                                   List<BoundServices> allServices,
                                                   BoundServicesState interfaceBoundServicesState);

    protected abstract void bindServiceOnInterfaceType(List<ListenableFuture<Void>> futures, String ifaceType,
                                                       BoundServices boundServiceNew, List<BoundServices> allServices);
}


