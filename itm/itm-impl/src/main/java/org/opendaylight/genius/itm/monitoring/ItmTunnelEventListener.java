/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.monitoring;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.AbstractDataChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;

@Singleton
public class ItmTunnelEventListener extends AbstractDataChangeListener<Interface> implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ItmTunnelEventListener.class);
    private final DataBroker broker;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    public static final JMXAlarmAgent alarmAgent = new JMXAlarmAgent();

    @Inject
    public ItmTunnelEventListener(final DataBroker dataBroker){
        super(Interface.class);
        this.broker = dataBroker;
    }

    @PostConstruct
    public void start() throws Exception {
        registerListener(this.broker);
        alarmAgent.registerMbean();
       logger.info("ItmTunnelEventListener Started");
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                logger.error("Error when cleaning up DataChangeListener.", e);
            }
            listenerRegistration = null;
        }
    }

    private void registerListener(final DataBroker db) {
        try {
            listenerRegistration = broker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                    getWildCardPath(), ItmTunnelEventListener.this, AsyncDataBroker.DataChangeScope.SUBTREE);
        } catch (final Exception e) {
            logger.error("ITM Monitor Interfaces DataChange listener registration fail!", e);
            throw new IllegalStateException("ITM Monitor registration Listener failed.", e);
        }
    }

    private InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> identifier, Interface del) {
        String ifName = del.getName() ;
        if( del.getType() != null && del.getType().equals(Tunnel.class)) {
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName,broker);
            if( internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                logger.trace("ITM Tunnel removed b/w srcDpn: {} and dstDpn: {} for tunnelType: {}", srcDpId, dstDpId, tunnelType);
                String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
            }else {
                ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
                if( externalTunnel != null) {
                    String srcNode = externalTunnel.getSourceDevice();
                    String dstNode = externalTunnel.getDestinationDevice();
                    String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                    logger.trace("ITM Tunnel removed b/w srcNode: {} and dstNode: {} for tunnelType: {}", srcNode, dstNode, tunnelType);
                    String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                    clearExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                }
            }
        }
    }

    @Override
    protected void update(InstanceIdentifier<Interface> identifier, Interface original, Interface update) {
        String ifName = update.getName() ;
        if( update.getType() != null && update.getType().equals(Tunnel.class)) {
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName,broker);
            if( internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                logger.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}", isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
                if(isTunnelInterfaceUp(update)) {
                    logger.trace("ITM Tunnel State is UP b/w srcDpn: {} and dstDpn: {} for tunnelType {} ", srcDpId, dstDpId, tunnelType);
                    String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                    clearInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                }else {
                    logger.trace("ITM Tunnel State is DOWN b/w srcDpn: {} and dstDpn: {}", srcDpId, dstDpId);
                    String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                    raiseInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                }
            }else{
                ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
                if( externalTunnel != null) {
                    String srcNode = externalTunnel.getSourceDevice();
                    String dstNode = externalTunnel.getDestinationDevice();
                    if(!srcNode.contains("hwvtep")){
                        srcNode = "openflow:" + externalTunnel.getSourceDevice();
                    }
                    if(!dstNode.contains("hwvtep")){
                        dstNode = "openflow:" + externalTunnel.getDestinationDevice();
                    }
                    String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                    logger.trace("ITM Tunnel state event changed from :{} to :{} for Tunnel Interface - {}", isTunnelInterfaceUp(original), isTunnelInterfaceUp(update), ifName);
                    if(isTunnelInterfaceUp(update)) {
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
        }
    }

    @Override
    protected void add(InstanceIdentifier<Interface> identifier, Interface add) {
        String ifName = add.getName() ;
        if( add.getType() != null && add.getType().equals(Tunnel.class)) {
            InternalTunnel internalTunnel = ItmUtils.getInternalTunnel(ifName,broker);
            if( internalTunnel != null) {
                BigInteger srcDpId = internalTunnel.getSourceDPN();
                BigInteger dstDpId = internalTunnel.getDestinationDPN();
                String tunnelType = ItmUtils.convertTunnelTypetoString(internalTunnel.getTransportType());
                if(!isTunnelInterfaceUp(add)) {
                    logger.trace("ITM Tunnel State during tep add is DOWN b/w srcDpn: {} and dstDpn: {} for tunnelType: {}", srcDpId, dstDpId, tunnelType);
                    String alarmText = getInternalAlarmText(srcDpId.toString(), dstDpId.toString(), tunnelType);
                    raiseInternalDataPathAlarm(srcDpId.toString(), dstDpId.toString(), tunnelType, alarmText);
                }
            }else {
                ExternalTunnel externalTunnel = ItmUtils.getExternalTunnel(ifName,broker);
                if( externalTunnel != null) {
                    String srcNode = externalTunnel.getSourceDevice();
                    String dstNode = externalTunnel.getDestinationDevice();
                    if(!srcNode.contains("hwvtep")){
                        srcNode = "openflow:" + externalTunnel.getSourceDevice();
                    }
                    if(!dstNode.contains("hwvtep")){
                        dstNode = "openflow:" + externalTunnel.getDestinationDevice();
                    }
                    String tunnelType = ItmUtils.convertTunnelTypetoString(externalTunnel.getTransportType());
                    if(!isTunnelInterfaceUp(add)) {
                        logger.trace("ITM Tunnel State during tep add is DOWN b/w srcNode: {} and dstNode: {} for tunnelType: {}", srcNode, dstNode, tunnelType);
                        String alarmText = getExternalAlarmText(srcNode, dstNode, tunnelType);
                        raiseExternalDataPathAlarm(srcNode, dstNode, tunnelType, alarmText);
                    }
                }
            }
        }
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

    private boolean isTunnelInterfaceUp( Interface intf) {
        return Interface.OperStatus.Up.equals(intf.getOperStatus());
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

}
