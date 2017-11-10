/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.LivenessState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
@Singleton
public class AlivenessMonitorListenerImpl implements AlivenessMonitorListener {
    private static final Logger LOG = LoggerFactory.getLogger(AlivenessMonitorListenerImpl.class);

    private final AlivenessMonitorUtils alivenessMonitorUtils;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public AlivenessMonitorListenerImpl(final DataBroker dataBroker, final NotificationService notificationService,
            final AlivenessMonitorService alivenessMonitorService,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            final AlivenessMonitorUtils alivenessMonitorUtils) {
        this.alivenessMonitorUtils = alivenessMonitorUtils;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        notificationService.registerNotificationListener(this);
    }

    @PostConstruct
    public void start() {
        LOG.info("AlivenessMonitorListener started");
    }

    @PreDestroy
    public void close() {
        LOG.info("AlivenessMonitorListener closed");
    }

    @Override
    public void onMonitorEvent(MonitorEvent notification) {
        Long monitorId = notification.getEventData().getMonitorId();
        String tunnelInterface = alivenessMonitorUtils.getInterfaceFromMonitorId(monitorId);
        if (tunnelInterface == null) {
            LOG.debug("Either monitoring for interface not started by Interfacemgr or it is not LLDP monitoring");
            return;
        }
        LivenessState livenessState = notification.getEventData().getMonitorState();
        LOG.debug("received monitor event for {} with livenessstate {}", tunnelInterface, livenessState);
        OperStatus opState = livenessState == LivenessState.Up ? OperStatus.Up : OperStatus.Down;
        interfaceManagerCommonUtils.setOpStateForInterface(tunnelInterface, opState);
    }
}
