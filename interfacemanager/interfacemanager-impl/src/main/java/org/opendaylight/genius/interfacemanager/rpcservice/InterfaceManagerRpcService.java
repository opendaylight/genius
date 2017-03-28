/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.rpcservice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetType;
import org.opendaylight.genius.mdsalutil.matches.MatchMplsLabel;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.DpnToInterfaceList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.IfIndexesInterfaceMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Singleton
public class InterfaceManagerRpcService implements OdlInterfaceRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerRpcService.class);

    private final DataBroker dataBroker;
    private final IMdsalApiManager mdsalMgr;

    @Inject
    public InterfaceManagerRpcService(final DataBroker dataBroker, final IMdsalApiManager iMdsalApiManager) {
        this.dataBroker = dataBroker;
        this.mdsalMgr = iMdsalApiManager;
    }

    @Override
    public Future<RpcResult<GetDpidFromInterfaceOutput>> getDpidFromInterface(GetDpidFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetDpidFromInterfaceOutput> rpcResultBuilder;
        try {
            BigInteger dpId = null;
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                rpcResultBuilder = newRpcErrorResult(
                        getDpidFromInterfaceErrorMessage(interfaceName, "missing Interface in Config DataStore"));
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                ParentRefs parentRefs = interfaceInfo.getAugmentation(ParentRefs.class);
                dpId = parentRefs.getDatapathNodeIdentifier();
            } else {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
                    .Interface ifState
                        = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
                if (ifState != null) {
                    String lowerLayerIf = ifState.getLowerLayerIf().get(0);
                    NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
                    dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                } else {
                    rpcResultBuilder = newRpcErrorResult(
                            getDpidFromInterfaceErrorMessage(interfaceName, "missing Interface-state"));
                    return Futures.immediateFuture(rpcResultBuilder.build());
                }
            }
            GetDpidFromInterfaceOutputBuilder output = new GetDpidFromInterfaceOutputBuilder().setDpid(dpId);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(getDpidFromInterfaceErrorMessage(interfaceName, e.getMessage()), e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private String getDpidFromInterfaceErrorMessage(final String interfaceName, final String dueTo) {
        return String.format("Retrieval of datapath id for the key {%s} failed due to %s",
                interfaceName, dueTo);
    }

    // TODO move these two helper methods to somewhere else, to be shared with other projects
    private <T extends DataObject> RpcResultBuilder<T> newRpcErrorResult(String message) {
        LOG.error(message);
        RpcResultBuilder<T> rpcResultBuilder = RpcResultBuilder
                .<T>failed().withError(RpcError.ErrorType.APPLICATION, message);
        return rpcResultBuilder;
    }

    private <T extends DataObject> RpcResultBuilder<T> newRpcErrorResult(String message, Throwable cause) {
        LOG.error(message, cause);
        RpcResultBuilder<T> rpcResultBuilder = RpcResultBuilder
                .<T>failed().withError(RpcError.ErrorType.APPLICATION, message, cause);
        return rpcResultBuilder;
    }

    @Override
    public Future<RpcResult<GetEndpointIpForDpnOutput>> getEndpointIpForDpn(GetEndpointIpForDpnInput input) {
        RpcResultBuilder<GetEndpointIpForDpnOutput> rpcResultBuilder;
        try {
            BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(input.getDpid());
            InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier = InterfaceMetaUtils
                    .getBridgeEntryIdentifier(bridgeEntryKey);
            BridgeEntry bridgeEntry = InterfaceMetaUtils.getBridgeEntryFromConfigDS(bridgeEntryInstanceIdentifier,
                    dataBroker);
            // local ip of any of the bridge interface entry will be the dpn end
            // point ip
            BridgeInterfaceEntry bridgeInterfaceEntry = bridgeEntry.getBridgeInterfaceEntry().get(0);
            InterfaceKey interfaceKey = new InterfaceKey(bridgeInterfaceEntry.getInterfaceName());
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            IfTunnel tunnel = interfaceInfo.getAugmentation(IfTunnel.class);
            GetEndpointIpForDpnOutputBuilder endpointIpForDpnOutput = new GetEndpointIpForDpnOutputBuilder()
                    .setLocalIps(Collections.singletonList(tunnel.getTunnelSource()));
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(endpointIpForDpnOutput.build());
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "Retrieval of endpoint of for dpn " + input.getDpid() + " failed", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetEgressInstructionsForInterfaceOutput>> getEgressInstructionsForInterface(
            GetEgressInstructionsForInterfaceInput input) {
        RpcResultBuilder<GetEgressInstructionsForInterfaceOutput> rpcResultBuilder;
        try {
            List<Instruction> instructions = IfmUtil.getEgressInstructionsForInterface(input.getIntfName(),
                    input.getTunnelKey(), dataBroker, false);
            GetEgressInstructionsForInterfaceOutputBuilder output = new GetEgressInstructionsForInterfaceOutputBuilder()
                    .setInstruction(instructions);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "Retrieval of egress instructions for the key " + input.getIntfName() + " failed", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetInterfaceTypeOutput>> getInterfaceType(GetInterfaceTypeInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetInterfaceTypeOutput> rpcResultBuilder;
        try {
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                String errMsg = String.format("getInterfaceType() Retrieval of Interface Type for the key {%s} failed "
                        + "due to missing Interface in Config DataStore", interfaceName);
                rpcResultBuilder = newRpcErrorResult(errMsg);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            GetInterfaceTypeOutputBuilder output = new GetInterfaceTypeOutputBuilder()
                    .setInterfaceType(interfaceInfo.getType());
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "getInterfaceType() Retrieval of interface type for the key " + interfaceName + " failed", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetTunnelTypeOutput> rpcResultBuilder;
        try {
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                String errMsg = String.format(
                        "Retrieval of Tunnel Type for the key {%s} failed due to missing Interface in Config DataStore",
                        interfaceName);
                rpcResultBuilder = newRpcErrorResult(errMsg);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                IfTunnel tnl = interfaceInfo.getAugmentation(IfTunnel.class);
                Class<? extends TunnelTypeBase> tunType = tnl.getTunnelInterfaceType();
                GetTunnelTypeOutputBuilder output = new GetTunnelTypeOutputBuilder().setTunnelType(tunType);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
            } else {
                rpcResultBuilder = newRpcErrorResult(
                        "Retrieval of interface type for the key {} failed: " + interfaceName);
            }
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "Retrieval of interface type for the key " + interfaceName + " failed", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetEgressActionsForInterfaceOutput>> getEgressActionsForInterface(
            GetEgressActionsForInterfaceInput input) {
        RpcResultBuilder<GetEgressActionsForInterfaceOutput> rpcResultBuilder;
        try {
            LOG.debug("Get Egress Action for interface {} with key {}", input.getIntfName(), input.getTunnelKey());
            List<Action> actionsList = IfmUtil.getEgressActionsForInterface(input.getIntfName(), input.getTunnelKey(),
                    input.getActionKey(), dataBroker, false);
            GetEgressActionsForInterfaceOutputBuilder output = new GetEgressActionsForInterfaceOutputBuilder()
                    .setAction(actionsList);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "Retrieval of egress actions " + input.getIntfName() + " failed", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetPortFromInterfaceOutput>> getPortFromInterface(GetPortFromInterfaceInput input) {
        RpcResultBuilder<GetPortFromInterfaceOutput> rpcResultBuilder;
        String interfaceName = input.getIntfName();
        try {
            BigInteger dpId = null;
            long portNo = 0;
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
                    .Interface ifState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName,
                            dataBroker);
            if (ifState != null) {
                String lowerLayerIf = ifState.getLowerLayerIf().get(0);
                NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
                dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
                String phyAddress = ifState.getPhysAddress().getValue();
                // FIXME Assuming portName and interfaceName are same
                GetPortFromInterfaceOutputBuilder output = new GetPortFromInterfaceOutputBuilder().setDpid(dpId)
                        .setPortname(interfaceName).setPortno(portNo).setPhyAddress(phyAddress);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
            } else {
                rpcResultBuilder = newRpcErrorResult(
                        getPortFromInterfaceErrorMessage(interfaceName, "missing Interface state"));
            }
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    getPortFromInterfaceErrorMessage(interfaceName, e.getMessage()), e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private String getPortFromInterfaceErrorMessage(final String interfaceName, final String errMsg) {
        return String.format("Retrieval of Port for the key {%s} failed due to %s", interfaceName, errMsg);
    }

    @Override
    public Future<RpcResult<GetNodeconnectorIdFromInterfaceOutput>> getNodeconnectorIdFromInterface(
            GetNodeconnectorIdFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetNodeconnectorIdFromInterfaceOutput> rpcResultBuilder;
        try {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
                .Interface ifState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
            String lowerLayerIf = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);

            GetNodeconnectorIdFromInterfaceOutputBuilder output = new GetNodeconnectorIdFromInterfaceOutputBuilder()
                    .setNodeconnectorId(nodeConnectorId);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "Retrieval of nodeconnector id for the key " + interfaceName + "failed", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetInterfaceFromIfIndexOutput>> getInterfaceFromIfIndex(
            GetInterfaceFromIfIndexInput input) {
        Integer ifIndex = input.getIfIndex();
        RpcResultBuilder<GetInterfaceFromIfIndexOutput> rpcResultBuilder = null;
        try {
            InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class)
                    .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
            Optional<IfIndexInterface> ifIndexesInterface = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id,
                    dataBroker);
            if (ifIndexesInterface.isPresent()) {
                String interfaceName = ifIndexesInterface.get().getInterfaceName();
                GetInterfaceFromIfIndexOutputBuilder output = new GetInterfaceFromIfIndexOutputBuilder()
                        .setInterfaceName(interfaceName);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
            }
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "Retrieval of interfaceName for the key " + ifIndex + "failed", e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetDpnInterfaceListOutput>> getDpnInterfaceList(GetDpnInterfaceListInput input) {
        BigInteger dpnid = input.getDpid();
        RpcResultBuilder<GetDpnInterfaceListOutput> rpcResultBuilder = null;
        try {
            InstanceIdentifier<DpnToInterface> id = InstanceIdentifier.builder(DpnToInterfaceList.class)
                    .child(DpnToInterface.class, new DpnToInterfaceKey(dpnid)).build();
            Optional<DpnToInterface> entry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker);
            if (entry.isPresent()) {
                List<InterfaceNameEntry> interfaceNameEntries = entry.get().getInterfaceNameEntry();
                if (interfaceNameEntries != null && !interfaceNameEntries.isEmpty()) {
                    List<String> interfaceList = interfaceNameEntries.stream().map(InterfaceNameEntry::getInterfaceName)
                            .collect(Collectors.toList());
                    GetDpnInterfaceListOutputBuilder output = new GetDpnInterfaceListOutputBuilder()
                            .setInterfacesList(interfaceList);
                    rpcResultBuilder = RpcResultBuilder.success();
                    rpcResultBuilder.withResult(output.build());
                }
            }
        } catch (Exception e) {
            rpcResultBuilder = newRpcErrorResult(
                    "Retrieval of interfaceNameList for the dpnId " + dpnid + "failed", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    protected static List<Instruction> buildInstructions(List<InstructionInfo> listInstructionInfo) {
        if (listInstructionInfo != null) {
            List<Instruction> instructions = new ArrayList<>();
            int instructionKey = 0;

            for (InstructionInfo instructionInfo : listInstructionInfo) {
                instructions.add(instructionInfo.buildInstruction(instructionKey));
                instructionKey++;
            }
            return instructions;
        }

        return null;
    }

    private ListenableFuture<Void> makeTerminatingServiceFlow(IfTunnel tunnelInfo, BigInteger dpnId,
            BigInteger tunnelKey, List<Instruction> instruction, int addOrRemove) {
        List<MatchInfo> mkMatches = new ArrayList<>();
        mkMatches.add(new MatchTunnelId(tunnelKey));
        short tableId = tunnelInfo.isInternal() ? NwConstants.INTERNAL_TUNNEL_TABLE : NwConstants.EXTERNAL_TUNNEL_TABLE;
        final String flowRef = getFlowRef(dpnId, tableId, tunnelKey);
        Flow terminatingSerFlow = MDSALUtil.buildFlowNew(tableId, flowRef, 5, "TST Flow Entry", 0, 0,
                IfmConstants.TUNNEL_TABLE_COOKIE.add(tunnelKey), mkMatches, instruction);
        if (addOrRemove == NwConstants.ADD_FLOW) {
            return mdsalMgr.installFlow(dpnId, terminatingSerFlow);
        }

        return mdsalMgr.removeFlow(dpnId, terminatingSerFlow);
    }

    private ListenableFuture<Void> makeLFIBFlow(BigInteger dpnId, BigInteger tunnelKey, List<Instruction> instruction,
            int addOrRemove) {
        List<MatchInfo> mkMatches = new ArrayList<>();
        mkMatches.add(MatchEthernetType.MPLS_UNICAST);
        mkMatches.add(new MatchMplsLabel(tunnelKey.longValue()));
        // Install the flow entry in L3_LFIB_TABLE
        String flowRef = getFlowRef(dpnId, NwConstants.L3_LFIB_TABLE, tunnelKey);

        Flow lfibFlow = MDSALUtil.buildFlowNew(NwConstants.L3_LFIB_TABLE, flowRef, IfmConstants.DEFAULT_FLOW_PRIORITY,
                "LFIB Entry", 0, 0, IfmConstants.COOKIE_VM_LFIB_TABLE, mkMatches, instruction);
        if (addOrRemove == NwConstants.ADD_FLOW) {
            return mdsalMgr.installFlow(dpnId, lfibFlow);
        }
        return mdsalMgr.removeFlow(dpnId, lfibFlow);
    }

    private String getFlowRef(BigInteger dpnId, short tableId, BigInteger tunnelKey) {
        return IfmConstants.TUNNEL_TABLE_FLOWID_PREFIX + dpnId + NwConstants.FLOWID_SEPARATOR + tableId
                + NwConstants.FLOWID_SEPARATOR + tunnelKey;
    }
}
