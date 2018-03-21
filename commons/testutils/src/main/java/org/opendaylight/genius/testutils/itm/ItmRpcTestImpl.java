/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils.itm;

import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddL2GwMlagDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.BuildExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.CreateTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwDeviceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.DeleteL2GwMlagDeviceInput;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.SetBfdEnableOnTunnelInput;
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
    public synchronized Future<RpcResult<Void>> buildExternalTunnelFromDpns(BuildExternalTunnelFromDpnsInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> removeExternalTunnelEndpoint(RemoveExternalTunnelEndpointInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public Future<RpcResult<GetDpnInfoOutput>> getDpnInfo(GetDpnInfoInput input) {
        throw new UnsupportedOperationException("getDpnInfo");
    }

    @Override
    public synchronized Future<RpcResult<Void>> addL2GwMlagDevice(AddL2GwMlagDeviceInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> removeExternalTunnelFromDpns(RemoveExternalTunnelFromDpnsInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> deleteL2GwDevice(DeleteL2GwDeviceInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> addL2GwDevice(AddL2GwDeviceInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<IsTunnelInternalOrExternalOutput>> isTunnelInternalOrExternal(
            IsTunnelInternalOrExternalInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized Future<RpcResult<GetTunnelInterfaceNameOutput>> getTunnelInterfaceName(
            GetTunnelInterfaceNameInput input) {
        String interfaceName = interfaceNames.get(input.getSourceDpid())
                .get(new String(tepIps.get(input.getDestinationDpid()).getValue()));
        GetTunnelInterfaceNameOutput output =
                new GetTunnelInterfaceNameOutputBuilder().setInterfaceName(interfaceName).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<IsDcgwPresentOutput>> isDcgwPresent(IsDcgwPresentInput input) {
        IsDcgwPresentOutput output = new IsDcgwPresentOutputBuilder().setRetVal(0L).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<GetExternalTunnelInterfaceNameOutput>> getExternalTunnelInterfaceName(
            GetExternalTunnelInterfaceNameInput input) {
        String interfaceName = externalInterfaceNames.get(new BigInteger(input.getSourceNode(), 10))
                .get(input.getDestinationNode());
        GetExternalTunnelInterfaceNameOutput output = new GetExternalTunnelInterfaceNameOutputBuilder()
                .setInterfaceName(interfaceName)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> createTerminatingServiceActions(
            CreateTerminatingServiceActionsInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<GetDpnEndpointIpsOutput>> getDpnEndpointIps(GetDpnEndpointIpsInput input) {
        GetDpnEndpointIpsOutput output = new GetDpnEndpointIpsOutputBuilder()
                .setNexthopipList(Lists.newArrayList(tepIps.get(input.getSourceDpid()))).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> deleteL2GwMlagDevice(DeleteL2GwMlagDeviceInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<GetInternalOrExternalInterfaceNameOutput>> getInternalOrExternalInterfaceName(
            GetInternalOrExternalInterfaceNameInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized Future<RpcResult<Void>> removeTerminatingServiceActions(
            RemoveTerminatingServiceActionsInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> addExternalTunnelEndpoint(AddExternalTunnelEndpointInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> setBfdEnableOnTunnel(SetBfdEnableOnTunnelInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized Future<RpcResult<GetEgressActionsForTunnelOutput>>
        getEgressActionsForTunnel(GetEgressActionsForTunnelInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public synchronized Future<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        throw new UnsupportedOperationException("TODO");
    }
}
