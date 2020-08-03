/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.rpc;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.serviceutils.tools.rpc.FutureRpcResults.fromListenableFuture;
import static org.opendaylight.yangtools.yang.common.RpcResultBuilder.failed;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.FluentFuture;
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.interfaces.InterfaceManagerService;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OfDpnTepConfigCache;
import org.opendaylight.genius.itm.cache.OfTepStateCache;
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
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.serviceutils.tools.rpc.FutureRpcResults;
import org.opendaylight.serviceutils.tools.rpc.FutureRpcResults.LogLevel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTepIpInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTepIpOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTepIpOutputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.get.dpn.info.output.Computes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.get.dpn.info.output.ComputesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
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
    private final RetryingManagedNewTransactionRunner retryingTxRunner;
    private final ItmConfig itmConfig;
    private final OfDpnTepConfigCache ofDpnTepConfigCache;
    private final OfTepStateCache ofTepStateCache;

    @Inject
    public ItmManagerRpcService(final DataBroker dataBroker, final IMdsalApiManager mdsalManager,
                                final ItmConfig itmConfig, final DPNTEPsInfoCache dpnTEPsInfoCache,
                                final IInterfaceManager interfaceManager, final DpnTepStateCache dpnTepStateCache,
                                final TunnelStateCache tunnelStateCache,
                                final InterfaceManagerService interfaceManagerService,
                                final OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                final DirectTunnelUtils directTunnelUtils, OfDpnTepConfigCache ofDpnTepConfigCache,
                                OfTepStateCache ofTepStateCache) {
        this.dataBroker = dataBroker;
        this.mdsalManager = mdsalManager;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        this.externalTunnelAddWorker = new ItmExternalTunnelAddWorker(itmConfig, dpnTEPsInfoCache);
        this.singleTransactionDataBroker = new SingleTransactionDataBroker(dataBroker);
        this.interfaceManager = interfaceManager;
        this.interfaceManagerService = interfaceManagerService;
        this.dpnTepStateCache = dpnTepStateCache;
        this.tunnelStateCache = tunnelStateCache;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.directTunnelUtils = directTunnelUtils;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.retryingTxRunner = new RetryingManagedNewTransactionRunner(dataBroker);
        this.itmConfig = itmConfig;
        this.ofDpnTepConfigCache = ofDpnTepConfigCache;
        this.ofTepStateCache = ofTepStateCache;
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
        Uint64 sourceDpn = input.getSourceDpid();
        Uint64 destinationDpn = input.getDestinationDpid();
        Optional<InternalTunnel> optTunnel = Optional.empty();

        if (interfaceManager.isItmOfTunnelsEnabled()) {
            //Destination DPN Id is not relevant in OF Tunnel scenario and so is ignored
            try {
                Optional<OfDpnTep> dpnstep = ofDpnTepConfigCache.get(sourceDpn.toJava());
                if (dpnstep.isPresent()) {
                    resultBld = RpcResultBuilder.success();
                    resultBld.withResult(new GetTunnelInterfaceNameOutputBuilder()
                            .setInterfaceName(dpnstep.get().getOfPortName())).build();
                    return Futures.immediateFuture(resultBld.build());
                } else {
                    LOG.error("OF tunnel is not available in ITM for source dpn {}", sourceDpn);
                    resultBld = RpcResultBuilder.failed();
                    return Futures.immediateFuture(resultBld.build());
                }
            } catch (ReadFailedException e) {
                LOG.error("ReadFailedException: cache read failed for source dpn {} reason: {}", sourceDpn,
                        e.getMessage());
                resultBld = RpcResultBuilder.failed();
                return Futures.immediateFuture(resultBld.build());
            }
        }
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
        if (tunnelName.startsWith("of")
                || (interfaceManager.isItmDirectTunnelsEnabled() && dpnTepStateCache.isInternal(tunnelName))) {
            return fromListenableFuture(LOG, input, () -> getEgressActionsForInternalTunnels(input.getIntfName(),
                    input.getTunnelKey() != null ? input.getTunnelKey().toJava() : null,
                    input.getActionKey())).onFailureLogLevel(LogLevel.ERROR).build();
        } else {
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
                        public void onSuccess(org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs
                                                      .rev160406.@NonNull GetEgressActionsForInterfaceOutput result) {
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
                    } ,MoreExecutors.directExecutor());
            return  settableFuture;
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
                        @Override
                        public void onSuccess(org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs
                                                      .rev160406.@NonNull GetTunnelTypeOutput result) {
                            GetTunnelTypeOutputBuilder output = new GetTunnelTypeOutputBuilder()
                                    .setTunnelType(result.getTunnelType());
                            settableFuture.set(RpcResultBuilder.<GetTunnelTypeOutput>success()
                                    .withResult(output.build()).build());
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            LOG.debug("RPC Call to Get tunnel type failed for interface {}", tunnelName);
                            String errMsg = String.format("RPC to Get tunnel type failed for interface %s",
                                    tunnelName);
                            settableFuture.set(RpcResultBuilder.<GetTunnelTypeOutput>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, errMsg, throwable).build());
                        }
                    },MoreExecutors.directExecutor());
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
        final Uint64 srcDpnId = Uint64.valueOf(input.getSourceNode());
        final Uint64 destDpnId = Uint64.valueOf(input.getDestinationNode());
        LOG.debug("setBfdParamOnTunnel srcDpnId: {}, destDpnId: {}", srcDpnId, destDpnId);
        final SettableFuture<RpcResult<SetBfdParamOnTunnelOutput>> result = SettableFuture.create();
        FluentFuture<Void> future = txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
            enableBFD(tx, srcDpnId, destDpnId, input.isMonitoringEnabled(), input.getMonitoringInterval().toJava());
            enableBFD(tx, destDpnId, srcDpnId, input.isMonitoringEnabled(), input.getMonitoringInterval().toJava());
        });

        future.addCallback(new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<SetBfdParamOnTunnelOutput>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = "Unable to remove external tunnel from DPN";
                LOG.error("remove ext tunnel failed. {}.", msg, error);
                result.set(RpcResultBuilder.<SetBfdParamOnTunnelOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        }, MoreExecutors.directExecutor());
        return result;
    }

    private void enableBFD(TypedWriteTransaction<Datastore.Configuration> tx, Uint64 srcDpnId, Uint64 destDpnId,
                           final Boolean enabled, final Integer interval) throws ReadFailedException {
        DpnTepInterfaceInfo dpnTepInterfaceInfo = dpnTepStateCache.getDpnTepInterface(srcDpnId, destDpnId);
        RemoteDpnsBuilder remoteDpnsBuilder = new RemoteDpnsBuilder();
        remoteDpnsBuilder.withKey(new RemoteDpnsKey(destDpnId)).setDestinationDpnId(destDpnId)
                .setTunnelName(dpnTepInterfaceInfo.getTunnelName()).setInternal(dpnTepInterfaceInfo.isInternal())
                .setMonitoringEnabled(enabled);
        if (enabled && interval != null) {
            remoteDpnsBuilder.setMonitoringInterval(interval);
        }
        RemoteDpns remoteDpn = remoteDpnsBuilder.build();
        Optional<OvsBridgeRefEntry> ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(srcDpnId);
        LOG.debug("setBfdParamOnTunnel TunnelName: {}, ovsBridgeRefEntry: {}", dpnTepInterfaceInfo.getTunnelName(),
                ovsBridgeRefEntry);
        directTunnelUtils.updateBfdConfiguration(srcDpnId, remoteDpn, ovsBridgeRefEntry);
        InstanceIdentifier<RemoteDpns> iid = InstanceIdentifier.builder(DpnTepsState.class)
                .child(DpnsTeps.class, new DpnsTepsKey(srcDpnId))
                .child(RemoteDpns.class,
                        new RemoteDpnsKey(destDpnId)).build();
        tx.mergeParentStructureMerge(iid, remoteDpn);
    }

    @Override
    public ListenableFuture<RpcResult<RemoveExternalTunnelEndpointOutput>> removeExternalTunnelEndpoint(
            RemoveExternalTunnelEndpointInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<RemoveExternalTunnelEndpointOutput>> result = SettableFuture.create();
        Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        FluentFuture<Void> future = txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> {
                ItmExternalTunnelDeleteWorker.deleteTunnels(meshedDpnList, input.getDestinationIp(),
                        input.getTunnelType(), tx);
                InstanceIdentifier<DcGatewayIp> extPath = InstanceIdentifier.builder(DcGatewayIpList.class)
                        .child(DcGatewayIp.class, new DcGatewayIpKey(input.getDestinationIp())).build();
                tx.delete(extPath);
            }
        );
        future.addCallback(new FutureCallback<Void>() {
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
        }, MoreExecutors.directExecutor());
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<RemoveExternalTunnelFromDpnsOutput>> removeExternalTunnelFromDpns(
            RemoveExternalTunnelFromDpnsInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<RemoveExternalTunnelFromDpnsOutput>> result = SettableFuture.create();
        List<DPNTEPsInfo> cfgDpnList = ItmUtils.getDpnTepListFromDpnId(dpnTEPsInfoCache, input.getDpnId());
        FluentFuture<Void> future = txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> ItmExternalTunnelDeleteWorker.deleteTunnels(cfgDpnList, input.getDestinationIp(),
                    input.getTunnelType(), tx));

        future.addCallback(new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<RemoveExternalTunnelFromDpnsOutput>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = "Unable to remove external tunnel from DPN";
                LOG.error("remove ext tunnel failed. {}.", msg, error);
                result.set(RpcResultBuilder.<RemoveExternalTunnelFromDpnsOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        }, MoreExecutors.directExecutor());
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<BuildExternalTunnelFromDpnsOutput>> buildExternalTunnelFromDpns(
            BuildExternalTunnelFromDpnsInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<BuildExternalTunnelFromDpnsOutput>> result = SettableFuture.create();
        FluentFuture<Void> extTunnelResultList =
            txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> externalTunnelAddWorker.buildTunnelsFromDpnToExternalEndPoint(input.getDpnId(),
                    input.getDestinationIp(),input.getTunnelType(), tx));

        extTunnelResultList.addCallback(new FutureCallback<Void>() {

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
        }, MoreExecutors.directExecutor());
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<AddExternalTunnelEndpointOutput>> addExternalTunnelEndpoint(
            AddExternalTunnelEndpointInput input) {
        // TODO Auto-generated method stub

        //Ignore the Futures for now
        final SettableFuture<RpcResult<AddExternalTunnelEndpointOutput>> result = SettableFuture.create();
        Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        InstanceIdentifier<DcGatewayIp> extPath = InstanceIdentifier.builder(DcGatewayIpList.class)
                .child(DcGatewayIp.class, new DcGatewayIpKey(input.getDestinationIp())).build();
        DcGatewayIp dcGatewayIp =
                new DcGatewayIpBuilder().setIpAddress(input.getDestinationIp())
                        .setTunnnelType(input.getTunnelType()).build();

        FluentFuture<Void> future = txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> {
                externalTunnelAddWorker.buildTunnelsToExternalEndPoint(meshedDpnList, input.getDestinationIp(),
                    input.getTunnelType(), tx);
                tx.mergeParentStructurePut(extPath, dcGatewayIp);
            }
        );
        future.addCallback(new FutureCallback<Void>() {
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
        }, MoreExecutors.directExecutor());
        return result;
    }

    @Override
    public ListenableFuture<RpcResult<GetExternalTunnelInterfaceNameOutput>> getExternalTunnelInterfaceName(
            GetExternalTunnelInterfaceNameInput input) {
        SettableFuture.create();
        RpcResultBuilder<GetExternalTunnelInterfaceNameOutput> resultBld;
        String sourceNode = input.getSourceNode();
        String dstNode = input.getDestinationNode();
        if (interfaceManager.isItmOfTunnelsEnabled()) {
            Optional<OfDpnTep> tepDetail;
            try {
                tepDetail = ofDpnTepConfigCache.get(new BigInteger(sourceNode));
            } catch (ReadFailedException e) {
                LOG.error("ReadFailedException: OF tunnel interface is not available in config DS for "
                        + "source dpn {} reason: {}", sourceNode, e.getMessage());
                resultBld = failed();
                return Futures.immediateFuture(resultBld.build());
            }
            if (tepDetail.isPresent()) {
                GetExternalTunnelInterfaceNameOutputBuilder output =
                        new GetExternalTunnelInterfaceNameOutputBuilder()
                                .setInterfaceName(tepDetail.get().getOfPortName());
                resultBld = RpcResultBuilder.success();
                resultBld.withResult(output.build());
                return Futures.immediateFuture(resultBld.build());
            } else {
                LOG.error("OF tunnel interface is not available in config DS for source dpn {}", sourceNode);
                resultBld = failed();
                return Futures.immediateFuture(resultBld.build());
            }
        }
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
        Uint16 serviceId = input.getServiceId();
        final List<MatchInfo> mkMatches = getTunnelMatchesForServiceId(serviceId);

        Flow terminatingServiceTableFlow = MDSALUtil.buildFlowNew(NwConstants.INTERNAL_TUNNEL_TABLE,
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE, serviceId), 5,
                "ITM Flow Entry :" + serviceId, 0, 0,
                Uint64.fromLongBits(ITMConstants.COOKIE_ITM.longValue() + serviceId.toJava()), mkMatches,
                input.getInstruction());

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

    public List<MatchInfo> getTunnelMatchesForServiceId(Uint16 serviceId) {
        final List<MatchInfo> mkMatches = new ArrayList<>();

        // Matching metadata
        mkMatches.add(new MatchTunnelId(Uint64.valueOf(serviceId)));

        return mkMatches;
    }

    private String getFlowRef(long termSvcTable, Uint16 svcId) {
        return String.valueOf(termSvcTable) + svcId;
    }

    @Override
    public ListenableFuture<RpcResult<GetInternalOrExternalInterfaceNameOutput>> getInternalOrExternalInterfaceName(
            GetInternalOrExternalInterfaceNameInput input) {
        RpcResultBuilder<GetInternalOrExternalInterfaceNameOutput> resultBld = failed();
        Uint64 srcDpn = input.getSourceDpid();
        IpAddress dstIp = input.getDestinationIp();
        if (interfaceManager.isItmOfTunnelsEnabled()) {
            Optional<OfDpnTep> tepDetail;
            try {
                tepDetail = ofDpnTepConfigCache.get(srcDpn.toJava());
            } catch (ReadFailedException e) {
                LOG.error("ReadFailedException: OF tunnel interface is not available in config DS for "
                        + "source dpn {} reason: {}", srcDpn, e.getMessage());
                return Futures.immediateFuture(resultBld.build());
            }
            if (tepDetail.isPresent()) {
                GetInternalOrExternalInterfaceNameOutputBuilder output =
                        new GetInternalOrExternalInterfaceNameOutputBuilder()
                                .setInterfaceName(tepDetail.get().getOfPortName());
                resultBld = RpcResultBuilder.success();
                resultBld.withResult(output.build());
                return Futures.immediateFuture(resultBld.build());
            } else {
                LOG.error("OF tunnel interface is not available in config DS for source dpn {}", srcDpn);
                return Futures.immediateFuture(resultBld.build());
            }
        }
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
                    Optional<InternalTunnel> optTunnel = Optional.empty();
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
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier
                            .builder(TransportZones.class)
                            .child(TransportZone.class,
                                    new TransportZoneKey(transportZone))
                            .child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    FluentFuture<Void> future =
                        retryingTxRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> tx.delete(path));
                    future.addCallback(new FutureCallback<Void>() {
                        @Override public void onSuccess(Void voidInstance) {
                            result.set(RpcResultBuilder.<DeleteL2GwDeviceOutput>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to delete HwVtep %s from datastore", nodeId);
                            LOG.error("Unable to delete HwVtep {}, {} from datastore", nodeId, hwIp);
                            result.set(RpcResultBuilder.<DeleteL2GwDeviceOutput>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    }, MoreExecutors.directExecutor());
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
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier
                            .builder(TransportZones.class)
                            .child(TransportZone.class,
                                    new TransportZoneKey(transportZone))
                            .child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    DeviceVteps deviceVtep = new DeviceVtepsBuilder().withKey(deviceVtepKey).setIpAddress(hwIp)
                            .setNodeId(nodeId).setTopologyId(input.getTopologyId()).build();
                    //TO DO: add retry if it fails
                    FluentFuture<Void> future = retryingTxRunner
                            .callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                                tx -> tx.mergeParentStructurePut(path, deviceVtep));

                    future.addCallback(new FutureCallback<Void>() {

                        @Override public void onSuccess(Void voidInstance) {
                            result.set(RpcResultBuilder.<AddL2GwDeviceOutput>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to write HwVtep %s to datastore", nodeId);
                            LOG.error("Unable to write HwVtep {}, {} to datastore", nodeId, hwIp);
                            result.set(RpcResultBuilder.<AddL2GwDeviceOutput>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    }, MoreExecutors.directExecutor());

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

    public ListenableFuture<RpcResult<GetTepIpOutput>> getTepIp(GetTepIpInput input) {
        RpcResultBuilder<GetTepIpOutput> resultBld;
        Uint64 sourceDpn = input.getDpnId();
        Optional<OfDpnTep> dpnstep;
        try {
            dpnstep = ofDpnTepConfigCache.get(sourceDpn.toJava());
        } catch (ReadFailedException e) {
            LOG.error("ReadFailedException: OF tunnel is not available in ITM for source dpn {} reason: {}",sourceDpn,
                    e.getMessage());
            resultBld = RpcResultBuilder.failed();
            return Futures.immediateFuture(resultBld.build());
        }
        if (dpnstep.isPresent()) {
            resultBld = RpcResultBuilder.success();
            resultBld.withResult(new GetTepIpOutputBuilder()
                    .setTepIp(dpnstep.get().getTepIp())).build();
            return Futures.immediateFuture(resultBld.build());
        } else {
            LOG.error("OF tunnel is not available in ITM for source dpn {}",sourceDpn);
            resultBld = RpcResultBuilder.failed();
            return Futures.immediateFuture(resultBld.build());
        }
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
                LOG.trace("writing hWvtep{}", deviceVtep);
                FluentFuture<Void> future =
                    retryingTxRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                        tx -> {
                            tx.mergeParentStructurePut(path, deviceVtep);
                            if (nodeId.size() == 2) {
                                LOG.trace("second node-id {}", nodeId.get(1));
                                DeviceVtepsKey deviceVtepKey2 = new DeviceVtepsKey(hwIp, nodeId.get(1));
                                InstanceIdentifier<DeviceVteps> path2 = InstanceIdentifier
                                        .builder(TransportZones.class)
                                        .child(TransportZone.class, new TransportZoneKey(transportZone))
                                        .child(DeviceVteps.class, deviceVtepKey2).build();
                                DeviceVteps deviceVtep2 = new DeviceVtepsBuilder().withKey(deviceVtepKey2)
                                        .setIpAddress(hwIp).setNodeId(nodeId.get(1))
                                        .setTopologyId(input.getTopologyId()).build();
                                LOG.trace("writing {}", deviceVtep2);
                                tx.mergeParentStructurePut(path2, deviceVtep2);
                            }
                        });
                future.addCallback(new FutureCallback<Void>() {
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
                FluentFuture<Void> future =
                    retryingTxRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                        tx -> {
                            DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId.get(0));
                            InstanceIdentifier<DeviceVteps> path =
                                    InstanceIdentifier.builder(TransportZones.class)
                                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                                            .child(DeviceVteps.class,
                                                    deviceVtepKey).build();
                            tx.delete(path);
                            DeviceVtepsKey deviceVtepKey2 = new DeviceVtepsKey(hwIp, nodeId.get(1));
                            InstanceIdentifier<DeviceVteps> path2 =
                                    InstanceIdentifier.builder(TransportZones.class)
                                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                                            .child(DeviceVteps.class,
                                                    deviceVtepKey2).build();
                            tx.delete(path2);
                        }
                    );

                future.addCallback(new FutureCallback<Void>() {

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
                }, MoreExecutors.directExecutor());
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

        Map<DcGatewayIpKey, DcGatewayIp> dcGatewayIpList = new HashMap<>();
        txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
            tx -> dcGatewayIpList.putAll(getDcGatewayIpList(tx))).isDone();
        String dcgwIpStr = input.getDcgwIp();
        IpAddress dcgwIpAddr = IpAddressBuilder.getDefaultInstance(dcgwIpStr);
        long retVal;

        if (!dcGatewayIpList.isEmpty()
                && dcGatewayIpList.values().stream().anyMatch(gwIp -> Objects.equal(gwIp.getIpAddress(), dcgwIpAddr))) {
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
        Uint64 srcDpn = input.getSourceDpid();
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
        Map<String, Uint64> computeNamesVsDpnIds
                = getDpnIdByComputeNodeNameFromOpInventoryNodes(input.getComputeNames());
        Map<Uint64, ComputesBuilder> dpnIdVsVtepsComputes
                = getTunnelEndPointByDpnIdFromTranPortZone(computeNamesVsDpnIds.values());
        List<Computes> computes = computeNamesVsDpnIds.entrySet().stream()
                .map(entry -> dpnIdVsVtepsComputes.get(entry.getValue()).setComputeName(entry.getKey()).build())
                .collect(Collectors.toList());
        return new GetDpnInfoOutputBuilder().setComputes(computes).build();
    }

    private Map<Uint64, ComputesBuilder> getTunnelEndPointByDpnIdFromTranPortZone(Collection<Uint64> dpnIds)
            throws ReadFailedException {
        TransportZones transportZones = singleTransactionDataBroker.syncRead(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(TransportZones.class).build());
        if (transportZones.getTransportZone() == null || transportZones.getTransportZone().isEmpty()) {
            throw new IllegalStateException("Failed to find transport zones in config datastore");
        }
        Map<Uint64, ComputesBuilder> result = new HashMap<>();
        for (TransportZone transportZone : transportZones.getTransportZone()) {
            for (Vteps vtep : transportZone.getVteps().values()) {
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
        for (Uint64 dpnId : dpnIds) {
            if (!result.containsKey(dpnId)) {
                throw new IllegalStateException("Failed to find dpn id " + dpnId + " in transport zone");
            }
        }
        return result;
    }

    private Map<String, Uint64> getDpnIdByComputeNodeNameFromOpInventoryNodes(List<String> nodeNames)
            throws ReadFailedException {
        Nodes operInventoryNodes = singleTransactionDataBroker.syncRead(
                LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Nodes.class).build());
        if (operInventoryNodes.getNode() == null || operInventoryNodes.getNode().isEmpty()) {
            throw new IllegalStateException("Failed to find operational inventory nodes datastore");
        }
        Map<String, Uint64> result = new HashMap<>();
        for (Node node : operInventoryNodes.getNode().values()) {
            String name = node.augmentation(FlowCapableNode.class).getDescription();
            if (nodeNames.contains(name)) {
                String[] nodeId = node.getId().getValue().split(":");
                result.put(name, Uint64.valueOf(nodeId[1]));
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

    private String getNodeId(Uint64 dpnId) throws ReadFailedException {
        InstanceIdentifier<BridgeRefEntry> path = InstanceIdentifier
                .builder(BridgeRefInfo.class)
                .child(BridgeRefEntry.class, new BridgeRefEntryKey(dpnId)).build();
        BridgeRefEntry bridgeRefEntry =
                singleTransactionDataBroker.syncRead(LogicalDatastoreType.OPERATIONAL, path);
        return bridgeRefEntry.getBridgeReference().getValue()
                .firstKeyOf(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                        .network.topology.topology.Node.class).getNodeId().getValue();
    }

    private ListenableFuture<GetEgressActionsForTunnelOutput>
        getEgressActionsForInternalTunnels(String interfaceName, Long tunnelKey, Integer actionKey)
            throws ExecutionException, InterruptedException, OperationFailedException {

        if (interfaceName.startsWith("of")) {
            Optional<OfTep> oftep = ofTepStateCache.get(interfaceName);
            if (!oftep.isPresent()) {
                throw new IllegalStateException("Interface information not present in oper DS for" + interfaceName);
            }
            List<ActionInfo> actions = getEgressActionInfosForOpenFlowTunnel(oftep.get().getIfIndex(),
                    oftep.get().getTepIp(), tunnelKey, actionKey);
            return Futures.immediateFuture(new GetEgressActionsForTunnelOutputBuilder()
                    .setAction(actions.stream().map(ActionInfo::buildAction).collect(Collectors.toList())).build());
        } else {
            DpnTepInterfaceInfo interfaceInfo = dpnTepStateCache.getTunnelFromCache(interfaceName);
            if (interfaceInfo == null) {
                throw new IllegalStateException("Interface information not present in config DS for" + interfaceName);
            }

            String tunnelType = ItmUtils.convertTunnelTypetoString(interfaceInfo.getTunnelType());
            if (!tunnelType.equalsIgnoreCase(ITMConstants.TUNNEL_TYPE_VXLAN)) {
                throw new IllegalArgumentException(tunnelType + " tunnel not handled by ITM");
            }

            Optional<DPNTEPsInfo> dpntePsInfoOptional = dpnTEPsInfoCache.get(InstanceIdentifier
                    .builder(DpnEndpoints.class)
                    .child(DPNTEPsInfo.class, new DPNTEPsInfoKey(
                            // FIXME: the cache should be caching this value, not just as a String
                            Uint64.valueOf(dpnTepStateCache.getTunnelEndPointInfoFromCache(
                                    interfaceInfo.getTunnelName()).getDstEndPointInfo())))
                    .build());
            Integer dstId;
            if (dpntePsInfoOptional.isPresent()) {
                dstId = dpntePsInfoOptional.get().getDstId();
            } else {
                dstId = directTunnelUtils.allocateId(ITMConstants.ITM_IDPOOL_NAME,
                        interfaceInfo.getRemoteDPN().toString());
            }

            List<ActionInfo> result = new ArrayList<>();
            long regValue = MetaDataUtil.getRemoteDpnMetadatForEgressTunnelTable(dstId);
            int actionKeyStart = actionKey == null ? 0 : actionKey;
            result.add(new ActionSetFieldTunnelId(actionKeyStart++,
                    Uint64.valueOf(tunnelKey != null ? tunnelKey : 0L)));
            result.add(new ActionRegLoad(actionKeyStart++, NxmNxReg6.class, MetaDataUtil.REG6_START_INDEX,
                    MetaDataUtil.REG6_END_INDEX, regValue));
            result.add(new ActionNxResubmit(actionKeyStart, NwConstants.EGRESS_TUNNEL_TABLE));

            return Futures.immediateFuture(new GetEgressActionsForTunnelOutputBuilder()
                    .setAction(result.stream().map(ActionInfo::buildAction).collect(Collectors.toList())).build());
        }
    }

    private static List<ActionInfo> getEgressActionInfosForOpenFlowTunnel(Uint16 ifIndex, IpAddress ipAddress,
                                                                          Long tunnelKey, Integer actionKey) {
        List<ActionInfo> result = new ArrayList<>();
        int actionKeyStart = actionKey == null ? 0 : actionKey;
        result.add(new ActionSetFieldTunnelId(actionKeyStart++,
                Uint64.valueOf(tunnelKey != null ? tunnelKey : 0L)));
        long regValue = MetaDataUtil.getReg6ValueForLPortDispatcher(ifIndex.intValue() ,
                NwConstants.DEFAULT_SERVICE_INDEX);
        result.add(new ActionRegLoad(actionKeyStart++, NxmNxReg6.class, ITMConstants.REG6_START_INDEX,
                ITMConstants.REG6_END_INDEX, regValue));
        result.add(new ActionNxResubmit(actionKeyStart, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE));

        return result;
    }

    public static Map<DcGatewayIpKey, DcGatewayIp>
        getDcGatewayIpList(TypedReadWriteTransaction<Datastore.Configuration> tx)
            throws ExecutionException, InterruptedException {
        Map<DcGatewayIpKey, DcGatewayIp> dcGatewayIpMap = new HashMap<>();
        FluentFuture<Optional<DcGatewayIpList>> future =
                tx.read(InstanceIdentifier.builder(DcGatewayIpList.class).build());
        future.addCallback(new FutureCallback<Optional<DcGatewayIpList>>() {
            @Override
            public void onSuccess(Optional<DcGatewayIpList> optional) {
                try {
                    // FIXME: why not just use the provided optional?
                    Optional<DcGatewayIpList> opt = future.get();
                    if (opt.isPresent()) {
                        DcGatewayIpList list = opt.get();
                        if (list != null) {
                            dcGatewayIpMap.putAll(list.getDcGatewayIp());
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("DcGateway IpList read failed", e);
                }
            }

            @Override
            public void onFailure(Throwable error) {
                LOG.error("DcGateway IpList read failed", error);
            }
        }, MoreExecutors.directExecutor());
        return dcGatewayIpMap;
    }
}
