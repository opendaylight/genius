/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.rpcservice;

import static org.opendaylight.genius.infra.FutureRpcResults.fromListenableFuture;
import static org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults.LogLevel.DEBUG;
import static org.opendaylight.genius.tools.mdsal.rpc.FutureRpcResults.LogLevel.NONE;

import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.interfacemanager.interfaces.InterfaceManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceManagerRpcService implements OdlInterfaceRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerRpcService.class);

    private final InterfaceManagerService interfaceManagerService;

    @Inject
    public InterfaceManagerRpcService(InterfaceManagerService interfaceManagerService) {
        this.interfaceManagerService = interfaceManagerService;
    }

    @Override
    public Future<RpcResult<GetEndpointIpForDpnOutput>> getEndpointIpForDpn(GetEndpointIpForDpnInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getEndpointIpForDpn(input)).build();
    }

    @Override
    public Future<RpcResult<GetInterfaceTypeOutput>> getInterfaceType(GetInterfaceTypeInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getInterfaceType(input)).build();
    }

    @Override
    public Future<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getTunnelType(input)).build();
    }

    @Override
    public Future<RpcResult<GetPortFromInterfaceOutput>> getPortFromInterface(GetPortFromInterfaceInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getPortFromInterface(input)).build();
    }

    @Override
    public Future<RpcResult<GetNodeconnectorIdFromInterfaceOutput>> getNodeconnectorIdFromInterface(
            GetNodeconnectorIdFromInterfaceInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getNodeconnectorIdFromInterface(input))
                .build();
    }

    @Override
    public Future<RpcResult<GetInterfaceFromIfIndexOutput>> getInterfaceFromIfIndex(
            GetInterfaceFromIfIndexInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getInterfaceFromIfIndex(input)).build();
    }

    @Override
    public Future<RpcResult<GetDpnInterfaceListOutput>> getDpnInterfaceList(GetDpnInterfaceListInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getDpnInterfaceList(input)).build();
    }

    @Override
    public Future<RpcResult<GetEgressInstructionsForInterfaceOutput>> getEgressInstructionsForInterface(
            GetEgressInstructionsForInterfaceInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getEgressInstructionsForInterface(input))
                .onFailureLogLevel(DEBUG).build();
    }

    @Override
    public Future<RpcResult<GetEgressActionsForInterfaceOutput>> getEgressActionsForInterface(
            GetEgressActionsForInterfaceInput input) {
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getEgressActionsForInterface(input))
                .onFailureLogLevel(DEBUG).build();
    }

    @Override
    public Future<RpcResult<GetDpidFromInterfaceOutput>> getDpidFromInterface(GetDpidFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        return fromListenableFuture(LOG, input, () -> interfaceManagerService.getDpidFromInterface(input))
                .withRpcErrorMessage(e -> getDpidFromInterfaceErrorMessage(interfaceName, e.getMessage()))
                .onFailureLogLevel(NONE) // do not LOG error in this case!
                .onFailure(cause -> {
                    if (!(cause instanceof IllegalArgumentException)) {
                        LOG.error("RPC getDpidFromInterface() failed; input = {}", input, cause);
                    }
                }).build();
    }

    private String getDpidFromInterfaceErrorMessage(final String interfaceName, final String dueTo) {
        return String.format("Retrieval of datapath id for the key {%s} failed due to %s",
                interfaceName, dueTo);
    }

}
