/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory;

import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedEgressServicesStateBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.helpers.FlowBasedEgressServicesStateUnbindHelper;

public class FlowBasedEgressServicesStateRendererFactory extends FlowBasedServicesStateRendererFactory {
    private static FlowBasedEgressServicesStateRendererFactory
            egressServicesRendererFactory = new FlowBasedEgressServicesStateRendererFactory();

    @Override
    public FlowBasedServicesStateAddable getFlowBasedServicesStateAddRenderer() {
        return FlowBasedEgressServicesStateBindHelper.getFlowBasedEgressServicesStateBindHelper();
    }

    @Override
    public FlowBasedServicesStateRemovable getFlowBasedServicesStateRemoveRenderer() {
        return FlowBasedEgressServicesStateUnbindHelper.getFlowBasedEgressServicesStateRemoveHelper();
    }

    public static FlowBasedServicesStateRendererFactory getFlowBasedServicesRendererFactory() {
        return egressServicesRendererFactory;
    }

}
