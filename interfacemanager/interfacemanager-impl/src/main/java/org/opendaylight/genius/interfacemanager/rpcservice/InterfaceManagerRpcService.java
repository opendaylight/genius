/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.rpcservice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.CreateTerminatingServiceActionsInput;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
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
    public InterfaceManagerRpcService(DataBroker dataBroker, IMdsalApiManager mdsalMgr) {
        this.dataBroker = dataBroker;
        this.mdsalMgr = mdsalMgr;
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
                rpcResultBuilder = getRpcErrorResultForGetDpnIdRpc(interfaceName, "missing Interface in Config DataStore");
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                ParentRefs parentRefs = interfaceInfo.getAugmentation(ParentRefs.class);
                dpId = parentRefs.getDatapathNodeIdentifier();
            } else {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                        InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
                if (ifState != null) {
                    String lowerLayerIf = ifState.getLowerLayerIf().get(0);
                    NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
                    dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                } else {
                     rpcResultBuilder = getRpcErrorResultForGetDpnIdRpc(interfaceName, "missing Interface-state");
                     return Futures.immediateFuture(rpcResultBuilder.build());
                }
            }
            GetDpidFromInterfaceOutputBuilder output = new GetDpidFromInterfaceOutputBuilder().setDpid(
                    dpId);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            rpcResultBuilder = getRpcErrorResultForGetDpnIdRpc(interfaceName, e.getMessage());
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private RpcResultBuilder<GetDpidFromInterfaceOutput> getRpcErrorResultForGetDpnIdRpc(String interfaceName, String errMsg) {
        errMsg = String.format("Retrieval of datapath id for the key {%s} failed due to %s", interfaceName, errMsg);
        LOG.error(errMsg);
        RpcResultBuilder<GetDpidFromInterfaceOutput> rpcResultBuilder = RpcResultBuilder.<GetDpidFromInterfaceOutput>failed().withError(RpcError.ErrorType.APPLICATION, errMsg);
        return rpcResultBuilder;
    }

    @Override
    public Future<RpcResult<Void>> createTerminatingServiceActions(final CreateTerminatingServiceActionsInput input) {
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        try{
            LOG.info("create terminatingServiceAction on DpnId = {} for tunnel-key {}", input.getDpid() , input.getTunnelKey());
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(input.getInterfaceName()),dataBroker);
            IfTunnel tunnelInfo = interfaceInfo.getAugmentation(IfTunnel.class);
            if(tunnelInfo != null) {
                ListenableFuture<Void> installFlowResult =tunnelInfo.getTunnelInterfaceType().isAssignableFrom(TunnelTypeMplsOverGre.class)?
                        makeLFIBFlow(input.getDpid(),input.getTunnelKey(), input.getInstruction(), NwConstants.ADD_FLOW) :
                        makeTerminatingServiceFlow(tunnelInfo, input.getDpid(), input.getTunnelKey(), input.getInstruction(), NwConstants.ADD_FLOW);
                Futures.addCallback(installFlowResult, new FutureCallback<Void>(){

                    @Override
                    public void onSuccess(Void aVoid) {
                        result.set(RpcResultBuilder.<Void>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to install terminating service flow for %s", input.getInterfaceName());
                        LOG.error("create terminating service actions failed. {}. {}", msg, error);
                        result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                    }
                });
                result.set(RpcResultBuilder.<Void>success().build());
            } else {
                String msg = String.format("Terminating Service Actions cannot be created for a non-tunnel interface %s",input.getInterfaceName());
                LOG.error(msg);
                result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg).build());
            }
        }catch(Exception e){
            String msg = String.format("create Terminating Service Actions for %s failed",input.getInterfaceName());
            LOG.error("create Terminating Service Actions for {} failed due to {}" ,input.getDpid(), e);
            result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, e.getMessage()).build());
        }
        return result;
    }

    @Override
    public Future<RpcResult<Void>> removeTerminatingServiceActions(final RemoveTerminatingServiceActionsInput input) {
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        try{
            dataBroker.newWriteOnlyTransaction();
            LOG.info("remove terminatingServiceAction on DpnId = {} for tunnel-key {}", input.getDpid() , input.getTunnelKey());

            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(input.getInterfaceName()),dataBroker);
            IfTunnel tunnelInfo = interfaceInfo.getAugmentation(IfTunnel.class);
            if(tunnelInfo != null) {
                ListenableFuture<Void> removeFlowResult = tunnelInfo.getTunnelInterfaceType().isAssignableFrom(TunnelTypeMplsOverGre.class)?
                        makeLFIBFlow(input.getDpid(),input.getTunnelKey(), null, NwConstants.DEL_FLOW) :
                        makeTerminatingServiceFlow(tunnelInfo, input.getDpid(), input.getTunnelKey(), null, NwConstants.DEL_FLOW);
                Futures.addCallback(removeFlowResult, new FutureCallback<Void>(){

                    @Override
                    public void onSuccess(Void aVoid) {
                        result.set(RpcResultBuilder.<Void>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to install terminating service flow %s", input.getInterfaceName());
                        LOG.error("create terminating service actions failed. {}. {}", msg, error);
                        result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                    }
                });
                result.set(RpcResultBuilder.<Void>success().build());
            } else {
                String msg = String.format("Terminating Service Actions cannot be removed for a non-tunnel interface %s",
                        input.getInterfaceName());
                LOG.error(msg);
                result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg).build());
            }
        }catch(Exception e){
            LOG.error("Remove Terminating Service Actions for {} failed due to {}" ,input.getDpid(), e);
            String msg = String.format("Remove Terminating Service Actions for %d failed.", input.getDpid());
            result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, e.getMessage()).build());
        }
        return result;
    }

    @Override
    public Future<RpcResult<GetEndpointIpForDpnOutput>> getEndpointIpForDpn(GetEndpointIpForDpnInput input) {
        RpcResultBuilder<GetEndpointIpForDpnOutput> rpcResultBuilder;
        try {
            BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(input.getDpid());
            InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier =
                    InterfaceMetaUtils.getBridgeEntryIdentifier(bridgeEntryKey);
            BridgeEntry bridgeEntry =
                    InterfaceMetaUtils.getBridgeEntryFromConfigDS(bridgeEntryInstanceIdentifier,
                            dataBroker);
            // local ip of any of the bridge interface entry will be the dpn end point ip
            BridgeInterfaceEntry bridgeInterfaceEntry = bridgeEntry.getBridgeInterfaceEntry().get(0);
            InterfaceKey interfaceKey = new InterfaceKey(bridgeInterfaceEntry.getInterfaceName());
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            IfTunnel tunnel = interfaceInfo.getAugmentation(IfTunnel.class);
            GetEndpointIpForDpnOutputBuilder endpointIpForDpnOutput = new GetEndpointIpForDpnOutputBuilder().setLocalIps(Arrays.asList(tunnel.getTunnelSource()));
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(endpointIpForDpnOutput.build());
        }catch(Exception e){
            LOG.error("Retrieval of endpoint of for dpn {} failed due to {}" ,input.getDpid(), e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetEgressInstructionsForInterfaceOutput>> getEgressInstructionsForInterface(GetEgressInstructionsForInterfaceInput input) {
        RpcResultBuilder<GetEgressInstructionsForInterfaceOutput> rpcResultBuilder;
        try {
            List<Instruction> instructions = IfmUtil.getEgressInstructionsForInterface(input.getIntfName(), input.getTunnelKey(), dataBroker, false);
            GetEgressInstructionsForInterfaceOutputBuilder output = new GetEgressInstructionsForInterfaceOutputBuilder().
                    setInstruction(instructions);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        }catch(Exception e){
            LOG.error("Retrieval of egress actions for the key {} failed due to {}" ,input.getIntfName(), e.getMessage());
            rpcResultBuilder = RpcResultBuilder.failed();
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
                String errMsg = String.format("Retrieval of Interface Type for the key {%s} failed due to missing Interface in Config DataStore", interfaceName);
                LOG.error(errMsg);
                rpcResultBuilder = RpcResultBuilder.<GetInterfaceTypeOutput>failed().withError(RpcError.ErrorType.APPLICATION, errMsg);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            GetInterfaceTypeOutputBuilder output = new GetInterfaceTypeOutputBuilder().setInterfaceType(interfaceInfo.getType());
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            LOG.error("Retrieval of interface type for the key {} failed due to {}", interfaceName, e);
            rpcResultBuilder = RpcResultBuilder.failed();
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
                String errMsg = String.format("Retrieval of Tunnel Type for the key {%s} failed due to missing Interface in Config DataStore", interfaceName);
                LOG.error(errMsg);
                rpcResultBuilder = RpcResultBuilder.<GetTunnelTypeOutput>failed().withError(RpcError.ErrorType.APPLICATION, errMsg);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                IfTunnel tnl = interfaceInfo.getAugmentation(IfTunnel.class);
                Class <? extends TunnelTypeBase> tun_type = tnl.getTunnelInterfaceType();
                GetTunnelTypeOutputBuilder output = new GetTunnelTypeOutputBuilder().setTunnelType(tun_type);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
            } else {
                LOG.error("Retrieval of interface type for the key {} failed", interfaceName);
                rpcResultBuilder = RpcResultBuilder.failed();
            }
        } catch (Exception e) {
            LOG.error("Retrieval of interface type for the key {} failed due to {}", interfaceName, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetEgressActionsForInterfaceOutput>> getEgressActionsForInterface(GetEgressActionsForInterfaceInput input) {
        RpcResultBuilder<GetEgressActionsForInterfaceOutput> rpcResultBuilder;
        try {
            LOG.debug("Get Egress Action for interface {} with key {}", input.getIntfName(), input.getTunnelKey());
            List<Action> actionsList = IfmUtil.getEgressActionsForInterface(input.getIntfName(),
                    input.getTunnelKey(), input.getActionKey(),
                    dataBroker, false);
            GetEgressActionsForInterfaceOutputBuilder output = new GetEgressActionsForInterfaceOutputBuilder().
                    setAction(actionsList);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        }catch(Exception e){
            LOG.error("Retrieval of egress actions for the key {} failed due to {}" ,input.getIntfName(), e.getMessage());
            rpcResultBuilder = RpcResultBuilder.failed();
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
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                    InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
            if (ifState != null) {
                String lowerLayerIf = ifState.getLowerLayerIf().get(0);
                NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
                dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
                portNo = Long.valueOf(IfmUtil.getPortNoFromNodeConnectorId(nodeConnectorId));
                // FIXME Assuming portName and interfaceName are same
                GetPortFromInterfaceOutputBuilder output = new GetPortFromInterfaceOutputBuilder().setDpid(dpId).
                        setPortname(interfaceName).setPortno(Long.valueOf(portNo));
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
            } else {
                rpcResultBuilder = getRpcErrorResultForGetPortRpc(interfaceName, "missing Interface state");
            }
        }catch(Exception e){
            rpcResultBuilder = getRpcErrorResultForGetPortRpc(interfaceName, e.getMessage());
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private RpcResultBuilder<GetPortFromInterfaceOutput> getRpcErrorResultForGetPortRpc(String interfaceName, String errMsg) {
        errMsg = String.format("Retrieval of Port for the key {%s} failed due to %s", interfaceName, errMsg);
        LOG.error(errMsg);
        RpcResultBuilder<GetPortFromInterfaceOutput> rpcResultBuilder = RpcResultBuilder.<GetPortFromInterfaceOutput>failed().withError(RpcError.ErrorType.APPLICATION, errMsg);
        return rpcResultBuilder;
    }

    @Override
    public Future<RpcResult<GetNodeconnectorIdFromInterfaceOutput>> getNodeconnectorIdFromInterface(GetNodeconnectorIdFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        RpcResultBuilder<GetNodeconnectorIdFromInterfaceOutput> rpcResultBuilder;
        try {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                    InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
            String lowerLayerIf = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);

            GetNodeconnectorIdFromInterfaceOutputBuilder output = new GetNodeconnectorIdFromInterfaceOutputBuilder().setNodeconnectorId(nodeConnectorId);
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
        } catch (Exception e) {
            LOG.error("Retrieval of nodeconnector id for the key {} failed due to {}", interfaceName, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetInterfaceFromIfIndexOutput>> getInterfaceFromIfIndex(GetInterfaceFromIfIndexInput input) {
        Integer ifIndex = input.getIfIndex();
        RpcResultBuilder<GetInterfaceFromIfIndexOutput> rpcResultBuilder = null;
        try {
            InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class).child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
            Optional<IfIndexInterface> ifIndexesInterface = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker);
            if(ifIndexesInterface.isPresent()) {
                String interfaceName = ifIndexesInterface.get().getInterfaceName();
                GetInterfaceFromIfIndexOutputBuilder output = new GetInterfaceFromIfIndexOutputBuilder().setInterfaceName(interfaceName);
                rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
            }
        } catch (Exception e) {
            LOG.error("Retrieval of interfaceName for the key {} failed due to {}", ifIndex, e);
            rpcResultBuilder = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<GetDpnInterfaceListOutput>> getDpnInterfaceList(GetDpnInterfaceListInput input) {
        BigInteger dpnid = input.getDpid();
        RpcResultBuilder<GetDpnInterfaceListOutput> rpcResultBuilder = null;
        try {
            InstanceIdentifier<DpnToInterface> id =
                    InstanceIdentifier.builder(DpnToInterfaceList.class).child(DpnToInterface.class , new DpnToInterfaceKey(dpnid)).build();
            Optional<DpnToInterface> entry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker);
            if (entry.isPresent()) {
                List<InterfaceNameEntry> interfaceNameEntries = entry.get().getInterfaceNameEntry();
                if (interfaceNameEntries != null && !interfaceNameEntries.isEmpty()) {
                    List<String> interfaceList = interfaceNameEntries.stream().map((a) -> a.getInterfaceName()).collect(Collectors.toList());
                    GetDpnInterfaceListOutputBuilder output = new GetDpnInterfaceListOutputBuilder().setInterfacesList(interfaceList);
                    rpcResultBuilder = RpcResultBuilder.success();
                    rpcResultBuilder.withResult(output.build());
                }
            }
        } catch (Exception e) {
            LOG.error("Retrieval of interfaceNameList for the dpnId {} failed due to {}", dpnid, e);
            rpcResultBuilder = RpcResultBuilder.failed();
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

    private ListenableFuture<Void> makeTerminatingServiceFlow(IfTunnel tunnelInfo, BigInteger dpnId, BigInteger tunnelKey, List<Instruction> instruction, int addOrRemove) {
        List<MatchInfo> mkMatches = new ArrayList<>();
        mkMatches.add(new MatchInfo(MatchFieldType.tunnel_id, new BigInteger[] {tunnelKey}));
        short tableId = tunnelInfo.isInternal() ? NwConstants.INTERNAL_TUNNEL_TABLE :
                NwConstants.EXTERNAL_TUNNEL_TABLE;
        final String flowRef = getFlowRef(dpnId,tableId, tunnelKey);
        Flow terminatingSerFlow = MDSALUtil.buildFlowNew(tableId, flowRef,
                5, "TST Flow Entry", 0, 0,
                IfmConstants.TUNNEL_TABLE_COOKIE.add(tunnelKey), mkMatches, instruction);
        if (addOrRemove == NwConstants.ADD_FLOW) {
            return mdsalMgr.installFlow(dpnId, terminatingSerFlow);
        }

        return mdsalMgr.removeFlow(dpnId, terminatingSerFlow);
    }

    private ListenableFuture<Void> makeLFIBFlow(BigInteger dpnId, BigInteger tunnelKey, List<Instruction> instruction, int addOrRemove) {
        List<MatchInfo> mkMatches = new ArrayList<>();
        mkMatches.add(new MatchInfo(MatchFieldType.eth_type,
                new long[]{0x8847L}));
        mkMatches.add(new MatchInfo(MatchFieldType.mpls_label, new String[]{Long.toString(tunnelKey.longValue())}));
        // Install the flow entry in L3_LFIB_TABLE
        String flowRef = getFlowRef(dpnId, NwConstants.L3_LFIB_TABLE, tunnelKey);

        Flow lfibFlow = MDSALUtil.buildFlowNew(NwConstants.L3_LFIB_TABLE, flowRef,
                IfmConstants.DEFAULT_FLOW_PRIORITY, "LFIB Entry", 0, 0,
                IfmConstants.COOKIE_VM_LFIB_TABLE, mkMatches, instruction);
        if (addOrRemove == NwConstants.ADD_FLOW) {
            return mdsalMgr.installFlow(dpnId, lfibFlow);
        }
        return mdsalMgr.removeFlow(dpnId, lfibFlow);
    }

    private String getFlowRef(BigInteger dpnId, short tableId, BigInteger tunnelKey) {
        return new StringBuffer().append(IfmConstants.TUNNEL_TABLE_FLOWID_PREFIX).append(dpnId).append(NwConstants.FLOWID_SEPARATOR)
                .append(tableId).append(NwConstants.FLOWID_SEPARATOR).append(tunnelKey).toString();
    }
}
