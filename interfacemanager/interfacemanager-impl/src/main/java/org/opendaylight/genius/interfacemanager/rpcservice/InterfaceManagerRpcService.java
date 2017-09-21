/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.rpcservice;

import static org.opendaylight.genius.infra.FutureRpcResults.LogLevel.DEBUG;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.FutureRpcResults;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceManagerRpcService implements OdlInterfaceRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerRpcService.class);

    private final DataBroker dataBroker;
    private final IMdsalApiManager mdsalMgr;

    @Inject
    public InterfaceManagerRpcService(final DataBroker dataBroker, final IMdsalApiManager mdsalApiManager) {
        this.dataBroker = dataBroker;
        this.mdsalMgr = mdsalApiManager;
    }

    @Override
    public Future<RpcResult<GetDpidFromInterfaceOutput>> getDpidFromInterface(GetDpidFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        return FutureRpcResults.fromListenableFuture(LOG, "getDpidFromInterface", input, () -> {
            BigInteger dpId;
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                throw new IllegalArgumentException(
                        getDpidFromInterfaceErrorMessage(interfaceName, "missing Interface in Config DataStore"));
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                ParentRefs parentRefs = interfaceInfo.getAugmentation(ParentRefs.class);
                dpId = parentRefs.getDatapathNodeIdentifier();
            } else {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                        .getInterfaceState(interfaceName, dataBroker);
                if (ifState != null) {
                    String lowerLayerIf = ifState.getLowerLayerIf().get(0);
                    NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
                    dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                } else {
                    throw new IllegalArgumentException(
                            getDpidFromInterfaceErrorMessage(interfaceName, "missing Interface-state"));
                }
            }
            return Futures.immediateFuture(new GetDpidFromInterfaceOutputBuilder().setDpid(dpId).build());
        }).withRpcErrorMessage(e -> getDpidFromInterfaceErrorMessage(interfaceName, e.getMessage()))
          .onFailure(e -> { /* do not LOG error here */ }).build();
    }

    private String getDpidFromInterfaceErrorMessage(final String interfaceName, final String dueTo) {
        return String.format("Retrieval of datapath id for the key {%s} failed due to %s",
                interfaceName, dueTo);
    }

    @Override
    public Future<RpcResult<GetEndpointIpForDpnOutput>> getEndpointIpForDpn(GetEndpointIpForDpnInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getEndpointIpForDpn", input, () -> {
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
            return Futures.immediateFuture(new GetEndpointIpForDpnOutputBuilder()
                    .setLocalIps(Collections.singletonList(tunnel.getTunnelSource())).build());
        }).build();
    }

    @Override
    public Future<RpcResult<GetEgressInstructionsForInterfaceOutput>> getEgressInstructionsForInterface(
            GetEgressInstructionsForInterfaceInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getEgressInstructionsForInterface", input, () -> {
            List<Instruction> instructions = IfmUtil.getEgressInstructionsForInterface(input.getIntfName(),
                    input.getTunnelKey(), dataBroker, false);
            return Futures.immediateFuture(
                    new GetEgressInstructionsForInterfaceOutputBuilder().setInstruction(instructions).build());
        }).onFailureLog(DEBUG).build();
    }

    @Override
    public Future<RpcResult<GetInterfaceTypeOutput>> getInterfaceType(GetInterfaceTypeInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getInterfaceType", input, () -> {
            String interfaceName = input.getIntfName();
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                throw new IllegalStateException(String.format("getInterfaceType() Retrieval of Interface Type for "
                        + "the key {%s} failed due to missing Interface in Config DataStore", interfaceName));
            }
            return Futures.immediateFuture(
                    new GetInterfaceTypeOutputBuilder().setInterfaceType(interfaceInfo.getType()).build());
        }).build();
    }

    @Override
    public Future<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getTunnelType", input, () -> {
            String interfaceName = input.getIntfName();
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                throw new IllegalArgumentException(String.format(
                        "Retrieval of Tunnel Type for the key {%s} failed due to missing Interface in Config DataStore",
                        interfaceName));
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                IfTunnel tnl = interfaceInfo.getAugmentation(IfTunnel.class);
                Class<? extends TunnelTypeBase> tunType = tnl.getTunnelInterfaceType();
                return Futures.immediateFuture(new GetTunnelTypeOutputBuilder().setTunnelType(tunType).build());
            } else {
                throw new IllegalArgumentException("Retrieval of interface type failed for key: " + interfaceName);
            }
        }).build();
    }

    @Override
    public Future<RpcResult<GetEgressActionsForInterfaceOutput>> getEgressActionsForInterface(
            GetEgressActionsForInterfaceInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getEgressActionsForInterface", input, () -> {
            List<Action> actionsList = IfmUtil.getEgressActionsForInterface(input.getIntfName(), input.getTunnelKey(),
                    input.getActionKey(), dataBroker, false);
            // TODO as above, simplify the success case later, as we have the failure case below
            return Futures
                    .immediateFuture(new GetEgressActionsForInterfaceOutputBuilder().setAction(actionsList).build());
        }).onFailureLog(DEBUG).build();
    }

    @Override
    public Future<RpcResult<GetPortFromInterfaceOutput>> getPortFromInterface(GetPortFromInterfaceInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getPortFromInterface", input, () -> {
            String interfaceName = input.getIntfName();
            BigInteger dpId = null;
            long portNo = 0;
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                    .getInterfaceState(interfaceName, dataBroker);
            if (ifState != null) {
                String lowerLayerIf = ifState.getLowerLayerIf().get(0);
                NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
                dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
                String phyAddress = ifState.getPhysAddress().getValue();
                // FIXME Assuming portName and interfaceName are same
                // TODO as above, simplify the success case later, as we have the failure case below
                return Futures.immediateFuture(new GetPortFromInterfaceOutputBuilder().setDpid(dpId)
                        .setPortname(interfaceName).setPortno(portNo).setPhyAddress(phyAddress).build());
            } else {
                throw new IllegalArgumentException(
                        "Retrieval of Port for the key " + interfaceName + " failed due to missing Interface state");
            }
        }).build();
    }

    @Override
    public Future<RpcResult<GetNodeconnectorIdFromInterfaceOutput>> getNodeconnectorIdFromInterface(
            GetNodeconnectorIdFromInterfaceInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getNodeconnectorIdFromInterface", input, () -> {
            String interfaceName = input.getIntfName();
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                    .getInterfaceState(interfaceName, dataBroker);
            String lowerLayerIf = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
            // TODO as above, simplify the success case later, as we have the failure case below
            return Futures.immediateFuture(
                    new GetNodeconnectorIdFromInterfaceOutputBuilder().setNodeconnectorId(nodeConnectorId).build());
        }).build();
    }

    @Override
    public Future<RpcResult<GetInterfaceFromIfIndexOutput>> getInterfaceFromIfIndex(
            GetInterfaceFromIfIndexInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getInterfaceFromIfIndex", input, () -> {
            Integer ifIndex = input.getIfIndex();
            InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class)
                    .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
            Optional<IfIndexInterface> ifIndexesInterface = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id,
                    dataBroker);

            if (!ifIndexesInterface.isPresent()) {
                throw new IllegalArgumentException(
                        "Could not find " + id.toString() + " in OperationalDS for idIndex=" + ifIndex);
            }
            String interfaceName = ifIndexesInterface.get().getInterfaceName();
            // TODO as above, simplify the success case later, as we have the failure case below
            return Futures.immediateFuture(
                    new GetInterfaceFromIfIndexOutputBuilder().setInterfaceName(interfaceName).build());
        }).build();
    }

    @Override
    public Future<RpcResult<GetDpnInterfaceListOutput>> getDpnInterfaceList(GetDpnInterfaceListInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getInterfaceFromIfIndex", input, () -> {
            BigInteger dpnid = input.getDpid();
            InstanceIdentifier<DpnToInterface> id = InstanceIdentifier.builder(DpnToInterfaceList.class)
                    .child(DpnToInterface.class, new DpnToInterfaceKey(dpnid)).build();
            Optional<DpnToInterface> entry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker);
            if (!entry.isPresent()) {
                LOG.warn("Could not find Operational DpnToInterface info for DPN {}. Returning empty list", dpnid);
                return buildEmptyInterfaceListResult();
            }

            List<InterfaceNameEntry> interfaceNameEntries = entry.get().getInterfaceNameEntry();
            if (interfaceNameEntries == null || interfaceNameEntries.isEmpty()) {
                LOG.debug("No Interface list found in Operational for DPN {}", dpnid);
                return buildEmptyInterfaceListResult();
            }
            List<String> interfaceList = interfaceNameEntries.stream().map(InterfaceNameEntry::getInterfaceName)
                    .collect(Collectors.toList());
            // TODO as above, simplify the success case later, as we have the failure case below
            return Futures
                    .immediateFuture(new GetDpnInterfaceListOutputBuilder().setInterfacesList(interfaceList).build());
        }).build();
    }

    private ListenableFuture<GetDpnInterfaceListOutput> buildEmptyInterfaceListResult() {
        GetDpnInterfaceListOutput emptyListResult =
            new GetDpnInterfaceListOutputBuilder().setInterfacesList(Collections.emptyList()).build();
        return Futures.immediateFuture(emptyListResult);
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
