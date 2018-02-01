/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.rpc;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIpKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVtepsKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetEgressActionsForTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetEgressActionsForTunnelOutput;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsDcgwPresentOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.IsTunnelInternalOrExternalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelFromDpnsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveTerminatingServiceActionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.SetBfdEnableOnTunnelInput;
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

    @Inject
    public ItmManagerRpcService(final DataBroker dataBroker, final IMdsalApiManager mdsalManager,
            final ItmConfig itmConfig, final DPNTEPsInfoCache dpnTEPsInfoCache) {
        this.dataBroker = dataBroker;
        this.mdsalManager = mdsalManager;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        this.externalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig, dpnTEPsInfoCache);
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
    public Future<RpcResult<GetTunnelInterfaceNameOutput>> getTunnelInterfaceName(GetTunnelInterfaceNameInput input) {
        RpcResultBuilder<GetTunnelInterfaceNameOutput> resultBld = null;
        BigInteger sourceDpn = input.getSourceDpid();
        BigInteger destinationDpn = input.getDestinationDpid();
        Optional<InternalTunnel> optTunnel = Optional.absent();

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
                resultBld = RpcResultBuilder.failed();
            }

        } else {
            resultBld = RpcResultBuilder.failed();
        }
        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public Future<RpcResult<GetEgressActionsForTunnelOutput>> getEgressActionsForTunnel(
            GetEgressActionsForTunnelInput input) {
        //TODO
        return null;
    }

    @Override
    public Future<RpcResult<GetTunnelTypeOutput>> getTunnelType(GetTunnelTypeInput input) {
        //TODO
        return null;
    }

    @Override
    public Future<RpcResult<Void>> setBfdEnableOnTunnel(SetBfdEnableOnTunnelInput input) {
        //TODO
        return null;
    }


    @Override
    public Future<RpcResult<Void>> removeExternalTunnelEndpoint(
            RemoveExternalTunnelEndpointInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
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
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override public void onFailure(Throwable error) {
                String msg = "Unable to delete DcGatewayIp " + input.getDestinationIp()
                        + " in datastore and tunnel type " + input.getTunnelType();
                LOG.error(msg);
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        });
        return result;
    }

    @Override
    public Future<RpcResult<Void>> removeExternalTunnelFromDpns(
            RemoveExternalTunnelFromDpnsInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        List<DPNTEPsInfo> cfgDpnList = ItmUtils.getDpnTepListFromDpnId(dpnTEPsInfoCache, input.getDpnId()) ;
        ItmExternalTunnelDeleteWorker.deleteTunnels(dataBroker, cfgDpnList,
                input.getDestinationIp(), input.getTunnelType());
        result.set(RpcResultBuilder.<Void>success().build());
        return result;
    }

    @Override
    public Future<RpcResult<Void>> buildExternalTunnelFromDpns(
            BuildExternalTunnelFromDpnsInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        List<ListenableFuture<Void>> extTunnelResultList = externalTunnelAddWorker
            .buildTunnelsFromDpnToExternalEndPoint(input.getDpnId(), input.getDestinationIp(),input.getTunnelType());
        for (ListenableFuture<Void> extTunnelResult : extTunnelResultList) {
            Futures.addCallback(extTunnelResult, new FutureCallback<Void>() {

                @Override
                public void onSuccess(Void voidInstance) {
                    result.set(RpcResultBuilder.<Void>success().build());
                }

                @Override
                public void onFailure(Throwable error) {
                    String msg = "Unable to create ext tunnel";
                    LOG.error("create ext tunnel failed. {}. {}", msg, error);
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                }
            });
        }
        return result;
    }

    @Override
    public Future<RpcResult<Void>> addExternalTunnelEndpoint(
            AddExternalTunnelEndpointInput input) {
        // TODO Auto-generated method stub

        //Ignore the Futures for now
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
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
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override public void onFailure(Throwable error) {
                String msg =
                        "Unable to create DcGatewayIp {} in datastore for ip " + input.getDestinationIp() + "and "
                                + "tunnel type " + input.getTunnelType();

                LOG.error(msg);
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        });
        return result;
    }

    @Override
    public Future<RpcResult<GetExternalTunnelInterfaceNameOutput>> getExternalTunnelInterfaceName(
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
            resultBld = RpcResultBuilder.failed();
        }

        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>>
        createTerminatingServiceActions(final CreateTerminatingServiceActionsInput input) {
        LOG.info("create terminatingServiceAction on DpnId = {} for service id {} and instructions {}",
                input.getDpnId() , input.getServiceId(), input.getInstruction());
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        int serviceId = input.getServiceId() ;
        final List<MatchInfo> mkMatches = getTunnelMatchesForServiceId(serviceId);
        byte[] vxLANHeader = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        // Flags Byte
        byte flags = (byte) 0x08;
        vxLANHeader[0] = flags;

        // Extract the serviceId details and imprint on the VxLAN Header
        vxLANHeader[4] = (byte) (serviceId >> 16);
        vxLANHeader[5] = (byte) (serviceId >> 8);
        vxLANHeader[6] = (byte) (serviceId >> 0);

        Flow terminatingServiceTableFlow = MDSALUtil.buildFlowNew(NwConstants.INTERNAL_TUNNEL_TABLE,
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE,serviceId), 5,
                String.format("%s:%d","ITM Flow Entry ",serviceId), 0, 0,
                ITMConstants.COOKIE_ITM.add(BigInteger.valueOf(serviceId)),mkMatches, input.getInstruction());

        ListenableFuture<Void> installFlowResult =
                mdsalManager.installFlow(input.getDpnId(), terminatingServiceTableFlow);
        Futures.addCallback(installFlowResult, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Unable to install terminating service flow for %s", input.getDpnId());
                LOG.error("create terminating service actions failed. {}. {}", msg, error);
                result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error)
                        .build());
            }
        });
        // result.set(RpcResultBuilder.<Void>success().build());
        return result;
    }

    @Override
    public Future<RpcResult<java.lang.Void>>
        removeTerminatingServiceActions(final RemoveTerminatingServiceActionsInput input) {
        LOG.info("remove terminatingServiceActions called with DpnId = {} and serviceId = {}",
                input.getDpnId(), input.getServiceId());
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        Flow terminatingServiceTableFlow = MDSALUtil.buildFlowNew(NwConstants.INTERNAL_TUNNEL_TABLE,
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE,input.getServiceId()), 5,
                String.format("%s:%d","ITM Flow Entry ",input.getServiceId()), 0, 0,
                ITMConstants.COOKIE_ITM.add(BigInteger.valueOf(input.getServiceId())),
                getTunnelMatchesForServiceId(input.getServiceId()), null);

        ListenableFuture<Void> installFlowResult =
                mdsalManager.removeFlow(input.getDpnId(), terminatingServiceTableFlow);
        Futures.addCallback(installFlowResult, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void voidInstance) {
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Unable to remove terminating service flow for %s", input.getDpnId());
                LOG.error("remove terminating service actions failed. {}. {}", msg, error);
                result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error)
                        .build());
            }
        });
        //result.set(RpcResultBuilder.<Void>success().build());

        return result ;
    }


    public List<MatchInfo> getTunnelMatchesForServiceId(int serviceId) {
        final List<MatchInfo> mkMatches = new ArrayList<>();
        byte[] vxLANHeader = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        // Flags Byte
        byte flag = (byte) 0x08;
        vxLANHeader[0] = flag;

        // Extract the serviceId details and imprint on the VxLAN Header
        vxLANHeader[4] = (byte) (serviceId >> 16);
        vxLANHeader[5] = (byte) (serviceId >> 8);
        vxLANHeader[6] = (byte) (serviceId >> 0);

        // Matching metadata
        mkMatches.add(new MatchTunnelId(BigInteger.valueOf(serviceId)));

        return mkMatches;
    }

    private String getFlowRef(long termSvcTable, int svcId) {
        return String.valueOf(termSvcTable) + svcId;
    }

    @Override
    public Future<RpcResult<GetInternalOrExternalInterfaceNameOutput>> getInternalOrExternalInterfaceName(
            GetInternalOrExternalInterfaceNameInput input) {
        RpcResultBuilder<GetInternalOrExternalInterfaceNameOutput> resultBld = RpcResultBuilder.failed();
        BigInteger srcDpn = input.getSourceDpid() ;
        srcDpn.toString();
        IpAddress dstIp = input.getDestinationIp() ;
        InstanceIdentifier<ExternalTunnel> path1 = InstanceIdentifier.create(ExternalTunnelList.class)
                .child(ExternalTunnel.class,
                        new ExternalTunnelKey(String.valueOf(dstIp), srcDpn.toString(), TunnelTypeMplsOverGre.class));

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
            if (meshedDpnList == null) {
                LOG.error("There are no tunnel mesh info in config DS");
                return Futures.immediateFuture(resultBld.build());
            }
            // Look for external tunnels if not look for internal tunnel
            for (DPNTEPsInfo teps : meshedDpnList) {
                TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
                if (dstIp.equals(firstEndPt.getIpAddress())) {
                    Optional<InternalTunnel> optTunnel = Optional.absent();
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
    public Future<RpcResult<java.lang.Void>> deleteL2GwDevice(DeleteL2GwDeviceInput input) {
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
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
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                for (TransportZone tzone : transportZones.getTransportZone()) {
                    if (!tzone.getTunnelType().equals(TunnelTypeVxlan.class)) {
                        continue;
                    }
                    foundVxlanTzone = true;
                    String transportZone = tzone.getZoneName();
                    if (tzone.getSubnets() == null || tzone.getSubnets().isEmpty()) {
                        result.set(RpcResultBuilder.<Void>failed()
                                .withError(RpcError.ErrorType.APPLICATION, "No subnets Configured").build());
                        return result;
                    }
                    SubnetsKey subnetsKey = tzone.getSubnets().get(0).getKey();
                    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId);
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                    //TO DO: add retry if it fails

                    transaction.delete(LogicalDatastoreType.CONFIGURATION, path);

                    ListenableFuture<Void> futureCheck = transaction.submit();
                    Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                        @Override public void onSuccess(Void voidInstance) {
                            result.set(RpcResultBuilder.<Void>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to delete HwVtep %s from datastore", nodeId);
                            LOG.error("Unable to delete HwVtep {}, {} from datastore", nodeId, hwIp);
                            result.set(RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    });

                }
            } else {
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No TransportZones configured").build());
                return result;
            }

            if (!foundVxlanTzone) {
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No VxLan TransportZones configured")
                        .build());
            }

            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Deleting l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public Future<RpcResult<java.lang.Void>> addL2GwDevice(AddL2GwDeviceInput input) {

        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
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
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No transportZone Configured").build());
                    return result;
                }
                for (TransportZone tzone : transportZones.getTransportZone()) {
                    if (!tzone.getTunnelType().equals(TunnelTypeVxlan.class)) {
                        continue;
                    }
                    String transportZone = tzone.getZoneName();
                    if (tzone.getSubnets() == null || tzone.getSubnets().isEmpty()) {
                        continue;
                    }
                    foundVxlanTzone = true;
                    SubnetsKey subnetsKey = tzone.getSubnets().get(0).getKey();
                    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId);
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    DeviceVteps deviceVtep = new DeviceVtepsBuilder().setKey(deviceVtepKey).setIpAddress(hwIp)
                            .setNodeId(nodeId).setTopologyId(input.getTopologyId()).build();
                    WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                    //TO DO: add retry if it fails
                    transaction.put(LogicalDatastoreType.CONFIGURATION, path, deviceVtep, true);

                    ListenableFuture<Void> futureCheck = transaction.submit();
                    Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                        @Override public void onSuccess(Void voidInstance) {
                            result.set(RpcResultBuilder.<Void>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to write HwVtep %s to datastore", nodeId);
                            LOG.error("Unable to write HwVtep {}, {} to datastore", nodeId, hwIp);
                            result.set(RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    });

                }
            } else {
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No TransportZones configured").build());
                return result;
            }

            if (!foundVxlanTzone) {
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No VxLan TransportZones configured")
                        .build());
            }

            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Adding l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public Future<RpcResult<java.lang.Void>> addL2GwMlagDevice(AddL2GwMlagDeviceInput input) {

        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
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
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                String transportZone = transportZones.getTransportZone().get(0).getZoneName();
                if (transportZones.getTransportZone().get(0).getSubnets() == null
                        || transportZones.getTransportZone().get(0).getSubnets().isEmpty()) {
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No subnets Configured").build());
                    return result;
                }
                SubnetsKey subnetsKey = transportZones.getTransportZone().get(0).getSubnets().get(0).getKey();
                DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId.get(0));
                InstanceIdentifier<DeviceVteps> path =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey).build();
                DeviceVteps deviceVtep = new DeviceVtepsBuilder().setKey(deviceVtepKey).setIpAddress(hwIp)
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
                            .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey2).build();
                    DeviceVteps deviceVtep2 = new DeviceVtepsBuilder().setKey(deviceVtepKey2).setIpAddress(hwIp)
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
                        result.set(RpcResultBuilder.<Void>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to write HwVtep %s to datastore", nodeId);
                        LOG.error("Unable to write HwVtep {}, {} to datastore", nodeId , hwIp);
                        result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error)
                                .build());
                    }
                });
            }
            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Adding l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public Future<RpcResult<Void>> deleteL2GwMlagDevice(DeleteL2GwMlagDeviceInput input) {

        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
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
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                String transportZone = tzones.getTransportZone().get(0).getZoneName();
                if (tzones.getTransportZone().get(0).getSubnets() == null || tzones.getTransportZone()
                        .get(0).getSubnets().isEmpty()) {
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No subnets Configured").build());
                    return result;
                }
                SubnetsKey subnetsKey = tzones.getTransportZone().get(0).getSubnets().get(0).getKey();
                DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, nodeId.get(0));
                InstanceIdentifier<DeviceVteps> path =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(Subnets.class, subnetsKey).child(DeviceVteps.class,
                                deviceVtepKey).build();
                WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
                //TO DO: add retry if it fails
                transaction.delete(LogicalDatastoreType.CONFIGURATION, path);

                DeviceVtepsKey deviceVtepKey2 = new DeviceVtepsKey(hwIp, nodeId.get(1));
                InstanceIdentifier<DeviceVteps> path2 =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(Subnets.class, subnetsKey).child(DeviceVteps.class,
                                deviceVtepKey2).build();
                //TO DO: add retry if it fails
                transaction.delete(LogicalDatastoreType.CONFIGURATION, path2);

                ListenableFuture<Void> futureCheck = transaction.submit();
                Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(Void voidInstance) {
                        result.set(RpcResultBuilder.<Void>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to write HwVtep %s to datastore", nodeId);
                        LOG.error("Unable to write HwVtep {}, {} to datastore", nodeId , hwIp);
                        result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error)
                                .build());
                    }
                });
            }
            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Deleting l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @Override
    public Future<RpcResult<IsTunnelInternalOrExternalOutput>> isTunnelInternalOrExternal(
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
    public Future<RpcResult<IsDcgwPresentOutput>> isDcgwPresent(IsDcgwPresentInput input) {
        RpcResultBuilder<IsDcgwPresentOutput> resultBld = RpcResultBuilder.success();

        List<DcGatewayIp> dcGatewayIpList = ItmUtils.getDcGatewayIpList(dataBroker);
        String dcgwIpStr = input.getDcgwIp();
        IpAddress dcgwIpAddr = new IpAddress(dcgwIpStr.toCharArray());
        long retVal;

        if (dcGatewayIpList != null
                && !dcGatewayIpList.isEmpty()
                && dcGatewayIpList.contains(dcgwIpAddr)) {
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
    public Future<RpcResult<GetDpnEndpointIpsOutput>> getDpnEndpointIps(GetDpnEndpointIpsInput input) {
        BigInteger srcDpn = input.getSourceDpid() ;
        RpcResultBuilder<GetDpnEndpointIpsOutput> resultBld = RpcResultBuilder.failed();
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
        tunnelEndPointList.forEach(tunnelEndPoint -> {
            nexthopIpList.add(tunnelEndPoint.getIpAddress());
        });

        GetDpnEndpointIpsOutputBuilder output = new GetDpnEndpointIpsOutputBuilder().setNexthopipList(nexthopIpList);
        resultBld = RpcResultBuilder.success();
        resultBld.withResult(output.build()) ;
        return Futures.immediateFuture(resultBld.build());
    }

}
