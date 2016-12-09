/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.LivenessState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
@Singleton
public class AlivenessMonitorListener implements org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorListener {
    private static final Logger LOG = LoggerFactory.getLogger(AlivenessMonitorListener.class);
    private final DataBroker dataBroker;

    @Inject
    public AlivenessMonitorListener(final DataBroker dataBroker, final NotificationService notificationService) {
        this.dataBroker = dataBroker;
        notificationService.registerNotificationListener(this);
    }

    @PostConstruct
    public void start() throws Exception {
        LOG.info("AlivenessMonitorListener started");
    }

    @PreDestroy
    public void close() throws Exception {
        LOG.info("AlivenessMonitorListener closed");
    }
    @Override
    public void onMonitorEvent(MonitorEvent notification) {
        Long monitorId = notification.getEventData().getMonitorId();
        String tunnelInterface = AlivenessMonitorUtils.getInterfaceFromMonitorId(dataBroker, monitorId);
        if (tunnelInterface == null) {
            LOG.debug("Either monitoring for interface - {} not started by Interfacemgr or it is not LLDP monitoring", tunnelInterface);
            return;
        }
        LivenessState livenessState = notification.getEventData().getMonitorState();
        LOG.debug("received monitor event for {} with livenessstate {}", tunnelInterface, livenessState);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus opState =
                livenessState == LivenessState.Up ? org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up :
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        InterfaceManagerCommonUtils.setOpStateForInterface(dataBroker, tunnelInterface, opState);
    }

}