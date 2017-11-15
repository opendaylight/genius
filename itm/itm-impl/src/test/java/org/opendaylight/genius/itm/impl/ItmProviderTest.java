/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.listeners.InterfaceStateListener;
import org.opendaylight.genius.itm.listeners.OvsdbNodeListener;
import org.opendaylight.genius.itm.listeners.TransportZoneListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorChangeListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorIntervalListener;
import org.opendaylight.genius.itm.listeners.VtepConfigSchemaListener;
import org.opendaylight.genius.itm.listeners.cache.DpnTepsInfoListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringIntervalListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringListener;
import org.opendaylight.genius.itm.listeners.cache.StateTunnelListListener;
import org.opendaylight.genius.itm.monitoring.ItmTunnelEventListener;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;

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
    @Mock OvsdbNodeListener ovsdbNodeListener;
    @Mock JobCoordinator jobCoordinator;

    @Test
    public void testClose() {
        ItmProvider provider = new ItmProvider(dataBroker, dpnTepsInfoListener, idManagerService,
                interfaceStateListener, itmManager, itmManagerRpcService, itmMonitoringListener,
                itmMonitoringIntervalListener, itmTunnelEventListener, stateTunnelListListener,
                tepCommandHelper, tunnelMonitorChangeListener, tunnelMonitorIntervalListener,
                transportZoneListener, vtepConfigSchemaListener, ovsdbNodeListener, jobCoordinator);
        // ensure no exceptions
        // currently this method is empty
        provider.close();
    }
}
