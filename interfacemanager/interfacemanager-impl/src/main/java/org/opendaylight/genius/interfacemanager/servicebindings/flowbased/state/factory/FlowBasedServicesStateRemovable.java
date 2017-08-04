/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;

public interface FlowBasedServicesStateRemovable {
    List<ListenableFuture<Void>> unbindServicesFromInterface(Interface ifState,ServicesInfo servicesInfo,
                                                             List<BoundServices> allServices, Integer ifIndex,
                                                             DataBroker dataBroker);

    List<ListenableFuture<Void>> unbindServicesFromInterfaceType(BigInteger dpnId, String ifaceName,
                                                                 ServicesInfo servicesInfo,
                                                                 List<BoundServices> allServices,
                                                                 DataBroker dataBroker);
}
