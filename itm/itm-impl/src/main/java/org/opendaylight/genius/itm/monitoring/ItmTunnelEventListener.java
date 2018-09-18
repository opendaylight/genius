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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.JMException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.UnprocessedTunnelsStateCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTunnelEventListener extends AsyncDataTreeChangeListenerBase<StateTunnelList,
        ItmTunnelEventListener> implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelEventListener.class);

    private final DataBroker broker;
    private final JobCoordinator jobCoordinator;
    private JMXAlarmAgent alarmAgent;
    private UnprocessedTunnelsStateCache unprocessedTunnelsStateCache;
    protected final DpnTepStateCache dpnTepStateCache;
    private final IInterfaceManager interfaceManager;

    @Inject
    public ItmTunnelEventListener(final DataBroker dataBroker,
                                  final DpnTepStateCache dpnTepStateCache,
                                  final IInterfaceManager interfaceManager,
                                  UnprocessedTunnelsStateCache unprocessedTunnelsStateCache,
                                  JobCoordinator jobCoordinator) {
        super(StateTunnelList.class, ItmTunnelEventListener.class);
        this.broker = dataBroker;
        this.dpnTepStateCache = dpnTepStateCache;
        this.interfaceManager = interfaceManager;
        this.jobCoordinator = jobCoordinator;
        this.unprocessedTunnelsStateCache = unprocessedTunnelsStateCache;
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
        LOG.trace("Tunnel Interface removed: {}", del.getTunnelInterfaceName());
        ItmTunnelRemoveAlarmWorker itmTunnelRemoveAlarmWorker = new ItmTunnelRemoveAlarmWorker(del);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelRemoveAlarmWorker);
        unprocessedTunnelsStateCache.remove(del.getTunnelInterfaceName());
    }

    @Override
    protected void update(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList original,
                          StateTunnelList update) {
        LOG.trace("Tunnel Interface updated. Old: {} New: {}", original, update);
        TunnelOperStatus operStatus = update.getOperState();
        if (!Objects.equals(original.getOperState(), update.getOperState())) {
            LOG.debug("Tunnel Interface {} changed state to {}", original.getTunnelInterfaceName(), operStatus);
            ItmTunnelUpdateAlarmWorker itmTunnelUpdateAlarmWorker = new ItmTunnelUpdateAlarmWorker(original, update);
            jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelUpdateAlarmWorker);
        }
    }

    @Override
    protected void add(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList add) {
        LOG.debug("Tunnel Interface added: {}", add.getTunnelInterfaceName());
        ItmTunnelAddAlarmWorker itmTunnelAddAlarmWorker = new ItmTunnelAddAlarmWorker(add);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelAddAlarmWorker);
        TunnelOperStatus operStatus = unprocessedTunnelsStateCache.remove(add.getTunnelInterfaceName());
        if (operStatus != null) {
            if (operStatus != add.getOperState()) {
                jobCoordinator.enqueueJob(add.getTunnelInterfaceName(),
                        new ItmTunnelStatusOutOfOrderEventWorker(identifier, add, operStatus));
            } else {
                LOG.debug("BFD status in unprocessed cache is the same as in DTCN for {} "
                    + "hence no operations ",add.getTunnelInterfaceName());
            }
        } else {
            LOG.debug("No Unprocessed tunnel state for {} ", add.getTunnelInterfaceName());
        }
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
            BigInteger srcDpnId;
            BigInteger dstDpnId;
            String tunnelTypeStr = null;
            String srcDpnIdStr = null;
            String dstDpnIdStr = null;
            boolean isInternal = true;
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                TunnelEndPointInfo tunnelEndPointInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(ifName);
                if (tunnelEndPointInfo != null) {
                    srcDpnIdStr = tunnelEndPointInfo.getSrcEndPointInfo();
                    dstDpnIdStr = tunnelEndPointInfo.getDstEndPointInfo();
                    DpnTepInterfaceInfo dpnTepInterfaceInfo = dpnTepStateCache.getTunnelFromCache(ifName);
                    if (dpnTepInterfaceInfo != null) {
                        tunnelTypeStr = ItmUtils.convertTunnelTypetoString(dpnTepInterfaceInfo.getTunnelType());
                    } else {
                        LOG.error("Could not get tunnel type for Tunnel Interface {}. Can not proceed further.",
                                ifName);
                        return null;
                    }
                } else {
                    isInternal = false;
                }
            } else {
                InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName, broker);
                if (internalTunnel != null) {
                    srcDpnId = internalTunnel.getSourceDPN();
                    dstDpnId = internalTunnel.getDestinationDPN();
                    srcDpnIdStr = srcDpnId.toString();
                    dstDpnIdStr = dstDpnId.toString();
                    tunnelTypeStr = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                } else {
                    isInternal = false;
                }
            }
            if (!isTunnelInterfaceUp(add) && isInternal) {
                LOG.trace("ITM Tunnel State during tep add is DOWN b/w srcDpn: {} and dstDpn: {} for tunnelType: {}",
                        srcDpnIdStr, dstDpnIdStr, tunnelTypeStr);
                String alarmText = getInternalAlarmText(srcDpnIdStr, dstDpnIdStr, tunnelTypeStr);
                raiseInternalDataPathAlarm(srcDpnIdStr, dstDpnIdStr, tunnelTypeStr, alarmText);
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
            boolean isInternal = true;
            BigInteger srcDpnId;
            BigInteger dstDpnId;
            String tunnelTypeStr = null;
            String srcDpnIdStr = null;
            String dstDpnIdStr = null;

            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                TunnelEndPointInfo tunnelEndPointInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(ifName);
                if (tunnelEndPointInfo != null) {
                    srcDpnIdStr = tunnelEndPointInfo.getSrcEndPointInfo();
                    dstDpnIdStr = tunnelEndPointInfo.getDstEndPointInfo();
                    DpnTepInterfaceInfo dpnTepInterfaceInfo = dpnTepStateCache.getTunnelFromCache(ifName);
                    if (dpnTepInterfaceInfo != null) {
                        tunnelTypeStr = ItmUtils.convertTunnelTypetoString(dpnTepInterfaceInfo.getTunnelType());
                    } else {
                        LOG.error("Could not get tunnel type for Tunnel Interface {}. Can not proceed further.",
                                ifName);
                        return null;
                    }
                } else {
                    isInternal = false;
                }
            } else {
                InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName, broker);
                if (internalTunnel != null) {
                    srcDpnId = internalTunnel.getSourceDPN();
                    dstDpnId = internalTunnel.getDestinationDPN();
                    String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                    LOG.trace("ITM Tunnel removed b/w srcDpn: {} and dstDpn: {} for tunnelType: {}",
                            srcDpnId, dstDpnId, tunnelType);
                    String alarmText = getInternalAlarmText(srcDpnId.toString(), dstDpnId.toString(), tunnelType);
                    clearInternalDataPathAlarm(srcDpnId.toString(), dstDpnId.toString(), tunnelType, alarmText);
                } else {
                    isInternal = false;
                }
            }
            if (isInternal && !isTunnelInterfaceUp(del)) {
                // If the deleted interface was down, need to clear the alarm
                LOG.trace("ITM Tunnel removed b/w srcDpn: {} and dstDpn: {} for tunnelType: {}",
                        srcDpnIdStr, dstDpnIdStr, tunnelTypeStr);
                String alarmText = getInternalAlarmText(srcDpnIdStr, dstDpnIdStr, tunnelTypeStr);
                clearInternalDataPathAlarm(srcDpnIdStr, dstDpnIdStr, tunnelTypeStr, alarmText);
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
            String ifName = update.getTunnelInterfaceName();
            boolean isInternal = true;
            String tunnelTypeStr = null;
            String srcDpnIdStr = null;
            String dstDpnIdStr = null;

            if (LOG.isTraceEnabled()) {
                LOG.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}",
                        isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
            }
            if (update.getOperState().equals(TunnelOperStatus.Unknown)) {
                return null;
            }
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                TunnelEndPointInfo tunnelEndPointInfo = dpnTepStateCache.getTunnelEndPointInfoFromCache(ifName);
                if (tunnelEndPointInfo != null) {
                    srcDpnIdStr = tunnelEndPointInfo.getSrcEndPointInfo();
                    dstDpnIdStr = tunnelEndPointInfo.getDstEndPointInfo();
                    DpnTepInterfaceInfo dpnTepInterfaceInfo = dpnTepStateCache.getTunnelFromCache(ifName);
                    if (dpnTepInterfaceInfo != null) {
                        tunnelTypeStr = ItmUtils.convertTunnelTypetoString(dpnTepInterfaceInfo.getTunnelType());
                    } else {
                        LOG.error("Could not get tunnel type for Tunnel Interface {}. Can not proceed further.",
                                ifName);
                        return null;
                    }
                } else {
                    isInternal = false;
                }
            } else {
                InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName, broker);
                if (internalTunnel != null) {
                    srcDpnIdStr = internalTunnel.getSourceDPN().toString();
                    dstDpnIdStr = internalTunnel.getDestinationDPN().toString();
                    tunnelTypeStr = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                } else {
                    isInternal = false;
                }
            }
            if (isInternal) {
                String alarmText = getInternalAlarmText(srcDpnIdStr, dstDpnIdStr, tunnelTypeStr);
                if (update.getOperState() == TunnelOperStatus.Up) {
                    LOG.trace("ITM Tunnel State is UP b/w srcDpn: {} and dstDpn: {} for tunnelType {} ",
                            srcDpnIdStr, dstDpnIdStr, tunnelTypeStr);
                    clearInternalDataPathAlarm(srcDpnIdStr, dstDpnIdStr, tunnelTypeStr, alarmText);
                } else if (update.getOperState() == TunnelOperStatus.Down) {
                    LOG.trace("ITM Tunnel State is DOWN b/w srcDpn: {} and dstDpn: {}",
                            srcDpnIdStr, dstDpnIdStr);
                    raiseInternalDataPathAlarm(srcDpnIdStr, dstDpnIdStr, tunnelTypeStr, alarmText);
                }
            }
            return null;
        }

    }

    private class ItmTunnelStatusOutOfOrderEventWorker implements Callable<List<ListenableFuture<Void>>> {
        private InstanceIdentifier<StateTunnelList> identifier;
        private StateTunnelList add;
        private TunnelOperStatus operStatus;

        ItmTunnelStatusOutOfOrderEventWorker(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList add,
                                             TunnelOperStatus operStatus) {
            this.identifier = identifier;
            this.add = add;
            this.operStatus = operStatus;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // Process any unprocessed interface bfd updates
            final List<ListenableFuture<Void>> futures = new ArrayList<>();
            LOG.debug(" Tunnel events are processed out order for {} hence updating it from cache",
                    add.getTunnelInterfaceName());
            WriteTransaction transaction = broker.newWriteOnlyTransaction();
            StateTunnelListBuilder stlBuilder = new StateTunnelListBuilder(add);
            stlBuilder.setOperState(operStatus);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, identifier, stlBuilder.build(), false);
            futures.add(transaction.submit());
            return futures;
        }
    }
}
