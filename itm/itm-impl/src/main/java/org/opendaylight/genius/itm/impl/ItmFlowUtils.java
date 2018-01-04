/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.globals.InterfaceServiceUtil;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchRegister;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelSourceIp;
import org.opendaylight.genius.utils.ServiceIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ItmFlowUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmFlowUtils.class);
    private final DataBroker dataBroker;
    private final IMdsalApiManager mdsalApiManager;
    private final IdManagerService idManager;
    private final ItmTepUtils itmTepUtils;
    private final IInterfaceManager ifManager;

    private static int DEFAULT_FLOW_PRIORITY = 5;

    @Inject
    public ItmFlowUtils(final DataBroker dataBroker, final IMdsalApiManager mdsalApiManager,
                        final IdManagerService idManager, final ItmTepUtils itmTepUtils,
                        final IInterfaceManager interfaceManager) {
        this.dataBroker = dataBroker;
        this.mdsalApiManager = mdsalApiManager;
        this.idManager = idManager;
        this.itmTepUtils = itmTepUtils;
        this.ifManager = interfaceManager;
    }

    public void addTunnelFlows(TepState tepState) {
        if (!itmTepUtils.TUNNEL_OPTIONS_VALUE_FLOW.equals(tepState.getTepOptionRemoteIp())) {
            DstTep dstTep = itmTepUtils.getDstTep(tepState.getTepNodeId(), tepState.getTepIfName());
            addTunnelIngressFlow(tepState, dstTep);
            addTunnelEgressFlow(tepState, dstTep);
        }
    }

    public void addTunnelIngressFlows(TepState tepState) {
        //TODO: Retained for OfTunnels
        SrcTep srcTep = itmTepUtils.getSrcTep(tepState.getTepNodeId());
        LOG.debug("srcTep: {} for tepState: {}", srcTep, tepState.getTepIfName());
        if (srcTep != null && srcTep.getDstTep() != null) {
            for (DstTep dstTep : srcTep.getDstTep()) {
                //addTunnelIngressFlow(tepState, dstTep);
                TepState dstTepState = itmTepUtils.getTepState(dstTep);
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
        if (fromTep == null) {
            LOG.warn("Unable to add Ingress flow. Remote tep information missing for tep ",
                tepState.getTepIfName());
        }
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new MatchInPort(tepState.getDpnId(), tepState.getTepOfPort()));
        if (fromTep != null && tepState.getTepOptionRemoteIp() != null
            && itmTepUtils.TUNNEL_OPTIONS_VALUE_FLOW.equals(tepState.getTepOptionRemoteIp())) {
            //Of tunnels use case
            matches.add(new NxMatchTunnelSourceIp(String.valueOf(fromTep.getTunnelIp().getValue())));
        }
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

    public void removeTunnelFlows(TepState tepState) {
        removeTunnelIngressFlows(tepState);
        removeTunnelEgressFlow(tepState);
    }

    public void removeTunnelIngressFlows(TepState tepState) {
        // Will be called for each individual tepState
        LOG.debug("Removing Ingress Flows for {}", tepState.getTepIfName());
        final BigInteger dpnId = tepState.getDpnId();
        final short tableId = NwConstants.VLAN_INTERFACE_INGRESS_TABLE;
        final String ifName = tepState.getTepIfName();
        String flowRef = getTunnelInterfaceFlowRef(dpnId, tableId, ifName);
        String flowName = getTunnelInterfaceFlowName(dpnId, tableId, ifName);
        mdsalApiManager.batchedRemoveFlow(dpnId,
            MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
                DEFAULT_FLOW_PRIORITY, flowName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE,
                Collections.emptyList(), Collections.emptyList()));
    }

    private String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName) {
        return String.valueOf(dpnId) + NwConstants.FLOWID_SEPARATOR + tableId + NwConstants.FLOWID_SEPARATOR + ifName;
    }

    private String getTunnelInterfaceFlowRef(BigInteger dpnId, short tableId, String ifName, short serviceIndex) {
        return getTunnelInterfaceFlowRef(dpnId, tableId, ifName) + NwConstants.FLOWID_SEPARATOR
            + serviceIndex;
    }

    private String getTunnelInterfaceFlowName(BigInteger dpnId, short tableId, String ifName) {
        return getTunnelInterfaceFlowRef(dpnId, tableId, ifName);
    }

    private String getTunnelInterfaceFlowName(BigInteger dpnId, short tableId, String ifName,
                                              short serviceIndex) {
        return getTunnelInterfaceFlowRef(dpnId, tableId, ifName, serviceIndex);
    }

    public void addTunnelEgressFlow(TepState tepState, DstTep dstTep) {
        List<Instruction> instructions = getDefaultEgressInstructionsForTunnel(dstTep, null, true);
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new NxMatchRegister(NxmNxReg6.class,
            MetaDataUtil.getReg6ValueForLPortDispatcher(dstTep.getTunnelIfIndex().intValue(),
                NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)));

        final BigInteger dpnId = tepState.getDpnId();
        final short tableId = NwConstants.EGRESS_LPORT_DISPATCHER_TABLE;
        final String ifName = dstTep.getTunnelIfName();
        String flowRef = getTunnelInterfaceFlowRef(dpnId, tableId, ifName,
            NwConstants.DEFAULT_EGRESS_SERVICE_INDEX);
        String flowName = getTunnelInterfaceFlowName(dpnId, tableId, ifName,
            NwConstants.DEFAULT_EGRESS_SERVICE_INDEX);
        Flow egressFlow = MDSALUtil.buildFlowNew(NwConstants.EGRESS_LPORT_DISPATCHER_TABLE, flowRef,
            NwConstants.DEFAULT_EGRESS_SERVICE_INDEX, flowName, 0, 0,
            NwConstants.EGRESS_DISPATCHER_TABLE_COOKIE, matches, instructions);
        //TODO: Handle this better. Batching?
        mdsalApiManager.installFlow(dpnId, egressFlow);
    }

    public void removeTunnelEgressFlow(TepState tepState) {
        final BigInteger dpnId = tepState.getDpnId();
        final short tableId = NwConstants.EGRESS_LPORT_DISPATCHER_TABLE;
        final String ifName = tepState.getTepIfName();
        String flowRef = getTunnelInterfaceFlowRef(dpnId, tableId, ifName,
            NwConstants.DEFAULT_EGRESS_SERVICE_INDEX);
        //TODO: Handle this better. Batching?
        mdsalApiManager.removeFlow(dpnId, tableId, new FlowId(flowRef));
    }

    public List<Instruction> getDefaultEgressInstructionsForTunnel(DstTep dstTep, Long tunnelKey,
                                                            boolean isDefaultEgress) {
        List<Instruction> instructions = new ArrayList<>();
        List<Action> actionList = MDSALUtil.buildActions(
            getEgressActionInfosForTunnel(dstTep, tunnelKey, 0, isDefaultEgress));
        instructions.add(MDSALUtil.buildApplyActionsInstruction(actionList));
        return instructions;
    }

    public List<ActionInfo> getEgressActionInfosForTunnel(DstTep dstTep, Long tunnelKey,
                                                          int actionKeyStart, boolean isDefaultEgress) {
        List<ActionInfo> result = new ArrayList<>();
        String tunnelType = ItmUtils.TUNNEL_TYPE_MAP.inverse().get(dstTep.getTepTunnelType());
        switch (tunnelType) {
            case ITMConstants.TUNNEL_TYPE_MPLSoGRE:
                // fall through
            case ITMConstants.TUNNEL_TYPE_GRE:
                if (!isDefaultEgress) {
                    // TODO tunnel_id to encode GRE key, once it is supported
                    // Until then, tunnel_id should be "cleaned", otherwise it
                    // stores the value coming from a VXLAN tunnel
                    if (tunnelKey == null) {
                        tunnelKey = 0L;
                    }
                }
                // fall through
            case ITMConstants.TUNNEL_TYPE_VXLAN:
                if (!isDefaultEgress) {
                    if (tunnelKey != null) {
                        result.add(new ActionSetFieldTunnelId(actionKeyStart++, BigInteger.valueOf(tunnelKey)));
                    }
                } else {
                    // TODO: For OF Tunnels default egress actions need to set tunnelIps
                    /*
                    if (BooleanUtils.isTrue(ifTunnel.isTunnelRemoteIpFlow()
                        && ifTunnel.getTunnelDestination() != null)) {
                        result.add(new ActionSetTunnelDestinationIp(actionKeyStart++, ifTunnel.getTunnelDestination()));
                    }
                    if (BooleanUtils.isTrue(ifTunnel.isTunnelSourceIpFlow()
                        && ifTunnel.getTunnelSource() != null)) {
                        result.add(new ActionSetTunnelSourceIp(actionKeyStart++, ifTunnel.getTunnelSource()));
                    }
                    */
                    result.add(new ActionGroup(dstTep.getTunnelOutGroupId()));
                }
                // fall through
                /*
            case LOGICAL_GROUP_INTERFACE:
                if (isDefaultEgress) {
                    result.add(new ActionGroup(groupId));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                break;
                */
            default:
                LOG.warn("Interface Type {} not supported yet", tunnelType);
                break;
        }
        return result;
    }
}
