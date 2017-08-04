/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractFlowBasedServicesConfigBindHelper to enable flow based ingress/egress service binding for interfaces.
 */
public abstract class AbstractFlowBasedServicesStateBindHelper implements FlowBasedServicesStateAddable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowBasedServicesStateBindHelper.class);

    protected final DataBroker dataBroker;

    /**
     * Create instance.
     * @param dataBroker instance of interfaceMgrProvider
     */
    public AbstractFlowBasedServicesStateBindHelper(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public boolean validate(String ifaceName, ServicesInfo servicesInfo, List<BoundServices> allServices) {
        if (servicesInfo == null) {
            LOG.trace("service info is null for interface {}", ifaceName);
            return false;
        }
        if (allServices == null || allServices.isEmpty()) {
            LOG.trace("bound services is empty for interface {}", ifaceName);
            return false;
        }
        return true;
    }

    public abstract void bindServicesOnInterface(List<ListenableFuture<Void>> futures, Interface ifState,
                                                 ServicesInfo servicesInfo, List<BoundServices> allServices);

    public abstract void bindServicesOnInterfaceType(List<ListenableFuture<Void>> futures, BigInteger dpnId,
                                                     ServicesInfo servicesInfo, List<BoundServices> allServices);
}


