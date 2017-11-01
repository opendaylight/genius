/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.interfaces;

import com.google.common.util.concurrent.ListenableFuture;
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

/**
 * {@link OdlInterfaceRpcService} as direct local OSGi service instead of RPC.
 * This interface has the same methods as that, just with a Future of the
 * respective *Output without the intermediary RpcResult.
 */
public interface InterfaceManagerService {

    ListenableFuture<GetEgressInstructionsForInterfaceOutput> getEgressInstructionsForInterface(
            GetEgressInstructionsForInterfaceInput input);

    ListenableFuture<GetInterfaceTypeOutput> getInterfaceType(GetInterfaceTypeInput input);

    ListenableFuture<GetPortFromInterfaceOutput> getPortFromInterface(GetPortFromInterfaceInput input);

    ListenableFuture<GetEgressActionsForInterfaceOutput> getEgressActionsForInterface(
            GetEgressActionsForInterfaceInput input);

    ListenableFuture<GetTunnelTypeOutput> getTunnelType(GetTunnelTypeInput input);

    ListenableFuture<GetNodeconnectorIdFromInterfaceOutput> getNodeconnectorIdFromInterface(
            GetNodeconnectorIdFromInterfaceInput input);

    ListenableFuture<GetEndpointIpForDpnOutput> getEndpointIpForDpn(GetEndpointIpForDpnInput input);

    ListenableFuture<GetInterfaceFromIfIndexOutput> getInterfaceFromIfIndex(GetInterfaceFromIfIndexInput input);

    ListenableFuture<GetDpnInterfaceListOutput> getDpnInterfaceList(GetDpnInterfaceListInput input);

    ListenableFuture<GetDpidFromInterfaceOutput> getDpidFromInterface(GetDpidFromInterfaceInput input);

}
