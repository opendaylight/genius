/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory;

import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedIngressServicesStateBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedIngressServicesStateUnbindHelper;

public class FlowBasedIngressServicesStateRendererFactory extends FlowBasedServicesStateRendererFactory {
    private static FlowBasedServicesStateRendererFactory ingressServicesStateRendererFactory = new FlowBasedIngressServicesStateRendererFactory();

    @Override
    public FlowBasedServicesStateAddable getFlowBasedServicesStateAddRenderer() {
        return FlowBasedIngressServicesStateBindHelper.getFlowBasedIngressServicesStateAddHelper();
    }

    @Override
    public FlowBasedServicesStateRemovable getFlowBasedServicesStateRemoveRenderer() {
        return FlowBasedIngressServicesStateUnbindHelper.getFlowBasedIngressServicesStateRemoveHelper();
    }

    public static FlowBasedServicesStateRendererFactory getIngressServicesStateRendererFactory() {
        return ingressServicesStateRendererFactory;
    }
}
