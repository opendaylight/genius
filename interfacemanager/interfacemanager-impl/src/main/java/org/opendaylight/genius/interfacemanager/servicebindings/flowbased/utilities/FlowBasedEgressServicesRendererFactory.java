/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities;

import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.confighelpers.FlowBasedEgressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.confighelpers.FlowBasedEgressServicesConfigUnbindHelper;

public class FlowBasedEgressServicesRendererFactory extends FlowBasedServicesRendererFactory{
    private static FlowBasedEgressServicesRendererFactory egressServicesRendererFactory = new FlowBasedEgressServicesRendererFactory();

    @Override
    public FlowBasedServicesAddable getFlowBasedServicesAddRenderer() {
        return FlowBasedEgressServicesConfigBindHelper.getFlowBasedEgressServicesAddHelper();
    }

    @Override
    public FlowBasedServicesRemovable getFlowBasedServicesRemoveRenderer() {
        return FlowBasedEgressServicesConfigUnbindHelper.getFlowBasedEgressServicesRemoveHelper();
    }

    public static FlowBasedServicesRendererFactory getFlowBasedServicesRendererFactory() {
        return egressServicesRendererFactory;
    }

}
