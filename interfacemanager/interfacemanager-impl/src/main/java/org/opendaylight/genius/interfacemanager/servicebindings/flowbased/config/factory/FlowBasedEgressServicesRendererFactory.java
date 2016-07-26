/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory;

import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedEgressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedEgressServicesConfigUnbindHelper;

public class FlowBasedEgressServicesRendererFactory extends FlowBasedServicesRendererFactory{
    private static FlowBasedEgressServicesRendererFactory egressServicesRendererFactory = new FlowBasedEgressServicesRendererFactory();

    @Override
    public FlowBasedServicesConfigAddable getFlowBasedServicesAddRenderer() {
        return FlowBasedEgressServicesConfigBindHelper.getFlowBasedEgressServicesAddHelper();
    }

    @Override
    public FlowBasedServicesConfigRemovable getFlowBasedServicesRemoveRenderer() {
        return FlowBasedEgressServicesConfigUnbindHelper.getFlowBasedEgressServicesRemoveHelper();
    }

    public static FlowBasedServicesRendererFactory getFlowBasedServicesRendererFactory() {
        return egressServicesRendererFactory;
    }

}
