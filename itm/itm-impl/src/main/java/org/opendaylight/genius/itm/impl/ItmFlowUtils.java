/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelSourceIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.TunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ItmFlowUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmFlowUtils.class);
    private final DataBroker dataBroker;
    private final IMdsalApiManager mdsalApiManager;
    private final IdManagerService idManager;
    private final ItmTepUtils itmTepUtils;

    private static int DEFAULT_FLOW_PRIORITY = 5;

    @Inject
    public ItmFlowUtils(final DataBroker dataBroker, final IMdsalApiManager mdsalApiManager,
                        final IdManagerService idManager, final ItmTepUtils itmTepUtils) {
        this.dataBroker = dataBroker;
        this.mdsalApiManager = mdsalApiManager;
        this.idManager = idManager;
        this.itmTepUtils = itmTepUtils;
    }

    public void addTunnelIngressFlows(TepState tepState) {
        //addTunnelIngressFlow(tepState);
        SrcTep srcTep = getSrcTep(tepState.getTepNodeId());
        LOG.debug("srcTep: {} for tepState: {}", srcTep, tepState.getTepIfName());
        if (srcTep != null && srcTep.getDstTep() != null) {
            for (DstTep dstTep : srcTep.getDstTep()) {
                addTunnelIngressFlow(tepState, dstTep);
                TepState dstTepState = getTepState(dstTep);
                if (dstTepState != null) {
                    /* TODO:
                     * Remote was already created, need to add flows to it
                     * for this end
                     */
                    LOG.error("TODO: Add flows for {} to other teps", srcTep.getSrcTepNodeId());
                    //addTunnelIngressFlow(dstTepState, srcTep);
                }
            }
        }
    }

    public void addTunnelIngressFlow(TepState tepState) {
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new MatchInPort(tepState.getDpnId(), tepState.getTepOfPort()));
        /* TODO: For future use case
        if (BooleanUtils.isTrue(tunnel.isTunnelRemoteIpFlow())) {
            matches.add(new NxMatchTunnelSourceIp(tunnel.getTunnelDestination().getIpv4Address()));
        }
        if (BooleanUtils.isTrue(tunnel.isTunnelSourceIpFlow())) {
            matches.add(new NxMatchTunnelDestinationIp(tunnel.getTunnelSource().getIpv4Address()));
        }
        */
        List<InstructionInfo> mkInstructions = new ArrayList<>();
        short tableId = NwConstants.VXLAN_TRUNK_INTERFACE_TABLE;
        if (tepState.getTepType().isAssignableFrom(TunnelTypeMplsOverGre.class)) {
            tableId = NwConstants.GRE_TRUNK_INTERFACE_TABLE;
        }
        mkInstructions.add(new InstructionGotoTable(tableId));
        BigInteger dpnId = tepState.getDpnId();
        String flowRef = getTunnelInterfaceFlowRef(dpnId,
            NwConstants.VLAN_INTERFACE_INGRESS_TABLE, tepState.getTepIfName());
        String flowName = getTunnelInterfaceFlowName(dpnId,
            NwConstants.VLAN_INTERFACE_INGRESS_TABLE,tepState.getTepIfName());
        FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
            DEFAULT_FLOW_PRIORITY, flowName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE, matches,
            mkInstructions);
        mdsalApiManager.batchedAddFlow(dpnId, flowEntity);
    }

    public void addTunnelIngressFlow(TepState tepState, DstTep fromTep) {
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new MatchInPort(tepState.getDpnId(), tepState.getTepOfPort()));
        matches.add(new NxMatchTunnelSourceIp(String.valueOf(fromTep.getTunnelIp().getValue())));
        List<InstructionInfo> mkInstructions = new ArrayList<>();
        mkInstructions.add(new InstructionWriteMetadata(
            MetaDataUtil.getLportTagMetaData(fromTep.getTunnelIfIndex().intValue()).or(BigInteger.ONE),
                MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG));

        short tableId = itmTepUtils.isTepDcgw(fromTep)
            ? NwConstants.L3_LFIB_TABLE
                : itmTepUtils.isTepOvs(fromTep) ? NwConstants.INTERNAL_TUNNEL_TABLE
                    : NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;

        /*
         * TODO: Add else for Hwvtep for goto  NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;
         */
        mkInstructions.add(new InstructionGotoTable(tableId));
        BigInteger dpnId = tepState.getDpnId();
        String flowRef = getTunnelInterfaceFlowRef(dpnId, tableId, fromTep.getTunnelIfName());
        String flowName = getTunnelInterfaceFlowName(dpnId, tableId, fromTep.getTunnelIfName());
        FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
            DEFAULT_FLOW_PRIORITY, flowName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE, matches,
            mkInstructions);
        LOG.trace("Adding flowId {} from tepState:{} to dstTep:{}",flowRef, tepState, fromTep);
        mdsalApiManager.batchedAddFlow(dpnId, flowEntity);
    }

    private String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName) {
        return String.valueOf(dpnId) + NwConstants.FLOWID_SEPARATOR + tableId + NwConstants.FLOWID_SEPARATOR + ifName;
    }

    private String getTunnelInterfaceFlowName(BigInteger dpnId, short tableId, String ifName) {
        return getTunnelInterfaceFlowRef(dpnId, tableId, ifName);
    }

    private SrcTep getSrcTep(String tepNodeId) {
        InstanceIdentifier<SrcTep> srcTepIid = InstanceIdentifier.create(TunnelInfo.class)
            .child(SrcTep.class, new SrcTepKey(tepNodeId));
        Optional<SrcTep> optSrcTep = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, srcTepIid, dataBroker);
        return optSrcTep.isPresent() ? optSrcTep.get() : null;
    }


    private TepState getTepState(DstTep dstTep) {
        InstanceIdentifier<TepState> iid = ItmTepUtils.createTepStateIdentifier(dstTep.getDstTepNodeId());
        Optional<TepState> optTepState = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, iid, dataBroker);
        return optTepState.isPresent() ? optTepState.get() : null;
    }
}
