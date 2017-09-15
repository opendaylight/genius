/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.rpcservice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.get.dpn._interface.list.output.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.get.dpn._interface.list.output.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
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
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetDpidFromInterfaceOutput>> getDpidFromInterface(GetDpidFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetDpidFromInterfaceOutput> rpcResultBuilder;
        LOG.debug("Get dpid for interface {}", input.getIntfName());
        try {
            BigInteger dpId;
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                rpcResultBuilder = getRpcErrorResultForGetDpnIdRpc(interfaceName,
                        "missing Interface in Config DataStore");
                return Futures.immediateFuture(rpcResultBuilder.build());
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
                    rpcResultBuilder = getRpcErrorResultForGetDpnIdRpc(interfaceName, "missing Interface-state");
                    return Futures.immediateFuture(rpcResultBuilder.build());
                }
            }
            GetDpidFromInterfaceOutputBuilder output = new GetDpidFromInterfaceOutputBuilder().setDpid(dpId);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("Dpid for interface {} is {}", input.getIntfName(), dpId);
        } catch (Exception e) {
            rpcResultBuilder = getRpcErrorResultForGetDpnIdRpc(interfaceName, e.getMessage());
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private RpcResultBuilder<GetDpidFromInterfaceOutput> getRpcErrorResultForGetDpnIdRpc(String interfaceName,
            String errMsg) {
        errMsg = String.format("Retrieval of datapath id for the key {%s} failed due to %s", interfaceName, errMsg);
        LOG.debug(errMsg);
        RpcResultBuilder<GetDpidFromInterfaceOutput> rpcResultBuilder = RpcResultBuilder
                .<GetDpidFromInterfaceOutput>failed().withError(RpcError.ErrorType.APPLICATION, errMsg);
        return rpcResultBuilder;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetEndpointIpForDpnOutput>> getEndpointIpForDpn(GetEndpointIpForDpnInput input) {
        RpcResultBuilder<GetEndpointIpForDpnOutput> rpcResultBuilder;
        LOG.debug("Get endpoint ip for dpn {}", input.getDpid());
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
            LOG.debug("Endpoint ip for dpn {} is {}", input.getDpid(), tunnel.getTunnelSource());
        } catch (Exception e) {
            LOG.error("Retrieval of endpoint of for dpn {} failed due to", input.getDpid(), e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetEgressInstructionsForInterfaceOutput>> getEgressInstructionsForInterface(
            GetEgressInstructionsForInterfaceInput input) {
        RpcResultBuilder<GetEgressInstructionsForInterfaceOutput> rpcResultBuilder;
        LOG.debug("Get Egress Instructions for interface {} with key {}", input.getIntfName(), input.getTunnelKey());
        try {
            List<Instruction> instructions = IfmUtil.getEgressInstructionsForInterface(input.getIntfName(),
                    input.getTunnelKey(), dataBroker, false);
            GetEgressInstructionsForInterfaceOutputBuilder output = new GetEgressInstructionsForInterfaceOutputBuilder()
                    .setInstruction(instructions);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("Egress Instructions for interface {} is {}", input.getIntfName(), instructions);
        } catch (Exception e) {
            String errMsg = String.format("Retrieval of egress instructions for the key {%s} failed due to %s",
                    input.getIntfName(), e.getMessage());
            LOG.debug(errMsg);
            rpcResultBuilder = RpcResultBuilder.<GetEgressInstructionsForInterfaceOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, errMsg);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetInterfaceTypeOutput>> getInterfaceType(GetInterfaceTypeInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetInterfaceTypeOutput> rpcResultBuilder;
        LOG.debug("Get interface type for interface {}", input.getIntfName());
        try {
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                String errMsg = String.format("Retrieval of Interface Type for the key {%s} failed due to "
                        + "missing Interface in Config DataStore", interfaceName);
                LOG.error(errMsg);
                rpcResultBuilder = RpcResultBuilder.<GetInterfaceTypeOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, errMsg);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            GetInterfaceTypeOutputBuilder output = new GetInterfaceTypeOutputBuilder()
                    .setInterfaceType(interfaceInfo.getType());
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("interface type for interface {} is {}", input.getIntfName(), interfaceInfo.getType());
        } catch (Exception e) {
            LOG.error("Retrieval of interface type for the key {}", interfaceName, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
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
                LOG.error(errMsg);
                rpcResultBuilder = RpcResultBuilder.<GetTunnelTypeOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, errMsg);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                IfTunnel tnl = interfaceInfo.getAugmentation(IfTunnel.class);
                Class<? extends TunnelTypeBase> tunType = tnl.getTunnelInterfaceType();
                GetTunnelTypeOutputBuilder output = new GetTunnelTypeOutputBuilder().setTunnelType(tunType);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
            } else {
                LOG.error("Retrieval of interface type for the key {} failed", interfaceName);
                rpcResultBuilder = RpcResultBuilder.failed();
            }
        } catch (Exception e) {
            LOG.error("Retrieval of interface type for the key {}", interfaceName, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
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
            LOG.debug("Egress Actions for interface {} is {}", input.getIntfName(), actionsList);
        } catch (Exception e) {
            String errMsg = String.format("Retrieval of egress actions for {%s} failed due to %s", input.getIntfName(),
                    e.getMessage());
            LOG.debug(errMsg);
            rpcResultBuilder = RpcResultBuilder.<GetEgressActionsForInterfaceOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, errMsg);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetPortFromInterfaceOutput>> getPortFromInterface(GetPortFromInterfaceInput input) {
        RpcResultBuilder<GetPortFromInterfaceOutput> rpcResultBuilder;
        String interfaceName = input.getIntfName();
        LOG.debug("Get port from interface {}", input.getIntfName());
        try {
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
                GetPortFromInterfaceOutputBuilder output = new GetPortFromInterfaceOutputBuilder().setDpid(dpId)
                        .setPortname(interfaceName).setPortno(portNo).setPhyAddress(phyAddress);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
                LOG.debug("port for interface {} is {}", input.getIntfName(), portNo);
            } else {
                rpcResultBuilder = getRpcErrorResultForGetPortRpc(interfaceName, "missing Interface state");
            }
        } catch (Exception e) {
            rpcResultBuilder = getRpcErrorResultForGetPortRpc(interfaceName, e.getMessage());
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private RpcResultBuilder<GetPortFromInterfaceOutput> getRpcErrorResultForGetPortRpc(String interfaceName,
            String errMsg) {
        errMsg = String.format("Retrieval of Port for the key {%s} failed due to %s", interfaceName, errMsg);
        LOG.error(errMsg);
        RpcResultBuilder<GetPortFromInterfaceOutput> rpcResultBuilder = RpcResultBuilder
                .<GetPortFromInterfaceOutput>failed().withError(RpcError.ErrorType.APPLICATION, errMsg);
        return rpcResultBuilder;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetNodeconnectorIdFromInterfaceOutput>> getNodeconnectorIdFromInterface(
            GetNodeconnectorIdFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetNodeconnectorIdFromInterfaceOutput> rpcResultBuilder;
        LOG.debug("Get nodeconnector id from interface {}", input.getIntfName());
        try {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                    .getInterfaceState(interfaceName, dataBroker);
            String lowerLayerIf = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);

            GetNodeconnectorIdFromInterfaceOutputBuilder output = new GetNodeconnectorIdFromInterfaceOutputBuilder()
                    .setNodeconnectorId(nodeConnectorId);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("nodeconnector id for interface {} is {}", input.getIntfName(), lowerLayerIf);
        } catch (Exception e) {
            LOG.error("Retrieval of nodeconnector id for the key {}", interfaceName, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetInterfaceFromIfIndexOutput>> getInterfaceFromIfIndex(
            GetInterfaceFromIfIndexInput input) {
        Integer ifIndex = input.getIfIndex();
        RpcResultBuilder<GetInterfaceFromIfIndexOutput> rpcResultBuilder = null;
        LOG.debug("Get interface from ifindex {}", input.getIfIndex());
        try {
            InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class)
                    .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
            Optional<IfIndexInterface> ifIndexesInterface = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id,
                    dataBroker);

            if (!ifIndexesInterface.isPresent()) {
                String errMsg = "Could not find " + id.toString() + " in OperationalDS for idIndex=" + ifIndex;
                return Futures.immediateFuture(RpcResultBuilder.<GetInterfaceFromIfIndexOutput>failed()
                                                   .withError(RpcError.ErrorType.APPLICATION, errMsg).build());
            }
            String interfaceName = ifIndexesInterface.get().getInterfaceName();
            GetInterfaceFromIfIndexOutput output =
                new GetInterfaceFromIfIndexOutputBuilder().setInterfaceName(interfaceName).build();
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output);
            LOG.debug("interface corresponding to ifindex {} is {}", input.getIfIndex(), interfaceName);
        } catch (Exception e) {
            LOG.error("Retrieval of interfaceName for the key {}", ifIndex, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetDpnInterfaceListOutput>> getDpnInterfaceList(GetDpnInterfaceListInput input) {
        BigInteger dpnid = input.getDpid();
        RpcResultBuilder<GetDpnInterfaceListOutput> rpcResultBuilder = null;
        List<Interfaces> interfacesList = new ArrayList<>();
        LOG.debug("Get interface list for dpn {}", input.getDpid());
        try {
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
            interfaceNameEntries.stream().forEach(
                (interfaceNameEntry) -> {
                    InterfacesBuilder intf = new InterfacesBuilder()
                            .setInterfaceName(interfaceNameEntry.getInterfaceName())
                            .setInterfaceType(interfaceNameEntry.getInterfaceType());
                    interfacesList.add(intf.build());
                });
            GetDpnInterfaceListOutput output =
                new GetDpnInterfaceListOutputBuilder().setInterfaces(interfacesList).build();
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output);
            LOG.debug("interface list for dpn {} is {}", input.getDpid(), interfacesList);
        } catch (Exception e) {
            LOG.error("Retrieval of interfaceNameList for the dpnId {}", dpnid, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    protected Future<RpcResult<GetDpnInterfaceListOutput>> buildEmptyInterfaceListResult() {
        GetDpnInterfaceListOutput emptyListResult =
            new GetDpnInterfaceListOutputBuilder().setInterfaces(Collections.emptyList()).build();
        return Futures.immediateFuture(RpcResultBuilder.<GetDpnInterfaceListOutput>success()
                                           .withResult(emptyListResult).build());
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
