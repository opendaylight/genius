/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedEgressServicesConfigBindHelper;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.helpers.FlowBasedEgressServicesConfigUnbindHelper;

@Singleton
public class FlowBasedEgressServicesRendererFactory extends FlowBasedServicesRendererFactory {
    private final FlowBasedEgressServicesConfigBindHelper flowBasedServicesConfigAddable;
    private final FlowBasedEgressServicesConfigUnbindHelper flowBasedServicesConfigRemovable;

    @Inject
    public FlowBasedEgressServicesRendererFactory(
            FlowBasedEgressServicesConfigBindHelper flowBasedServicesConfigAddable,
            FlowBasedEgressServicesConfigUnbindHelper flowBasedServicesConfigRemovable) {
        super();
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
