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
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
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

    @Inject
    public InterfaceManagerRpcService(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetDpidFromInterfaceOutput>> getDpidFromInterface(GetDpidFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        LOG.debug("Get dpid for interface {}", input.getIntfName());
        try {
            BigInteger dpId;
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                return newRpcErrorResultFutureWithoutLogging(
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
                    return newRpcErrorResultFutureWithoutLogging(
                            getDpidFromInterfaceErrorMessage(interfaceName, "missing Interface-state"));
                }
            }
            GetDpidFromInterfaceOutputBuilder output = new GetDpidFromInterfaceOutputBuilder().setDpid(dpId);
            // TODO build a similar abstraction for successful immediate Future RpcResult as for failures (below)
            RpcResultBuilder<GetDpidFromInterfaceOutput> rpcResultBuilder;
            rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("Dpid for interface {} is {}", input.getIntfName(), dpId);
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultFutureWithoutLogging(
                    getDpidFromInterfaceErrorMessage(interfaceName, e.getMessage()), e);
        }
    }

    private String getDpidFromInterfaceErrorMessage(final String interfaceName, final String dueTo) {
        return String.format("Retrieval of datapath id for the key {%s} failed due to %s",
                interfaceName, dueTo);
    }

    // TODO move the following helper methods to somewhere else, to be shared with other projects
    private <T extends DataObject> Future<RpcResult<T>> newRpcErrorResultFutureWithoutLogging(String message) {
        RpcResultBuilder<T> rpcResultBuilder = RpcResultBuilder
                .<T>failed().withError(RpcError.ErrorType.APPLICATION, message);
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private <T extends DataObject> Future<RpcResult<T>> newRpcErrorResultFutureWithoutLogging(String message,
            Throwable cause) {
        RpcResultBuilder<T> rpcResultBuilder = RpcResultBuilder
                .<T>failed().withError(RpcError.ErrorType.APPLICATION, message, cause);
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    private <T extends DataObject> Future<RpcResult<T>> newRpcErrorResultWithErrorLog(String message) {
        LOG.error(message);
        return newRpcErrorResultFutureWithoutLogging(message);
    }

    private <T extends DataObject> Future<RpcResult<T>> newRpcErrorResultWithErrorLog(String message, Throwable cause) {
        LOG.error(message, cause);
        return newRpcErrorResultFutureWithoutLogging(message, cause);
    }

    private <T extends DataObject> Future<RpcResult<T>> newRpcErrorResultWithDebugLog(String message, Throwable cause) {
        LOG.debug(message, cause);
        return newRpcErrorResultFutureWithoutLogging(message, cause);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetEndpointIpForDpnOutput>> getEndpointIpForDpn(GetEndpointIpForDpnInput input) {
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
            // TODO as above, simplify the success case later, as we have the failure case below
            RpcResultBuilder<GetEndpointIpForDpnOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(endpointIpForDpnOutput.build());
            LOG.debug("Endpoint ip for dpn {} is {}", input.getDpid(), tunnel.getTunnelSource());
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultWithErrorLog(
                    "getEndpointIpForDpn() Retrieval of endpoint for dpn " + input.getDpid() + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetEgressInstructionsForInterfaceOutput>> getEgressInstructionsForInterface(
            GetEgressInstructionsForInterfaceInput input) {
        LOG.debug("Get Egress Instructions for interface {} with key {}", input.getIntfName(), input.getTunnelKey());
        try {
            List<Instruction> instructions = IfmUtil.getEgressInstructionsForInterface(input.getIntfName(),
                    input.getTunnelKey(), dataBroker, false);
            GetEgressInstructionsForInterfaceOutputBuilder output = new GetEgressInstructionsForInterfaceOutputBuilder()
                    .setInstruction(instructions);
            // TODO as above, simplify the success case later, as we have the failure case below
            RpcResultBuilder<GetEgressInstructionsForInterfaceOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("Egress Instructions for interface {} is {}", input.getIntfName(), instructions);
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultWithDebugLog("getEgressInstructionsForInterface() Retrieval of egress "
                    + "instructions for the key " + input.getIntfName() + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetInterfaceTypeOutput>> getInterfaceType(GetInterfaceTypeInput input) {
        String interfaceName = input.getIntfName();
        LOG.debug("Get interface type for interface {}", input.getIntfName());
        try {
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                String errMsg = String.format("getInterfaceType() Retrieval of Interface Type for the key {%s} failed "
                        + "due to missing Interface in Config DataStore", interfaceName);
                return newRpcErrorResultFutureWithoutLogging(errMsg);
            }
            GetInterfaceTypeOutputBuilder output = new GetInterfaceTypeOutputBuilder()
                    .setInterfaceType(interfaceInfo.getType());
            // TODO as above, simplify the success case later, as we have the failure case below
            RpcResultBuilder<GetInterfaceTypeOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("interface type for interface {} is {}", input.getIntfName(), interfaceInfo.getType());
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultWithErrorLog(
                    "getInterfaceType() Retrieval of interface type for the key " + interfaceName + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        String interfaceName = input.getIntfName();
        try {
            InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
            Interface interfaceInfo = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey, dataBroker);
            if (interfaceInfo == null) {
                String errMsg = String.format(
                        "Retrieval of Tunnel Type for the key {%s} failed due to missing Interface in Config DataStore",
                        interfaceName);
                return newRpcErrorResultWithErrorLog(errMsg);
            }
            if (Tunnel.class.equals(interfaceInfo.getType())) {
                IfTunnel tnl = interfaceInfo.getAugmentation(IfTunnel.class);
                Class<? extends TunnelTypeBase> tunType = tnl.getTunnelInterfaceType();
                GetTunnelTypeOutputBuilder output = new GetTunnelTypeOutputBuilder().setTunnelType(tunType);
                // TODO as above, simplify the success case later, as we have the failure case below
                RpcResultBuilder<GetTunnelTypeOutput> rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
                return Futures.immediateFuture(rpcResultBuilder.build());
            } else {
                return newRpcErrorResultWithErrorLog(
                        "Retrieval of interface type for the key {} failed: " + interfaceName);
            }
        } catch (Exception e) {
            return newRpcErrorResultWithErrorLog(
                    "getTunnelType() Retrieval of interface type for the key " + interfaceName + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetEgressActionsForInterfaceOutput>> getEgressActionsForInterface(
            GetEgressActionsForInterfaceInput input) {
        try {
            LOG.debug("Get Egress Action for interface {} with key {}", input.getIntfName(), input.getTunnelKey());
            List<Action> actionsList = IfmUtil.getEgressActionsForInterface(input.getIntfName(), input.getTunnelKey(),
                    input.getActionKey(), dataBroker, false);
            GetEgressActionsForInterfaceOutputBuilder output = new GetEgressActionsForInterfaceOutputBuilder()
                    .setAction(actionsList);
            // TODO as above, simplify the success case later, as we have the failure case below
            RpcResultBuilder<GetEgressActionsForInterfaceOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("Egress Actions for interface {} is {}", input.getIntfName(), actionsList);
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultWithDebugLog(
                    "Retrieval of egress actions " + input.getIntfName() + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetPortFromInterfaceOutput>> getPortFromInterface(GetPortFromInterfaceInput input) {
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
                // TODO as above, simplify the success case later, as we have the failure case below
                RpcResultBuilder<GetPortFromInterfaceOutput> rpcResultBuilder = RpcResultBuilder.success();
                rpcResultBuilder.withResult(output.build());
                LOG.debug("port for interface {} is {}", input.getIntfName(), portNo);
                return Futures.immediateFuture(rpcResultBuilder.build());
            } else {
                return newRpcErrorResultWithErrorLog(
                        getPortFromInterfaceErrorMessage(interfaceName, "missing Interface state"));
            }
        } catch (Exception e) {
            return newRpcErrorResultWithErrorLog(
                    getPortFromInterfaceErrorMessage(interfaceName, e.getMessage()), e);
        }
    }

    private String getPortFromInterfaceErrorMessage(final String interfaceName, final String errMsg) {
        return String.format("Retrieval of Port for the key {%s} failed due to %s", interfaceName, errMsg);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetNodeconnectorIdFromInterfaceOutput>> getNodeconnectorIdFromInterface(
            GetNodeconnectorIdFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        LOG.debug("Get nodeconnector id from interface {}", input.getIntfName());
        try {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                    .getInterfaceState(interfaceName, dataBroker);
            String lowerLayerIf = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);

            GetNodeconnectorIdFromInterfaceOutputBuilder output = new GetNodeconnectorIdFromInterfaceOutputBuilder()
                    .setNodeconnectorId(nodeConnectorId);
            // TODO as above, simplify the success case later, as we have the failure case below
            RpcResultBuilder<GetNodeconnectorIdFromInterfaceOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output.build());
            LOG.debug("nodeconnector id for interface {} is {}", input.getIntfName(), lowerLayerIf);
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultWithErrorLog("getNodeconnectorIdFromInterface() Retrieval of "
                    + "nodeconnector id for the key " + interfaceName + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetInterfaceFromIfIndexOutput>> getInterfaceFromIfIndex(
            GetInterfaceFromIfIndexInput input) {
        Integer ifIndex = input.getIfIndex();
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
            // TODO as above, simplify the success case later, as we have the failure case below
            RpcResultBuilder<GetInterfaceFromIfIndexOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output);
            LOG.debug("interface corresponding to ifindex {} is {}", input.getIfIndex(), interfaceName);
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultWithErrorLog(
                    "getInterfaceFromIfIndex() Retrieval of interfaceName for the key " + ifIndex + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<GetDpnInterfaceListOutput>> getDpnInterfaceList(GetDpnInterfaceListInput input) {
        BigInteger dpnid = input.getDpid();
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
            List<String> interfaceList = interfaceNameEntries.stream().map(InterfaceNameEntry::getInterfaceName)
                    .collect(Collectors.toList());
            GetDpnInterfaceListOutput output =
                new GetDpnInterfaceListOutputBuilder().setInterfacesList(interfaceList).build();
            // TODO as above, simplify the success case later, as we have the failure case below
            RpcResultBuilder<GetDpnInterfaceListOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(output);
            LOG.debug("interface list for dpn {} is {}", input.getDpid(), interfaceList);
            return Futures.immediateFuture(rpcResultBuilder.build());
        } catch (Exception e) {
            return newRpcErrorResultWithErrorLog(
                    "getDpnInterfaceList() Retrieval of interfaceNameList for the dpnId " + dpnid + " failed", e);
        }
    }

    private Future<RpcResult<GetDpnInterfaceListOutput>> buildEmptyInterfaceListResult() {
        GetDpnInterfaceListOutput emptyListResult =
            new GetDpnInterfaceListOutputBuilder().setInterfacesList(Collections.emptyList()).build();
        return Futures.immediateFuture(RpcResultBuilder.<GetDpnInterfaceListOutput>success()
                                           .withResult(emptyListResult).build());
    }
}
