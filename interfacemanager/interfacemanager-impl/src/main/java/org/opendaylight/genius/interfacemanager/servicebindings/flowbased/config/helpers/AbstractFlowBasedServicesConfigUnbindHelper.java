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
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesConfigUnbindHelper extends FlowInstallHelper implements
        FlowBasedServicesConfigRemovable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesConfigUnbindHelper.class);

    private final DataBroker dataBroker;
    protected final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    protected final InterfaceMetaUtils interfaceMetaUtils;

    /**
     * Create instance.
     * @param dataBroker instance of interfaceMgrProvider
     */
    protected AbstractFlowBasedServicesConfigUnbindHelper(final DataBroker dataBroker,
                                                          final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                                          final InterfaceMetaUtils interfaceMetaUtils) {
        this.dataBroker = dataBroker;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.interfaceMetaUtils = interfaceMetaUtils;
    }

    protected DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    public void unbindService(List<ListenableFuture<Void>> futures, String interfaceName, BoundServices boundServiceOld,
                              List<BoundServices> boundServices,  Class<? extends ServiceModeBase> serviceMode) {
        if (FlowBasedServicesUtils.isInterfaceTypeBasedServiceBinding(interfaceName)) {
            unbindServiceOnInterfaceType(futures, interfaceName, boundServiceOld, boundServices);
        } else {
            BoundServicesState boundServicesState = FlowBasedServicesUtils.getBoundServicesState(
                    dataBroker, interfaceName, serviceMode);
            if (boundServicesState == null) {
                LOG.error("bound-service-state is not present for interface:{}, service-mode:{}, "
                                + "service-name:{}, service-priority:{}", interfaceName, serviceMode,
                        boundServiceOld.getServiceName(), boundServiceOld.getServicePriority());
                return;
            }

            if (boundServices.isEmpty()) {
                FlowBasedServicesUtils.removeBoundServicesState(futures, dataBroker, interfaceName, serviceMode);
            }
            if (L2vlan.class.equals(boundServicesState.getInterfaceType())
                    || Tunnel.class.equals(boundServicesState.getInterfaceType())) {
                unbindServiceOnInterface(futures, boundServiceOld, boundServices, boundServicesState);
            }
        }
    }


    protected abstract void unbindServiceOnInterface(List<ListenableFuture<Void>> futures,
                                                     BoundServices boundServiceOld, List<BoundServices> allServices,
                                                     BoundServicesState boundServicesState);

    protected abstract void unbindServiceOnInterfaceType(List<ListenableFuture<Void>> futures, String ifaceType,
                                                         BoundServices boundServiceOld,
                                                         List<BoundServices> allServices);
}


