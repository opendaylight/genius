/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.monitoring;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.genius.itm.globals.ITMConstants;
import java.util.Objects;

public class ItmTunnelEventListener extends AsyncDataTreeChangeListenerBase<StateTunnelList, ItmTunnelEventListener> implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ItmTunnelEventListener.class);
    private final DataBroker broker;
    
    public static final JMXAlarmAgent alarmAgent = new JMXAlarmAgent();

    public ItmTunnelEventListener(final DataBroker db){
        super(StateTunnelList.class, ItmTunnelEventListener.class);
        broker = db;
        registerListener(db);
        alarmAgent.registerMbean();
    }

    private void registerListener(final DataBroker db) {
        try {
            registerListener(LogicalDatastoreType.OPERATIONAL,db);
        } catch (final Exception e) {
            logger.error("ITM Monitor Tunnel Interfaces DataTreeChange listener registration fail!", e);
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
        logger.trace("Tunnel Interface removed: {}", del.getTunnelInterfaceName());
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        ItmTunnelRemoveAlarmWorker itmTunnelRemoveAlarmWorker = new ItmTunnelRemoveAlarmWorker(del);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelRemoveAlarmWorker);
    }

    @Override
    protected void update(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList original, StateTunnelList update) {
        logger.trace("Tunnel Interface updated. Old: {} New: {}", original, update);
        TunnelOperStatus operStatus = update.getOperState();
        if (!Objects.equals(original.getOperState(), update.getOperState())) {
            logger.debug("Tunnel Interface {} changed state to {}", original.getTunnelInterfaceName(), operStatus);
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            ItmTunnelUpdateAlarmWorker itmTunnelUpdateAlarmWorker = new ItmTunnelUpdateAlarmWorker(original, update);
            jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelUpdateAlarmWorker);
        }
    }

    @Override
    protected void add(InstanceIdentifier<StateTunnelList> identifier, StateTunnelList add) {
        logger.debug("Tunnel Interface of type Tunnel added: {}", add.getTunnelInterfaceName());
        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        ItmTunnelAddAlarmWorker itmTunnelAddAlarmWorker = new ItmTunnelAddAlarmWorker(add);
        // For now, its all queued in one queue. If any delay in alarm being raised, queue based on interface Name
        jobCoordinator.enqueueJob(ITMConstants.ITM_ALARM, itmTunnelAddAlarmWorker);
    }

    @Override
    public void close() throws Exception {
        logger.info("Tunnel Event listener Closed");
    }

    public void raiseInternalDataPathAlarm(String srcDpnId, String dstDpnId, String tunnelType, String alarmText) {

        StringBuilder source = new StringBuilder();
        source.append("srcDpn=openflow:").append(srcDpnId).append("-dstDpn=openflow:").append(dstDpnId).append("-tunnelType").append(tunnelType);

        logger.trace("Raising DataPathConnectionFailure alarm... alarmText {} source {} ", alarmText, source);
        //Invokes JMX raiseAlarm method
        alarmAgent.invokeFMraisemethod("DataPathConnectionFailure", alarmText, source.toString());
    }

    public void clearInternalDataPathAlarm(String srcDpnId, String dstDpnId, String tunnelType, String alarmText) {
        StringBuilder source = new StringBuilder();

        source.append("srcDpn=openflow:").append(srcDpnId).append("-dstDpn=openflow:").append(dstDpnId).append("-tunnelType").append(tunnelType);
        logger.trace("Clearing DataPathConnectionFailure alarm of source {} alarmText {} ", source, alarmText);
        //Invokes JMX clearAlarm method
        alarmAgent.invokeFMclearmethod("DataPathConnectionFailure", alarmText, source.toString());
    }

    public void raiseExternalDataPathAlarm(String srcDevice, String dstDevice, String tunnelType, String alarmText) {

        StringBuilder source = new StringBuilder();
        source.append("srcDevice=").append(srcDevice).append("-dstDevice=").append(dstDevice).append("-tunnelType").append(tunnelType);

        logger.trace("Raising DataPathConnectionFailure alarm... alarmText {} source {} ", alarmText, source);
        //Invokes JMX raiseAlarm method
        alarmAgent.invokeFMraisemethod("DataPathConnectionFailure", alarmText, source.toString());
    }


    public void clearExternalDataPathAlarm(String srcDevice, String dstDevice, String tunnelType, String alarmText) {
        StringBuilder source = new StringBuilder();
        source.append("srcDevice=").append(srcDevice).append("-dstDevice=").append(dstDevice).append("-tunnelType").append(tunnelType);
        logger.trace("Clearing DataPathConnectionFailure alarm of source {} alarmText {} ", source, alarmText);
        //Invokes JMX clearAlarm method
        alarmAgent.invokeFMclearmethod("DataPathConnectionFailure", alarmText, source.toString());

    }

    private boolean isTunnelInterfaceUp( StateTunnelList intf) {
        boolean interfaceUp = (intf.getOperState().equals(Interface.OperStatus.Up)) ? true :false ;
        return interfaceUp ;
    }

    private String getInternalAlarmText(String srcDpId, String dstDpId, String tunnelType) {
        StringBuilder alarmText = new StringBuilder();
        alarmText.append("Data Path Connectivity is lost between ").append("openflow:").append(srcDpId).append(" and openflow:")
                .append(dstDpId).append(" for tunnelType:").append(tunnelType);
        return alarmText.toString();
    }

    private String getExternalAlarmText(String srcNode, String dstNode, String tunnelType) {
        StringBuilder alarmText = new StringBuilder();
        alarmText.append("Data Path Connectivity is lost between ").append(srcNode).append(" and ").append(
                dstNode).append(" for tunnelType:").append(tunnelType);
        return alarmText.toString();
    }

    private class ItmTunnelAddAlarmWorker implements Callable<List<ListenableFuture<Void>>> {
        private StateTunnelList add;

        public ItmTunnelAddAlarmWorker(StateTunnelList tnlIface) {
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
                        logger.trace("ITM Tunnel State during tep add is DOWN b/w srcDpn: {} and dstDpn: {} for tunnelType: {}", srcDpId, dstDpId, tunnelType);
                        String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                        raiseInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                    }
                }else {
                    ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
                    if (externalTunnel != null) {
                        String srcNode = externalTunnel.getSourceDevice();
                        String dstNode = externalTunnel.getDestinationDevice();
                        if(!srcNode.contains("hwvtep")){
                            srcNode = "openflow:" + externalTunnel.getSourceDevice();
                        }
                        if (!dstNode.contains("hwvtep")){
                            dstNode = "openflow:" + externalTunnel.getDestinationDevice();
                        }
                        String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                        if (!isTunnelInterfaceUp(add)) {
                            logger.trace("ITM Tunnel State during tep add is DOWN b/w srcNode: {} and dstNode: {} for tunnelType: {}", srcNode, dstNode, tunnelType);
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

        public ItmTunnelRemoveAlarmWorker(StateTunnelList tnlIface) {
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
                    logger.trace("ITM Tunnel removed b/w srcDpn: {} and dstDpn: {} for tunnelType: {}", srcDpId, dstDpId, tunnelType);
                    String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                    clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                }else {
                    ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
                    if (externalTunnel != null) {
                        String srcNode = externalTunnel.getSourceDevice();
                        String dstNode = externalTunnel.getDestinationDevice();
                        String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                        logger.trace("ITM Tunnel removed b/w srcNode: {} and dstNode: {} for tunnelType: {}", srcNode, dstNode, tunnelType);
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

        public ItmTunnelUpdateAlarmWorker(StateTunnelList original, StateTunnelList update) {
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
                    logger.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}", isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
                    if (update.getOperState().equals(Interface.OperStatus.Unknown)){
                        return null;
                    }
                    else if (update.getOperState().equals(Interface.OperStatus.Up)) {
                        logger.trace("ITM Tunnel State is UP b/w srcDpn: {} and dstDpn: {} for tunnelType {} ", srcDpId, dstDpId, tunnelType);
                        String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                        clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                    }else if (update.getOperState().equals(Interface.OperStatus.Down)){
                        logger.trace("ITM Tunnel State is DOWN b/w srcDpn: {} and dstDpn: {}", srcDpId, dstDpId);
                        String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                        raiseInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                    }
                }else{
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
                        logger.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}", isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
                        if (isTunnelInterfaceUp(update)) {
                            logger.trace("ITM Tunnel State is UP b/w srcNode: {} and dstNode: {} for tunnelType: {}", srcNode, dstNode, tunnelType);
                            String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                            clearExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                        }else {
                            logger.trace("ITM Tunnel State is DOWN b/w srcNode: {} and dstNode: {}", srcNode, dstNode);
                            String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                            raiseExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                        }
                    }
                }
            return null;
        }

    }
}
