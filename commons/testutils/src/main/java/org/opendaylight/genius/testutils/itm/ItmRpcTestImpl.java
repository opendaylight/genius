/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils.itm;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetDpnEndpointIpsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetDpnEndpointIpsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetDpnEndpointIpsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetDpnInfoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetDpnInfoOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetEgressActionsForTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetEgressActionsForTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetWatchPortForTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetWatchPortForTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.SetBfdParamOnTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.SetBfdParamOnTunnelOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public final class ItmRpcTestImpl implements ItmRpcService {

    private final Map<BigInteger, IpAddress> tepIps = new ConcurrentHashMap<>();
    private final Map<BigInteger, Map<String, String>> interfaceNames = new ConcurrentHashMap<>();
    private final Map<BigInteger, Map<String, String>> externalInterfaceNames = new ConcurrentHashMap<>();

    public synchronized void addDpn(BigInteger dpnId, String tepIp) {
        tepIps.put(dpnId, IpAddressBuilder.getDefaultInstance(tepIp));
    }

    public synchronized void addInterface(BigInteger dpnId, String dstTep, String interfaceName) {
        interfaceNames.putIfAbsent(dpnId, new ConcurrentHashMap<>());
        interfaceNames.get(dpnId).put(dstTep, interfaceName);
    }

    public synchronized void addL2GwInterface(BigInteger dpnId, String nodeId, String interfaceName) {
        externalInterfaceNames.putIfAbsent(dpnId, new ConcurrentHashMap<>());
        externalInterfaceNames.get(dpnId).put(nodeId, interfaceName);
    }

    public synchronized void addExternalInterface(BigInteger dpnId, String dstTep, String interfaceName) {
        //dstTep = IpAddressBuilder.getDefaultInstance(dstTep).toString();
        externalInterfaceNames.putIfAbsent(dpnId, new ConcurrentHashMap<>());
        externalInterfaceNames.get(dpnId).put(dstTep, interfaceName);
    }

    @Override
    public synchronized ListenableFuture<RpcResult<BuildExternalTunnelFromDpnsOutput>> buildExternalTunnelFromDpns(
            BuildExternalTunnelFromDpnsInput input) {
        return RpcResultBuilder.<BuildExternalTunnelFromDpnsOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<RemoveExternalTunnelEndpointOutput>> removeExternalTunnelEndpoint(
            RemoveExternalTunnelEndpointInput input) {
        return RpcResultBuilder.<RemoveExternalTunnelEndpointOutput>success().buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetDpnInfoOutput>> getDpnInfo(GetDpnInfoInput input) {
        throw new UnsupportedOperationException("getDpnInfo");
    }

    @Override
    public synchronized ListenableFuture<RpcResult<AddL2GwMlagDeviceOutput>> addL2GwMlagDevice(
            AddL2GwMlagDeviceInput input) {
        return RpcResultBuilder.<AddL2GwMlagDeviceOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<RemoveExternalTunnelFromDpnsOutput>> removeExternalTunnelFromDpns(
            RemoveExternalTunnelFromDpnsInput input) {
        return RpcResultBuilder.<RemoveExternalTunnelFromDpnsOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<DeleteL2GwDeviceOutput>> deleteL2GwDevice(
            DeleteL2GwDeviceInput input) {
        return RpcResultBuilder.<DeleteL2GwDeviceOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<AddL2GwDeviceOutput>> addL2GwDevice(AddL2GwDeviceInput input) {
        return RpcResultBuilder.<AddL2GwDeviceOutput>success().buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetWatchPortForTunnelOutput>> getWatchPortForTunnel(
            GetWatchPortForTunnelInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized ListenableFuture<RpcResult<IsTunnelInternalOrExternalOutput>> isTunnelInternalOrExternal(
            IsTunnelInternalOrExternalInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized ListenableFuture<RpcResult<GetTunnelInterfaceNameOutput>> getTunnelInterfaceName(
            GetTunnelInterfaceNameInput input) {
        String interfaceName = interfaceNames.get(input.getSourceDpid())
                .get(tepIps.get(input.getDestinationDpid()).stringValue());
        GetTunnelInterfaceNameOutput output =
                new GetTunnelInterfaceNameOutputBuilder().setInterfaceName(interfaceName).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<IsDcgwPresentOutput>> isDcgwPresent(IsDcgwPresentInput input) {
        IsDcgwPresentOutput output = new IsDcgwPresentOutputBuilder().setRetVal(0L).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<GetExternalTunnelInterfaceNameOutput>>
            getExternalTunnelInterfaceName(GetExternalTunnelInterfaceNameInput input) {
        String interfaceName = externalInterfaceNames.get(new BigInteger(input.getSourceNode(), 10))
                .get(input.getDestinationNode());
        GetExternalTunnelInterfaceNameOutput output = new GetExternalTunnelInterfaceNameOutputBuilder()
                .setInterfaceName(interfaceName)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<CreateTerminatingServiceActionsOutput>>
        createTerminatingServiceActions(CreateTerminatingServiceActionsInput input) {
        return RpcResultBuilder.<CreateTerminatingServiceActionsOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<GetDpnEndpointIpsOutput>> getDpnEndpointIps(
            GetDpnEndpointIpsInput input) {
        GetDpnEndpointIpsOutput output = new GetDpnEndpointIpsOutputBuilder()
                .setNexthopipList(Lists.newArrayList(tepIps.get(input.getSourceDpid()))).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<DeleteL2GwMlagDeviceOutput>> deleteL2GwMlagDevice(
            DeleteL2GwMlagDeviceInput input) {
        return RpcResultBuilder.<DeleteL2GwMlagDeviceOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<GetInternalOrExternalInterfaceNameOutput>>
            getInternalOrExternalInterfaceName(GetInternalOrExternalInterfaceNameInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized ListenableFuture<RpcResult<RemoveTerminatingServiceActionsOutput>>
        removeTerminatingServiceActions(RemoveTerminatingServiceActionsInput input) {
        return RpcResultBuilder.<RemoveTerminatingServiceActionsOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<AddExternalTunnelEndpointOutput>> addExternalTunnelEndpoint(
            AddExternalTunnelEndpointInput input) {
        return RpcResultBuilder.<AddExternalTunnelEndpointOutput>success().buildFuture();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<SetBfdParamOnTunnelOutput>> setBfdParamOnTunnel(
            SetBfdParamOnTunnelInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized ListenableFuture<RpcResult<GetEgressActionsForTunnelOutput>>
        getEgressActionsForTunnel(GetEgressActionsForTunnelInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized ListenableFuture<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        throw new UnsupportedOperationException("TODO");
    }
}
