/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;

/**
 * Resolves FlowBasedServicesStateRendererFactory instances.
 *
 * @author Thomas Pantelis
 */
@Singleton
public class FlowBasedServicesStateRendererFactoryResolver {
    private final FlowBasedIngressServicesStateRendererFactory flowBasedIngressServicesStateRendererFactory;
    private final FlowBasedEgressServicesStateRendererFactory flowBasedEgressServicesStateRendererFactory;

    @Inject
    public FlowBasedServicesStateRendererFactoryResolver(
            FlowBasedIngressServicesStateRendererFactory flowBasedIngressServicesStateRendererFactory,
            FlowBasedEgressServicesStateRendererFactory flowBasedEgressServicesStateRendererFactory) {
        this.flowBasedIngressServicesStateRendererFactory = flowBasedIngressServicesStateRendererFactory;
        this.flowBasedEgressServicesStateRendererFactory = flowBasedEgressServicesStateRendererFactory;
    }

    public FlowBasedServicesStateRendererFactory getFlowBasedServicesStateRendererFactory(
            Class<? extends ServiceModeBase> serviceMode) {
        return ServiceModeIngress.class.equals(serviceMode) ? flowBasedIngressServicesStateRendererFactory :
            flowBasedEgressServicesStateRendererFactory;

    }
}
