/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.genius.itm.listeners.*;
import org.opendaylight.genius.itm.listeners.cache.DpnTepsInfoListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringIntervalListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringListener;
import org.opendaylight.genius.itm.listeners.cache.StateTunnelListListener;
import org.opendaylight.genius.itm.monitoring.ItmTunnelEventListener;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;

import static org.mockito.Mockito.mock;

public class ItmProviderTest {

    @Mock DataBroker dataBroker;
    @Mock DpnTepsInfoListener dpnTepsInfoListener;
    @Mock IdManagerService idManagerService;
    @Mock InterfaceStateListener interfaceStateListener;
    @Mock ITMManager itmManager;
    @Mock ItmManagerRpcService itmManagerRpcService;
    @Mock ItmMonitoringListener itmMonitoringListener;
    @Mock ItmMonitoringIntervalListener itmMonitoringIntervalListener;
    @Mock ItmTunnelEventListener itmTunnelEventListener;
    @Mock StateTunnelListListener stateTunnelListListener;
    @Mock TepCommandHelper tepCommandHelper;
    @Mock TunnelMonitorChangeListener tunnelMonitorChangeListener;
    @Mock TunnelMonitorIntervalListener tunnelMonitorIntervalListener;
    @Mock TransportZoneListener transportZoneListener;
    @Mock VtepConfigSchemaListener vtepConfigSchemaListener;

    @Test
    public void testClose() throws Exception {
        ItmProvider provider = new ItmProvider(dataBroker, dpnTepsInfoListener, idManagerService, interfaceStateListener,
                itmManager, itmManagerRpcService, itmMonitoringListener, itmMonitoringIntervalListener, itmTunnelEventListener,
                stateTunnelListListener, tepCommandHelper, tunnelMonitorChangeListener, tunnelMonitorIntervalListener,
                transportZoneListener, vtepConfigSchemaListener);

        // ensure no exceptions
        // currently this method is empty
        provider.close();
    }
}
