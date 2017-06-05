/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedIngressServicesStateBindHelper;

@Singleton
public class FlowBasedIngressServicesStateRendererFactory extends FlowBasedServicesStateRendererFactory {
    private final FlowBasedIngressServicesStateBindHelper flowBasedServicesStateAddable;

    @Inject
    public FlowBasedIngressServicesStateRendererFactory(
            FlowBasedIngressServicesStateBindHelper flowBasedServicesStateAddable) {
        this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
    }

    @Override
    public FlowBasedServicesStateAddable getFlowBasedServicesStateAddRenderer() {
        return flowBasedServicesStateAddable;
    }
}
