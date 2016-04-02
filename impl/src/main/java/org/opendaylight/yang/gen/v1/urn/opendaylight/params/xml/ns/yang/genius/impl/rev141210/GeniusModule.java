/*
 * Copyright © 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html INTERNAL and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.genius.impl.rev141210;

import org.opendaylight.genius.impl.GeniusProvider;

public class GeniusModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.genius.impl.rev141210.AbstractGeniusModule {
    public GeniusModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public GeniusModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.genius.impl.rev141210.GeniusModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        GeniusProvider provider = new GeniusProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
