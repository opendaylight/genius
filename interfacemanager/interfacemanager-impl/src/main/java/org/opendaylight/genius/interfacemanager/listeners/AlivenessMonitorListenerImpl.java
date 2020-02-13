/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.commons.AlivenessMonitorUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.infrautils.utils.concurrent.ListenableFutures;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.LivenessState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorEvent;
import org.opendaylight.yangtools.yang.common.Uint32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
@Singleton
public class AlivenessMonitorListenerImpl implements AlivenessMonitorListener {
    private static final Logger LOG = LoggerFactory.getLogger(AlivenessMonitorListenerImpl.class);

    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public AlivenessMonitorListenerImpl(@Reference final DataBroker dataBroker,
                                        @Reference final NotificationService notificationService) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
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
        ListenableFutures.addErrorLogging(txRunner.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
            Uint32 monitorId = notification.getEventData().getMonitorId();
            String tunnelInterface = AlivenessMonitorUtils.getInterfaceFromMonitorId(tx, monitorId);
            if (tunnelInterface == null) {
                LOG.debug("Either monitoring for interface not started by Interfacemgr or it is not LLDP monitoring");
                return;
            }
            LivenessState livenessState = notification.getEventData().getMonitorState();
            LOG.debug("received monitor event for {} with livenessstate {}", tunnelInterface, livenessState);
            OperStatus opState = livenessState == LivenessState.Up ? OperStatus.Up : OperStatus.Down;
            InterfaceManagerCommonUtils.setOpStateForInterface(tx, tunnelInterface, opState);
        }), LOG, "Error processing monitor event {}", notification);
    }
}
