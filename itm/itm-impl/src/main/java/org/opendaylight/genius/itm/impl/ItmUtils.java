/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionPuntToController;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.VtepConfigSchemas;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.VtepIpPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchemaKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.vtep.config.schema.DpnIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.vtep.config.schema.DpnIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.vtep.config.schema.DpnIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.ip.pools.VtepIpPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.ip.pools.VtepIpPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembershipBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TepsNotHostedInTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TepsNotHostedInTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmUtils {

    public static final String DUMMY_IP_ADDRESS = "0.0.0.0";
    public static final String TUNNEL_TYPE_VXLAN = "VXLAN";
    public static final String TUNNEL_TYPE_GRE = "GRE";
    public static final String TUNNEL = "tun";
    public static final IpPrefix DUMMY_IP_PREFIX = new IpPrefix(
        ITMConstants.DUMMY_PREFIX.toCharArray());
    public static ItmCache itmCache = new ItmCache();

    private static final Logger LOG = LoggerFactory.getLogger(ItmUtils.class);

    public static final ImmutableMap<String, Class<? extends TunnelTypeBase>>
        TUNNEL_TYPE_MAP =
        new ImmutableMap.Builder<String, Class<? extends TunnelTypeBase>>()
            .put(ITMConstants.TUNNEL_TYPE_GRE, TunnelTypeGre.class)
            .put(ITMConstants.TUNNEL_TYPE_MPLSoGRE, TunnelTypeMplsOverGre.class)
            .put(ITMConstants.TUNNEL_TYPE_VXLAN, TunnelTypeVxlan.class)
            .build();

    public static final FutureCallback<Void> DEFAULT_CALLBACK = new FutureCallback<Void>() {
        @Override
        public void onSuccess(Void result) {
            LOG.debug("Success in Datastore write operation");
        }

        @Override
        public void onFailure(Throwable error) {
            LOG.error("Error in Datastore write operation", error);
        }
    };

    public static <T extends DataObject> Optional<T> read(LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path, DataBroker broker) {
        try (ReadOnlyTransaction tx = broker.newReadOnlyTransaction()) {
            return tx.read(datastoreType, path).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends DataObject> void asyncWrite(LogicalDatastoreType datastoreType,
                                                         InstanceIdentifier<T> path, T data, DataBroker broker, FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.put(datastoreType, path, data, true);
        Futures.addCallback(tx.submit(), callback);
    }

    public static <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> asyncUpdate(LogicalDatastoreType datastoreType,
                                                                                                           InstanceIdentifier<T> path, T data, DataBroker broker, FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.merge(datastoreType, path, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
        Futures.addCallback(future, callback);
        return future;
    }

    public static <T extends DataObject> void asyncDelete(LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path, DataBroker broker, FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.delete(datastoreType, path);
        Futures.addCallback(tx.submit(), callback);
    }

    public static <T extends DataObject> CheckedFuture<Void, TransactionCommitFailedException> asyncBulkRemove(final DataBroker broker, final LogicalDatastoreType datastoreType,
                                                                                                               List<InstanceIdentifier<T>> pathList, FutureCallback<Void> callback) {
        CheckedFuture<Void, TransactionCommitFailedException> future = null;
        if (!pathList.isEmpty()) {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            for (InstanceIdentifier<T> path : pathList) {
                tx.delete(datastoreType, path);
            }
            future = tx.submit();
            Futures.addCallback(future, callback);
        }
        return future;
    }

    public static String getInterfaceName(final BigInteger datapathid, final String portName, final Integer vlanId) {
        return String.format("%s:%s:%s", datapathid, portName, vlanId);
    }

    public static BigInteger getDpnIdFromInterfaceName(String interfaceName) {
        String[] dpnStr = interfaceName.split(":");
        BigInteger dpnId = new BigInteger(dpnStr[0]);
        return dpnId;
    }

    public static String getTrunkInterfaceName(IdManagerService idManager, String parentInterfaceName,
                                               String localHostName, String remoteHostName, String tunnelType) {
        String tunnelTypeStr;
        if(tunnelType.contains("TunnelTypeGre")) {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_GRE;
        } else {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_VXLAN;
        }
        String trunkInterfaceName = String.format(  "%s:%s:%s:%s", parentInterfaceName, localHostName,
                remoteHostName, tunnelTypeStr);
        LOG.trace("trunk interface name is {}", trunkInterfaceName);
        trunkInterfaceName = String.format("%s%s", TUNNEL, getUniqueIdString(trunkInterfaceName));
        return trunkInterfaceName;
    }

    public static void releaseIdForTrunkInterfaceName(IdManagerService idManager, String parentInterfaceName, String localHostName, String remoteHostName, String tunnelType) {
        String tunnelTypeStr;
        if(tunnelType.contains("TunnelTypeGre")) {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_GRE;
        } else {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_VXLAN;
        }
        String trunkInterfaceName = String.format("%s:%s:%s:%s", parentInterfaceName, localHostName, remoteHostName, tunnelTypeStr);
        LOG.trace("Releasing Id for trunkInterface - {}", trunkInterfaceName );
        //releaseId(idManager, trunkInterfaceName) ;
    }

    public static InetAddress getInetAddressFromIpAddress(IpAddress ip) {
        return InetAddresses.forString(ip.getIpv4Address().getValue());
    }

    public static InstanceIdentifier<DPNTEPsInfo> getDPNTEPInstance(BigInteger dpIdKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<DPNTEPsInfo> dpnTepInfoBuilder =
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class,
                        new DPNTEPsInfoKey(dpIdKey));
        InstanceIdentifier<DPNTEPsInfo> dpnInfo = dpnTepInfoBuilder.build();
        return dpnInfo;
    }

    public static DPNTEPsInfo createDPNTepInfo(BigInteger dpId, List<TunnelEndPoints> endpoints) {

        return new DPNTEPsInfoBuilder().setKey(new DPNTEPsInfoKey(dpId)).setTunnelEndPoints(endpoints).build();
    }

    public static TunnelEndPoints createTunnelEndPoints(BigInteger dpnId, IpAddress ipAddress, String portName,
                                                        boolean isOfTunnel, int vlanId, IpPrefix prefix,
                                                        IpAddress gwAddress, List<TzMembership> zones,
                                                        Class<? extends TunnelTypeBase>  tunnel_type) {
        // when Interface Mgr provides support to take in Dpn Id
        return new TunnelEndPointsBuilder().setKey(new TunnelEndPointsKey(ipAddress, portName,tunnel_type, vlanId))
                .setSubnetMask(prefix).setGwIpAddress(gwAddress).setTzMembership(zones)
                .setOptionOfTunnel(isOfTunnel).setInterfaceName(ItmUtils.getInterfaceName(dpnId, portName, vlanId))
                .setTunnelType(tunnel_type).build();
    }

    public static DpnEndpoints createDpnEndpoints(List<DPNTEPsInfo> dpnTepInfo) {
        return new DpnEndpointsBuilder().setDPNTEPsInfo(dpnTepInfo).build();
    }

    public static InstanceIdentifier<Interface> buildId(String interfaceName) {
        InstanceIdentifierBuilder<Interface> idBuilder =
                InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName));
        InstanceIdentifier<Interface> id = idBuilder.build();
        return id;
    }

    public static Interface buildTunnelInterface(BigInteger dpn, String ifName, String desc, boolean enabled,
                                                 Class<? extends TunnelTypeBase> tunType, IpAddress localIp,
                                                 IpAddress remoteIp, IpAddress gatewayIp, Integer vlanId,
                                                 boolean internal, Boolean monitorEnabled,
                                                 Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                                 Integer monitorInterval, boolean useOfTunnel) {
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dpn).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        Long monitoringInterval=null;
        if( vlanId > 0) {
            IfL2vlan l2vlan = new IfL2vlanBuilder().setVlanId(new VlanId(vlanId)).build();
            builder.addAugmentation(IfL2vlan.class, l2vlan);
        }
        LOG.debug("buildTunnelInterface: monitorProtocol = {} and monitorInterval = {}",monitorProtocol.getName(),monitorInterval);


        if(monitorInterval != null) {
            monitoringInterval = monitorInterval.longValue();
        }

        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(remoteIp).setTunnelGateway(gatewayIp)
                .setTunnelSource(localIp).setTunnelInterfaceType(tunType).setInternal(internal)
                .setMonitorEnabled(monitorEnabled).setMonitorProtocol(monitorProtocol)
                .setMonitorInterval(monitoringInterval).setTunnelRemoteIpFlow(useOfTunnel)
                .build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        return builder.build();
    }

    public static Interface buildHwTunnelInterface(String tunnelIfName, String desc, boolean enabled, String topo_id,
                                                   String node_id, Class<? extends TunnelTypeBase> tunType, IpAddress srcIp, IpAddress destIp,
                                                   IpAddress gWIp, Boolean monitor_enabled, Class<? extends TunnelMonitoringTypeBase> monitorProtocol, Integer monitor_interval){
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(tunnelIfName)).setName(
                tunnelIfName).setDescription(desc).
                setEnabled(enabled).setType(Tunnel.class);
        List<NodeIdentifier> nodeIds = new ArrayList<>();
        NodeIdentifier hWnode = new NodeIdentifierBuilder().setKey(new NodeIdentifierKey(topo_id)).setTopologyId(
                topo_id).
                setNodeId(node_id).build();
        nodeIds.add(hWnode);
        ParentRefs parent = new ParentRefsBuilder().setNodeIdentifier(nodeIds).build();
        builder.addAugmentation(ParentRefs.class, parent);
        Long monitoringInterval = (long) ITMConstants.DEFAULT_MONITOR_INTERVAL;
        Boolean monitoringEnabled = true;
        Class<? extends TunnelMonitoringTypeBase> monitoringProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        if(monitor_interval!= null) {
            monitoringInterval = monitor_interval.longValue();
        }
        if(monitor_enabled!=null  ) {
            monitoringEnabled = monitor_enabled;
        }
        if(monitorProtocol!=null) {
            monitoringProtocol = monitorProtocol;
        }
        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(destIp).setTunnelGateway(gWIp).setTunnelSource(
                srcIp).setMonitorEnabled(monitoringEnabled).setMonitorProtocol(monitorProtocol).setMonitorInterval(100L).
                setTunnelInterfaceType(tunType).setInternal(false).build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        LOG.trace("iftunnel {} built from hwvtep {} ", tunnel, node_id);
        return builder.build();
    }


    public static InternalTunnel buildInternalTunnel( BigInteger srcDpnId, BigInteger dstDpnId,
                                                      Class<? extends TunnelTypeBase> tunType,
                                                      String trunkInterfaceName) {
        InternalTunnel tnl = new InternalTunnelBuilder().setKey(new InternalTunnelKey(dstDpnId, srcDpnId, tunType)).setDestinationDPN(dstDpnId)
                .setSourceDPN(srcDpnId).setTransportType(tunType)
                .setTunnelInterfaceName(trunkInterfaceName).build();
        return tnl ;
    }

    public static ExternalTunnel buildExternalTunnel(String srcNode, String dstNode,
                                                     Class<? extends TunnelTypeBase> tunType,
                                                     String trunkInterfaceName) {
        ExternalTunnel extTnl = new ExternalTunnelBuilder().setKey(
                new ExternalTunnelKey(dstNode, srcNode, tunType))
                .setSourceDevice(srcNode).setDestinationDevice(dstNode)
                .setTunnelInterfaceName(trunkInterfaceName)
                .setTransportType(tunType).build();
        return extTnl ;
    }

    public static List<DPNTEPsInfo> getTunnelMeshInfo(DataBroker dataBroker) {
        List<DPNTEPsInfo> dpnTEPs= null ;

        // Read the Mesh Information from Cache if not read from the DS
        dpnTEPs = getTunnelMeshInfo() ;
        if( dpnTEPs != null ) {
            return dpnTEPs ;
        }

        // Read the EndPoint Info from the operational database
        InstanceIdentifierBuilder<DpnEndpoints> depBuilder = InstanceIdentifier.builder( DpnEndpoints.class);
        InstanceIdentifier<DpnEndpoints> deps = depBuilder.build();
        Optional<DpnEndpoints> dpnEps = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, deps, dataBroker);
        if (dpnEps.isPresent()) {
            DpnEndpoints tn= dpnEps.get() ;
            dpnTEPs = tn.getDPNTEPsInfo();
            LOG.debug( "Read from CONFIGURATION datastore - No. of Dpns " , dpnTEPs.size() );
        } else {
            LOG.debug( "No Dpn information in CONFIGURATION datastore "  );
        }
        return dpnTEPs ;
    }

    // Reading the Mesh Information from Cache
    public static List<DPNTEPsInfo> getTunnelMeshInfo(){
        List<DPNTEPsInfo> dpnTepsInfo = null ;
        List<Object> values = null ;

        values = DataStoreCache.getValues(ITMConstants.DPN_TEPs_Info_CACHE_NAME);
        if( values != null ) {
            dpnTepsInfo = new ArrayList<>() ;
            for( Object value : values ) {
                dpnTepsInfo.add((DPNTEPsInfo)value) ;
            }
        }
        return dpnTepsInfo ;
    }

    public static int getUniqueId(IdManagerService idManager, String idKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setIdKey(idKey).build();

        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            if(rpcResult.isSuccessful()) {
                return rpcResult.getResult().getIdValue().intValue();
            } else {
                LOG.warn("RPC Call to Get Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id",e);
        }
        return 0;
    }

    public static String getUniqueIdString(String idKey) {
        return UUID.nameUUIDFromBytes(idKey.getBytes()).toString().substring(0, 12).replace("-", "");
    }

    public static void releaseId(IdManagerService idManager, String idKey) {
        ReleaseIdInput idInput =
                new ReleaseIdInputBuilder().setPoolName(ITMConstants.ITM_IDPOOL_NAME).setIdKey(idKey).build();
        try {
            Future<RpcResult<Void>> result = idManager.releaseId(idInput);
            RpcResult<Void> rpcResult = result.get();
            if(!rpcResult.isSuccessful()) {
                LOG.warn("RPC Call to Get Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id for key {}", idKey, e);
        }
    }

    public static List<DPNTEPsInfo> getDPNTEPListFromDPNId(DataBroker dataBroker, List<BigInteger> dpnIds) {
        List<DPNTEPsInfo> meshedDpnList = getTunnelMeshInfo(dataBroker) ;
        List<DPNTEPsInfo> cfgDpnList = new ArrayList<>();
        if( null != meshedDpnList) {
            for(BigInteger dpnId : dpnIds) {
                for( DPNTEPsInfo teps : meshedDpnList ) {
                    if( dpnId.equals(teps.getDPNID())) {
                        cfgDpnList.add( teps) ;
                    }
                }
            }
        }
        return cfgDpnList;
    }

    public static void setUpOrRemoveTerminatingServiceTable(BigInteger dpnId, IMdsalApiManager mdsalManager, boolean addFlag) {
        String logmsg = addFlag ? "Installing" : "Removing";
        LOG.trace( logmsg + " PUNT to Controller flow in DPN {} ", dpnId );
        List<ActionInfo> listActionInfo = new ArrayList<>();
        listActionInfo.add(new ActionPuntToController());

        try {
            List<MatchInfo> mkMatches = new ArrayList<>();

            mkMatches.add(new MatchTunnelId(BigInteger.valueOf(ITMConstants.LLDP_SERVICE_ID)));

            List<InstructionInfo> mkInstructions = new ArrayList<>();
            mkInstructions.add(new InstructionApplyActions(listActionInfo));

            FlowEntity terminatingServiceTableFlowEntity = MDSALUtil
                    .buildFlowEntity(
                            dpnId,
                            NwConstants.INTERNAL_TUNNEL_TABLE,
                            getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE,
                                    ITMConstants.LLDP_SERVICE_ID), 5, String.format("%s:%d","ITM Flow Entry ",ITMConstants.LLDP_SERVICE_ID),
                            0, 0, ITMConstants.COOKIE_ITM
                                    .add(BigInteger.valueOf(ITMConstants.LLDP_SERVICE_ID)),
                            mkMatches, mkInstructions);
            if(addFlag) {
                mdsalManager.installFlow(terminatingServiceTableFlowEntity);
            } else {
                mdsalManager.removeFlow(terminatingServiceTableFlowEntity);
            }
        } catch (Exception e) {
            LOG.error("Error while setting up Table 36 for {}", dpnId, e);
        }
    }

    private static String getFlowRef(long termSvcTable, int svcId) {
        return String.valueOf(termSvcTable) + svcId;
    }
    public static InstanceIdentifier<VtepConfigSchema> getVtepConfigSchemaIdentifier(String schemaName) {
        return InstanceIdentifier.builder(VtepConfigSchemas.class)
                .child(VtepConfigSchema.class, new VtepConfigSchemaKey(schemaName)).build();
    }

    public static InstanceIdentifier<VtepConfigSchema> getVtepConfigSchemaIdentifier() {
        return InstanceIdentifier.builder(VtepConfigSchemas.class).child(VtepConfigSchema.class).build();
    }
    public static InstanceIdentifier<VtepConfigSchemas> getVtepConfigSchemasIdentifier() {
        return InstanceIdentifier.builder(VtepConfigSchemas.class).build();
    }
    public static InstanceIdentifier<VtepIpPool> getVtepIpPoolIdentifier(String subnetCidr) {
        return InstanceIdentifier.builder(VtepIpPools.class).child(VtepIpPool.class, new VtepIpPoolKey(subnetCidr))
                .build();
    }
    public static VtepConfigSchema validateForAddVtepConfigSchema(VtepConfigSchema schema,
                                                                  List<VtepConfigSchema> existingSchemas) {
        VtepConfigSchema validSchema = validateVtepConfigSchema(schema);
        for (VtepConfigSchema existingSchema : emptyIfNull(existingSchemas)) {
            if (!StringUtils.equalsIgnoreCase(schema.getSchemaName(), existingSchema.getSchemaName())
                    && schema.getSubnet().equals(existingSchema.getSubnet())) {
                String subnetCidr = getSubnetCidrAsString(schema.getSubnet());
                Preconditions.checkArgument(false, "VTEP schema with subnet [" + subnetCidr +
                        "] already exists. Multiple VTEP schemas with same subnet is not allowed.");
            }
        }
        if (isNotEmpty(getDpnIdList(validSchema.getDpnIds()))) {
            String tzone = validSchema.getTransportZoneName();
            List<BigInteger> lstDpns = getConflictingDpnsAlreadyConfiguredWithTz(validSchema.getSchemaName(), tzone,
                    getDpnIdList(validSchema.getDpnIds()), existingSchemas);
            if (!lstDpns.isEmpty()) {
                Preconditions.checkArgument(false,
                        "DPN's " + lstDpns + " already configured for transport zone " +
                                tzone + ". Only one end point per transport Zone per Dpn is allowed.");
            }
            if (schema.getTunnelType().equals(TunnelTypeGre.class)){
                validateForSingleGreTep(validSchema.getSchemaName(), getDpnIdList(validSchema.getDpnIds()), existingSchemas);
            }
        }
        return validSchema;
    }
    private static void validateForSingleGreTep(String schemaName, List<BigInteger> lstDpnsForAdd,
                                                List<VtepConfigSchema> existingSchemas) {
        for (VtepConfigSchema existingSchema : emptyIfNull(existingSchemas)) {
            if (TunnelTypeGre.class.equals(existingSchema.getTunnelType())
                    && !StringUtils.equalsIgnoreCase(schemaName, existingSchema.getSchemaName())) {
                List<BigInteger> lstConflictingDpns = new ArrayList<>(getDpnIdList(existingSchema.getDpnIds()));
                lstConflictingDpns.retainAll(emptyIfNull(lstDpnsForAdd));
                if (!lstConflictingDpns.isEmpty()) {
                    String errMsg = "DPN's " + lstConflictingDpns +
                            " already configured with GRE TEP. Mutiple GRE TEP's on a single DPN are not allowed.";
                    Preconditions.checkArgument(false, errMsg);
                }
            }
        }
    }
    public static VtepConfigSchema validateVtepConfigSchema(VtepConfigSchema schema) {
        Preconditions.checkNotNull(schema);
        Preconditions.checkArgument(StringUtils.isNotBlank(schema.getSchemaName()));
        Preconditions.checkArgument(StringUtils.isNotBlank(schema.getPortName()));
        Preconditions.checkArgument(schema.getVlanId() >= 0 && schema.getVlanId() < 4095,
                "Invalid VLAN ID, range (0-4094)");
        Preconditions.checkArgument(StringUtils.isNotBlank(schema.getTransportZoneName()));
        Preconditions.checkNotNull(schema.getSubnet());
        String subnetCidr = getSubnetCidrAsString(schema.getSubnet());
        SubnetUtils subnetUtils = new SubnetUtils(subnetCidr);
        IpAddress gatewayIp = schema.getGatewayIp();
        if (gatewayIp != null) {
            String strGatewayIp = String.valueOf(gatewayIp.getValue());
            if (!strGatewayIp.equals(ITMConstants.DUMMY_IP_ADDRESS) && !subnetUtils.getInfo().isInRange(strGatewayIp)) {
                Preconditions.checkArgument(false, "Gateway IP address " + strGatewayIp +
                        " is not in subnet range " + subnetCidr);
            }
        }
        ItmUtils.getExcludeIpAddresses(schema.getExcludeIpFilter(), subnetUtils.getInfo());
        return new VtepConfigSchemaBuilder(schema).setTunnelType(schema.getTunnelType()).build();
    }
    public static String validateTunnelType(String tunnelType) {
        if (tunnelType == null) {
            tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
        } else {
            tunnelType = StringUtils.upperCase(tunnelType);
            String error = "Invalid tunnel type. Valid values: " +
                    ITMConstants.TUNNEL_TYPE_VXLAN + " | " + ITMConstants.TUNNEL_TYPE_GRE;
            Preconditions.checkArgument(ITMConstants.TUNNEL_TYPE_VXLAN.equals(tunnelType)
                    || ITMConstants.TUNNEL_TYPE_GRE.equals(tunnelType), error);
        }
        return tunnelType;
    }
    private static List<BigInteger> getConflictingDpnsAlreadyConfiguredWithTz(String schemaName, String tzone,
                                                                              List<BigInteger> lstDpns, List<VtepConfigSchema> existingSchemas) {
        List<BigInteger> lstConflictingDpns = new ArrayList<>();
        for (VtepConfigSchema schema : emptyIfNull(existingSchemas)) {
            if (!StringUtils.equalsIgnoreCase(schemaName, schema.getSchemaName())
                    && StringUtils.equals(schema.getTransportZoneName(), tzone)) {
                lstConflictingDpns = new ArrayList<>(getDpnIdList(schema.getDpnIds()));
                lstConflictingDpns.retainAll(lstDpns);
                if (!lstConflictingDpns.isEmpty()) {
                    break;
                }
            }
        }
        return lstConflictingDpns;
    }
    public static VtepConfigSchema constructVtepConfigSchema(String schemaName, String portName, Integer vlanId,
                                                             String subnetMask, String gatewayIp, String transportZone,String tunnelType, List<BigInteger> dpnIds,
                                                             String excludeIpFilter) {
        IpAddress gatewayIpObj = StringUtils.isBlank(gatewayIp) ? null : new IpAddress(gatewayIp.toCharArray());
        IpPrefix subnet = StringUtils.isBlank(subnetMask) ? null : new IpPrefix(subnetMask.toCharArray());
        Class<? extends TunnelTypeBase> tunType ;
        if( tunnelType.equals(ITMConstants.TUNNEL_TYPE_VXLAN)) {
            tunType = TunnelTypeVxlan.class ;
        } else {
            tunType = TunnelTypeGre.class ;
        }
        VtepConfigSchemaBuilder schemaBuilder = new VtepConfigSchemaBuilder().setSchemaName(schemaName)
                .setPortName(portName).setVlanId(vlanId).setSubnet(subnet).setGatewayIp(gatewayIpObj)
                .setTransportZoneName(transportZone).setTunnelType(tunType).setDpnIds(getDpnIdsListFromBigInt(dpnIds))
                .setExcludeIpFilter(excludeIpFilter);
        return schemaBuilder.build();
    }
    public static List<IpAddress> getExcludeIpAddresses(String excludeIpFilter, SubnetInfo subnetInfo) {
        final List<IpAddress> lstIpAddress = new ArrayList<>();
        if (StringUtils.isBlank(excludeIpFilter)) {
            return lstIpAddress;
        }
        final String[] arrIps = StringUtils.split(excludeIpFilter, ',');
        for (String ip : arrIps) {
            if (StringUtils.countMatches(ip, "-") == 1) {
                final String[] arrIpRange = StringUtils.split(ip, '-');
                String strStartIp = StringUtils.trim(arrIpRange[0]);
                String strEndIp = StringUtils.trim(arrIpRange[1]);
                Preconditions.checkArgument(InetAddresses.isInetAddress(strStartIp),
                        "Invalid exclude IP filter: invalid IP address value " + strStartIp);
                Preconditions.checkArgument(InetAddresses.isInetAddress(strEndIp),
                        "Invalid exclude IP filter: invalid IP address value " + strEndIp);
                Preconditions.checkArgument(subnetInfo.isInRange(strStartIp),
                        "Invalid exclude IP filter: IP address [" + strStartIp +
                                "] not in subnet range " + subnetInfo.getCidrSignature());
                Preconditions.checkArgument(subnetInfo.isInRange(strEndIp),
                        "Invalid exclude IP filter: IP address [" + strEndIp +
                                "] not in subnet range " + subnetInfo.getCidrSignature());
                int startIp = subnetInfo.asInteger(strStartIp);
                int endIp = subnetInfo.asInteger(strEndIp);

                Preconditions.checkArgument(startIp < endIp,
                        "Invalid exclude IP filter: Invalid range [" + ip + "] ");
                for (int i = startIp; i <= endIp; i++) {
                    String ipAddress = ipFormat(toIpArray(i));
                    validateAndAddIpAddressToList(subnetInfo, lstIpAddress, ipAddress);
                }
            } else {
                validateAndAddIpAddressToList(subnetInfo, lstIpAddress, ip);
            }
        }
        return lstIpAddress;
    }
    private static void validateAndAddIpAddressToList(SubnetInfo subnetInfo, final List<IpAddress> lstIpAddress,
                                                      String ipAddress) {
        String ip = StringUtils.trim(ipAddress);
        Preconditions.checkArgument(InetAddresses.isInetAddress(ip),
                "Invalid exclude IP filter: invalid IP address value " + ip);
        Preconditions.checkArgument(subnetInfo.isInRange(ip),
                "Invalid exclude IP filter: IP address [" + ip + "] not in subnet range " +
                        subnetInfo.getCidrSignature());
        lstIpAddress.add(new IpAddress(ip.toCharArray()));
    }
    private static int[] toIpArray(int val) {
        int[] ret = new int[4];
        for (int j = 3; j >= 0; --j) {
            ret[j] |= val >>> 8 * (3 - j) & 0xff;
        }
        return ret;
    }
    private static String ipFormat(int[] octets) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < octets.length; ++i) {
            str.append(octets[i]);
            if (i != octets.length - 1) {
                str.append(".");
            }
        }
        return str.toString();
    }
    public static VtepConfigSchema validateForUpdateVtepSchema(String schemaName, List<BigInteger> lstDpnsForAdd,
                                                               List<BigInteger> lstDpnsForDelete, IITMProvider itmProvider) {
        Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));
        if ((lstDpnsForAdd == null || lstDpnsForAdd.isEmpty())
                && (lstDpnsForDelete == null || lstDpnsForDelete.isEmpty())) {
            Preconditions.checkArgument(false,
                    "DPN ID list for add | delete is null or empty in schema " + schemaName);
        }
        VtepConfigSchema schema = itmProvider.getVtepConfigSchema(schemaName);
        if (schema == null) {
            Preconditions.checkArgument(false, "Specified VTEP Schema [" + schemaName +
                    "] doesn't exists!");
        }
        List<BigInteger> existingDpnIds = getDpnIdList(schema.getDpnIds());
        if (isNotEmpty(lstDpnsForAdd)) {
            //  if (isNotEmpty(existingDpnIds)) {
            List<BigInteger> lstAlreadyExistingDpns = new ArrayList<>(existingDpnIds);
            lstAlreadyExistingDpns.retainAll(lstDpnsForAdd);
            Preconditions.checkArgument(lstAlreadyExistingDpns.isEmpty(),
                    "DPN ID's " + lstAlreadyExistingDpns +
                            " already exists in VTEP schema [" + schemaName + "]");
            //    }
            if (schema.getTunnelType().equals(TunnelTypeGre.class)) {
                validateForSingleGreTep(schema.getSchemaName(), lstDpnsForAdd, itmProvider.getAllVtepConfigSchemas());
            }
        }
        if (isNotEmpty(lstDpnsForDelete)) {
            if (existingDpnIds == null || existingDpnIds.isEmpty()) {
                String builder = "DPN ID's " + lstDpnsForDelete +
                        " specified for delete from VTEP schema [" + schemaName +
                        "] are not configured in the schema.";
                Preconditions.checkArgument(false, builder);
            } else if (!existingDpnIds.containsAll(lstDpnsForDelete)) {
                List<BigInteger> lstConflictingDpns = new ArrayList<>(lstDpnsForDelete);
                lstConflictingDpns.removeAll(existingDpnIds);
                String builder = "DPN ID's " + lstConflictingDpns +
                        " specified for delete from VTEP schema [" + schemaName +
                        "] are not configured in the schema.";
                Preconditions.checkArgument(false, builder);
            }
        }
        return schema;
    }
    public static String getSubnetCidrAsString(IpPrefix subnet) {
        return subnet == null ? StringUtils.EMPTY : String.valueOf(subnet.getValue());
    }
    public static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }
    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }
    public static HwVtep createHwVtepObject(String topo_id, String node_id, IpAddress ipAddress, IpPrefix ipPrefix, IpAddress gatewayIP, int vlanID, Class<? extends TunnelTypeBase> tunnel_type, TransportZone transportZone) {
        HwVtep hwVtep = new HwVtep();
        hwVtep.setGatewayIP(gatewayIP);
        hwVtep.setHwIp(ipAddress);
        hwVtep.setIpPrefix(ipPrefix);
        hwVtep.setNode_id(node_id);
        hwVtep.setTopo_id(topo_id);
        hwVtep.setTransportZone(transportZone.getZoneName());
        hwVtep.setTunnel_type(tunnel_type);
        hwVtep.setVlanID(vlanID);
        return hwVtep;
    }

    public static String getHwParentIf(String topo_id, String srcNodeid) {
        return String.format("%s:%s", topo_id, srcNodeid);
    }

    public static <T extends DataObject> void syncWrite(LogicalDatastoreType datastoreType,
                                                        InstanceIdentifier<T> path, T data, DataBroker broker) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.put(datastoreType, path, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("ITMUtils:SyncWrite , Error writing to datastore (path, data) : ({}, {})", path, data);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static List<BigInteger> getDpnIdList( List<DpnIds> dpnIds ) {
        List<BigInteger> dpnList = new ArrayList<>() ;
        for( DpnIds dpn : dpnIds) {
            dpnList.add(dpn.getDPN()) ;
        }
        return dpnList ;
    }

    public static List<DpnIds> getDpnIdsListFromBigInt( List<BigInteger> dpnIds) {
        List<DpnIds> dpnIdList = new ArrayList<>() ;
        DpnIdsBuilder builder = new DpnIdsBuilder() ;
        for( BigInteger dpnId : dpnIds) {
            dpnIdList.add(builder.setKey(new DpnIdsKey(dpnId)).setDPN(dpnId).build() );
        }
        return dpnIdList;
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> buildStateInterfaceId(String interfaceName) {
        InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> idBuilder =
                InstanceIdentifier.builder(InterfacesState.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class,
                                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                                        interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> id = idBuilder.build();
        return id;
    }
    public static Boolean readMonitoringStateFromCache(DataBroker dataBroker) {
        InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.create(TunnelMonitorParams.class);
        TunnelMonitorParams tunnelMonitorParams = (TunnelMonitorParams) DataStoreCache.get(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME,iid,"MonitorParams",dataBroker,true);
        if(tunnelMonitorParams!=null) {
            return tunnelMonitorParams.isEnabled();
        } else {
            return ITMConstants.DEFAULT_MONITOR_ENABLED;
        }
    }

    public static Integer readMonitorIntervalfromCache(DataBroker dataBroker) {
        InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.create(TunnelMonitorInterval.class);
        TunnelMonitorInterval tunnelMonitorIOptional = (TunnelMonitorInterval)DataStoreCache.get(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME,iid,"Interval",dataBroker,true);
        if(tunnelMonitorIOptional!=null) {
            return tunnelMonitorIOptional.getInterval();
        }
        return null;

    }

    public static Integer determineMonitorInterval(DataBroker dataBroker) {
        Integer monitorInterval = ItmUtils.readMonitorIntervalfromCache(dataBroker);
        LOG.debug("determineMonitorInterval: monitorInterval from DS = {}", monitorInterval);
        if(monitorInterval==null){
            Class<? extends TunnelMonitoringTypeBase> monitorProtocol = determineMonitorProtocol(dataBroker);
            if(monitorProtocol.isAssignableFrom(TunnelMonitoringTypeBfd.class)) {
                monitorInterval = ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL;
            } else {
                monitorInterval = ITMConstants.DEFAULT_MONITOR_INTERVAL;
            }
        }
        LOG.debug("determineMonitorInterval: monitorInterval = {}", monitorInterval);
        InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        TunnelMonitorInterval intervalBuilder = new TunnelMonitorIntervalBuilder().setInterval(monitorInterval).build();
        ItmUtils.asyncUpdate(LogicalDatastoreType.OPERATIONAL,iid, intervalBuilder, dataBroker, ItmUtils.DEFAULT_CALLBACK);
        return monitorInterval;
    }

    public static List<String> getInternalTunnelInterfaces(DataBroker dataBroker){
        List<String> tunnelList = new ArrayList<>();
        Collection<String> internalInterfaces = itmCache.getAllInternalInterfaces();
        if(internalInterfaces == null) {
            updateTunnelsCache(dataBroker);
            internalInterfaces = itmCache.getAllInternalInterfaces();
        }
        LOG.debug("ItmUtils.getTunnelList Cache Internal Interfaces size: {} ", internalInterfaces.size());
        if(internalInterfaces!=null) {
            tunnelList.addAll(internalInterfaces);
        }
        LOG.trace("ItmUtils.getTunnelList Internal: {}", tunnelList);
        return tunnelList;
    }

    public static List<String> getTunnelsofTzone(List<HwVtep> hwVteps, String tzone, DataBroker dataBroker, Boolean hwVtepsExist) {

        List<String> tunnels = new ArrayList<>();
        InstanceIdentifier<TransportZone> path = InstanceIdentifier.builder(TransportZones.class).
                child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        if (tZoneOptional.isPresent()) {
            TransportZone transportZone = tZoneOptional.get();
            Class<? extends TunnelTypeBase> tunType = transportZone.getTunnelType();
            if (transportZone.getSubnets() != null && !transportZone.getSubnets().isEmpty()) {
                for (Subnets sub : transportZone.getSubnets()) {
                    if (sub.getVteps() != null && !sub.getVteps().isEmpty()) {
                        for (Vteps vtepLocal : sub.getVteps()) {
                            for (Vteps vtepRemote : sub.getVteps()) {
                                if (!vtepLocal.equals(vtepRemote)) {
                                    InternalTunnelKey key = new InternalTunnelKey(vtepRemote.getDpnId(), vtepLocal.getDpnId(), tunType);
                                    InstanceIdentifier<InternalTunnel> intIID =
                                            InstanceIdentifier.builder(TunnelList.class).
                                                    child(InternalTunnel.class, key).build();
                                    Optional<InternalTunnel> TunnelsOptional =
                                            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, intIID, dataBroker);
                                    if (TunnelsOptional.isPresent()) {
                                        String tunnelInterfaceName = TunnelsOptional.get().getTunnelInterfaceName();
                                        LOG.trace("Internal Tunnel added {}", tunnelInterfaceName);
                                        tunnels.add(tunnelInterfaceName);
                                    }
                                }
                            }
                            if(hwVteps!= null && !hwVteps.isEmpty()) {
                                for (HwVtep hwVtep : hwVteps) {
                                    tunnels.add(getExtTunnel(hwVtep.getNode_id(), vtepLocal.getDpnId().toString(),
                                            tunType, dataBroker));
                                    tunnels.add(getExtTunnel(vtepLocal.getDpnId().toString(), hwVtep.getNode_id(),
                                            tunType, dataBroker));
                                }
                            }
                        }
                    }
                }
            }
            if (hwVtepsExist) {
                for (HwVtep hwVtep : hwVteps) {
                    for (HwVtep hwVtepOther : hwVteps) {
                        if (!hwVtep.getHwIp().equals(hwVtepOther.getHwIp())) {
                            tunnels.add(getExtTunnel(hwVtep.getNode_id(), hwVtepOther.getNode_id(), tunType, dataBroker));
                            tunnels.add(getExtTunnel(hwVtepOther.getNode_id(), hwVtep.getNode_id(), tunType, dataBroker));
                        }
                    }
                }
            }
        }
        return tunnels;
    }

    public static List<String> getInternalTunnelsofTzone(String tzone, DataBroker dataBroker) {
        List<String> tunnels = new ArrayList<>();
        LOG.trace("Getting internal tunnels of {}",tzone);
        InstanceIdentifier<TransportZone> path = InstanceIdentifier.builder(TransportZones.class).
                child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        if (tZoneOptional.isPresent()) {
            TransportZone transportZone = tZoneOptional.get();
            if (transportZone.getSubnets() != null && !transportZone.getSubnets().isEmpty()) {
                for (Subnets sub : transportZone.getSubnets()) {
                    if (sub.getVteps() != null && !sub.getVteps().isEmpty()) {
                        for (Vteps vtepLocal : sub.getVteps()) {
                            for (Vteps vtepRemote : sub.getVteps()) {
                                if (!vtepLocal.equals(vtepRemote)) {
                                    InternalTunnelKey key =
                                            new InternalTunnelKey(vtepRemote.getDpnId(), vtepLocal.getDpnId(),
                                                    transportZone.getTunnelType());
                                    InstanceIdentifier<InternalTunnel> intIID =
                                            InstanceIdentifier.builder(TunnelList.class).
                                                    child(InternalTunnel.class, key).build();
                                    Optional<InternalTunnel> tunnelsOptional =
                                            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, intIID, dataBroker);
                                    if (tunnelsOptional.isPresent()) {
                                        String tunnelInterfaceName = tunnelsOptional.get().getTunnelInterfaceName();
                                        LOG.trace("Internal Tunnel added {}", tunnelInterfaceName);
                                        tunnels.add(tunnelInterfaceName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return tunnels;
    }

    private static String getExtTunnel(String node_id, String dpId,Class<? extends TunnelTypeBase> tunType, DataBroker dataBroker) {
        LOG.trace("getting ext tunnel for {} and dpId {}",node_id,dpId);
        ExternalTunnelKey key = getExternalTunnelKey(dpId, node_id, tunType);
        InstanceIdentifier<ExternalTunnel> intIID = InstanceIdentifier.builder(ExternalTunnelList.class).
                child(ExternalTunnel.class, key).build();
        Optional<ExternalTunnel> tunnelsOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, intIID, dataBroker);
        if (tunnelsOptional.isPresent()) {
            String tunnelInterfaceName = tunnelsOptional.get().getTunnelInterfaceName();
            LOG.trace("ext tunnel returned {} ", tunnelInterfaceName);
            return tunnelInterfaceName;
        }
        return null;
    }
    public static ExternalTunnelKey getExternalTunnelKey(String dst , String src, Class<? extends TunnelTypeBase> tunType) {
        if (src.indexOf("physicalswitch") > 0) {
            src = src.substring(0, src.indexOf("physicalswitch") - 1);
        }
        if (dst.indexOf("physicalswitch") > 0) {
            dst = dst.substring(0, dst.indexOf("physicalswitch") - 1);
        }
        return new ExternalTunnelKey(dst, src, tunType);
    }

    public static List<TunnelEndPoints> getTEPsForDpn( BigInteger srcDpn, List<DPNTEPsInfo> dpnList) {
        for (DPNTEPsInfo dpn : dpnList) {
            if( dpn.getDPNID().equals(srcDpn)) {
                return dpn.getTunnelEndPoints() ;
            }
        }
        return null ;
    }
    public static TunnelList getAllInternalTunnels(DataBroker broker) {
        InstanceIdentifier<TunnelList> tunnelListInstanceIdentifier = InstanceIdentifier.builder(TunnelList.class).build();
        return read(LogicalDatastoreType.CONFIGURATION, tunnelListInstanceIdentifier, broker).orNull();
    }
    public static InternalTunnel getInternalTunnel(String interfaceName, DataBroker broker) {
        InternalTunnel internalTunnel = null;
        TunnelList tunnelList = getAllInternalTunnels(broker);
        if (tunnelList != null && tunnelList.getInternalTunnel() != null) {
            List<InternalTunnel> internalTunnels = tunnelList.getInternalTunnel();
            for (InternalTunnel tunnel : internalTunnels) {
                if (tunnel.getTunnelInterfaceName().equalsIgnoreCase(interfaceName)) {
                    internalTunnel = tunnel;
                    break;
                }
            }
        }
        return internalTunnel;
    }
    public static ExternalTunnel getExternalTunnel(String interfaceName, DataBroker broker) {
        ExternalTunnel externalTunnel = null;
        List<ExternalTunnel> externalTunnels = getAllExternalTunnels(broker);
        for (ExternalTunnel tunnel : externalTunnels) {
            if (StringUtils.equalsIgnoreCase(interfaceName, tunnel.getTunnelInterfaceName())) {
                externalTunnel = tunnel;
                break;
            }
        }
        return externalTunnel;
    }
    public static List<ExternalTunnel> getAllExternalTunnels(DataBroker broker) {
        List<ExternalTunnel> result = null;
        InstanceIdentifier<ExternalTunnelList> id = InstanceIdentifier.builder(ExternalTunnelList.class).build();
        Optional<ExternalTunnelList> tunnelList = read(LogicalDatastoreType.CONFIGURATION, id, broker);
        if (tunnelList.isPresent()) {
            result = tunnelList.get().getExternalTunnel();
        }
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }
    public static String convertTunnelTypetoString(Class<? extends TunnelTypeBase> tunType ) {
        String tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
        if( tunType.equals(TunnelTypeVxlan.class)) {
            tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN ;
        } else if( tunType.equals(TunnelTypeGre.class) ) {
            tunnelType = ITMConstants.TUNNEL_TYPE_GRE ;
        } else if (tunnelType.equals(TunnelTypeMplsOverGre.class)) {
            tunnelType = ITMConstants.TUNNEL_TYPE_MPLSoGRE;
        }
        return tunnelType ;
    }


    public static boolean isItmIfType(Class<? extends InterfaceType> ifType) {
        return ifType != null && ifType.isAssignableFrom(Tunnel.class);
    }

    public static StateTunnelListKey getTunnelStateKey( org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface iface) {
        StateTunnelListKey key = null;
        if(isItmIfType(iface.getType())) {
            key = new StateTunnelListKey(iface.getName());
        }
        return key;
    }

    public static void updateTunnelsCache(DataBroker broker) {
        List<InternalTunnel> internalTunnels = getAllInternalTunnels(broker, LogicalDatastoreType.CONFIGURATION);
        for (InternalTunnel tunnel : internalTunnels) {
            itmCache.addInternalTunnel(tunnel);
        }
        List<ExternalTunnel> externalTunnels = getAllExternalTunnels(broker, LogicalDatastoreType.CONFIGURATION);
        for (ExternalTunnel tunnel : externalTunnels) {
            itmCache.addExternalTunnel(tunnel);
        }
    }

    public static List<ExternalTunnel> getAllExternalTunnels(DataBroker dataBroker, LogicalDatastoreType datastoreType) {
        List<ExternalTunnel> result = null;
        InstanceIdentifier<ExternalTunnelList> iid = InstanceIdentifier.builder(ExternalTunnelList.class).build();
        Optional<ExternalTunnelList> tunnelList = read(LogicalDatastoreType.CONFIGURATION, iid, dataBroker);
        if (tunnelList.isPresent()) {
            result = tunnelList.get().getExternalTunnel();
        }
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    public static List<InternalTunnel> getAllInternalTunnels(DataBroker dataBroker, LogicalDatastoreType datastoreType) {
        List<InternalTunnel> result = null;
        InstanceIdentifier<TunnelList> iid = InstanceIdentifier.builder(TunnelList.class).build();
        Optional<TunnelList> tunnelList = read(LogicalDatastoreType.CONFIGURATION, iid, dataBroker);
        if (tunnelList.isPresent()) {
            result = tunnelList.get().getInternalTunnel();
        }
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    public static Interface getInterface(
            String name, DataBroker broker) {
        Interface result = itmCache.getInterface(name);
        if (result == null) {
            InstanceIdentifier<Interface> iid =
                    InstanceIdentifier.builder(Interfaces.class)
                            .child(Interface.class, new InterfaceKey(name)).build();
            Optional<Interface> optInterface = read(LogicalDatastoreType.CONFIGURATION, iid, broker);
            if (optInterface.isPresent()) {
                result = optInterface.get();
                itmCache.addInterface(result);
            }
        }
        return result;
    }

    public static Class<? extends TunnelMonitoringTypeBase> readMonitoringProtocolFromCache(DataBroker dataBroker) {
        InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.create(TunnelMonitorParams.class);
        TunnelMonitorParams tunnelMonitorParams = (TunnelMonitorParams) DataStoreCache.get(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME,iid,"MonitorParams",dataBroker,true);
        if(tunnelMonitorParams!=null) {
            return tunnelMonitorParams.getMonitorProtocol();
        }
        return null;
    }

    public static Class<? extends TunnelMonitoringTypeBase> determineMonitorProtocol(DataBroker dataBroker) {
        Class<? extends TunnelMonitoringTypeBase> monitoringProtocol = ItmUtils.readMonitoringProtocolFromCache(dataBroker);
        LOG.debug("determineMonitorProtocol: monitorProtocol from DS = {}", monitoringProtocol);
        if(monitoringProtocol==null) {
            monitoringProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;
        }
        LOG.debug("determineMonitorProtocol: monitorProtocol = {}", monitoringProtocol);
        Boolean monitorState = ItmUtils.readMonitoringStateFromCache(dataBroker);
        if(monitorState==null) {
            monitorState = true;
        }
        LOG.debug("determineMonitorProtocol: monitorState = {}", monitorState);
        InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        TunnelMonitorParams protocolBuilder = new TunnelMonitorParamsBuilder().setEnabled(monitorState).setMonitorProtocol(monitoringProtocol).build();
        ItmUtils.asyncUpdate(LogicalDatastoreType.OPERATIONAL,iid, protocolBuilder, dataBroker, ItmUtils.DEFAULT_CALLBACK);
        return monitoringProtocol;
    }

    public static List<DcGatewayIp> getDcGatewayIpList(DataBroker broker){
        InstanceIdentifier<DcGatewayIpList> dcGatewayIpListid = InstanceIdentifier.builder(DcGatewayIpList.class).build();
        Optional<DcGatewayIpList> dcGatewayIpListConfig = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, dcGatewayIpListid, broker);
        if(dcGatewayIpListConfig.isPresent()){
            DcGatewayIpList containerList = dcGatewayIpListConfig.get();
            if(containerList != null){
                return containerList.getDcGatewayIp();
            }
        }
        return null;
    }

    public static boolean falseIfNull(Boolean value) {
        return value == null ? false : value;
    }

    public static <T> List<T> getIntersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<>();
        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }
        LOG.debug( " getIntersection - L1 {}, L2 - {}, Intersection - {}", list1, list2, list);
        return list;
    }

    public static void addTransportZoneMembership(List<TzMembership> zones, String zoneName) {
        zones.add(new TzMembershipBuilder().setZoneName(zoneName).build());
    }

    public static  List<TzMembership> createTransportZoneMembership(String zoneName) {
        List<TzMembership> zones = new ArrayList<>();
        zones.add(new TzMembershipBuilder().setZoneName(zoneName).build());
        return zones;
    }

    /**
     * Returns the transport zone from Configuration datastore.
     *
     * @param tzName transport zone name
     * @param dataBroker data broker handle to perform operations on datastore
     */
    // FIXME: Better is to implement cache to avoid datastore read.
    public static TransportZone getTransportZoneFromConfigDS(String tzName, DataBroker dataBroker) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
            .child(TransportZone.class, new TransportZoneKey(tzName)).build();
        Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath,
            dataBroker);
        if (tZoneOptional.isPresent()) {
            return tZoneOptional.get();
        }
        return null;
    }

    /**
     * Gets the transport zone in TepsNotHosted list in the Configuration Datastore
     * based on transport zone name
     *
     * @param unknownTz transport zone name
     *
     * @param dataBroker data broker handle to perform read operations on config datastore
     *
     * @return the TepsNotHostedInTransportZone object in the TepsNotHosted list in Config DS
     */
    public static TepsNotHostedInTransportZone getUnknownTransportZoneFromITMConfigDS(
        String unknownTz, DataBroker dataBroker) {
        InstanceIdentifier<TepsNotHostedInTransportZone> unknownTzPath =
            InstanceIdentifier.builder(TransportZones.class)
                .child(TepsNotHostedInTransportZone.class,
                    new TepsNotHostedInTransportZoneKey(unknownTz)).build();
        Optional<TepsNotHostedInTransportZone> unknownTzOptional =
            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, unknownTzPath, dataBroker);
        if (unknownTzOptional.isPresent()) {
            return unknownTzOptional.get();
        }
        return null;
    }

    /**
     * Gets the bridge datapath ID from Network topology Node's OvsdbBridgeAugmentation
     * in the Operational DS.
     *
     * @param node Network Topology Node
     *
     * @param bridge bridge name
     *
     * @param dataBroker data broker handle to perform operations on datastore
     *
     * @return the datapath ID of bridge in string form
     */
    public static String getBridgeDpid(Node node, String bridge, DataBroker dataBroker) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = null;
        Node bridgeNode = null;
        String datapathId = null;

        NodeId ovsdbNodeId = node.getKey().getNodeId();

        NodeId brNodeId = new NodeId(ovsdbNodeId.getValue()
            + "/" + ITMConstants.BRIDGE_URI_PREFIX + "/" + bridge);

        InstanceIdentifier<Node> bridgeIid =
            InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(IfmConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(brNodeId));

        Optional<Node> opBridgeNode = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);

        if (opBridgeNode.isPresent()) {
            bridgeNode = opBridgeNode.get();
        }
        if (bridgeNode != null) {
            ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        }

        if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
            datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
        }
        return datapathId;
    }

    /**
     * Gets the Network topology Node from Operational Datastore
     * based on Bridge Augmentation.
     *
     * @param bridgeAugmentation bridge augmentation of OVSDB node
     *
     * @param dataBroker data broker handle to perform operations on datastore
     *
     * @return the Network Topology Node i.e. OVSDB node which is managing the specified bridge
     */
    public static Node getOvsdbNode(OvsdbBridgeAugmentation bridgeAugmentation,
        DataBroker dataBroker) {
        Node ovsdbNode = null;
        Optional<Node> opOvsdbNode = null;
        if (bridgeAugmentation != null) {
            InstanceIdentifier<Node> ovsdbNodeIid =
                (InstanceIdentifier<Node>) bridgeAugmentation.getManagedBy().getValue();
            opOvsdbNode = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, ovsdbNodeIid, dataBroker);
        }
        if (opOvsdbNode.isPresent()) {
            ovsdbNode = opOvsdbNode.get();
        }
        return ovsdbNode;
    }

    /**
     * Gets the bridge datapath ID in string form from
     * Network topology Node's OvsdbBridgeAugmentation in the Operational DS.
     *
     * @param augmentedNode Ovsdb Augmented Network Topology Node
     *
     * @return the datapath ID of bridge in string form
     */
    public static String getStrDatapathId(OvsdbBridgeAugmentation augmentedNode) {
        String datapathId = null;
        if (augmentedNode != null && augmentedNode.getDatapathId() != null) {
            datapathId = augmentedNode.getDatapathId().getValue();
        }
        return datapathId;
    }

    /**
     * Returns the dummy subnet (255.255.255.255/32) as IpPrefix object
     *
     * @return the dummy subnet (255.255.255.255/32) in IpPrefix object
     */
    public static IpPrefix getDummySubnet() {
        return DUMMY_IP_PREFIX;
    }

    /**
     * Deletes the transport zone from Configuration datastore.
     *
     * @param tzName transport zone name
     * @param dataBroker data broker handle to perform operations on datastore
     */
    public static void deleteTransportZoneFromConfigDS(String tzName, DataBroker dataBroker) {
        // check whether transport-zone exists in config DS.
        TransportZone tZoneFromConfigDS = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
        if (tZoneFromConfigDS != null) {
            // it exists, delete default-TZ now
            InstanceIdentifier<TransportZone> path = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class,
                    new TransportZoneKey(tzName)).build();
            LOG.debug("Removing {} transport-zone from config DS.", tzName);
            ItmUtils.asyncDelete(LogicalDatastoreType.CONFIGURATION, path, dataBroker, ItmUtils.DEFAULT_CALLBACK);
        }
    }

    /**
     * Validates the tunnelType argument and returnsTunnelTypeBase class object
     * corresponding to tunnelType obtained in String format.
     *
     * @param tunnelType type of tunnel in string form
     *
     * @return tunnel-type in TunnelTypeBase object
     */
    public static Class<? extends TunnelTypeBase> getTunnelType(String tunnelType) {
        // validate tunnelType string, in case it is NULL or empty, then
        // take VXLAN tunnel type by default
        if (tunnelType == null || tunnelType.isEmpty()) {
            return TUNNEL_TYPE_MAP.get(ITMConstants.TUNNEL_TYPE_VXLAN);
        } else if (!tunnelType.equals(ITMConstants.TUNNEL_TYPE_VXLAN) &&
            !tunnelType.equals(ITMConstants.TUNNEL_TYPE_GRE)) {
            // if tunnel type is some incorrect value, then
            // take VXLAN tunnel type by default
            return TUNNEL_TYPE_MAP.get(ITMConstants.TUNNEL_TYPE_VXLAN);
        }

        // return TunnelTypeBase object corresponding to tunnel-type
        return TUNNEL_TYPE_MAP.get(tunnelType);
    }

    public static List<TzMembership> removeTransportZoneMembership(TunnelEndPoints endPts, List<TzMembership> zones){
        LOG.trace( " RemoveTransportZoneMembership TEPs {}, Membership to be removed {} ", endPts, zones);
        List<TzMembership> existingTzList = new ArrayList<>(endPts.getTzMembership()) ;
        for( TzMembership membership : zones) {
            existingTzList.remove(new TzMembershipBuilder().setZoneName(membership.getZoneName()).build());
        }
        LOG.debug( "Modified Membership List {}", existingTzList);
        return existingTzList;
    }

    public static List<TzMembership> getOriginalTzMembership(TunnelEndPoints srcTep, BigInteger dpnId, List<DPNTEPsInfo> meshedDpnList) {
        LOG.trace( "Original Membership for source DPN {}, source TEP {}", dpnId, srcTep) ;
        for (DPNTEPsInfo dstDpn : meshedDpnList) {
            if( dpnId.equals(dstDpn.getDPNID()) ){
                List<TunnelEndPoints> endPts = dstDpn.getTunnelEndPoints();
                for( TunnelEndPoints tep : endPts) {
                    if( tep.getIpAddress().equals( srcTep.getIpAddress())){
                        LOG.debug( "Original Membership size " + tep.getTzMembership().size()) ;
                        return tep.getTzMembership() ;
                    }
                }
            }
        }
        return null ;
    }
}
