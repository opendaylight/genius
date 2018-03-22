/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.monitoring;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.JMException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTunnelEventListener extends AbstractSyncDataTreeChangeListener<StateTunnelList> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelEventListener.class);

    private final DataBroker broker;
    private final JobCoordinator jobCoordinator;
    private JMXAlarmAgent alarmAgent;

    @Inject
    public ItmTunnelEventListener(DataBroker dataBroker, JobCoordinator jobCoordinator) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.create(TunnelsState.class).child(StateTunnelList.class));
        this.broker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        try {
            alarmAgent = new JMXAlarmAgent();
            alarmAgent.registerMbean();
        } catch (JMException e) {
            LOG.error("Can not initialize the Alarm agent", e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    @PreDestroy
    public void close() {
        try {
            if (alarmAgent != null) {
                alarmAgent.unregisterMbean();
            }
        } catch (final Exception e) {
            LOG.error("Error when cleaning up DataChangeListener.", e);
        }
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<StateTunnelList> instanceIdentifier,
                       @Nonnull StateTunnelList stateTunnelList) {
        LOG.trace("Tunnel Interface added: {}", stateTunnelList.getTunnelInterfaceName());
        ItmTunnelRemoveAlarmWorker itmTunnelRemoveAlarmWorker = new ItmTunnelRemoveAlarmWorker(stateTunnelList);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelRemoveAlarmWorker);
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<StateTunnelList> instanceIdentifier,
                       @Nonnull StateTunnelList originalTunnelList, @Nonnull StateTunnelList updatedTunnelList) {
        LOG.trace("Tunnel Interface updated. Old: {} New: {}", originalTunnelList, updatedTunnelList);
        TunnelOperStatus operStatus = updatedTunnelList.getOperState();
        if (!Objects.equals(originalTunnelList.getOperState(), updatedTunnelList.getOperState())) {
            LOG.debug("Tunnel Interface {} changed state to {}", originalTunnelList.getTunnelInterfaceName(),
                      operStatus);
            ItmTunnelUpdateAlarmWorker itmTunnelUpdateAlarmWorker = new ItmTunnelUpdateAlarmWorker(originalTunnelList,
                                                                                                   updatedTunnelList);
            jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelUpdateAlarmWorker);
        }
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<StateTunnelList> instanceIdentifier,
                    @Nonnull StateTunnelList stateTunnelList) {
        LOG.debug("Tunnel Interface of type Tunnel added: {}", stateTunnelList.getTunnelInterfaceName());
        ItmTunnelAddAlarmWorker itmTunnelAddAlarmWorker = new ItmTunnelAddAlarmWorker(stateTunnelList);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelAddAlarmWorker);
    }

    private void raiseInternalDataPathAlarm(String srcDpnId, String dstDpnId, String tunnelType, String alarmText) {
        StringBuilder source = new StringBuilder();
        source.append("srcDpn=openflow:").append(srcDpnId).append("-dstDpn=openflow:").append(dstDpnId)
                .append("-tunnelType").append(tunnelType);

        LOG.trace("Raising DataPathConnectionFailure alarm... alarmText {} source {} ", alarmText, source);
        // Invokes JMX raiseAlarm method
        if (alarmAgent != null) {
            alarmAgent.invokeFMraisemethod("DataPathConnectionFailure", alarmText, source.toString());
        }
    }

    private void clearInternalDataPathAlarm(String srcDpnId, String dstDpnId, String tunnelType, String alarmText) {
        StringBuilder source = new StringBuilder();

        source.append("srcDpn=openflow:").append(srcDpnId).append("-dstDpn=openflow:").append(dstDpnId)
                .append("-tunnelType").append(tunnelType);
        LOG.trace("Clearing DataPathConnectionFailure alarm of source {} alarmText {} ", source, alarmText);
        // Invokes JMX clearAlarm method
        if (alarmAgent != null) {
            alarmAgent.invokeFMclearmethod("DataPathConnectionFailure", alarmText, source.toString());
        }
    }

    private void raiseExternalDataPathAlarm(String srcDevice, String dstDevice, String tunnelType, String alarmText) {

        StringBuilder source = new StringBuilder();
        source.append("srcDevice=").append(srcDevice).append("-dstDevice=").append(dstDevice).append("-tunnelType")
                .append(tunnelType);

        LOG.trace("Raising DataPathConnectionFailure alarm... alarmText {} source {} ", alarmText, source);
        // Invokes JMX raiseAlarm method
        if (alarmAgent != null) {
            alarmAgent.invokeFMraisemethod("DataPathConnectionFailure", alarmText, source.toString());
        }
    }

    private void clearExternalDataPathAlarm(String srcDevice, String dstDevice, String tunnelType, String alarmText) {
        StringBuilder source = new StringBuilder();
        source.append("srcDevice=").append(srcDevice).append("-dstDevice=").append(dstDevice).append("-tunnelType")
                .append(tunnelType);
        LOG.trace("Clearing DataPathConnectionFailure alarm of source {} alarmText {} ", source, alarmText);
        // Invokes JMX clearAlarm method
        if (alarmAgent != null) {
            alarmAgent.invokeFMclearmethod("DataPathConnectionFailure", alarmText, source.toString());
        }
    }

    private boolean isTunnelInterfaceUp(StateTunnelList intf) {
        return (intf.getOperState() == TunnelOperStatus.Up);
    }

    private String getInternalAlarmText(String srcDpId, String dstDpId, String tunnelType) {
        StringBuilder alarmText = new StringBuilder();
        alarmText.append("Data Path Connectivity is lost between ").append("openflow:").append(srcDpId)
                .append(" and openflow:").append(dstDpId).append(" for tunnelType:").append(tunnelType);
        return alarmText.toString();
    }

    private String getExternalAlarmText(String srcNode, String dstNode, String tunnelType) {
        StringBuilder alarmText = new StringBuilder();
        alarmText.append("Data Path Connectivity is lost between ").append(srcNode).append(" and ").append(dstNode)
                .append(" for tunnelType:").append(tunnelType);
        return alarmText.toString();
    }

    private class ItmTunnelAddAlarmWorker implements Callable<List<ListenableFuture<Void>>> {
        private final StateTunnelList add;

        ItmTunnelAddAlarmWorker(StateTunnelList tnlIface) {
            this.add = tnlIface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            String ifName = add.getTunnelInterfaceName();
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName, broker);
            if (internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                if (!isTunnelInterfaceUp(add)) {
                    LOG.trace("ITM Tunnel State during tep add is DOWN b/w srcDpn: {} and dstDpn: {} for tunnelType: "
                                      + "{}", srcDpId, dstDpId, tunnelType);
                    String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                    raiseInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                }
            } else {
                ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName, broker);
                if (externalTunnel != null) {
                    String srcNode = externalTunnel.getSourceDevice();
                    String dstNode = externalTunnel.getDestinationDevice();
                    if (!srcNode.contains("hwvtep")) {
                        srcNode = "openflow:" + externalTunnel.getSourceDevice();
                    }
                    if (!dstNode.contains("hwvtep")) {
                        dstNode = "openflow:" + externalTunnel.getDestinationDevice();
                    }
                    String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                    if (!isTunnelInterfaceUp(add)) {
                        LOG.trace("ITM Tunnel State during tep add is DOWN b/w srcNode: {} and dstNode: {} for "
                                          + "tunnelType: {}", srcNode, dstNode, tunnelType);
                        String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                        raiseExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                    }
                }
            }
            return null;
        }
    }

    private class ItmTunnelRemoveAlarmWorker implements Callable<List<ListenableFuture<Void>>> {
        private final StateTunnelList del;

        ItmTunnelRemoveAlarmWorker(StateTunnelList tnlIface) {
            this.del = tnlIface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            String ifName = del.getTunnelInterfaceName();
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName, broker);
            if (internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                LOG.trace("ITM Tunnel removed b/w srcDpn: {} and dstDpn: {} for tunnelType: {}", srcDpId, dstDpId,
                          tunnelType);
                String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
            } else {
                ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName, broker);
                if (externalTunnel != null) {
                    String srcNode = externalTunnel.getSourceDevice();
                    String dstNode = externalTunnel.getDestinationDevice();
                    String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                    LOG.trace("ITM Tunnel removed b/w srcNode: {} and dstNode: {} for tunnelType: {}", srcNode, dstNode,
                              tunnelType);
                    String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                    clearExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                }
            }
            return null;
        }
    }

    private class ItmTunnelUpdateAlarmWorker implements Callable<List<ListenableFuture<Void>>> {
        private final StateTunnelList update;
        private final StateTunnelList original;

        ItmTunnelUpdateAlarmWorker(StateTunnelList original, StateTunnelList update) {
            this.update = update;
            this.original = original;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            String ifName = update.getTunnelInterfaceName();
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName, broker);
            if (internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                if (LOG.isTraceEnabled()) {
                    LOG.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}",
                              isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
                }
                switch (update.getOperState()) {
                    case Up: {
                        LOG.trace("ITM Tunnel State is UP b/w srcDpn: {} and dstDpn: {} for tunnelType {} ", srcDpId,
                                dstDpId, tunnelType);
                        String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                        clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                        break;
                    }
                    case Down: {
                        LOG.trace("ITM Tunnel State is DOWN b/w srcDpn: {} and dstDpn: {}", srcDpId, dstDpId);
                        String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                        raiseInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                        break;
                    }
                    case Unknown:
                    default:
                        return null;
                }
            }
             /*else{
                    // TODO: Uncomment this when tunnel towards DC gateway or HwVtep is supported
                    ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
                    if (externalTunnel != null) {
                        String srcNode = externalTunnel.getSourceDevice();
                        String dstNode = externalTunnel.getDestinationDevice();
                        if (!srcNode.contains("hwvtep")){
                            srcNode = "openflow:" + externalTunnel.getSourceDevice();
                        }
                        if (!dstNode.contains("hwvtep")){
                            dstNode = "openflow:" + externalTunnel.getDestinationDevice();
                        }
                        String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                        logger.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}",
                         isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
                        if (isTunnelInterfaceUp(update)) {
                            logger.trace("ITM Tunnel State is UP b/w srcNode: {} and dstNode: {} for tunnelType: {}",
                             srcNode, dstNode, tunnelType);
                            String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                            clearExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                        }else {
                            logger.trace("ITM Tunnel State is DOWN b/w srcNode: {} and dstNode: {}", srcNode, dstNode);
                            String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                            raiseExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                        }
                    }
                }*/
            return null;
        }
    }
}
