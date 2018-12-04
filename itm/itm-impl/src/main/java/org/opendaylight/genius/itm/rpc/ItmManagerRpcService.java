/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.rpc;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.serviceutils.tools.mdsal.rpc.FutureRpcResults.LogLevel.ERROR;
import static org.opendaylight.serviceutils.tools.mdsal.rpc.FutureRpcResults.fromListenableFuture;
import static org.opendaylight.yangtools.yang.common.RpcResultBuilder.failed;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.interfaces.InterfaceManagerService;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.serviceutils.tools.mdsal.rpc.FutureRpcResults;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.DeviceVtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetDpnInfoOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetEgressActionsForTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetEgressActionsForTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetEgressActionsForTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetWatchPortForTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetWatchPortForTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.SetBfdParamOnTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.SetBfdParamOnTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.SetBfdParamOnTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.get.dpn.info.output.Computes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.get.dpn.info.output.ComputesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmManagerRpcService implements ItmRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(ItmManagerRpcService.class);

    private final DataBroker dataBroker;
    private final IMdsalApiManager mdsalManager;
    private final DPNTEPsInfoCache dpnTEPsInfoCache;
    private final ItmExternalTunnelAddWorker externalTunnelAddWorker;
    private final SingleTransactionDataBroker singleTransactionDataBroker;
    private final IInterfaceManager interfaceManager;
    private final InterfaceManagerService interfaceManagerService;
    private final DpnTepStateCache dpnTepStateCache;
    private final TunnelStateCache tunnelStateCache;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private final DirectTunnelUtils directTunnelUtils;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public ItmManagerRpcService(final DataBroker dataBroker, final IMdsalApiManager mdsalManager,
                                final ItmConfig itmConfig, final DPNTEPsInfoCache dpnTEPsInfoCache,
                                final IInterfaceManager interfaceManager, final DpnTepStateCache dpnTepStateCache,
                                final TunnelStateCache tunnelStateCache,
                                final InterfaceManagerService interfaceManagerService,
                                final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                final DirectTunnelUtils directTunnelUtils) {
        this.dataBroker = dataBroker;
        this.mdsalManager = mdsalManager;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        this.externalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig, dpnTEPsInfoCache);
        this.singleTransactionDataBroker = new SingleTransactionDataBroker(dataBroker);
        this.interfaceManager = interfaceManager;
        this.interfaceManagerService = interfaceManagerService;
        this.dpnTepStateCache = dpnTepStateCache;
        this.tunnelStateCache = tunnelStateCache;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.directTunnelUtils = directTunnelUtils;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @PostConstruct
    public void start() {
        LOG.info("ItmManagerRpcService Started");
    }

    @PreDestroy
    public void close() {
        LOG.info("ItmManagerRpcService Closed");
    }

    @Override
    public ListenableFuture<RpcResult<GetTunnelInterfaceNameOutput>> getTunnelInterfaceName(
            GetTunnelInterfaceNameInput input) {
        RpcResultBuilder<GetTunnelInterfaceNameOutput> resultBld = null;
        BigInteger sourceDpn = input.getSourceDpid();
        BigInteger destinationDpn = input.getDestinationDpid();
        Optional<InternalTunnel> optTunnel = Optional.absent();

        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            DpnTepInterfaceInfo interfaceInfo = dpnTepStateCache.getDpnTepInterface(sourceDpn, destinationDpn);
            if (interfaceInfo != null) {
                resultBld = RpcResultBuilder.success();
                resultBld.withResult(new GetTunnelInterfaceNameOutputBuilder()
                        .setInterfaceName(interfaceInfo.getTunnelName())).build();
                return Futures.immediateFuture(resultBld.build());
            }
        }
        if (ItmUtils.isTunnelAggregationUsed(input.getTunnelType())) {
            optTunnel = ItmUtils.getInternalTunnelFromDS(sourceDpn, destinationDpn,
                                                         TunnelTypeLogicalGroup.class, dataBroker);
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: getTunnelInterfaceName {}", optTunnel);
        }
        if (!optTunnel.isPresent()) {
            optTunnel = ItmUtils.getInternalTunnelFromDS(sourceDpn, destinationDpn, input.getTunnelType(), dataBroker);
        }
        if (optTunnel.isPresent()) {
            InternalTunnel tunnel = optTunnel.get();
            GetTunnelInterfaceNameOutputBuilder output = new GetTunnelInterfaceNameOutputBuilder() ;
            List<String> tunnelInterfaces = tunnel.getTunnelInterfaceNames();
            if (tunnelInterfaces != null && !tunnelInterfaces.isEmpty()) {
                output.setInterfaceName(tunnelInterfaces.get(0));
                resultBld = RpcResultBuilder.success();
                resultBld.withResult(output.build());
            } else {
                resultBld = failed();
            }

        } else {
            resultBld = failed();
        }
        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public ListenableFuture<RpcResult<GetEgressActionsForTunnelOutput>>
        getEgressActionsForTunnel(GetEgressActionsForTunnelInput input) {
        String tunnelName = input.getIntfName();
        if (tunnelName == null) {
            return Futures.immediateFuture(RpcResultBuilder.<GetEgressActionsForTunnelOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION,
                    "tunnel name not set for GetEgressActionsForTunnel call").build());
        }
        if (!dpnTepStateCache.isInternal(tunnelName) || !interfaceManager.isItmDirectTunnelsEnabled()) {
            // Re-direct the RPC to Interface Manager
            // From the rpc input and get the output and copy to output
            org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406
                    .GetEgressActionsForInterfaceInputBuilder inputIfmBuilder =
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406
                            .GetEgressActionsForInterfaceInputBuilder().setIntfName(input.getIntfName())
                            .setTunnelKey(input.getTunnelKey()).setActionKey(input.getActionKey());
            SettableFuture<RpcResult<GetEgressActionsForTunnelOutput>> settableFuture = SettableFuture.create();
            Futures.addCallback(interfaceManagerService.getEgressActionsForInterface(inputIfmBuilder.build()),
                    new FutureCallback<org.opendaylight.yang.gen.v1.urn.opendaylight.genius
                            .interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput>() {
                        @Override
                        public void onSuccess(@Nonnull org.opendaylight.yang.gen.v1.urn.opendaylight.genius
                                .interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput result) {
                            GetEgressActionsForTunnelOutputBuilder output =
                                    new GetEgressActionsForTunnelOutputBuilder().setAction(result.getAction());
                            settableFuture.set(RpcResultBuilder.<GetEgressActionsForTunnelOutput>success()
                                    .withResult(output.build()).build());
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            LOG.debug("RPC Call to Get egress actions failed for interface {}", tunnelName);
                            String errMsg = String.format("RPC call to get egress actions failed for interface %s",
                                    tunnelName);
                            settableFuture.set(RpcResultBuilder.<GetEgressActionsForTunnelOutput>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, errMsg, throwable).build());
                        }
                });
            return  settableFuture;
        } else {
            return fromListenableFuture(LOG, input, () -> getEgressActionsForInterface(input.getIntfName(),
                    input.getTunnelKey(), input.getActionKey())).onFailureLogLevel(ERROR).build();
        }
    }

    @Override
    public ListenableFuture<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        String tunnelName = input.getIntfName();
        if (tunnelName == null) {
            return Futures.immediateFuture(RpcResultBuilder.<GetTunnelTypeOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION,
                    "tunnel name not set for getTunnelType call").build());
        }

        if (!dpnTepStateCache.isInternal(tunnelName) || !interfaceManager.isItmDirectTunnelsEnabled()) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406
                    .GetTunnelTypeInputBuilder inputBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight
                    .genius.interfacemanager.rpcs.rev160406.GetTunnelTypeInputBuilder()
                    .setIntfName(input.getIntfName());
            SettableFuture<RpcResult<GetTunnelTypeOutput>> settableFuture = SettableFuture.create();
            Futures.addCallback(interfaceManagerService.getTunnelType(inputBuilder.build()),
                    new FutureCallback<org.opendaylight.yang.gen.v1.urn.opendaylight.genius
                            .interfacemanager.rpcs.rev160406.GetTunnelTypeOutput>() {
                        public void onSuccess(@Nonnull org.opendaylight.yang.gen.v1.urn.opendaylight.genius
                                .interfacemanager.rpcs.rev160406.GetTunnelTypeOutput result) {
                            GetTunnelTypeOutputBuilder output = new GetTunnelTypeOutputBuilder()
                                    .setTunnelType(result.getTunnelType());
                            settableFuture.set(RpcResultBuilder.<GetTunnelTypeOutput>success()
                                    .withResult(output.build()).build());
                        }

                        public void onFailure(Throwable throwable) {
                            LOG.debug("RPC Call to Get tunnel type failed for interface {}", tunnelName);
                            String errMsg = String.format("RPC to Get tunnel type failed for interface %s",
                                    tunnelName);
                            settableFuture.set(RpcResultBuilder.<GetTunnelTypeOutput>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, errMsg, throwable).build());
                        }
                });
            return settableFuture;
        } else {
            LOG.debug("get tunnel type from ITM for interface name {}", input.getIntfName());
            return FutureRpcResults.fromBuilder(LOG, input, () -> {
                DpnTepInterfaceInfo ifInfo = dpnTepStateCache.getTunnelFromCache(input.getIntfName());
                return new GetTunnelTypeOutputBuilder().setTunnelType(ifInfo.getTunnelType());
            }).build();
        }
    }

    @Override
    public ListenableFuture<RpcResult<SetBfdParamOnTunnelOutput>> setBfdParamOnTunnel(
            SetBfdParamOnTunnelInput input) {
        BigInteger srcDpnId = new BigInteger(input.getSourceNode());
        BigInteger destDpnId = new BigInteger(input.getDestinationNode());
        DpnTepInterfaceInfo dpnTepInterfaceInfo = dpnTepStateCache.getDpnTepInterface(srcDpnId, destDpnId);
        return fromListenableFuture(LOG, input,
            () -> Futures.transform(txRunner.callWithNewWriteOnlyTransactionAndSubmit(tx -> {
                java.util.Objects.requireNonNull(dpnTepInterfaceInfo, "dpnTepInterfaceInfo");
                RemoteDpnsBuilder remoteDpnsBuilder = new RemoteDpnsBuilder();
                remoteDpnsBuilder.withKey(new RemoteDpnsKey(destDpnId));
                remoteDpnsBuilder.setDestinationDpnId(destDpnId);
                remoteDpnsBuilder.setTunnelName(dpnTepInterfaceInfo.getTunnelName());
                remoteDpnsBuilder.setInternal(dpnTepInterfaceInfo.isInternal());
                if (input.isMonitoringEnabled() && input.getMonitoringInterval() != null) {
                    remoteDpnsBuilder.setMonitoringInterval(input.getMonitoringInterval());
                }
                remoteDpnsBuilder.setMonitoringEnabled(input.isMonitoringEnabled());
                RemoteDpns remoteDpn = remoteDpnsBuilder.build();
                Optional<OvsBridgeRefEntry> ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(srcDpnId);
                directTunnelUtils.updateBfdConfiguration(srcDpnId, remoteDpn, ovsBridgeRefEntry);
                InstanceIdentifier<RemoteDpns> iid = InstanceIdentifier.builder(DpnTepsState.class)
                    .child(DpnsTeps.class, new DpnsTepsKey(srcDpnId))
                    .child(RemoteDpns.class,
                        new RemoteDpnsKey(destDpnId)).build();
                tx.merge(LogicalDatastoreType.CONFIGURATION, iid, remoteDpn);
            }), unused -> new SetBfdParamOnTunnelOutputBuilder().build(), MoreExecutors.directExecutor())).build();
    }

    @Override
    public ListenableFuture<RpcResult<RemoveExternalTunnelEndpointOutput>> removeExternalTunnelEndpoint(
            RemoveExternalTunnelEndpointInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<RemoveExternalTunnelEndpointOutput>> result = SettableFuture.create();
        Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        ItmExternalTunnelDeleteWorker.deleteTunnels(dataBroker, meshedDpnList,
                input.getDestinationIp(), input.getTunnelType());
        InstanceIdentifier<DcGatewayIp> extPath = InstanceIdentifier.builder(DcGatewayIpList.class)
                .child(DcGatewayIp.class, new DcGatewayIpKey(input.getDestinationIp())).build();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, extPath);
        ListenableFuture<Void> futureCheck = transaction.submit();
        Futures.addCallback(futureCheck, new FutureCallback<Void>() {

            @Override public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<RemoveExternalTunnelEndpointOutput>success().build());
            }

            @Override public void onFailure(Throwable error) {
                String msg = "Unable to delete DcGatewayIp " + input.getDestinationIp()
                        + " in datastore and tunnel type " + input.getTunnelType();
                LOG.error("Unable to delete DcGatewayIp {} in datastore and tunnel type {}", input.getDestinationIp(),
                        input.getTunnelType());
                result.set(RpcResultBuilder.<RemoveExternalTunnelEndpointOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        });
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<RemoveExternalTunnelFromDpnsOutput>> removeExternalTunnelFromDpns(
            RemoveExternalTunnelFromDpnsInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<RemoveExternalTunnelFromDpnsOutput>> result = SettableFuture.create();
        List<DPNTEPsInfo> cfgDpnList = ItmUtils.getDpnTepListFromDpnId(dpnTEPsInfoCache, input.getDpnId()) ;
        ItmExternalTunnelDeleteWorker.deleteTunnels(dataBroker, cfgDpnList,
                input.getDestinationIp(), input.getTunnelType());
        result.set(RpcResultBuilder.<RemoveExternalTunnelFromDpnsOutput>success().build());
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<BuildExternalTunnelFromDpnsOutput>> buildExternalTunnelFromDpns(
            BuildExternalTunnelFromDpnsInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<BuildExternalTunnelFromDpnsOutput>> result = SettableFuture.create();
        List<ListenableFuture<Void>> extTunnelResultList = externalTunnelAddWorker
            .buildTunnelsFromDpnToExternalEndPoint(input.getDpnId(), input.getDestinationIp(),input.getTunnelType());
        for (ListenableFuture<Void> extTunnelResult : extTunnelResultList) {
            Futures.addCallback(extTunnelResult, new FutureCallback<Void>() {

                @Override
                public void onSuccess(Void voidInstance) {
                    result.set(RpcResultBuilder.<BuildExternalTunnelFromDpnsOutput>success().build());
                }

                @Override
                public void onFailure(Throwable error) {
                    String msg = "Unable to create ext tunnel";
                    LOG.error("create ext tunnel failed. {}.", msg, error);
                    result.set(RpcResultBuilder.<BuildExternalTunnelFromDpnsOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                }
            });
        }
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<AddExternalTunnelEndpointOutput>> addExternalTunnelEndpoint(
            AddExternalTunnelEndpointInput input) {
        // TODO Auto-generated method stub

        //Ignore the Futures for now
        final SettableFuture<RpcResult<AddExternalTunnelEndpointOutput>> result = SettableFuture.create();
        Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        externalTunnelAddWorker.buildTunnelsToExternalEndPoint(meshedDpnList,
                input.getDestinationIp(), input.getTunnelType());
        InstanceIdentifier<DcGatewayIp> extPath = InstanceIdentifier.builder(DcGatewayIpList.class)
                .child(DcGatewayIp.class, new DcGatewayIpKey(input.getDestinationIp())).build();
        DcGatewayIp dcGatewayIp =
                new DcGatewayIpBuilder().setIpAddress(input.getDestinationIp())
                        .setTunnnelType(input.getTunnelType()).build();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, extPath,dcGatewayIp, true);
        ListenableFuture<Void> futureCheck = writeTransaction.submit();
        Futures.addCallback(futureCheck, new FutureCallback<Void>() {

            @Override public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<AddExternalTunnelEndpointOutput>success().build());
            }

            @Override public void onFailure(Throwable error) {
                String msg =
                        "Unable to create DcGatewayIp {} in datastore for ip " + input.getDestinationIp() + "and "
                                + "tunnel type " + input.getTunnelType();

                LOG.error("Unable to create DcGatewayIp in datastore for ip {} and tunnel type {}",
                        input.getDestinationIp() , input.getTunnelType());
                result.set(RpcResultBuilder.<AddExternalTunnelEndpointOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        });
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<GetExternalTunnelInterfaceNameOutput>> getExternalTunnelInterfaceName(
            GetExternalTunnelInterfaceNameInput input) {
        SettableFuture.create();
        RpcResultBuilder<GetExternalTunnelInterfaceNameOutput> resultBld;
        String sourceNode = input.getSourceNode();
        String dstNode = input.getDestinationNode();
        ExternalTunnelKey externalTunnelKey = new ExternalTunnelKey(dstNode, sourceNode, input.getTunnelType());
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(
                ExternalTunnelList.class)
                .child(ExternalTunnel.class, externalTunnelKey);
        ExternalTunnel exTunnel =
                ItmUtils.getExternalTunnelbyExternalTunnelKey(externalTunnelKey, path, this.dataBroker);
        if (exTunnel != null) {
            GetExternalTunnelInterfaceNameOutputBuilder output = new GetExternalTunnelInterfaceNameOutputBuilder();
            output.setInterfaceName(exTunnel.getTunnelInterfaceName());
            resultBld = RpcResultBuilder.success();
            resultBld.withResult(output.build());
        } else {
            resultBld = failed();
        }

        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public ListenableFuture<RpcResult<CreateTerminatingServiceActionsOutput>>
        createTerminatingServiceActions(final CreateTerminatingServiceActionsInput input) {
        LOG.info("create terminatingServiceAction on DpnId = {} for service id {} and instructions {}",
                input.getDpnId() , input.getServiceId(), input.getInstruction());
        final SettableFuture<RpcResult<CreateTerminatingServiceActionsOutput>> result = SettableFuture.create();
        int serviceId = input.getServiceId() ;
        final List<MatchInfo> mkMatches = getTunnelMatchesForServiceId(serviceId);

        Flow terminatingServiceTableFlow = MDSALUtil.buildFlowNew(NwConstants.INTERNAL_TUNNEL_TABLE,
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE,serviceId), 5,
                String.format("%s:%d","ITM Flow Entry ",serviceId), 0, 0,
                ITMConstants.COOKIE_ITM.add(BigInteger.valueOf(serviceId)),mkMatches, input.getInstruction());

        ListenableFuture<Void> installFlowResult = txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> mdsalManager.addFlow(tx, input.getDpnId(), terminatingServiceTableFlow));
        Futures.addCallback(installFlowResult, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<CreateTerminatingServiceActionsOutput>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Unable to install terminating service flow for %s", input.getDpnId());
                LOG.error("create terminating service actions failed. {}", msg, error);
                result.set(RpcResultBuilder.<CreateTerminatingServiceActionsOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error)
                        .build());
            }
        }, MoreExecutors.directExecutor());
        // result.set(RpcResultBuilder.<Void>success().build());
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<RemoveTerminatingServiceActionsOutput>>
        removeTerminatingServiceActions(final RemoveTerminatingServiceActionsInput input) {
        LOG.info("remove terminatingServiceActions called with DpnId = {} and serviceId = {}",
                input.getDpnId(), input.getServiceId());
        final SettableFuture<RpcResult<RemoveTerminatingServiceActionsOutput>> result = SettableFuture.create();

        ListenableFuture<Void> removeFlowResult = txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
            tx -> mdsalManager.removeFlow(tx, input.getDpnId(),
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE, input.getServiceId()),
                NwConstants.INTERNAL_TUNNEL_TABLE));
        Futures.addCallback(removeFlowResult, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<RemoveTerminatingServiceActionsOutput>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Unable to remove terminating service flow for %s", input.getDpnId());
                LOG.error("remove terminating service actions failed. {}", msg, error);
                result.set(RpcResultBuilder.<RemoveTerminatingServiceActionsOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error)
                        .build());
            }
        }, MoreExecutors.directExecutor());
        //result.set(RpcResultBuilder.<Void>success().build());

        return result ;
    }


    public List<MatchInfo> getTunnelMatchesForServiceId(int serviceId) {
        final List<MatchInfo> mkMatches = new ArrayList<>();

        // Matching metadata
        mkMatches.add(new MatchTunnelId(BigInteger.valueOf(serviceId)));

        return mkMatches;
    }

    private String getFlowRef(long termSvcTable, int svcId) {
        return String.valueOf(termSvcTable) + svcId;
    }

    @Override
    public ListenableFuture<RpcResult<GetInternalOrExternalInterfaceNameOutput>> getInternalOrExternalInterfaceName(
            GetInternalOrExternalInterfaceNameInput input) {
        RpcResultBuilder<GetInternalOrExternalInterfaceNameOutput> resultBld = failed();
        BigInteger srcDpn = input.getSourceDpid() ;
        IpAddress dstIp = input.getDestinationIp() ;
        InstanceIdentifier<ExternalTunnel> path1 = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class,
                        new ExternalTunnelKey(dstIp.stringValue(), srcDpn.toString(), input.getTunnelType()));

        Optional<ExternalTunnel> optExtTunnel = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path1, dataBroker);

        if (optExtTunnel != null && optExtTunnel.isPresent()) {
            ExternalTunnel extTunnel = optExtTunnel.get();
            GetInternalOrExternalInterfaceNameOutputBuilder output =
                    new GetInternalOrExternalInterfaceNameOutputBuilder()
                            .setInterfaceName(extTunnel.getTunnelInterfaceName());
            resultBld = RpcResultBuilder.success();
            resultBld.withResult(output.build()) ;
        } else {
            Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();

            // Look for external tunnels if not look for internal tunnel
            for (DPNTEPsInfo teps : meshedDpnList) {
                TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
                if (dstIp.equals(firstEndPt.getIpAddress())) {
                    Optional<InternalTunnel> optTunnel = Optional.absent();
                    if (interfaceManager.isItmDirectTunnelsEnabled()) {
                        DpnTepInterfaceInfo interfaceInfo =
                                dpnTepStateCache.getDpnTepInterface(srcDpn, teps.getDPNID());
                        if (interfaceInfo != null) {
                            resultBld = RpcResultBuilder.success();
                            resultBld.withResult(new GetInternalOrExternalInterfaceNameOutputBuilder()
                                    .setInterfaceName(interfaceInfo.getTunnelName())).build();
                            return Futures.immediateFuture(resultBld.build());
                        }
                    }

                    if (ItmUtils.isTunnelAggregationUsed(input.getTunnelType())) {
                        optTunnel = ItmUtils.getInternalTunnelFromDS(srcDpn, teps.getDPNID(),
                                                                     TunnelTypeLogicalGroup.class, dataBroker);
                        LOG.debug("MULTIPLE_VxLAN_TUNNELS: getInternalOrExternalInterfaceName {}", optTunnel);
                    }
                    if (!optTunnel.isPresent()) {
                        optTunnel = ItmUtils.getInternalTunnelFromDS(srcDpn, teps.getDPNID(),
                                                                     input.getTunnelType(), dataBroker);
                    }
                    if (optTunnel.isPresent()) {
                        InternalTunnel tunnel = optTunnel.get();
                        List<String> tunnelInterfaces = tunnel.getTunnelInterfaceNames();
                        if (tunnelInterfaces != null && !tunnelInterfaces.isEmpty()) {
                            GetInternalOrExternalInterfaceNameOutputBuilder
                                    output =
                                    new GetInternalOrExternalInterfaceNameOutputBuilder()
                                            .setInterfaceName(tunnelInterfaces.get(0));
                            resultBld = RpcResultBuilder.success();
                            resultBld.withResult(output.build());
                        } else {
                            LOG.error("No tunnel interface found between source DPN {} ans destination IP {}", srcDpn,
                                    dstIp);
                        }
                        break;
                    } else {
                        LOG.error("Tunnel not found for source DPN {} ans destination IP {}", srcDpn, dstIp);
                    }
                }
            }
        }
        return Futures.immediateFuture(resultBld.build());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public ListenableFuture<RpcResult<DeleteL2GwDeviceOutput>> deleteL2GwDevice(DeleteL2GwDeviceInput input) {
        final SettableFuture<RpcResult<DeleteL2GwDeviceOutput>> result = SettableFuture.create();
        boolean foundVxlanTzone = false;
        try {
            final IpAddress hwIp = input.getIpAddress();
            final String nodeId = input.getNodeId();
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                    containerPath, dataBroker);
            if (transportZonesOptional.isPresent()) {
                TransportZones transportZones = transportZonesOptional.get();
                if (transportZones.getTransportZone() == null || transportZones.getTransportZone().isEmpty()) {
                    LOG.error("No teps configured");
                    result.set(RpcResultBuilder.<DeleteL2GwDeviceOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                for (TransportZone tzone : transportZones.getTransportZone()) {
                    if (!TunnelTypeVxlan.class.equals(tzone.getTunnelType())) {
                        continue;
                    }
                    foundVxlanTzone = true;
                    String transportZone = tzone.getZoneName();
                    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId);
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                    //TO DO: add retry if it fails

                    transaction.delete(LogicalDatastoreType.CONFIGURATION, path);

                    ListenableFuture<Void> futureCheck = transaction.submit();
                    Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                        @Override public void onSuccess(Void voidInstance) {
                            result.set(RpcResultBuilder.<DeleteL2GwDeviceOutput>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to delete HwVtep %s from datastore", nodeId);
                            LOG.error("Unable to delete HwVtep {}, {} from datastore", nodeId, hwIp);
                            result.set(RpcResultBuilder.<DeleteL2GwDeviceOutput>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    });

                }
            } else {
                result.set(RpcResultBuilder.<DeleteL2GwDeviceOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No TransportZones configured").build());
                return result;
            }

            if (!foundVxlanTzone) {
                result.set(RpcResultBuilder.<DeleteL2GwDeviceOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No VxLan TransportZones configured")
                        .build());
            }

            return result;
        } catch (Exception e) {
            RpcResultBuilder<DeleteL2GwDeviceOutput> resultBuilder = RpcResultBuilder.<DeleteL2GwDeviceOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Deleting l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public ListenableFuture<RpcResult<AddL2GwDeviceOutput>> addL2GwDevice(AddL2GwDeviceInput input) {

        final SettableFuture<RpcResult<AddL2GwDeviceOutput>> result = SettableFuture.create();
        boolean foundVxlanTzone = false;
        try {
            final IpAddress hwIp = input.getIpAddress();
            final String nodeId = input.getNodeId();
            //iterate through all transport zones and put TORs under vxlan
            //if no vxlan tzone is cnfigured, return an error.
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                    containerPath, dataBroker);
            if (transportZonesOptional.isPresent()) {
                TransportZones transportZones = transportZonesOptional.get();
                if (transportZones.getTransportZone() == null || transportZones.getTransportZone().isEmpty()) {
                    LOG.error("No transportZone configured");
                    result.set(RpcResultBuilder.<AddL2GwDeviceOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No transportZone Configured").build());
                    return result;
                }
                for (TransportZone tzone : transportZones.getTransportZone()) {
                    if (!TunnelTypeVxlan.class.equals(tzone.getTunnelType())) {
                        continue;
                    }
                    String transportZone = tzone.getZoneName();
                    foundVxlanTzone = true;
                    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId);
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    DeviceVteps deviceVtep = new DeviceVtepsBuilder().withKey(deviceVtepKey).setIpAddress(hwIp)
                            .setNodeId(nodeId).setTopologyId(input.getTopologyId()).build();
                    WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                    //TO DO: add retry if it fails
                    transaction.put(LogicalDatastoreType.CONFIGURATION, path, deviceVtep, true);

                    ListenableFuture<Void> futureCheck = transaction.submit();
                    Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                        @Override public void onSuccess(Void voidInstance) {
                            result.set(RpcResultBuilder.<AddL2GwDeviceOutput>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to write HwVtep %s to datastore", nodeId);
                            LOG.error("Unable to write HwVtep {}, {} to datastore", nodeId, hwIp);
                            result.set(RpcResultBuilder.<AddL2GwDeviceOutput>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    });

                }
            } else {
                result.set(RpcResultBuilder.<AddL2GwDeviceOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No TransportZones configured").build());
                return result;
            }

            if (!foundVxlanTzone) {
                result.set(RpcResultBuilder.<AddL2GwDeviceOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No VxLan TransportZones configured")
                        .build());
            }

            return result;
        } catch (Exception e) {
            RpcResultBuilder<AddL2GwDeviceOutput> resultBuilder = RpcResultBuilder.<AddL2GwDeviceOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Adding l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @Override
    public ListenableFuture<RpcResult<GetWatchPortForTunnelOutput>> getWatchPortForTunnel(
            GetWatchPortForTunnelInput input) {
        throw new UnsupportedOperationException("TODO");
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public ListenableFuture<RpcResult<AddL2GwMlagDeviceOutput>> addL2GwMlagDevice(AddL2GwMlagDeviceInput input) {

        final SettableFuture<RpcResult<AddL2GwMlagDeviceOutput>> result = SettableFuture.create();
        try {
            final IpAddress hwIp = input.getIpAddress();
            final List<String> nodeId = input.getNodeId();
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                    containerPath, dataBroker);
            if (transportZonesOptional.isPresent()) {
                TransportZones transportZones = transportZonesOptional.get();
                if (transportZones.getTransportZone() == null || transportZones.getTransportZone().isEmpty()) {
                    LOG.error("No teps configured");
                    result.set(RpcResultBuilder.<AddL2GwMlagDeviceOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                String transportZone = transportZones.getTransportZone().get(0).getZoneName();
                DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId.get(0));
                InstanceIdentifier<DeviceVteps> path =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(DeviceVteps.class, deviceVtepKey).build();
                DeviceVteps deviceVtep = new DeviceVtepsBuilder().withKey(deviceVtepKey).setIpAddress(hwIp)
                        .setNodeId(nodeId.get(0)).setTopologyId(input.getTopologyId()).build();
                WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
                //TO DO: add retry if it fails
                LOG.trace("writing hWvtep{}",deviceVtep);
                writeTransaction.put(LogicalDatastoreType.CONFIGURATION, path, deviceVtep, true);

                if (nodeId.size() == 2) {
                    LOG.trace("second node-id {}",nodeId.get(1));
                    DeviceVtepsKey deviceVtepKey2 = new DeviceVtepsKey(hwIp, nodeId.get(1));
                    InstanceIdentifier<DeviceVteps> path2 = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(DeviceVteps.class, deviceVtepKey2).build();
                    DeviceVteps deviceVtep2 = new DeviceVtepsBuilder().withKey(deviceVtepKey2).setIpAddress(hwIp)
                            .setNodeId(nodeId.get(1))
                            .setTopologyId(input.getTopologyId()).build();
                    //TO DO: add retry if it fails
                    LOG.trace("writing {}",deviceVtep2);
                    writeTransaction.put(LogicalDatastoreType.CONFIGURATION, path2, deviceVtep2, true);
                }
                ListenableFuture<Void> futureCheck = writeTransaction.submit();
                Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(Void voidInstance) {
                        result.set(RpcResultBuilder.<AddL2GwMlagDeviceOutput>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to write HwVtep %s to datastore", nodeId);
                        LOG.error("Unable to write HwVtep {}, {} to datastore", nodeId , hwIp);
                        result.set(RpcResultBuilder.<AddL2GwMlagDeviceOutput>failed()
                                .withError(RpcError.ErrorType.APPLICATION, msg, error)
                                .build());
                    }
                }, MoreExecutors.directExecutor());
            }
            return result;
        } catch (RuntimeException e) {
            RpcResultBuilder<AddL2GwMlagDeviceOutput> resultBuilder = RpcResultBuilder.<AddL2GwMlagDeviceOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Adding l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public ListenableFuture<RpcResult<DeleteL2GwMlagDeviceOutput>> deleteL2GwMlagDevice(
            DeleteL2GwMlagDeviceInput input) {
        final SettableFuture<RpcResult<DeleteL2GwMlagDeviceOutput>> result = SettableFuture.create();
        try {
            final IpAddress hwIp = input.getIpAddress();
            final List<String> nodeId = input.getNodeId();
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> transportZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                    containerPath, dataBroker);
            if (transportZonesOptional.isPresent()) {
                TransportZones tzones = transportZonesOptional.get();
                if (tzones.getTransportZone() == null || tzones.getTransportZone().isEmpty()) {
                    LOG.error("No teps configured");
                    result.set(RpcResultBuilder.<DeleteL2GwMlagDeviceOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                String transportZone = tzones.getTransportZone().get(0).getZoneName();
                DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId.get(0));
                InstanceIdentifier<DeviceVteps> path =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(DeviceVteps.class,
                                deviceVtepKey).build();
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                //TO DO: add retry if it fails
                transaction.delete(LogicalDatastoreType.CONFIGURATION, path);

                DeviceVtepsKey deviceVtepKey2 = new DeviceVtepsKey(hwIp, nodeId.get(1));
                InstanceIdentifier<DeviceVteps> path2 =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(DeviceVteps.class,
                                deviceVtepKey2).build();
                //TO DO: add retry if it fails
                transaction.delete(LogicalDatastoreType.CONFIGURATION, path2);

                ListenableFuture<Void> futureCheck = transaction.submit();
                Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(Void voidInstance) {
                        result.set(RpcResultBuilder.<DeleteL2GwMlagDeviceOutput>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to write HwVtep %s to datastore", nodeId);
                        LOG.error("Unable to write HwVtep {}, {} to datastore", nodeId , hwIp);
                        result.set(RpcResultBuilder.<DeleteL2GwMlagDeviceOutput>failed()
                                .withError(RpcError.ErrorType.APPLICATION, msg, error)
                                .build());
                    }
                });
            }
            return result;
        } catch (Exception e) {
            RpcResultBuilder<DeleteL2GwMlagDeviceOutput> resultBuilder =
                    RpcResultBuilder.<DeleteL2GwMlagDeviceOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                            "Deleting l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @Override
    public ListenableFuture<RpcResult<IsTunnelInternalOrExternalOutput>> isTunnelInternalOrExternal(
            IsTunnelInternalOrExternalInput input) {
        RpcResultBuilder<IsTunnelInternalOrExternalOutput> resultBld;
        String tunIfName = input.getTunnelInterfaceName();
        long tunVal = 0;
        IsTunnelInternalOrExternalOutputBuilder output = new IsTunnelInternalOrExternalOutputBuilder()
                        .setTunnelType(tunVal);

        if (ItmUtils.ITM_CACHE.getInternalTunnel(tunIfName) != null) {
            tunVal = 1;
        } else if (ItmUtils.ITM_CACHE.getExternalTunnel(tunIfName) != null) {
            tunVal = 2;
        }
        output.setTunnelType(tunVal);
        resultBld = RpcResultBuilder.success();
        resultBld.withResult(output.build());
        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public ListenableFuture<RpcResult<IsDcgwPresentOutput>> isDcgwPresent(IsDcgwPresentInput input) {
        RpcResultBuilder<IsDcgwPresentOutput> resultBld = RpcResultBuilder.success();

        List<DcGatewayIp> dcGatewayIpList = ItmUtils.getDcGatewayIpList(dataBroker);
        String dcgwIpStr = input.getDcgwIp();
        IpAddress dcgwIpAddr = IpAddressBuilder.getDefaultInstance(dcgwIpStr);
        long retVal;

        if (dcGatewayIpList != null && !dcGatewayIpList.isEmpty()
                && dcGatewayIpList.stream().anyMatch(gwIp -> Objects.equal(gwIp.getIpAddress(), dcgwIpAddr))) {
            //Match found
            retVal = 1;
            IsDcgwPresentOutputBuilder output = new IsDcgwPresentOutputBuilder().setRetVal(retVal);
            resultBld.withResult(output.build());
        } else {
            //Match not found
            retVal = 2;
            IsDcgwPresentOutputBuilder output = new IsDcgwPresentOutputBuilder().setRetVal(retVal);
            resultBld.withResult(output.build());
        }
        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public ListenableFuture<RpcResult<GetDpnEndpointIpsOutput>> getDpnEndpointIps(GetDpnEndpointIpsInput input) {
        BigInteger srcDpn = input.getSourceDpid() ;
        RpcResultBuilder<GetDpnEndpointIpsOutput> resultBld = failed();
        InstanceIdentifier<DPNTEPsInfo> tunnelInfoId =
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class,
                        new DPNTEPsInfoKey(srcDpn)).build();
        Optional<DPNTEPsInfo> tunnelInfo = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tunnelInfoId, dataBroker);
        if (!tunnelInfo.isPresent()) {
            LOG.error("tunnelInfo is not present");
            return Futures.immediateFuture(resultBld.build());
        }

        List<TunnelEndPoints> tunnelEndPointList = tunnelInfo.get().getTunnelEndPoints();
        if (tunnelEndPointList == null || tunnelEndPointList.isEmpty()) {
            LOG.error("tunnelEndPointList is null or empty");
            return Futures.immediateFuture(resultBld.build());
        }

        List<IpAddress> nexthopIpList = new ArrayList<>();
        tunnelEndPointList.forEach(tunnelEndPoint -> nexthopIpList.add(tunnelEndPoint.getIpAddress()));

        GetDpnEndpointIpsOutputBuilder output = new GetDpnEndpointIpsOutputBuilder().setNexthopipList(nexthopIpList);
        resultBld = RpcResultBuilder.success();
        resultBld.withResult(output.build()) ;
        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public ListenableFuture<RpcResult<GetDpnInfoOutput>> getDpnInfo(GetDpnInfoInput input) {
        return FutureRpcResults.fromListenableFuture(LOG, "getDpnInfo", input,
            () -> Futures.immediateFuture(getDpnInfoInternal(input))).build();
    }

    private GetDpnInfoOutput getDpnInfoInternal(GetDpnInfoInput input) throws ReadFailedException {
        Map<String, BigInteger> computeNamesVsDpnIds
                = getDpnIdByComputeNodeNameFromOpInventoryNodes(input.getComputeNames());
        Map<BigInteger, ComputesBuilder> dpnIdVsVtepsComputes
                = getTunnelEndPointByDpnIdFromTranPortZone(computeNamesVsDpnIds.values());
        List<Computes> computes = computeNamesVsDpnIds.entrySet().stream()
                .map(entry -> dpnIdVsVtepsComputes.get(entry.getValue()).setComputeName(entry.getKey()).build())
                .collect(Collectors.toList());
        return new GetDpnInfoOutputBuilder().setComputes(computes).build();
    }

    private Map<BigInteger, ComputesBuilder> getTunnelEndPointByDpnIdFromTranPortZone(Collection<BigInteger> dpnIds)
            throws ReadFailedException {
        TransportZones transportZones = singleTransactionDataBroker.syncRead(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(TransportZones.class).build());
        if (transportZones.getTransportZone() == null || transportZones.getTransportZone().isEmpty()) {
            throw new IllegalStateException("Failed to find transport zones in config datastore");
        }
        Map<BigInteger, ComputesBuilder> result = new HashMap<>();
        for (TransportZone transportZone : transportZones.getTransportZone()) {
                for (Vteps vtep : transportZone.getVteps()) {
                    if (dpnIds.contains(vtep.getDpnId())) {
                        result.putIfAbsent(vtep.getDpnId(),
                            new ComputesBuilder()
                                .setZoneName(transportZone.getZoneName())
                                .setDpnId(vtep.getDpnId())
                                .setNodeId(getNodeId(vtep.getDpnId()))
                                .setTepIp(Collections.singletonList(vtep.getIpAddress())));
                    }
                }
        }
        for (BigInteger dpnId : dpnIds) {
            if (!result.containsKey(dpnId)) {
                throw new IllegalStateException("Failed to find dpn id " + dpnId + " in transport zone");
            }
        }
        return result;
    }

    private Map<String, BigInteger> getDpnIdByComputeNodeNameFromOpInventoryNodes(List<String> nodeNames)
            throws ReadFailedException {
        Nodes operInventoryNodes = singleTransactionDataBroker.syncRead(
                LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Nodes.class).build());
        if (operInventoryNodes.getNode() == null || operInventoryNodes.getNode().isEmpty()) {
            throw new IllegalStateException("Failed to find operational inventory nodes datastore");
        }
        Map<String, BigInteger> result = new HashMap<>();
        for (Node node : operInventoryNodes.getNode()) {
            String name = node.augmentation(FlowCapableNode.class).getDescription();
            if (nodeNames.contains(name)) {
                String[] nodeId = node.getId().getValue().split(":");
                result.put(name, new BigInteger(nodeId[1]));
            }
        }
        for (String nodeName : nodeNames) {
            if (!result.containsKey(nodeName)) {
                throw new IllegalStateException("Failed to find dpn id of compute node name from oper inventory "
                        + nodeName);
            }
        }
        return result;
    }

    private String getNodeId(BigInteger dpnId) throws ReadFailedException {
        InstanceIdentifier<BridgeRefEntry> path = InstanceIdentifier
                .builder(BridgeRefInfo.class)
                .child(BridgeRefEntry.class, new BridgeRefEntryKey(dpnId)).build();
        BridgeRefEntry bridgeRefEntry =
                singleTransactionDataBroker.syncRead(LogicalDatastoreType.OPERATIONAL, path);
        return bridgeRefEntry.getBridgeReference().getValue()
                .firstKeyOf(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                        .network.topology.topology.Node.class).getNodeId().getValue();
    }

    private ListenableFuture<GetEgressActionsForTunnelOutput> getEgressActionsForInterface(String interfaceName,
           Long tunnelKey, Integer actionKey) throws ReadFailedException {
        int actionKeyStart = actionKey == null ? 0 : actionKey;
        DpnTepInterfaceInfo interfaceInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);
        if (interfaceInfo == null) {
            throw new IllegalStateException("Interface information not present in config DS for" + interfaceName);
        }
        Optional<StateTunnelList> ifState = tunnelStateCache
                .get(tunnelStateCache.getStateTunnelListIdentifier(interfaceName));
        if (ifState.isPresent()) {
            String tunnelType = ItmUtils.convertTunnelTypetoString(interfaceInfo.getTunnelType());
            List<Action> actions = getEgressActionInfosForInterface(tunnelType, ifState.get().getPortNumber(),
                    tunnelKey, actionKeyStart).stream().map(ActionInfo::buildAction).collect(Collectors.toList());
            return Futures.immediateFuture(new GetEgressActionsForTunnelOutputBuilder().setAction(actions).build());
        }
        throw new IllegalStateException("Interface information not present in oper DS for" + interfaceName);
    }

    private static List<ActionInfo> getEgressActionInfosForInterface(String tunnelType, String portNo, Long tunnelKey,
                                                                     int actionKeyStart) {
        List<ActionInfo> result = new ArrayList<>();
        switch (tunnelType) {
            case ITMConstants.TUNNEL_TYPE_GRE:
            case ITMConstants.TUNNEL_TYPE_MPLSoGRE:
                // Invoke IFM RPC and pass it on to the caller.
                LOG.warn("Interface Type {} not handled by ITM", tunnelType);
                break;
            case ITMConstants.TUNNEL_TYPE_VXLAN:
                //TODO tunnel_id to encode GRE key, once it is supported
                // Until then, tunnel_id should be "cleaned", otherwise it stores the value coming from a VXLAN tunnel
                result.add(new ActionSetFieldTunnelId(actionKeyStart++,
                        BigInteger.valueOf(tunnelKey != null ? tunnelKey : 0L)));
                result.add(new ActionOutput(actionKeyStart, new Uri(portNo)));
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", tunnelType);
                break;
        }
        return result;
    }
}
