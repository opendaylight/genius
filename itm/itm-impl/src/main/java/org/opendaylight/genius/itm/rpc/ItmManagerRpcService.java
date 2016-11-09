/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetExternalTunnelInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetInternalOrExternalInterfaceNameOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutputBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmManagerRpcService implements ItmRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(ItmManagerRpcService.class);
    DataBroker dataBroker;
    private IMdsalApiManager mdsalManager;


    public void setMdsalManager(IMdsalApiManager mdsalManager) {
        this.mdsalManager = mdsalManager;
    }

    IdManagerService idManagerService;

    public ItmManagerRpcService(DataBroker dataBroker, IdManagerService idManagerService) {
        this.dataBroker = dataBroker;
        this.idManagerService = idManagerService;
    }

    @Override
    public Future<RpcResult<GetTunnelInterfaceNameOutput>> getTunnelInterfaceName(GetTunnelInterfaceNameInput input) {
        RpcResultBuilder<GetTunnelInterfaceNameOutput> resultBld = null;
        BigInteger sourceDpn = input.getSourceDpid() ;
        BigInteger destinationDpn = input.getDestinationDpid() ;
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(
                TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(destinationDpn, sourceDpn, input.getTunnelType()));

        Optional<InternalTunnel> tnl = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);

        if( tnl != null && tnl.isPresent())
        {
            InternalTunnel tunnel = tnl.get();
            GetTunnelInterfaceNameOutputBuilder output = new GetTunnelInterfaceNameOutputBuilder() ;
            output.setInterfaceName(tunnel.getTunnelInterfaceName()) ;
            resultBld = RpcResultBuilder.success();
            resultBld.withResult(output.build()) ;
        }else {
            resultBld = RpcResultBuilder.failed();
        }

        return Futures.immediateFuture(resultBld.build());
    }


    @Override
    public Future<RpcResult<Void>> removeExternalTunnelEndpoint(
            RemoveExternalTunnelEndpointInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        List<DPNTEPsInfo> meshedDpnList = ItmUtils.getTunnelMeshInfo(dataBroker) ;
        ItmExternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService,meshedDpnList , input.getDestinationIp(), input.getTunnelType());
        InstanceIdentifier<DcGatewayIp> extPath= InstanceIdentifier.builder(DcGatewayIpList.class).child(DcGatewayIp.class, new DcGatewayIpKey(input.getDestinationIp())).build();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.CONFIGURATION, extPath);
        ListenableFuture<Void> futureCheck = t.submit();
        Futures.addCallback(futureCheck, new FutureCallback<Void>() {

            @Override public void onSuccess(Void aVoid) {
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override public void onFailure(Throwable error) {
                String msg =
                        "Unable to delete DcGatewayIp " + input.getDestinationIp() + " in datastore and tunnel type " + input.getTunnelType();
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
        List<DPNTEPsInfo> cfgDpnList = ItmUtils.getDPNTEPListFromDPNId(dataBroker, input.getDpnId()) ;
        ItmExternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService, cfgDpnList, input.getDestinationIp(), input.getTunnelType());
        result.set(RpcResultBuilder.<Void>success().build());
        return result;
    }

    @Override
    public Future<RpcResult<Void>> buildExternalTunnelFromDpns(
            BuildExternalTunnelFromDpnsInput input) {
        //Ignore the Futures for now
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        List<ListenableFuture<Void>> extTunnelResultList = ItmExternalTunnelAddWorker.buildTunnelsFromDpnToExternalEndPoint(dataBroker, idManagerService,input.getDpnId(), input.getDestinationIp(), input.getTunnelType());
        for (ListenableFuture<Void> extTunnelResult : extTunnelResultList) {
            Futures.addCallback(extTunnelResult, new FutureCallback<Void>(){

                @Override
                public void onSuccess(Void aVoid) {
                    result.set(RpcResultBuilder.<Void>success().build());
                }

                @Override
                public void onFailure(Throwable error) {
                    String msg = "Unable to create ext tunnel";
                    LOG.error("create ext tunnel failed. {}. {}", msg, error);
                    result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                }
            });
        }
        result.set(RpcResultBuilder.<Void>success().build());
        return result;
    }

    @Override
    public Future<RpcResult<Void>> addExternalTunnelEndpoint(
            AddExternalTunnelEndpointInput input) {
        // TODO Auto-generated method stub

        //Ignore the Futures for now
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        List<DPNTEPsInfo> meshedDpnList = ItmUtils.getTunnelMeshInfo(dataBroker) ;
        ItmExternalTunnelAddWorker.buildTunnelsToExternalEndPoint(dataBroker, idManagerService,meshedDpnList, input.getDestinationIp(), input.getTunnelType()) ;InstanceIdentifier<DcGatewayIp> extPath= InstanceIdentifier.builder(DcGatewayIpList.class).child(DcGatewayIp.class, new DcGatewayIpKey(input.getDestinationIp())).build();
        DcGatewayIp dcGatewayIp = new DcGatewayIpBuilder().setIpAddress(input.getDestinationIp()).setTunnnelType(input.getTunnelType()).build();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        t.put(LogicalDatastoreType.CONFIGURATION, extPath,dcGatewayIp, true);
        ListenableFuture<Void> futureCheck = t.submit();
        Futures.addCallback(futureCheck, new FutureCallback<Void>() {

            @Override public void onSuccess(Void aVoid) {
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override public void onFailure(Throwable error) {
                String msg =
                        "Unable to create DcGatewayIp {} in datastore for ip "+ input.getDestinationIp() + "and tunnel type " + input.getTunnelType();
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
        InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(
                ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(dstNode, sourceNode, input.getTunnelType()));

        Optional<ExternalTunnel> ext = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);

        if( ext != null && ext.isPresent())
        {
            ExternalTunnel exTunnel = ext.get();
            GetExternalTunnelInterfaceNameOutputBuilder output = new GetExternalTunnelInterfaceNameOutputBuilder() ;
            output.setInterfaceName(exTunnel.getTunnelInterfaceName()) ;
            resultBld = RpcResultBuilder.success();
            resultBld.withResult(output.build()) ;
        }else {
            resultBld = RpcResultBuilder.failed();
        }

        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>> createTerminatingServiceActions(final CreateTerminatingServiceActionsInput input) {
        LOG.info("create terminatingServiceAction on DpnId = {} for service id {} and instructions {}", input.getDpnId() , input.getServiceId(), input.getInstruction());
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        int serviceId = input.getServiceId() ;
        List<MatchInfo> mkMatches = getTunnelMatchesForServiceId(serviceId);
        byte[] vxLANHeader = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        // Flags Byte
        byte Flags = (byte) 0x08;
        vxLANHeader[0] = Flags;

        // Extract the serviceId details and imprint on the VxLAN Header
        vxLANHeader[4] = (byte) (serviceId >> 16);
        vxLANHeader[5] = (byte) (serviceId >> 8);
        vxLANHeader[6] = (byte) (serviceId >> 0);

        // Matching metadata
//        mkMatches.add(new MatchInfo(MatchFieldType.tunnel_id, new BigInteger[] {
//                new BigInteger(1, vxLANHeader),
//                MetaDataUtil.METADA_MASK_VALID_TUNNEL_ID_BIT_AND_TUNNEL_ID }));

        Flow terminatingServiceTableFlow = MDSALUtil.buildFlowNew(NwConstants.INTERNAL_TUNNEL_TABLE,
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE,serviceId), 5, String.format("%s:%d","ITM Flow Entry ",serviceId),
                0, 0, ITMConstants.COOKIE_ITM.add(BigInteger.valueOf(serviceId)),mkMatches, input.getInstruction());

        ListenableFuture<Void> installFlowResult = mdsalManager.installFlow(input.getDpnId(), terminatingServiceTableFlow);
        Futures.addCallback(installFlowResult, new FutureCallback<Void>(){

            @Override
            public void onSuccess(Void aVoid) {
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Unable to install terminating service flow for %s", input.getDpnId());
                LOG.error("create terminating service actions failed. {}. {}", msg, error);
                result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        });
        // result.set(RpcResultBuilder.<Void>success().build());
        return result;
    }

    @Override
    public Future<RpcResult<java.lang.Void>> removeTerminatingServiceActions(final RemoveTerminatingServiceActionsInput input) {
        LOG.info("remove terminatingServiceActions called with DpnId = {} and serviceId = {}", input.getDpnId(), input.getServiceId());
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        Flow terminatingServiceTableFlow = MDSALUtil.buildFlowNew(NwConstants.INTERNAL_TUNNEL_TABLE,
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE,input.getServiceId()), 5, String.format("%s:%d","ITM Flow Entry ",input.getServiceId()),
                0, 0, ITMConstants.COOKIE_ITM.add(BigInteger.valueOf(input.getServiceId())),getTunnelMatchesForServiceId(input.getServiceId()), null );

        ListenableFuture<Void> installFlowResult = mdsalManager.removeFlow(input.getDpnId(), terminatingServiceTableFlow);
        Futures.addCallback(installFlowResult, new FutureCallback<Void>(){

            @Override
            public void onSuccess(Void aVoid) {
                result.set(RpcResultBuilder.<Void>success().build());
            }

            @Override
            public void onFailure(Throwable error) {
                String msg = String.format("Unable to remove terminating service flow for %s", input.getDpnId());
                LOG.error("remove terminating service actions failed. {}. {}", msg, error);
                result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error).build());
            }
        });
        //result.set(RpcResultBuilder.<Void>success().build());

        return result ;
    }


    public List<MatchInfo> getTunnelMatchesForServiceId(int serviceId) {
        List<MatchInfo> mkMatches = new ArrayList<>();
        byte[] vxLANHeader = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        // Flags Byte
        byte Flags = (byte) 0x08;
        vxLANHeader[0] = Flags;

        // Extract the serviceId details and imprint on the VxLAN Header
        vxLANHeader[4] = (byte) (serviceId >> 16);
        vxLANHeader[5] = (byte) (serviceId >> 8);
        vxLANHeader[6] = (byte) (serviceId >> 0);

        // Matching metadata
        mkMatches.add(new MatchInfo(MatchFieldType.tunnel_id, new BigInteger[]{
                BigInteger.valueOf(serviceId)}));

        return mkMatches;
    }

    private String getFlowRef(long termSvcTable, int svcId) {
        return new StringBuffer().append(termSvcTable).append(svcId).toString();
    }

    @Override
    public Future<RpcResult<GetInternalOrExternalInterfaceNameOutput>> getInternalOrExternalInterfaceName(
            GetInternalOrExternalInterfaceNameInput input) {
        RpcResultBuilder<GetInternalOrExternalInterfaceNameOutput> resultBld = RpcResultBuilder.failed();
        BigInteger srcDpn = input.getSourceDpid() ;
        srcDpn.toString();
        IpAddress dstIp = input.getDestinationIp() ;
        InstanceIdentifier<ExternalTunnel> path1 = InstanceIdentifier.create(
                ExternalTunnelList.class)
                .child(ExternalTunnel.class, new ExternalTunnelKey(String.valueOf(dstIp), srcDpn.toString(), TunnelTypeMplsOverGre.class));

        Optional<ExternalTunnel> ext = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path1, dataBroker);

        if( ext != null && ext.isPresent())
        {
            ExternalTunnel extTunnel = ext.get();
            GetInternalOrExternalInterfaceNameOutputBuilder output = new GetInternalOrExternalInterfaceNameOutputBuilder().setInterfaceName(extTunnel.getTunnelInterfaceName() );
            resultBld = RpcResultBuilder.success();
            resultBld.withResult(output.build()) ;
        } else {
            List<DPNTEPsInfo> meshedDpnList = ItmUtils.getTunnelMeshInfo(dataBroker);
            if(meshedDpnList == null){
                LOG.error("There are no tunnel mesh info in config DS");
                return Futures.immediateFuture(resultBld.build());
            }
            // Look for external tunnels if not look for internal tunnel
            for (DPNTEPsInfo teps : meshedDpnList) {
                TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
                if (dstIp.equals(firstEndPt.getIpAddress())) {
                    InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(
                            TunnelList.class)
                            .child(InternalTunnel.class, new InternalTunnelKey(teps.getDPNID(), srcDpn, input.getTunnelType()));

                    Optional<InternalTunnel> tnl = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
                    if (tnl != null && tnl.isPresent()) {
                        InternalTunnel tunnel = tnl.get();
                        GetInternalOrExternalInterfaceNameOutputBuilder
                                output =
                                new GetInternalOrExternalInterfaceNameOutputBuilder()
                                        .setInterfaceName(tunnel.getTunnelInterfaceName());
                        resultBld = RpcResultBuilder.success();
                        resultBld.withResult(output.build());
                        break;
                    }else{
                        LOG.error("Tunnel not found for source DPN {} ans destination IP {}", srcDpn, dstIp);
                    }
                }
            }
        }
        return Futures.immediateFuture(resultBld.build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>> deleteL2GwDevice(DeleteL2GwDeviceInput input) {
        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        boolean foundVxlanTzone = false;
        try {
            final IpAddress hwIp = input.getIpAddress();
            final String node_id = input.getNodeId();
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, containerPath, dataBroker);
            if (tZonesOptional.isPresent()) {
                TransportZones tZones = tZonesOptional.get();
                if (tZones.getTransportZone() == null || tZones.getTransportZone().isEmpty()) {
                    LOG.error("No teps configured");
                    result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                for (TransportZone tzone : tZones.getTransportZone()) {
                    if (!(tzone.getTunnelType().equals(TunnelTypeVxlan.class)))
                        continue;
                    foundVxlanTzone = true;
                    String transportZone = tzone.getZoneName();
                    if (tzone.getSubnets() == null || tzone.getSubnets().isEmpty()) {
                        result.set(RpcResultBuilder.<Void>failed()
                                .withError(RpcError.ErrorType.APPLICATION, "No subnets Configured").build());
                        return result;
                    }
                    SubnetsKey subnetsKey = tzone.getSubnets().get(0).getKey();
                    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, node_id);
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    WriteTransaction t = dataBroker.newWriteOnlyTransaction();
                    //TO DO: add retry if it fails

                    t.delete(LogicalDatastoreType.CONFIGURATION, path);

                    ListenableFuture<Void> futureCheck = t.submit();
                    Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                        @Override public void onSuccess(Void aVoid) {
                            result.set(RpcResultBuilder.<Void>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to delete HwVtep {} from datastore", node_id);
                            LOG.error("Unable to delete HwVtep {}, {} from datastore", node_id, hwIp);
                            result.set(RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    });

                }
            }
            else {
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No TransportZones configured").build());
                return result;
            }

            if(foundVxlanTzone == false)
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No VxLan TransportZones configured")
                        .build());

            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed().
                    withError(RpcError.ErrorType.APPLICATION, "Deleting l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @Override
    public Future<RpcResult<java.lang.Void>> addL2GwDevice(AddL2GwDeviceInput input) {

        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        boolean foundVxlanTzone = false;
        try {
            final IpAddress hwIp = input.getIpAddress();
            final String node_id = input.getNodeId();
            //iterate through all transport zones and put TORs under vxlan
            //if no vxlan tzone is cnfigured, return an error.
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, containerPath,
                    dataBroker);
            if (tZonesOptional.isPresent()) {
                TransportZones tZones = tZonesOptional.get();
                if (tZones.getTransportZone() == null || tZones.getTransportZone().isEmpty()) {
                    LOG.error("No transportZone configured");
                    result.set(RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "No transportZone Configured").build());
                    return result;
                }
                for (TransportZone tzone : tZones.getTransportZone()) {
                    if (!(tzone.getTunnelType().equals(TunnelTypeVxlan.class)))
                        continue;
                    foundVxlanTzone = true;
                    String transportZone = tzone.getZoneName();
                    if (tzone.getSubnets() == null || tzone.getSubnets().isEmpty()) {
                        result.set(RpcResultBuilder.<Void>failed()
                                .withError(RpcError.ErrorType.APPLICATION, "No subnets Configured").build());
                        return result;
                    }
                    SubnetsKey subnetsKey = tzone.getSubnets().get(0).getKey();
                    DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, node_id);
                    InstanceIdentifier<DeviceVteps> path = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey)
                            .build();
                    DeviceVteps deviceVtep = new DeviceVtepsBuilder().setKey(deviceVtepKey).setIpAddress(hwIp)
                            .setNodeId(node_id).setTopologyId(input.getTopologyId()).build();
                    WriteTransaction t = dataBroker.newWriteOnlyTransaction();
                    //TO DO: add retry if it fails
                    t.put(LogicalDatastoreType.CONFIGURATION, path, deviceVtep, true);

                    ListenableFuture<Void> futureCheck = t.submit();
                    Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                        @Override public void onSuccess(Void aVoid) {
                            result.set(RpcResultBuilder.<Void>success().build());
                        }

                        @Override public void onFailure(Throwable error) {
                            String msg = String.format("Unable to write HwVtep {} to datastore", node_id);
                            LOG.error("Unable to write HwVtep {}, {} to datastore", node_id, hwIp);
                            result.set(RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                        }
                    });

                }
            }
            else {
                result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No TransportZones configured").build());
                return result;
            }

            if(foundVxlanTzone == false)
                result.set(RpcResultBuilder.<Void>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "No VxLan TransportZones configured")
                        .build());

            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed().
                    withError(RpcError.ErrorType.APPLICATION, "Adding l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @Override
    public Future<RpcResult<java.lang.Void>> addL2GwMlagDevice(AddL2GwMlagDeviceInput input)
    {

        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        try {
            final IpAddress hwIp = input.getIpAddress();
            final List<String> node_id = input.getNodeId();
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, containerPath, dataBroker);
            if (tZonesOptional.isPresent()) {
                TransportZones tZones = tZonesOptional.get();
                if (tZones.getTransportZone() == null || tZones.getTransportZone().isEmpty()) {
                    LOG.error("No teps configured");
                    result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                String transportZone = tZones.getTransportZone().get(0).getZoneName();
                if (tZones.getTransportZone().get(0).getSubnets() == null || tZones.getTransportZone().get(0).getSubnets().isEmpty()) {
                    result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No subnets Configured").build());
                    return result;
                }
                SubnetsKey subnetsKey = tZones.getTransportZone().get(0).getSubnets().get(0).getKey();
                DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, node_id.get(0));
                InstanceIdentifier<DeviceVteps> path =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey).build();
                DeviceVteps deviceVtep = new DeviceVtepsBuilder().setKey(deviceVtepKey).setIpAddress(hwIp).setNodeId(node_id.get(0)).setTopologyId(input.getTopologyId()).build();
                WriteTransaction t = dataBroker.newWriteOnlyTransaction();
                //TO DO: add retry if it fails
                LOG.trace("writing hWvtep{}",deviceVtep);
                t.put(LogicalDatastoreType.CONFIGURATION, path, deviceVtep, true);

                if(node_id.size() == 2) {
                    LOG.trace("second node-id {}",node_id.get(1));
                    DeviceVtepsKey deviceVtepKey2 = new DeviceVtepsKey(hwIp, node_id.get(1));
                    InstanceIdentifier<DeviceVteps> path2 = InstanceIdentifier.builder(TransportZones.class)
                            .child(TransportZone.class, new TransportZoneKey(transportZone))
                            .child(Subnets.class, subnetsKey).child(DeviceVteps.class, deviceVtepKey2).build();
                    DeviceVteps deviceVtep2 = new DeviceVtepsBuilder().setKey(deviceVtepKey2).setIpAddress(hwIp).setNodeId(node_id.get(1))
                            .setTopologyId(input.getTopologyId()).build();
                    //TO DO: add retry if it fails
                    LOG.trace("writing {}",deviceVtep2);
                    t.put(LogicalDatastoreType.CONFIGURATION, path2, deviceVtep2, true);
                }ListenableFuture<Void> futureCheck = t.submit();
                Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(Void aVoid) {
                        result.set(RpcResultBuilder.<Void>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to write HwVtep {} to datastore", node_id);
                        LOG.error("Unable to write HwVtep {}, {} to datastore", node_id , hwIp);
                        result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                    }
                });
            }
            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed().
                    withError(RpcError.ErrorType.APPLICATION, "Adding l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }
    @Override
    public Future<RpcResult<Void>> deleteL2GwMlagDevice(DeleteL2GwMlagDeviceInput input) {

        final SettableFuture<RpcResult<Void>> result = SettableFuture.create();
        try {
            final IpAddress hwIp = input.getIpAddress();
            final List<String> node_id = input.getNodeId();
            InstanceIdentifier<TransportZones> containerPath = InstanceIdentifier.create(TransportZones.class);
            Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, containerPath, dataBroker);
            if (tZonesOptional.isPresent()) {
                TransportZones tZones = tZonesOptional.get();
                if (tZones.getTransportZone() == null || tZones.getTransportZone().isEmpty()) {
                    LOG.error("No teps configured");
                    result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No teps Configured").build());
                    return result;
                }
                String transportZone = tZones.getTransportZone().get(0).getZoneName();
                if (tZones.getTransportZone().get(0).getSubnets() == null || tZones.getTransportZone().get(0).getSubnets().isEmpty()) {
                    result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, "No subnets Configured").build());
                    return result;
                }
                SubnetsKey subnetsKey = tZones.getTransportZone().get(0).getSubnets().get(0).getKey();
                DeviceVtepsKey deviceVtepKey = new DeviceVtepsKey(hwIp, node_id.get(0));
                InstanceIdentifier<DeviceVteps> path =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(Subnets.class, subnetsKey).child(DeviceVteps.class,
                                deviceVtepKey).build();
                WriteTransaction t = dataBroker.newWriteOnlyTransaction();
                //TO DO: add retry if it fails
                t.delete(LogicalDatastoreType.CONFIGURATION, path);

                DeviceVtepsKey deviceVtepKey2 = new DeviceVtepsKey(hwIp, node_id.get(1));
                InstanceIdentifier<DeviceVteps> path2 =
                        InstanceIdentifier.builder(TransportZones.class)
                                .child(TransportZone.class, new TransportZoneKey(transportZone))
                                .child(Subnets.class, subnetsKey).child(DeviceVteps.class,
                                deviceVtepKey2).build();
                //TO DO: add retry if it fails
                t.delete(LogicalDatastoreType.CONFIGURATION, path2);

                ListenableFuture<Void> futureCheck = t.submit();
                Futures.addCallback(futureCheck, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(Void aVoid) {
                        result.set(RpcResultBuilder.<Void>success().build());
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        String msg = String.format("Unable to write HwVtep {} to datastore", node_id);
                        LOG.error("Unable to write HwVtep {}, {} to datastore", node_id , hwIp);
                        result.set(RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, msg, error).build());
                    }
                });
            }
            return result;
        } catch (Exception e) {
            RpcResultBuilder<java.lang.Void> resultBuilder = RpcResultBuilder.<Void>failed().
                    withError(RpcError.ErrorType.APPLICATION, "Deleting l2 Gateway to DS Failed", e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    @Override
    public Future<RpcResult<IsTunnelInternalOrExternalOutput>> isTunnelInternalOrExternal(
            IsTunnelInternalOrExternalInput input) {
        RpcResultBuilder<IsTunnelInternalOrExternalOutput> resultBld;
        String tunIfName = input.getTunnelInterfaceName();
        long tunVal = 0;
        IsTunnelInternalOrExternalOutputBuilder output = new IsTunnelInternalOrExternalOutputBuilder().setTunnelType(tunVal);

        if(ItmUtils.itmCache.getInternalTunnel(tunIfName) != null) {
            tunVal = 1;
        } else if (ItmUtils.itmCache.getExternalTunnel(tunIfName) != null) {
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

        if((dcGatewayIpList != null) &&
                (!dcGatewayIpList.isEmpty()) &&
                (dcGatewayIpList.contains(dcgwIpAddr))) {
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

}
