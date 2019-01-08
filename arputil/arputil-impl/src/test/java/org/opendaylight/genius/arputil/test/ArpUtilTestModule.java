/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.opendaylight.genius.arputil.internal.ArpUtilImpl;
import org.opendaylight.genius.mdsal.testutils.DataBrokerTestWiring;
import org.opendaylight.infrautils.inject.guice.testutils.AbstractGuiceJsr250Module;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

public class ArpUtilTestModule extends AbstractGuiceJsr250Module {

    @Override
    protected void configureBindings() throws Exception {

        DataBroker dataBroker = DataBrokerTestWiring.dataBroker();
        bind(DataBroker.class).toInstance(dataBroker);

        TestOdlInterfaceRpcService testOdlInterfaceRpcService = TestOdlInterfaceRpcService.newInstance();
        bind(OdlInterfaceRpcService.class).toInstance(testOdlInterfaceRpcService);
        TestPacketProcessingService testPacketProcessingService = TestPacketProcessingService.newInstance();
        bind(PacketProcessingService.class).toInstance(testPacketProcessingService);
        bind(NotificationService.class).toInstance(mock(NotificationService.class));
        bind(NotificationPublishService.class).toInstance(mock(NotificationPublishService.class));
        bind(OdlArputilService.class).to(ArpUtilImpl.class);
        bind(MetricProvider.class).toInstance(mock(MetricProvider.class, RETURNS_DEEP_STUBS));
    }
}
