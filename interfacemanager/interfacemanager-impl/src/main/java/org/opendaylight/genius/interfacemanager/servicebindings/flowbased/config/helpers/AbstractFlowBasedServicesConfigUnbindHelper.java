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
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory.FlowBasedServicesConfigRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesConfigUnbindHelper implements FlowBasedServicesConfigRemovable {

    @Override
    public final void unbindService(List<ListenableFuture<Void>> futures, String interfaceName,
                                    BoundServices boundServiceOld, List<BoundServices> boundServices,
                                    BoundServicesState boundServicesState) {

        if (FlowBasedServicesUtils.isInterfaceTypeBasedServiceBinding(interfaceName)) {
            unbindServiceOnInterfaceType(futures, boundServiceOld, boundServices);
        } else if (L2vlan.class.equals(boundServicesState.getInterfaceType())
            || Tunnel.class.equals(boundServicesState.getInterfaceType())) {
            unbindServiceOnInterface(futures, boundServiceOld, boundServices, boundServicesState);
        }
    }


    protected abstract void unbindServiceOnInterface(List<ListenableFuture<Void>> futures,
                                                     BoundServices boundServiceOld, List<BoundServices> allServices,
                                                     BoundServicesState boundServicesState);

    protected abstract void unbindServiceOnInterfaceType(List<ListenableFuture<Void>> futures,
                                                         BoundServices boundServiceOld,
                                                         List<BoundServices> allServices);
}


