/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;

/**
 * Resolves FlowBasedServicesRendererFactory instances.
 *
 * @author Thomas Pantelis
 */
@Singleton
public class FlowBasedServicesRendererFactoryResolver {
    private final FlowBasedIngressServicesRendererFactory flowBasedIngressServicesRendererFactory;
    private final FlowBasedEgressServicesRendererFactory flowBasedEgressServicesRendererFactory;

    @Inject
    public FlowBasedServicesRendererFactoryResolver(
            FlowBasedIngressServicesRendererFactory flowBasedIngressServicesRendererFactory,
            FlowBasedEgressServicesRendererFactory flowBasedEgressServicesRendererFactory) {
        this.flowBasedIngressServicesRendererFactory = flowBasedIngressServicesRendererFactory;
        this.flowBasedEgressServicesRendererFactory = flowBasedEgressServicesRendererFactory;
    }

    public FlowBasedServicesRendererFactory getFlowBasedServicesRendererFactory(
            Class<? extends ServiceModeBase> serviceMode) {
        return ServiceModeIngress.class.equals(serviceMode) ? flowBasedIngressServicesRendererFactory :
            flowBasedEgressServicesRendererFactory;
    }
}
