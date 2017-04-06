/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;


import org.opendaylight.genius.itm.globals.ITMConstants;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;

/**
 * Dependency Injection Wiring for {@link ItmTest}.
 *
 * @author Michael Vorburger
 * @author Tarun Thakur
 */

public class ItmTestModule extends AbstractItmTestModule {

    @Override
    protected void configureBindings() {
        super.configureBindings();
        // Bindings for services from this project
        ItmConfig itmConfigObj = new ItmConfigBuilder()
            .setDefTzEnabled(true).setDefTzTunnelType(ITMConstants.TUNNEL_TYPE_VXLAN).build();
        bind(ItmConfig.class).toInstance(itmConfigObj);
    }

}
