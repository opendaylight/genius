/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

public class MDSALUtilProvider implements BindingAwareConsumer, IMdsalApiManager, AutoCloseable {

    private static final Logger s_logger = LoggerFactory.getLogger(MDSALUtilProvider.class);
    private MDSALManager mdSalMgr;
    private static final long FIXED_DELAY_IN_MILLISECONDS = 5000;

    @Override
    public void onSessionInitialized(ConsumerContext session) {

        s_logger.info( " Session Initiated for MD SAL Util Provider") ;

        try {
            final DataBroker dataBroker;
            final PacketProcessingService packetProcessingService;
            dataBroker = session.getSALService(DataBroker.class);
            packetProcessingService = session.getRpcService(PacketProcessingService.class);
             mdSalMgr = new MDSALManager( dataBroker, packetProcessingService) ;
        }catch( Exception e) {
            s_logger.error( "Error initializing MD SAL Util Services " + e );
        }
    }


    @Override
    public void close() throws Exception {
        mdSalMgr.close();
        s_logger.info("MDSAL Manager Closed");
    }


    @Override
    public void installFlow(FlowEntity flowEntity) {
          mdSalMgr.installFlow(flowEntity);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> installFlow(BigInteger dpId, Flow flowEntity) {
        return mdSalMgr.installFlow(dpId, flowEntity);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> installFlow(BigInteger dpId, FlowEntity flowEntity) {
        return mdSalMgr.installFlow(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpId, Flow flowEntity) {
        return mdSalMgr.removeFlowNew(dpId, flowEntity);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> removeFlow(BigInteger dpId, FlowEntity flowEntity) {
        return mdSalMgr.removeFlowNew(dpId, flowEntity.getFlowBuilder().build());
    }

    @Override
    public void removeFlow(FlowEntity flowEntity) {
        mdSalMgr.removeFlow(flowEntity);
    }

    @Override
    public void installGroup(GroupEntity groupEntity) {
        mdSalMgr.installGroup(groupEntity);
    }

    @Override
    public void modifyGroup(GroupEntity groupEntity) {
        mdSalMgr.modifyGroup(groupEntity);
    }


    @Override
    public void removeGroup(GroupEntity groupEntity) {
        mdSalMgr.removeGroup(groupEntity);
    }

    @Override
    public void sendPacketOut(BigInteger dpnId, int groupId, byte[] payload) {
        mdSalMgr.sendPacketOut(dpnId, groupId, payload);
    }


    @Override
    public void sendPacketOutWithActions(BigInteger dpnId, long groupId,
                                         byte[] payload, List<ActionInfo> actionInfos) {
        mdSalMgr.sendPacketOutWithActions(dpnId, groupId, payload, actionInfos);
    }


    @Override
    public void sendARPPacketOutWithActions(BigInteger dpnId, byte[] payload,
                                            List<ActionInfo> action_info) {
        mdSalMgr.sendARPPacketOutWithActions(dpnId, payload, action_info);
    }

    @Override
    public void syncRemoveFlow(FlowEntity flowEntity, long delayTime) {
        mdSalMgr.syncSetUpFlow(flowEntity,  delayTime, true);
    }

    @Override
    public void syncInstallFlow(FlowEntity flowEntity, long delayTime) {
        mdSalMgr.syncSetUpFlow(flowEntity, delayTime, false);
    }

    @Override
    public void syncInstallGroup(GroupEntity groupEntity, long delayTime) {
        mdSalMgr.syncSetUpGroup(groupEntity, delayTime, false);
    }

    @Override
    public void syncInstallGroup(BigInteger dpId, Group group, long delayTime) {
        mdSalMgr.syncSetUpGroup(dpId, group, delayTime, false);
    }

    @Override
    public void syncRemoveGroup(GroupEntity groupEntity) {
        mdSalMgr.syncSetUpGroup(groupEntity, FIXED_DELAY_IN_MILLISECONDS, true);
    }

    @Override
    public void syncRemoveGroup(BigInteger dpId, Group group) {
        mdSalMgr.syncSetUpGroup(dpId, group, FIXED_DELAY_IN_MILLISECONDS, true);
    }

    @Override
    public void addFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        mdSalMgr.writeFlowEntity(flowEntity, tx);
    }

    @Override
    public void addFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx) {
        mdSalMgr.writeFlow(dpId, flow, tx);
    }

    @Override
    public void removeFlowToTx(BigInteger dpId, Flow flow, WriteTransaction tx) {
        mdSalMgr.deleteFlow(dpId, flow, tx);
    }

    @Override
    public void removeFlowToTx(FlowEntity flowEntity, WriteTransaction tx) {
        mdSalMgr.deleteFlowEntity(flowEntity, tx);
    }

    @Override
    public void addGroupToTx(GroupEntity groupEntity, WriteTransaction tx) {
        mdSalMgr.writeGroupEntity(groupEntity, tx);
    }

    @Override
    public void addGroupToTx(BigInteger dpId, Group group, WriteTransaction tx) {
        mdSalMgr.writeGroup(dpId, group, tx);
    }

    @Override
    public void removeGroupToTx(GroupEntity groupEntity, WriteTransaction tx) {
        mdSalMgr.removeGroupEntity(groupEntity, tx);
    }

    @Override
    public void removeGroupToTx(BigInteger dpId, Group group, WriteTransaction tx) {
        mdSalMgr.deleteGroup(dpId, group, tx);
    }

}
