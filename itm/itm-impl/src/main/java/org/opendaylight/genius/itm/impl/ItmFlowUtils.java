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
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchRegister;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelSourceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tep.states.TepState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.SrcTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.tep.rev180106.tunnel.info.src.tep.DstTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
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

    public void addTunnelFlows(TepState tepState) {
        if (itmTepUtils.TUNNEL_OPTIONS_VALUE_FLOW.equals(tepState.getTepOptionRemoteIp())) {
            DstTep dstTep = itmTepUtils.getDstTep(tepState.getTepNodeId(), tepState.getTepIfName());
            //TODO: addTunnelEgressGroup();
            addTunnelIngressFlow(tepState, dstTep);
            //TODO: Not needed unless support service binding on tunnels
            //addTunnelEgressFlow(tepState, dstTep);
        } else {
            LOG.error("FIXME: Calling wrong method to add tunnelFlows");
        }
    }

    public void addTunnelFlows(InstanceIdentifier<OvsdbTerminationPointAugmentation> tpIid,
                               OvsdbTerminationPointAugmentation tp) {
        if (itmTepUtils.isOfTunnel(tp)) {
            LOG.error("FIXME: Wrong addTunnelFlows method called for OFtunnels");
        }
        Map<String,String> externalIds = itmTepUtils.getIfaceExternalIds(tp);
        BigInteger dpnId = itmTepUtils.getDpnId(tpIid);
        long ofPort = tp.getOfport().longValue();
        Long ifIndex = Long.parseLong(externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_IFINDEX));
        long groupId = Long.parseLong(externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_GROUPID));
        String localIp = itmTepUtils.getOption(itmTepUtils.TUNNEL_OPTIONS_LOCAL_IP, tp.getOptions());
        String remoteIp = itmTepUtils.getOption(itmTepUtils.TUNNEL_OPTIONS_REMOTE_IP, tp.getOptions());
        String peerNodeType = externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_PEER_TYPE);
        String tunnelType = externalIds.get(itmTepUtils.IFACE_EXTERNAL_ID_TUNNEL_TYPE);
        addTunnelEgressGroup(dpnId, remoteIp, groupId, ofPort);
        addTunnelIngressFlow(tp.getName(), dpnId, ofPort, ifIndex, localIp, remoteIp, false, peerNodeType);
        if (ifIndex != null && ifIndex != 0) {
            addDefaultTunnelEgressFlow(dpnId, tp.getName(), tunnelType, ifIndex.intValue(), groupId);
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
            LOG.warn("Unable to add Ingress flow. Remote tep information missing for tep",
                tepState.getTepIfName());
            return;
        }
        addTunnelIngressFlow(tepState.getTepIfName(), tepState.getDpnId(), tepState.getTepOfPort(),
            fromTep.getTunnelIfIndex(), String.valueOf(tepState.getTepIp().getValue()),
            String.valueOf(fromTep.getTunnelIp().getValue()), true,
            itmTepUtils.TEP_NODE_TYPE_TO_STR_MAP.get(fromTep.getTepNodeType()));
    }

    private void addTunnelIngressFlow(String ifName, BigInteger dpnId, long ofPort, Long ifIndex,
                                     String localIp, String remoteIp, boolean isOfTunnel, String dstNodeType) {
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new MatchInPort(dpnId, ofPort));
        if (isOfTunnel) {
            //Of tunnels use case
            matches.add(new NxMatchTunnelSourceIp(remoteIp));
        }
        List<InstructionInfo> mkInstructions = new ArrayList<>();
        if (ifIndex != null  && ifIndex != 0) {
            mkInstructions.add(new InstructionWriteMetadata(
                MetaDataUtil.getLportTagMetaData(ifIndex.intValue()).or(BigInteger.ONE),
                MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG));
        }

        short tableId = itmTepUtils.TEP_PEER_TYPE_DCGW.equals(dstNodeType)
            ? NwConstants.L3_LFIB_TABLE
            : itmTepUtils.TEP_PEER_TYPE_OVSDB.equals(dstNodeType) ? NwConstants.INTERNAL_TUNNEL_TABLE
            : NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;

        mkInstructions.add(new InstructionGotoTable(tableId));
        String flowRef = getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, ifName);
        String flowName = getTunnelInterfaceFlowName(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, ifName);
        FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
            DEFAULT_FLOW_PRIORITY, flowName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE, matches,
            mkInstructions);
        LOG.trace("Adding flowId {} from {} to {}",flowRef, localIp, remoteIp);
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
        final String ifName = tepState.getTepIfName();
        removeTunnelIngressFlow(dpnId, ifName);
    }

    private void removeTunnelIngressFlow(BigInteger dpnId, String ifName) {
        String flowRef = getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, ifName);
        String flowName = getTunnelInterfaceFlowName(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, ifName);
        LOG.debug("Deleting flow {}", flowRef);
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

    public void addTunnelDefaultEgressFlow(TepState tepState, DstTep dstTep) {
        String tunnelType = ItmUtils.TUNNEL_TYPE_MAP.inverse().get(dstTep.getTepTunnelType());
        addDefaultTunnelEgressFlow(tepState.getDpnId(), dstTep.getTunnelIfName(), tunnelType,
            dstTep.getTunnelIfIndex().intValue(), dstTep.getTunnelOutGroupId());
    }

    public void addDefaultTunnelEgressFlow(BigInteger dpnId, String ifName, String tunnelType, int ifIndex,
                                           long groupId) {
        if (ifIndex == 0) {
            LOG.error("Add TunnelEgressFlows called for {} without IfIndex", ifName);
        }
        List<Instruction> instructions = getDefaultEgressInstructionsForTunnel(tunnelType, groupId, null, true);
        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new NxMatchRegister(NxmNxReg6.class,
            MetaDataUtil.getReg6ValueForLPortDispatcher(ifIndex,
                NwConstants.DEFAULT_EGRESS_SERVICE_INDEX)));

        final short tableId = NwConstants.EGRESS_LPORT_DISPATCHER_TABLE;
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
        /*
         * TODO: Currently we don't add flows for internal tunnels,
         * and other tunnels go through IFM. So to avoid deleting
         * a flow we know doesn't exist, we will not delete any flows.
         */
        final BigInteger dpnId = tepState.getDpnId();
        final short tableId = NwConstants.EGRESS_LPORT_DISPATCHER_TABLE;
        final String ifName = tepState.getTepIfName();
        String flowRef = getTunnelInterfaceFlowRef(dpnId, tableId, ifName,
            NwConstants.DEFAULT_EGRESS_SERVICE_INDEX);
        //TODO: Handle this better. Batching?
        mdsalApiManager.removeFlow(dpnId, tableId, new FlowId(flowRef));
    }

    public List<Instruction> getDefaultEgressInstructionsForTunnel(String tunnelType, long groupId, Long tunnelKey,
                                                            boolean isDefaultEgress) {
        List<Instruction> instructions = new ArrayList<>();
        List<Action> actionList = MDSALUtil.buildActions(
            getEgressActionInfosForTunnel(tunnelType, groupId, tunnelKey, 0, isDefaultEgress));
        instructions.add(MDSALUtil.buildApplyActionsInstruction(actionList));
        return instructions;
    }

    public List<ActionInfo> getEgressActionInfosForTunnel(String tunnelType, long groupId, Long tunnelKey,
                                                          int actionKeyStart, boolean isDefaultEgress) {
        List<ActionInfo> result = new ArrayList<>();
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
                LOG.error("TunnelType {} not supported by tunnelZones yet", tunnelType);
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
                    result.add(new ActionGroup(groupId));
                }
                // fall through
                /*
            case LOGICAL_GROUP_INTERFACE:
                if (isDefaultEgress) {
                    result.add(new ActionGroup(groupId));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                */
                break;
            default:
                LOG.warn("Interface Type {} not supported yet", tunnelType);
                break;
        }
        return result;
    }

    // Group stuff
    public void addTunnelEgressGroup(BigInteger dpnId, String groupName, long groupId, long ofPort) {
        mdsalApiManager.syncInstallGroup(dpnId, makeEgressGroup(groupName, groupId, ofPort));
    }

    public void removeTunnelEgressGroup(BigInteger dpnId, String groupName, long groupId, long ofPort) {
        mdsalApiManager.syncRemoveGroup(dpnId, makeEgressGroup(groupName, groupId, ofPort));
    }

    private Group makeEgressGroup(String groupName, long groupId, long ofPort) {
        List<Bucket> listBuckets = new ArrayList<>();
        if (ofPort != 0) {
            listBuckets.add(createBucket(groupName, 0, ofPort));
        }
        groupName = ITMConstants.ITM_SERVICE_NAME + NwConstants.FLOWID_SEPARATOR + groupName;
        return MDSALUtil.buildGroup(groupId, groupName, GroupTypes.GroupAll,
            MDSALUtil.buildBucketLists(listBuckets));
    }

    private Bucket createBucket(String groupName, int bucketId, long portNumber) {
        //TODO: Handle for OfTunnels
        List<ActionInfo> listActionInfo = new ArrayList<>();
        listActionInfo.add(new ActionOutput(0, new Uri(String.valueOf(portNumber))));
        return MDSALUtil.buildBucket(MDSALUtil.buildActions(listActionInfo), bucketId);
    }
}
