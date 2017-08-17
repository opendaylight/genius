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
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.JMException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTunnelEventListener extends AsyncDataTreeChangeListenerBase<StateTunnelList,
        ItmTunnelEventListener> implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelEventListener.class);
    private final DataBroker broker;
    private JMXAlarmAgent alarmAgent;

    @Inject
    public ItmTunnelEventListener(final DataBroker dataBroker) {
        super(StateTunnelList.class, ItmTunnelEventListener.class);
        this.broker = dataBroker;
        try {
            this.alarmAgent = new JMXAlarmAgent();
        } catch (JMException e) {
            LOG.error("Can not initialize the Alarm agent", e);
        }
    }

    @PostConstruct
    public void start() throws JMException {
        registerListener(this.broker);
        if (alarmAgent != null) {
            alarmAgent.registerMbean();
        }
        LOG.info("ItmTunnelEventListener Started");
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

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void registerListener(final DataBroker db) {
        try {
            registerListener(LogicalDatastoreType.OPERATIONAL,db);
        } catch (final Exception e) {
            LOG.error("ITM Monitor Tunnel Interfaces DataTreeChange listener registration fail!", e);
            throw new IllegalStateException("ITM Monitor registration Listener failed.", e);
        }
    }

    @Override
    protected InstanceIdentifier<StateTunnelList> getWildCardPath() {
        return InstanceIdentifier.create(TunnelsState.class).child(StateTunnelList.class);
    }

    @Override
    protected ItmTunnelEventListener getDataTreeChangeListener() {
        return this;
    }

    @Override
    protected void remove(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList del) {
        LOG.trace("Tunnel Interface added: {}", del.getTunnelInterfaceName());
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        ItmTunnelRemoveAlarmWorker itmTunnelRemoveAlarmWorker = new ItmTunnelRemoveAlarmWorker(del);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelRemoveAlarmWorker);
    }

    @Override
    protected void update(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList original,
                          StateTunnelList update) {
        LOG.trace("Tunnel Interface updated. Old: {} New: {}", original, update);
        TunnelOperStatus operStatus = update.getOperState();
        if (!Objects.equals(original.getOperState(), update.getOperState())) {
            LOG.debug("Tunnel Interface {} changed state to {}", original.getTunnelInterfaceName(), operStatus);
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            ItmTunnelUpdateAlarmWorker itmTunnelUpdateAlarmWorker = new ItmTunnelUpdateAlarmWorker(original, update);
            jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelUpdateAlarmWorker);
        }
    }

    @Override
    protected void add(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList add) {
        LOG.debug("Tunnel Interface of type Tunnel added: {}", add.getTunnelInterfaceName());
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        ItmTunnelAddAlarmWorker itmTunnelAddAlarmWorker = new ItmTunnelAddAlarmWorker(add);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelAddAlarmWorker);
    }

    public void raiseInternalDataPathAlarm(String srcDpnId, String dstDpnId, String tunnelType, String alarmText) {
        StringBuilder source = new StringBuilder();
        source.append("srcDpn=openflow:").append(srcDpnId).append("-dstDpn=openflow:").append(dstDpnId)
                .append("-tunnelType").append(tunnelType);

        LOG.trace("Raising DataPathConnectionFailure alarm... alarmText {} source {} ", alarmText, source);
        // Invokes JMX raiseAlarm method
        if (alarmAgent != null) {
            alarmAgent.invokeFMraisemethod("DataPathConnectionFailure", alarmText, source.toString());
        }
    }

    public void clearInternalDataPathAlarm(String srcDpnId, String dstDpnId, String tunnelType, String alarmText) {
        StringBuilder source = new StringBuilder();

        source.append("srcDpn=openflow:").append(srcDpnId).append("-dstDpn=openflow:").append(dstDpnId)
                .append("-tunnelType").append(tunnelType);
        LOG.trace("Clearing DataPathConnectionFailure alarm of source {} alarmText {} ", source, alarmText);
        // Invokes JMX clearAlarm method
        if (alarmAgent != null) {
            alarmAgent.invokeFMclearmethod("DataPathConnectionFailure", alarmText, source.toString());
        }
    }

    public void raiseExternalDataPathAlarm(String srcDevice, String dstDevice, String tunnelType, String alarmText) {

        StringBuilder source = new StringBuilder();
        source.append("srcDevice=").append(srcDevice).append("-dstDevice=").append(dstDevice).append("-tunnelType")
                .append(tunnelType);

        LOG.trace("Raising DataPathConnectionFailure alarm... alarmText {} source {} ", alarmText, source);
        // Invokes JMX raiseAlarm method
        if (alarmAgent != null) {
            alarmAgent.invokeFMraisemethod("DataPathConnectionFailure", alarmText, source.toString());
        }
    }

    public void clearExternalDataPathAlarm(String srcDevice, String dstDevice, String tunnelType, String alarmText) {
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
        private StateTunnelList add;

        ItmTunnelAddAlarmWorker(StateTunnelList tnlIface) {
            this.add = tnlIface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            String ifName = add.getTunnelInterfaceName() ;
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName,broker);
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
                ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
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
        private StateTunnelList del;

        ItmTunnelRemoveAlarmWorker(StateTunnelList tnlIface) {
            this.del = tnlIface;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            String ifName = del.getTunnelInterfaceName() ;
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName,broker);
            if (internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                LOG.trace("ITM Tunnel removed b/w srcDpn: {} and dstDpn: {} for tunnelType: {}",
                        srcDpId, dstDpId, tunnelType);
                String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
            } else {
                ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
                if (externalTunnel != null) {
                    String srcNode = externalTunnel.getSourceDevice();
                    String dstNode = externalTunnel.getDestinationDevice();
                    String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                    LOG.trace("ITM Tunnel removed b/w srcNode: {} and dstNode: {} for tunnelType: {}", srcNode,
                            dstNode, tunnelType);
                    String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                    clearExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                }
            }
            return null;
        }

    }

    private class ItmTunnelUpdateAlarmWorker implements Callable<List<ListenableFuture<Void>>> {
        private StateTunnelList update;
        private StateTunnelList original;

        ItmTunnelUpdateAlarmWorker(StateTunnelList original, StateTunnelList update) {
            this.update = update;
            this.original = original;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            String ifName = update.getTunnelInterfaceName() ;
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName,broker);
            if (internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                if (LOG.isTraceEnabled()) {
                    LOG.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}",
                            isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
                }
                if (update.getOperState() == TunnelOperStatus.Unknown) {
                    return null;
                } else if (update.getOperState() == TunnelOperStatus.Up) {
                    LOG.trace("ITM Tunnel State is UP b/w srcDpn: {} and dstDpn: {} for tunnelType {} ",
                            srcDpId, dstDpId, tunnelType);
                    String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                    clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                } else if (update.getOperState() == TunnelOperStatus.Down) {
                    LOG.trace("ITM Tunnel State is DOWN b/w srcDpn: {} and dstDpn: {}", srcDpId, dstDpId);
                    String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                    raiseInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                }
            } /*else{
                    // Uncomment this when tunnel towards DC gateway or HwVtep is supported
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
