/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;


import java.util.ArrayList;
import java.util.List;

import org.opendaylight.genius.datastoreutils.testutils.JobCoordinatorEventsWaiter;
import org.opendaylight.genius.datastoreutils.testutils.TestableJobCoordinatorEventsWaiter;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.itm.config.TunnelAggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.itm.config.TunnelAggregationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.itm.config.TunnelAggregationKey;

public class ItmTunnelAggregationConfigTest extends AbstractItmTestModule {

    @Override
    protected void configureBindings() {

        super.configureBindings();

        List<TunnelAggregation> tunnelValues = new ArrayList<>();
        tunnelValues.add(new TunnelAggregationBuilder().setKey(new TunnelAggregationKey("vxlan"))
                .setEnabled(true).setTunnelType("vxlan").build());
        ItmConfig itmConfig = new ItmConfigBuilder().setTunnelAggregation(tunnelValues).build();

        bind(ItmConfig.class).toInstance(itmConfig);
        bind(ItmTunnelAggregationHelper.class);
        bind(JobCoordinatorEventsWaiter.class).to(TestableJobCoordinatorEventsWaiter.class);
    }

}
