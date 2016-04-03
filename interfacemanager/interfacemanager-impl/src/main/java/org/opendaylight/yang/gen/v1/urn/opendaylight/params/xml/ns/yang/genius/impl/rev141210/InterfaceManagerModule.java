/*
 * Copyright © 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html INTERNAL and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.genius.impl.rev141210;

import org.opendaylight.genius.impl.InterfaceManagerProvider;

public class InterfaceManagerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.genius.impl.rev141210.AbstractInterfaceManagerModule {
    public InterfaceManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InterfaceManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.genius.impl.rev141210.InterfaceManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        InterfaceManagerProvider provider = new InterfaceManagerProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
