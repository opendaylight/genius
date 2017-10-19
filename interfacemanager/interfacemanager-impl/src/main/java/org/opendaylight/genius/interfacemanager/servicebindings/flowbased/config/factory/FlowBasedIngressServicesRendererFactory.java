/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedIngressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedIngressServicesConfigUnbindHelper;

@Singleton
public class FlowBasedIngressServicesRendererFactory extends FlowBasedServicesRendererFactory {
    private final FlowBasedIngressServicesConfigBindHelper flowBasedServicesConfigAddable;
    private final FlowBasedIngressServicesConfigUnbindHelper flowBasedServicesConfigRemovable;

    @Inject
    public FlowBasedIngressServicesRendererFactory(
            FlowBasedIngressServicesConfigBindHelper flowBasedServicesConfigAddable,
            FlowBasedIngressServicesConfigUnbindHelper flowBasedServicesConfigRemovable) {
        this.flowBasedServicesConfigAddable = flowBasedServicesConfigAddable;
        this.flowBasedServicesConfigRemovable = flowBasedServicesConfigRemovable;
    }

    @Override
    public FlowBasedServicesConfigAddable getFlowBasedServicesAddRenderer() {
        return flowBasedServicesConfigAddable;
    }

    @Override
    public FlowBasedServicesConfigRemovable getFlowBasedServicesRemoveRenderer() {
        return flowBasedServicesConfigRemovable;
    }
}
