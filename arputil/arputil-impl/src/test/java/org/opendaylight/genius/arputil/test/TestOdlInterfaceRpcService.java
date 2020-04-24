/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import static org.opendaylight.genius.arputil.test.ArpUtilTestUtil.DPN_ID;
import static org.opendaylight.genius.arputil.test.ArpUtilTestUtil.INTERFACE_NAME;
import static org.opendaylight.genius.arputil.test.ArpUtilTestUtil.PORT_NUMBER;
import static org.opendaylight.genius.arputil.test.ArpUtilTestUtil.URI;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.mockito.Mockito;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public abstract class TestOdlInterfaceRpcService implements OdlInterfaceRpcService {

    public static TestOdlInterfaceRpcService newInstance() {
        return Mockito.mock(TestOdlInterfaceRpcService.class, realOrException());
    }

    @Override
    public ListenableFuture<RpcResult<GetPortFromInterfaceOutput>> getPortFromInterface(
            GetPortFromInterfaceInput input) {
        RpcResultBuilder<GetPortFromInterfaceOutput> rpcResultBuilder;
        GetPortFromInterfaceOutputBuilder output = new GetPortFromInterfaceOutputBuilder().setDpid(DPN_ID)
                .setPortname(INTERFACE_NAME).setPortno(PORT_NUMBER).setPhyAddress("1F:1F:1F:1F:1F:1F");
        rpcResultBuilder = RpcResultBuilder.success();
        rpcResultBuilder.withResult(output.build());
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<GetEgressActionsForInterfaceOutput>>
        getEgressActionsForInterface(GetEgressActionsForInterfaceInput input) {

        RpcResultBuilder<GetEgressActionsForInterfaceOutput> rpcResultBuilder;
        List<ActionInfo> listActionInfo = new ArrayList<>();
        List<Action> actionsList = new ArrayList<>();
        listActionInfo.add(new ActionOutput(1, new Uri(URI)));
        for (ActionInfo actionInfo : listActionInfo) {
            actionsList.add(actionInfo.buildAction());
        }
        GetEgressActionsForInterfaceOutputBuilder output = new GetEgressActionsForInterfaceOutputBuilder()
                .setAction(actionsList);
        rpcResultBuilder = RpcResultBuilder.success();
        rpcResultBuilder.withResult(output.build());
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<GetInterfaceFromIfIndexOutput>>
        getInterfaceFromIfIndex(GetInterfaceFromIfIndexInput input) {
        RpcResultBuilder<GetInterfaceFromIfIndexOutput> rpcResultBuilder;
        GetInterfaceFromIfIndexOutputBuilder output = new GetInterfaceFromIfIndexOutputBuilder()
                .setInterfaceName(ArpUtilTestUtil.INTERFACE_NAME);
        rpcResultBuilder = RpcResultBuilder.success();
        rpcResultBuilder.withResult(output.build());
        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}
