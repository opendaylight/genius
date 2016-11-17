/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ITMManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ITMManager.class);

    private final DataBroker broker;
    private IMdsalApiManager mdsalManager;
    private NotificationPublishService notificationPublishService;

    List<DPNTEPsInfo> meshedDpnList;

    @Override
    public void close() throws Exception {
        LOG.info("ITMManager Closed");
    }

    public ITMManager(final DataBroker db) {
        broker = db;
    }

    public void setMdsalManager(IMdsalApiManager mdsalManager) {
        this.mdsalManager = mdsalManager;
    }

    public void setNotificationPublishService(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }
    protected void initTunnelMonitorDataInConfigDS() {
        new Thread() {
            public void run() {
                boolean readSucceeded = false;
                InstanceIdentifier<TunnelMonitorParams> monitorPath = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
                while (!readSucceeded) {
                    try {
                        Optional<TunnelMonitorParams> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, monitorPath, broker);
                        // Store default values only when tunnel monitor data is not initialized
                        if (!storedTunnelMonitor.isPresent()) {
                            TunnelMonitorParams monitorEnabled =
                                    new TunnelMonitorParamsBuilder().setEnabled(ITMConstants.DEFAULT_MONITOR_ENABLED).build();
                            ItmUtils.asyncUpdate(LogicalDatastoreType.CONFIGURATION, monitorPath, monitorEnabled, broker, ItmUtils.DEFAULT_CALLBACK);

                            InstanceIdentifier<TunnelMonitorInterval> intervalPath = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
                            TunnelMonitorInterval monitorInteval =
                                    new TunnelMonitorIntervalBuilder().setInterval(ITMConstants.DEFAULT_MONITOR_INTERVAL).build();
                            ItmUtils.asyncUpdate(LogicalDatastoreType.CONFIGURATION, intervalPath, monitorInteval, broker, ItmUtils.DEFAULT_CALLBACK);
                        }
                        readSucceeded = true;
                    } catch (Exception e) {
                        LOG.warn("Unable to read monitor enabled info; retrying after some delay");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            return;
                        }
                    }
                }
            }
        }.start();
    }

    protected boolean getTunnelMonitorEnabledFromConfigDS() {
        InstanceIdentifier<TunnelMonitorParams> path = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker).transform(
                TunnelMonitorParams::isEnabled).or(true);
    }

    protected Class<? extends TunnelMonitoringTypeBase> getTunnelMonitorTypeFromConfigDS() {

        Class<? extends TunnelMonitoringTypeBase> tunnelMonitorType = TunnelMonitoringTypeBfd.class;
        InstanceIdentifier<TunnelMonitorParams> path = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        Optional<TunnelMonitorParams> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker);
        if (storedTunnelMonitor.isPresent() && storedTunnelMonitor.get().getMonitorProtocol() != null) {
            tunnelMonitorType = storedTunnelMonitor.get().getMonitorProtocol();
        }
        return tunnelMonitorType;
    }

    protected int getTunnelMonitorIntervalFromConfigDS() {
        InstanceIdentifier<TunnelMonitorInterval> path = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, broker).transform(
                TunnelMonitorInterval::getInterval).or(ITMConstants.DEFAULT_MONITOR_INTERVAL);
    }
}
