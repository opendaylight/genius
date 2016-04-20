/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
public class AlivenessMonitorListener implements org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorListener {
    private static final Logger LOG = LoggerFactory.getLogger(AlivenessMonitorListener.class);
    private DataBroker dataBroker;

    public AlivenessMonitorListener(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void onMonitorEvent(MonitorEvent notification) {
        Long monitorId = notification.getEventData().getMonitorId();
        String trunkInterfaceName = AlivenessMonitorUtils.getInterfaceFromMonitorId(dataBroker, monitorId);
        if (trunkInterfaceName == null) {
            LOG.debug("Either monitoring for interface - {} not started by Interfacemgr or it is not LLDP monitoring", trunkInterfaceName);
            return;
        }
        LivenessState livenessState = notification.getEventData().getMonitorState();
        Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(trunkInterfaceName),
                dataBroker);
        IfTunnel tunnelInfo = interfaceInfo.getAugmentation(IfTunnel.class);
        // Not handling monitoring event if it is GRE Trunk Interface.
        if (tunnelInfo.getTunnelInterfaceType().isAssignableFrom(TunnelTypeGre.class)) {
            return;
        }
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus opState =
                livenessState == LivenessState.Up ? org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up :
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        InterfaceManagerCommonUtils.setOpStateForInterface(dataBroker, trunkInterfaceName, opState);
    }

}