/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import static java.util.Collections.emptyList;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.interfaces._interface.NodeIdentifierKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.tunnel.optional.params.TunnelOptions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.tunnel.optional.params.TunnelOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.tunnel.optional.params.TunnelOptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeExternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeHwvtep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.DstInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.state.tunnel.list.SrcInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneKey;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItmUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmUtils.class);

    private static final String TUNNEL = "tun";
    private static final IpPrefix DUMMY_IP_PREFIX = IpPrefixBuilder.getDefaultInstance(ITMConstants.DUMMY_PREFIX);
    private static final long DEFAULT_MONITORING_INTERVAL = 100L;
    public static final ItmCache ITM_CACHE = new ItmCache();

    public static final ImmutableMap<String, Class<? extends TunnelTypeBase>>
            TUNNEL_TYPE_MAP =
            new ImmutableMap.Builder<String, Class<? extends TunnelTypeBase>>()
                    .put(ITMConstants.TUNNEL_TYPE_GRE, TunnelTypeGre.class)
                    .put(ITMConstants.TUNNEL_TYPE_MPLSoGRE, TunnelTypeMplsOverGre.class)
                    .put(ITMConstants.TUNNEL_TYPE_VXLAN, TunnelTypeVxlan.class)
                    .build();

    private static final BiMap<String,Class<? extends TunnelTypeBase>> STRING_CLASS_IMMUTABLE_BI_MAP =
            ImmutableBiMap.copyOf(TUNNEL_TYPE_MAP);

    private ItmUtils() {
    }

    public static final FutureCallback<Void> DEFAULT_CALLBACK = new FutureCallback<Void>() {
        @Override
        public void onSuccess(Void result) {
            LOG.debug("Success in Datastore write operation");
        }

        @Override
        public void onFailure(@Nonnull Throwable error) {
            LOG.error("Error in Datastore write operation", error);
        }
    };

    /**
     * Synchronous blocking read from data store.
     *
     * @deprecated Use
     * {@link SingleTransactionDataBroker#syncReadOptional(DataBroker, LogicalDatastoreType, InstanceIdentifier)}
     *             instead of this.
     */
    @Deprecated
    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T extends DataObject> Optional<T> read(LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path, DataBroker broker) {
        try (ReadOnlyTransaction tx = broker.newReadOnlyTransaction()) {
            return tx.read(datastoreType, path).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Asynchronous non-blocking write to data store.
     *
     * @deprecated Use {@link ManagedNewTransactionRunner} instead of this.
     */
    @Deprecated
    public static <T extends DataObject> void asyncWrite(LogicalDatastoreType datastoreType,
                                                         InstanceIdentifier<T> path, T data, DataBroker broker,
                                                         FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.put(datastoreType, path, data, true);
        Futures.addCallback(tx.submit(), callback);
    }

    /**
     * Asynchronous non-blocking update to data store.
     *
     * @deprecated Use {@link ManagedNewTransactionRunner} instead of this.
     */
    @Deprecated
    public static <T extends DataObject> void asyncUpdate(LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path, T data, DataBroker broker,
                                                          FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.merge(datastoreType, path, data, true);
        Futures.addCallback(tx.submit(), callback);
    }

    /**
     * Asynchronous non-blocking single delete to data store.
     *
     * @deprecated Use {@link ManagedNewTransactionRunner} instead of this.
     */
    @Deprecated
    public static <T extends DataObject> void asyncDelete(LogicalDatastoreType datastoreType,
                                                          InstanceIdentifier<T> path, DataBroker broker,
                                                          FutureCallback<Void> callback) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.delete(datastoreType, path);
        Futures.addCallback(tx.submit(), callback);
    }

    /**
     * Asynchronous non-blocking bulk delete to data store.
     *
     * @deprecated Use {@link ManagedNewTransactionRunner} instead of this.
     */
    @Deprecated
    public static <T extends DataObject> void asyncBulkRemove(final DataBroker broker,
                                                              final LogicalDatastoreType datastoreType,
                                                              List<InstanceIdentifier<T>> pathList,
                                                              FutureCallback<Void> callback) {
        if (!pathList.isEmpty()) {
            WriteTransaction tx = broker.newWriteOnlyTransaction();
            for (InstanceIdentifier<T> path : pathList) {
                tx.delete(datastoreType, path);
            }
            Futures.addCallback(tx.submit(), callback);
        }
    }

    public static String getInterfaceName(final BigInteger datapathid, final String portName, final Integer vlanId) {
        return String.format("%s:%s:%s", datapathid, portName, vlanId);
    }

    public static BigInteger getDpnIdFromInterfaceName(String interfaceName) {
        String[] dpnStr = interfaceName.split(":");
        return new BigInteger(dpnStr[0]);
    }

    public static String getTrunkInterfaceName(String parentInterfaceName,
                                               String localHostName, String remoteHostName, String tunnelType) {
        String tunnelTypeStr;
        if (tunnelType.contains("TunnelTypeGre")) {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_GRE;
        } else if (tunnelType.contains("TunnelTypeLogicalGroup")) {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_LOGICAL_GROUP_VXLAN;
        } else {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_VXLAN;
        }
        String trunkInterfaceName = String.format("%s:%s:%s:%s", parentInterfaceName, localHostName,
                remoteHostName, tunnelTypeStr);
        LOG.trace("trunk interface name is {}", trunkInterfaceName);
        trunkInterfaceName = String.format("%s%s", TUNNEL, getUniqueIdString(trunkInterfaceName));
        return trunkInterfaceName;
    }

    public static void releaseIdForTrunkInterfaceName(String parentInterfaceName,
                                                      String localHostName, String remoteHostName, String tunnelType) {
        String tunnelTypeStr;
        if (tunnelType.contains("TunnelTypeGre")) {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_GRE;
        } else {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_VXLAN;
        }
        String trunkInterfaceName = String.format("%s:%s:%s:%s", parentInterfaceName, localHostName,
                remoteHostName, tunnelTypeStr);
        LOG.trace("Releasing Id for trunkInterface - {}", trunkInterfaceName);
    }

    public static String getLogicalTunnelGroupName(BigInteger srcDpnId, BigInteger destDpnId) {
        String tunnelTypeStr = ITMConstants.TUNNEL_TYPE_LOGICAL_GROUP_VXLAN;
        String groupName = String.format("%s:%s:%s", srcDpnId.toString(), destDpnId.toString(), tunnelTypeStr);
        LOG.trace("logical tunnel group name is {}", groupName);
        groupName = String.format("%s%s", TUNNEL, getUniqueIdString(groupName));
        return groupName;
    }

    public static InetAddress getInetAddressFromIpAddress(IpAddress ip) {
        return IetfInetUtil.INSTANCE.inetAddressFor(ip);
    }

    public static InstanceIdentifier<DPNTEPsInfo> getDpnTepInstance(BigInteger dpIdKey, String tZones) {
        return InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, new DPNTEPsInfoKey(dpIdKey,
                tZones)).build();
    }

    public static DPNTEPsInfo createDPNTepInfo(BigInteger dpId, List<TunnelEndPoints> endpoints) {
        StringBuilder tZones = new StringBuilder();
        for(TunnelEndPoints teps : endpoints) {
            List<TzMembership> zones = teps.getTzMembership();
            for(TzMembership zone: zones){
                tZones.append(zone);
            }
        }

        return new DPNTEPsInfoBuilder().withKey(new DPNTEPsInfoKey(dpId, tZones.toString()))
                .setTunnelEndPoints(endpoints).build();
    }

    public static TunnelEndPoints createTunnelEndPoints(BigInteger dpnId, IpAddress ipAddress, String portName,
                                                        boolean isOfTunnel, int vlanId, IpPrefix prefix,
                                                        IpAddress gwAddress, List<TzMembership> zones,
                                                        Class<? extends TunnelTypeBase>  tunnelType,
                                                        String tos) {
        // when Interface Mgr provides support to take in Dpn Id
        return new TunnelEndPointsBuilder().withKey(new TunnelEndPointsKey(ipAddress, portName,tunnelType, vlanId))
                .setSubnetMask(prefix).setGwIpAddress(gwAddress).setTzMembership(zones)
                .setOptionOfTunnel(isOfTunnel).setInterfaceName(ItmUtils.getInterfaceName(dpnId, portName, vlanId))
                .setTunnelType(tunnelType)
                .setOptionTunnelTos(tos)
                .build();
    }

    public static DpnEndpoints createDpnEndpoints(List<DPNTEPsInfo> dpnTepInfo) {
        return new DpnEndpointsBuilder().setDPNTEPsInfo(dpnTepInfo).build();
    }

    public static InstanceIdentifier<Interface> buildId(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static InstanceIdentifier<IfTunnel> buildTunnelId(String ifName) {
        return InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(ifName)).augmentation(IfTunnel.class).build();
    }

    public static Interface buildLogicalTunnelInterface(BigInteger dpn, String ifName, String desc, boolean enabled) {
        InterfaceBuilder builder = new InterfaceBuilder().withKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(dpn).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);

        IfTunnel tunnel = new IfTunnelBuilder()
                .setTunnelDestination(IpAddressBuilder.getDefaultInstance(ITMConstants.DUMMY_IP_ADDRESS))
                .setTunnelSource(IpAddressBuilder.getDefaultInstance(ITMConstants.DUMMY_IP_ADDRESS)).setInternal(true)
                .setMonitorEnabled(false).setTunnelInterfaceType(TunnelTypeLogicalGroup.class)
                .setTunnelRemoteIpFlow(false).build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        return builder.build();
    }

    public static Interface buildTunnelInterface(BigInteger dpn, String ifName, String desc, boolean enabled,
                                                 Class<? extends TunnelTypeBase> tunType, IpAddress localIp,
                                                 IpAddress remoteIp, IpAddress gatewayIp, Integer vlanId,
                                                 boolean internal, Boolean monitorEnabled,
                                                 Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                                 Integer monitorInterval, boolean useOfTunnel,
                                                 List<TunnelOptions> tunOptions) {

        return buildTunnelInterface(dpn, ifName, desc, enabled, tunType, localIp, remoteIp,  gatewayIp,  vlanId,
                                    internal,  monitorEnabled, monitorProtocol, monitorInterval,  useOfTunnel, null,
                                    tunOptions);
    }

    public static Interface buildTunnelInterface(BigInteger dpn, String ifName, String desc, boolean enabled,
                                                 Class<? extends TunnelTypeBase> tunType, IpAddress localIp,
                                                 IpAddress remoteIp, IpAddress gatewayIp, Integer vlanId,
                                                 boolean internal, Boolean monitorEnabled,
                                                 Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                                 Integer monitorInterval, boolean useOfTunnel, String parentIfaceName,
                                                 List<TunnelOptions> tunnelOptions) {
        InterfaceBuilder builder = new InterfaceBuilder().withKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs =
                new ParentRefsBuilder().setDatapathNodeIdentifier(dpn).setParentInterface(parentIfaceName).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        Long monitoringInterval = null;
        if (vlanId > 0) {
            IfL2vlan l2vlan = new IfL2vlanBuilder().setVlanId(new VlanId(vlanId)).build();
            builder.addAugmentation(IfL2vlan.class, l2vlan);
        }
        LOG.debug("buildTunnelInterface: monitorProtocol = {} and monitorInterval = {}",
                monitorProtocol.getName(),monitorInterval);

        if (monitorInterval != null) {
            monitoringInterval = monitorInterval.longValue();
        }

        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(remoteIp).setTunnelGateway(gatewayIp)
                .setTunnelSource(localIp).setTunnelInterfaceType(tunType).setInternal(internal)
                .setMonitorEnabled(monitorEnabled).setMonitorProtocol(monitorProtocol)
                .setMonitorInterval(monitoringInterval).setTunnelRemoteIpFlow(useOfTunnel)
                .setTunnelOptions(tunnelOptions)
                .build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        return builder.build();
    }

    public static Interface buildHwTunnelInterface(String tunnelIfName, String desc, boolean enabled, String topoId,
                                                   String nodeId, Class<? extends TunnelTypeBase> tunType,
                                                   IpAddress srcIp, IpAddress destIp, IpAddress gwIp,
                                                   Boolean monitorEnabled,
                                                   Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                                   Integer monitorInterval) {
        InterfaceBuilder builder = new InterfaceBuilder().withKey(new InterfaceKey(tunnelIfName))
                .setName(tunnelIfName).setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        List<NodeIdentifier> nodeIds = new ArrayList<>();
        NodeIdentifier hwNode = new NodeIdentifierBuilder().withKey(new NodeIdentifierKey(topoId))
                .setTopologyId(topoId).setNodeId(nodeId).build();
        nodeIds.add(hwNode);
        ParentRefs parent = new ParentRefsBuilder().setNodeIdentifier(nodeIds).build();
        builder.addAugmentation(ParentRefs.class, parent);
        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(destIp).setTunnelGateway(gwIp)
                .setTunnelSource(srcIp).setMonitorEnabled(monitorEnabled == null || monitorEnabled)
                .setMonitorProtocol(monitorProtocol == null ? ITMConstants.DEFAULT_MONITOR_PROTOCOL : monitorProtocol)
                .setMonitorInterval(DEFAULT_MONITORING_INTERVAL).setTunnelInterfaceType(tunType).setInternal(false)
                .build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        LOG.trace("iftunnel {} built from hwvtep {} ", tunnel, nodeId);
        return builder.build();
    }

    public static InternalTunnel buildInternalTunnel(BigInteger srcDpnId, BigInteger dstDpnId,
                                                     Class<? extends TunnelTypeBase> tunType,
                                                     String trunkInterfaceName) {
        return new InternalTunnelBuilder().withKey(new InternalTunnelKey(dstDpnId, srcDpnId, tunType))
                .setDestinationDPN(dstDpnId)
                .setSourceDPN(srcDpnId).setTransportType(tunType)
                .setTunnelInterfaceNames(Collections.singletonList(trunkInterfaceName)).build();
    }

    public static ExternalTunnel buildExternalTunnel(String srcNode, String dstNode,
                                                     Class<? extends TunnelTypeBase> tunType,
                                                     String trunkInterfaceName) {
        return new ExternalTunnelBuilder().withKey(
                new ExternalTunnelKey(dstNode, srcNode, tunType))
                .setSourceDevice(srcNode).setDestinationDevice(dstNode)
                .setTunnelInterfaceName(trunkInterfaceName)
                .setTransportType(tunType).build();
    }

    private static String getUniqueIdString(String idKey) {
        return UUID.nameUUIDFromBytes(idKey.getBytes(StandardCharsets.UTF_8)).toString().substring(0, 12)
                .replace("-", "");
    }

    public static List<DPNTEPsInfo> getDpnTepListFromDpnId(DPNTEPsInfoCache dpnTEPsInfoCache, List<BigInteger> dpnIds) {
        Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        List<DPNTEPsInfo> cfgDpnList = new ArrayList<>();
        for (BigInteger dpnId : dpnIds) {
            for (DPNTEPsInfo teps : meshedDpnList) {
                if (dpnId.equals(teps.getDPNID())) {
                    cfgDpnList.add(teps);
                }
            }
        }

        return cfgDpnList;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void addTerminatingServiceTable(TypedReadWriteTransaction<Configuration> tx,
        BigInteger dpnId, IMdsalApiManager mdsalManager) {
        LOG.trace("Installing PUNT to Controller flow in DPN {} ", dpnId);
        List<ActionInfo> listActionInfo = new ArrayList<>();
        listActionInfo.add(new ActionPuntToController());

        try {
            List<MatchInfo> mkMatches = new ArrayList<>();

            mkMatches.add(new MatchTunnelId(BigInteger.valueOf(ITMConstants.LLDP_SERVICE_ID)));

            List<InstructionInfo> mkInstructions = new ArrayList<>();
            mkInstructions.add(new InstructionApplyActions(listActionInfo));

            FlowEntity terminatingServiceTableFlowEntity = MDSALUtil
                .buildFlowEntity(dpnId, NwConstants.INTERNAL_TUNNEL_TABLE,
                    getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE, ITMConstants.LLDP_SERVICE_ID),
                    5, String.format("%s:%d","ITM Flow Entry ", ITMConstants.LLDP_SERVICE_ID), 0, 0,
                    ITMConstants.COOKIE_ITM.add(BigInteger.valueOf(ITMConstants.LLDP_SERVICE_ID)),
                    mkMatches, mkInstructions);
            mdsalManager.addFlow(tx, terminatingServiceTableFlowEntity);
        } catch (Exception e) {
            LOG.error("Error while setting up Table 36 for {}", dpnId, e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void removeTerminatingServiceTable(TypedReadWriteTransaction<Configuration> tx,
        BigInteger dpnId, IMdsalApiManager mdsalManager) {
        LOG.trace("Removing PUNT to Controller flow in DPN {} ", dpnId);

        try {
            mdsalManager.removeFlow(tx, dpnId,
                getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE, ITMConstants.LLDP_SERVICE_ID),
                NwConstants.INTERNAL_TUNNEL_TABLE);
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
            if (!(!StringUtils.equalsIgnoreCase(schema.getSchemaName(), existingSchema.getSchemaName())
                    && Objects.equals(schema.getSubnet(), existingSchema.getSubnet()))) {
                String subnetCidr = getSubnetCidrAsString(schema.getSubnet());
                Preconditions.checkArgument(
                    !(!StringUtils.equalsIgnoreCase(schema.getSchemaName(), existingSchema.getSchemaName())
                        && Objects.equals(schema.getSubnet(), existingSchema.getSubnet())),
                    "VTEP schema with subnet [" + subnetCidr
                        + "] already exists. Multiple VTEP schemas with same subnet is not allowed.");
            }
        }
        if (isNotEmpty(getDpnIdList(validSchema.getDpnIds()))) {
            String tzone = validSchema.getTransportZoneName();
            List<BigInteger> lstDpns = getConflictingDpnsAlreadyConfiguredWithTz(validSchema.getSchemaName(), tzone,
                    getDpnIdList(validSchema.getDpnIds()), existingSchemas);
            Preconditions.checkArgument(lstDpns.isEmpty(),
                "DPN's " + lstDpns + " already configured for transport zone "
                    + tzone + ". Only one end point per transport Zone per Dpn is allowed.");
            if (TunnelTypeGre.class.equals(schema.getTunnelType())) {
                validateForSingleGreTep(validSchema.getSchemaName(), getDpnIdList(validSchema.getDpnIds()),
                        existingSchemas);
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
                Preconditions.checkArgument(lstConflictingDpns.isEmpty(), "DPN's " + lstConflictingDpns
                    + " already configured with GRE TEP. Mutiple GRE TEP's on a single DPN are not allowed.");
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
            String strGatewayIp = gatewayIp.stringValue();
            Preconditions.checkArgument(
                ITMConstants.DUMMY_IP_ADDRESS.equals(strGatewayIp) || subnetUtils.getInfo().isInRange(strGatewayIp),
                "Gateway IP address " + strGatewayIp + " is not in subnet range " + subnetCidr);
        }
        ItmUtils.getExcludeIpAddresses(schema.getExcludeIpFilter(), subnetUtils.getInfo());
        return new VtepConfigSchemaBuilder(schema).setTunnelType(schema.getTunnelType()).build();
    }

    public static String validateTunnelType(String tunnelType) {
        if (tunnelType == null) {
            tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
        } else {
            tunnelType = StringUtils.upperCase(tunnelType);
            String error = "Invalid tunnel type. Valid values: "
                    + ITMConstants.TUNNEL_TYPE_VXLAN + " | " + ITMConstants.TUNNEL_TYPE_GRE;
            Preconditions.checkArgument(ITMConstants.TUNNEL_TYPE_VXLAN.equals(tunnelType)
                    || ITMConstants.TUNNEL_TYPE_GRE.equals(tunnelType), error);
        }
        return tunnelType;
    }

    private static List<BigInteger> getConflictingDpnsAlreadyConfiguredWithTz(String schemaName, String tzone,
                                                                              List<BigInteger> lstDpns,
                                                                              List<VtepConfigSchema> existingSchemas) {
        List<BigInteger> lstConflictingDpns = new ArrayList<>();
        for (VtepConfigSchema schema : emptyIfNull(existingSchemas)) {
            if (!StringUtils.equalsIgnoreCase(schemaName, schema.getSchemaName())
                    && StringUtils.equals(schema.getTransportZoneName(), tzone)) {
                lstConflictingDpns = new ArrayList<>(getDpnIdList(nullToEmpty(schema.getDpnIds())));
                lstConflictingDpns.retainAll(lstDpns);
                if (!lstConflictingDpns.isEmpty()) {
                    break;
                }
            }
        }
        return lstConflictingDpns;
    }

    public static VtepConfigSchema constructVtepConfigSchema(String schemaName, String portName, Integer vlanId,
                                                             String subnetMask, String gatewayIp, String transportZone,
                                                             String tunnelType, List<BigInteger> dpnIds,
                                                             String excludeIpFilter) {
        IpAddress gatewayIpObj = StringUtils.isBlank(gatewayIp) ? null : IpAddressBuilder.getDefaultInstance(gatewayIp);
        IpPrefix subnet = StringUtils.isBlank(subnetMask) ? null : IpPrefixBuilder.getDefaultInstance(subnetMask);
        Class<? extends TunnelTypeBase> tunType ;
        if (tunnelType.equals(ITMConstants.TUNNEL_TYPE_VXLAN)) {
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
                        "Invalid exclude IP filter: IP address [" + strStartIp
                                + "] not in subnet range " + subnetInfo.getCidrSignature());
                Preconditions.checkArgument(subnetInfo.isInRange(strEndIp),
                        "Invalid exclude IP filter: IP address [" + strEndIp
                                + "] not in subnet range " + subnetInfo.getCidrSignature());
                int startIp = subnetInfo.asInteger(strStartIp);
                int endIp = subnetInfo.asInteger(strEndIp);

                Preconditions.checkArgument(startIp < endIp,
                        "Invalid exclude IP filter: Invalid range [" + ip + "] ");
                for (int iter = startIp; iter <= endIp; iter++) {
                    String ipAddress = ipFormat(toIpArray(iter));
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
                "Invalid exclude IP filter: IP address [" + ip + "] not in subnet range "
                        + subnetInfo.getCidrSignature());
        lstIpAddress.add(IpAddressBuilder.getDefaultInstance(ip));
    }

    private static int[] toIpArray(int val) {
        int[] ret = new int[4];
        for (int iter = 3; iter >= 0; --iter) {
            ret[iter] |= val >>> 8 * (3 - iter) & 0xff;
        }
        return ret;
    }

    private static String ipFormat(int[] octets) {
        StringBuilder str = new StringBuilder();
        for (int iter = 0; iter < octets.length; ++iter) {
            str.append(octets[iter]);
            if (iter != octets.length - 1) {
                str.append(".");
            }
        }
        return str.toString();
    }

    public static VtepConfigSchema validateForUpdateVtepSchema(String schemaName, List<BigInteger> lstDpnsForAdd,
                                                               List<BigInteger> lstDpnsForDelete,
                                                               IITMProvider itmProvider) {
        Preconditions.checkArgument(StringUtils.isNotBlank(schemaName));
        Preconditions.checkArgument(lstDpnsForAdd != null && !lstDpnsForAdd.isEmpty() && lstDpnsForDelete != null
                && !lstDpnsForDelete.isEmpty(),
            "DPN ID list for add | delete is null or empty in schema " + schemaName);
        VtepConfigSchema schema = itmProvider.getVtepConfigSchema(schemaName);
        Preconditions.checkArgument(schema != null, "Specified VTEP Schema [" + schemaName + "] doesn't exist!");
        List<BigInteger> existingDpnIds = getDpnIdList(nullToEmpty(schema.getDpnIds()));
        if (isNotEmpty(lstDpnsForAdd)) {
            List<BigInteger> lstAlreadyExistingDpns = new ArrayList<>(existingDpnIds);
            lstAlreadyExistingDpns.retainAll(lstDpnsForAdd);
            Preconditions.checkArgument(lstAlreadyExistingDpns.isEmpty(),
                    "DPN ID's " + lstAlreadyExistingDpns
                            + " already exists in VTEP schema [" + schemaName + "]");
            if (TunnelTypeGre.class.equals(schema.getTunnelType())) {
                validateForSingleGreTep(schema.getSchemaName(), lstDpnsForAdd, itmProvider.getAllVtepConfigSchemas());
            }
        }
        if (isNotEmpty(lstDpnsForDelete)) {
            Preconditions.checkArgument(!existingDpnIds.isEmpty(), "DPN ID's " + lstDpnsForDelete
                + " specified for delete from VTEP schema [" + schemaName
                + "] are not configured in the schema.");
            if (!existingDpnIds.containsAll(lstDpnsForDelete)) {
                List<BigInteger> lstConflictingDpns = new ArrayList<>(lstDpnsForDelete);
                lstConflictingDpns.removeAll(existingDpnIds);
                throw new IllegalArgumentException("DPN ID's " + lstConflictingDpns
                    + " specified for delete from VTEP schema [" + schemaName
                    + "] are not configured in the schema.");
            }
        }
        return schema;
    }

    public static String getSubnetCidrAsString(IpPrefix subnet) {
        return subnet == null ? StringUtils.EMPTY : subnet.stringValue();
    }

    public static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? emptyList() : list;
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    @Nonnull
    public static HwVtep createHwVtepObject(String topoId, String nodeId, IpAddress ipAddress, IpPrefix ipPrefix,
                                            IpAddress gatewayIP, int vlanID,
                                            Class<? extends TunnelTypeBase> tunneltype, TransportZone transportZone) {
        HwVtep hwVtep = new HwVtep();
        hwVtep.setGatewayIP(gatewayIP);
        hwVtep.setHwIp(ipAddress);
        hwVtep.setIpPrefix(ipPrefix);
        hwVtep.setNodeId(nodeId);
        hwVtep.setTopoId(topoId);
        hwVtep.setTransportZone(transportZone.getZoneName());
        hwVtep.setTunnelType(tunneltype);
        hwVtep.setVlanID(vlanID);
        return hwVtep;
    }

    public static String getHwParentIf(String topoId, String srcNodeid) {
        return String.format("%s:%s", topoId, srcNodeid);
    }

    /**
     * Synchronous blocking write to data store.
     *
     * @deprecated Use
     * {@link SingleTransactionDataBroker#syncWrite(DataBroker, LogicalDatastoreType, InstanceIdentifier, DataObject)}
     *             instead of this.
     */
    @Deprecated
    public static <T extends DataObject> void syncWrite(LogicalDatastoreType datastoreType,
                                                        InstanceIdentifier<T> path, T data, DataBroker broker) {
        WriteTransaction tx = broker.newWriteOnlyTransaction();
        tx.put(datastoreType, path, data, true);
        try {
            tx.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("ITMUtils:SyncWrite , Error writing to datastore (path, data) : ({}, {})", path, data);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Nonnull
    public static List<BigInteger> getDpnIdList(List<DpnIds> dpnIds) {
        List<BigInteger> dpnList = new ArrayList<>() ;
        for (DpnIds dpn : dpnIds) {
            dpnList.add(dpn.getDPN()) ;
        }
        return dpnList ;
    }

    public static List<DpnIds> getDpnIdsListFromBigInt(List<BigInteger> dpnIds) {
        List<DpnIds> dpnIdList = new ArrayList<>();
        DpnIdsBuilder builder = new DpnIdsBuilder();
        for (BigInteger dpnId : dpnIds) {
            dpnIdList.add(builder.withKey(new DpnIdsKey(dpnId)).setDPN(dpnId).build());
        }
        return dpnIdList;
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
            .rev140508.interfaces.state.Interface> buildStateInterfaceId(
            String interfaceName) {
        return InstanceIdentifier.builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
                               .state.Interface.class,
                       new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
                               .rev140508.interfaces.state.InterfaceKey(
                               interfaceName)).build();
    }

    @Nonnull
    public static List<String> getInternalTunnelInterfaces(DataBroker dataBroker) {
        List<String> tunnelList = new ArrayList<>();
        Collection<String> internalInterfaces = ITM_CACHE.getAllInternalInterfaces();
        if (internalInterfaces.isEmpty()) {
            updateTunnelsCache(dataBroker);
            internalInterfaces = ITM_CACHE.getAllInternalInterfaces();
        }
        LOG.debug("ItmUtils.getTunnelList Cache Internal Interfaces size: {} ", internalInterfaces.size());
        tunnelList.addAll(internalInterfaces);
        LOG.trace("ItmUtils.getTunnelList Internal: {}", tunnelList);
        return tunnelList;
    }

    public static List<InternalTunnel> getInternalTunnelsFromCache(DataBroker dataBroker) {
        List<InternalTunnel> tunnelList = new ArrayList<>();
        Collection<InternalTunnel> internalInterfaces = ITM_CACHE.getAllInternalTunnel();
        if (internalInterfaces.isEmpty()) {
            updateTunnelsCache(dataBroker);
            internalInterfaces = ITM_CACHE.getAllInternalTunnel();
        }
        LOG.debug("Number of Internal Tunnel Interfaces in cache: {} ", internalInterfaces.size());
        tunnelList.addAll(internalInterfaces);
        LOG.trace("List of Internal Tunnels: {}", tunnelList);
        return tunnelList;
    }

    public static List<String> getTunnelsofTzone(List<HwVtep> hwVteps, String tzone, DataBroker dataBroker,
                                                 Boolean hwVtepsExist) {
        List<String> tunnels = new ArrayList<>();
        InstanceIdentifier<TransportZone> path = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> transportZoneOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        if (transportZoneOptional.isPresent()) {
            TransportZone transportZone = transportZoneOptional.get();
            Class<? extends TunnelTypeBase> tunType = transportZone.getTunnelType();
            if (transportZone.getSubnets() != null && !transportZone.getSubnets().isEmpty()) {
                for (Subnets sub : transportZone.getSubnets()) {
                    if (sub.getVteps() != null && !sub.getVteps().isEmpty()) {
                        for (Vteps vtepLocal : sub.getVteps()) {
                            for (Vteps vtepRemote : sub.getVteps()) {
                                if (!vtepLocal.equals(vtepRemote)) {
                                    InternalTunnelKey key = new InternalTunnelKey(vtepRemote.getDpnId(),
                                            vtepLocal.getDpnId(), tunType);
                                    InstanceIdentifier<InternalTunnel> intIID =
                                            InstanceIdentifier.builder(TunnelList.class)
                                                    .child(InternalTunnel.class, key).build();
                                    Optional<InternalTunnel> tunnelsOptional =
                                            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, intIID, dataBroker);
                                    if (tunnelsOptional.isPresent()) {
                                        List<String> tunnelInterfaceNames = tunnelsOptional
                                                .get().getTunnelInterfaceNames();
                                        if (tunnelInterfaceNames != null && !tunnelInterfaceNames.isEmpty()) {
                                            String tunnelInterfaceName = tunnelInterfaceNames.get(0);
                                            LOG.trace("Internal Tunnel added {}", tunnelInterfaceName);
                                            tunnels.add(tunnelInterfaceName);
                                        }
                                    }
                                }
                            }
                            if (hwVteps != null && !hwVteps.isEmpty()) {
                                for (HwVtep hwVtep : hwVteps) {
                                    tunnels.add(getExtTunnel(hwVtep.getNodeId(), vtepLocal.getDpnId().toString(),
                                            tunType, dataBroker));
                                    tunnels.add(getExtTunnel(vtepLocal.getDpnId().toString(), hwVtep.getNodeId(),
                                            tunType, dataBroker));
                                }
                            }
                        }
                    }
                }
            }
            if (hwVtepsExist) {
                for (HwVtep hwVtep : nullToEmpty(hwVteps)) {
                    for (HwVtep hwVtepOther : nullToEmpty(hwVteps)) {
                        if (!hwVtep.getHwIp().equals(hwVtepOther.getHwIp())) {
                            tunnels.add(getExtTunnel(hwVtep.getNodeId(), hwVtepOther.getNodeId(),
                                    tunType, dataBroker));
                            tunnels.add(getExtTunnel(hwVtepOther.getNodeId(), hwVtep.getNodeId(),
                                    tunType, dataBroker));
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
        InstanceIdentifier<TransportZone> path = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> transportZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, dataBroker);
        if (transportZoneOptional.isPresent()) {
            TransportZone transportZone = transportZoneOptional.get();
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
                                            InstanceIdentifier.builder(TunnelList.class)
                                                    .child(InternalTunnel.class, key).build();
                                    Optional<InternalTunnel> tunnelsOptional =
                                            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, intIID, dataBroker);
                                    if (tunnelsOptional.isPresent()) {
                                        List<String> tunnelInterfaceNames = tunnelsOptional.get()
                                                .getTunnelInterfaceNames();
                                        if (tunnelInterfaceNames != null && !tunnelInterfaceNames.isEmpty()) {
                                            String tunnelInterfaceName = tunnelInterfaceNames.get(0);
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
        }
        return tunnels;
    }

    private static String getExtTunnel(String nodeId, String dpId,Class<? extends TunnelTypeBase> tunType, DataBroker
            dataBroker) {
        LOG.trace("getting ext tunnel for {} and dpId {}",nodeId,dpId);
        ExternalTunnelKey key = getExternalTunnelKey(dpId, nodeId, tunType);
        InstanceIdentifier<ExternalTunnel> intIID = InstanceIdentifier.builder(ExternalTunnelList.class)
                .child(ExternalTunnel.class, key).build();
        Optional<ExternalTunnel> tunnelsOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, intIID, dataBroker);
        if (tunnelsOptional.isPresent()) {
            String tunnelInterfaceName = tunnelsOptional.get().getTunnelInterfaceName();
            LOG.trace("ext tunnel returned {} ", tunnelInterfaceName);
            return tunnelInterfaceName;
        }
        return null;
    }

    @SuppressFBWarnings("RV_CHECK_FOR_POSITIVE_INDEXOF")
    public static ExternalTunnelKey getExternalTunnelKey(String dst , String src,
                                                         Class<? extends TunnelTypeBase> tunType) {
        final int srcIndex = src.indexOf("physicalswitch");
        if (srcIndex > 0) {
            src = src.substring(0, srcIndex - 1);
        }
        final int dstIndex = dst.indexOf("physicalswitch");
        if (dstIndex > 0) {
            dst = dst.substring(0, dstIndex - 1);
        }
        return new ExternalTunnelKey(dst, src, tunType);
    }

    public static List<TunnelEndPoints> getTEPsForDpn(BigInteger srcDpn, Collection<DPNTEPsInfo> dpnList) {
        for (DPNTEPsInfo dpn : dpnList) {
            if (Objects.equals(dpn.getDPNID(), srcDpn)) {
                return new ArrayList<>(nullToEmpty(dpn.getTunnelEndPoints()));
            }
        }
        return null;
    }

    public static List<InternalTunnel> getAllInternalTunnels(DataBroker dataBroker) {
        List<InternalTunnel> result = null;
        InstanceIdentifier<TunnelList> iid = InstanceIdentifier.builder(TunnelList.class).build();
        Optional<TunnelList> tunnelList = read(LogicalDatastoreType.CONFIGURATION, iid, dataBroker);

        if (tunnelList.isPresent()) {
            result = tunnelList.get().getInternalTunnel();
        }
        if (result == null) {
            result = emptyList();
        }
        return result;
    }

    public static InternalTunnel getInternalTunnel(String interfaceName, DataBroker broker) {
        InternalTunnel internalTunnel = ITM_CACHE.getInternalTunnel(interfaceName);
        if (internalTunnel == null) {
            updateTunnelsCache(broker);
            internalTunnel = ITM_CACHE.getInternalTunnel(interfaceName);
        }
        return internalTunnel;
    }

    public static ExternalTunnel getExternalTunnel(String interfaceName, DataBroker broker) {
        ExternalTunnel externalTunnel = ITM_CACHE.getExternalTunnel(interfaceName);
        if (externalTunnel == null) {
            updateTunnelsCache(broker);
            externalTunnel = ITM_CACHE.getExternalTunnel(interfaceName);
        }
        return externalTunnel;
    }

    private static List<ExternalTunnel> getAllExternalTunnels(DataBroker dataBroker) {
        List<ExternalTunnel> result = null;
        InstanceIdentifier<ExternalTunnelList> iid = InstanceIdentifier.builder(ExternalTunnelList.class).build();
        Optional<ExternalTunnelList> tunnelList = read(LogicalDatastoreType.CONFIGURATION, iid, dataBroker);
        if (tunnelList.isPresent()) {
            result = tunnelList.get().getExternalTunnel();
        }
        if (result == null) {
            result = emptyList();
        }
        return result;
    }

    public static String convertTunnelTypetoString(Class<? extends TunnelTypeBase> tunType) {
        String tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
        if (tunType.equals(TunnelTypeVxlan.class)) {
            tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN ;
        } else if (tunType.equals(TunnelTypeGre.class)) {
            tunnelType = ITMConstants.TUNNEL_TYPE_GRE ;
        } else if (tunType.equals(TunnelTypeMplsOverGre.class)) {
            tunnelType = ITMConstants.TUNNEL_TYPE_MPLSoGRE;
        } else if (tunType.equals(TunnelTypeLogicalGroup.class)) {
            tunnelType = ITMConstants.TUNNEL_TYPE_LOGICAL_GROUP_VXLAN;
        }
        return tunnelType;
    }


    public static boolean isItmIfType(Class<? extends InterfaceType> ifType) {
        return ifType != null && ifType.isAssignableFrom(Tunnel.class);
    }

    public static StateTunnelListKey getTunnelStateKey(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                               .interfaces.rev140508.interfaces.state.Interface iface) {
        StateTunnelListKey key = null;
        if (isItmIfType(iface.getType())) {
            key = new StateTunnelListKey(iface.getName());
        }
        return key;
    }

    private static void updateTunnelsCache(DataBroker broker) {
        List<InternalTunnel> internalTunnels = getAllInternalTunnels(broker);
        for (InternalTunnel tunnel : internalTunnels) {
            ITM_CACHE.addInternalTunnel(tunnel);
        }
        List<ExternalTunnel> externalTunnels = getAllExternalTunnels(broker);
        for (ExternalTunnel tunnel : externalTunnels) {
            ITM_CACHE.addExternalTunnel(tunnel);
        }
    }

    public static Interface getInterface(
            String name, IInterfaceManager ifaceManager) {
        Interface result = ITM_CACHE.getInterface(name);
        if (result == null) {
            result = ifaceManager.getInterfaceInfoFromConfigDataStore(name);
            if (result != null) {
                ITM_CACHE.addInterface(result);
            }
        }
        return result;
    }

    public static List<DcGatewayIp> getDcGatewayIpList(DataBroker broker) {
        InstanceIdentifier<DcGatewayIpList> dcGatewayIpListid =
                InstanceIdentifier.builder(DcGatewayIpList.class).build();
        Optional<DcGatewayIpList> dcGatewayIpListConfig =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, dcGatewayIpListid, broker);
        if (dcGatewayIpListConfig.isPresent()) {
            DcGatewayIpList containerList = dcGatewayIpListConfig.get();
            if (containerList != null) {
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
        for (T iter : list1) {
            if (list2.contains(iter)) {
                list.add(iter);
            }
        }
        LOG.debug(" getIntersection - L1 {}, L2 - {}, Intersection - {}", list1, list2, list);
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

    public static String getTZonesFromTunnelEndPointList (List<TunnelEndPoints> tepsList) {
        StringBuilder tZones = new StringBuilder();
        for(TunnelEndPoints endPoints : tepsList) {
            List<TzMembership> zones = endPoints.getTzMembership();
            for(TzMembership zone: zones){
                tZones.append(zone);
            }
        }
        return tZones.toString();
    }


    /**
     * Gets the transport zone in TepsNotHosted list in the Operational Datastore, based on transport zone name.
     *
     * @param unknownTz transport zone name
     *
     * @param dataBroker data broker handle to perform read operations on Oper datastore
     *
     * @return the TepsInNotHostedTransportZone object in the TepsNotHosted list in Oper DS
     */
    public static TepsInNotHostedTransportZone getUnknownTransportZoneFromITMOperDS(
        String unknownTz, DataBroker dataBroker) {
        InstanceIdentifier<TepsInNotHostedTransportZone> unknownTzPath =
            InstanceIdentifier.builder(NotHostedTransportZones.class)
                    .child(TepsInNotHostedTransportZone.class,
                    new TepsInNotHostedTransportZoneKey(unknownTz)).build();
        Optional<TepsInNotHostedTransportZone> unknownTzOptional =
            ItmUtils.read(LogicalDatastoreType.OPERATIONAL, unknownTzPath, dataBroker);
        if (unknownTzOptional.isPresent()) {
            return unknownTzOptional.get();
        }
        return null;
    }

    /**
     * Gets the bridge datapath ID from Network topology Node's OvsdbBridgeAugmentation, in the Operational DS.
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

        NodeId ovsdbNodeId = node.key().getNodeId();

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
            ovsdbBridgeAugmentation = bridgeNode.augmentation(OvsdbBridgeAugmentation.class);
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
        Optional<Node> opOvsdbNode = Optional.absent();
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
     * Returns the dummy subnet (255.255.255.255/32) as IpPrefix object.
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
        TransportZone transportZoneFromConfigDS = ItmUtils.getTransportZoneFromConfigDS(tzName, dataBroker);
        if (transportZoneFromConfigDS != null) {
            // it exists, delete default-TZ now
            InstanceIdentifier<TransportZone> path = InstanceIdentifier.builder(TransportZones.class)
                    .child(TransportZone.class, new TransportZoneKey(tzName)).build();
            LOG.debug("Removing {} transport-zone from config DS.", tzName);
            try {
                SingleTransactionDataBroker.syncDelete(dataBroker, LogicalDatastoreType.CONFIGURATION, path);
            } catch (TransactionCommitFailedException e) {
                LOG.error("deleteTransportZoneFromConfigDS failed. {} could not be deleted.", tzName, e);
            }
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
        } else if (!tunnelType.equals(ITMConstants.TUNNEL_TYPE_VXLAN)
                && !tunnelType.equals(ITMConstants.TUNNEL_TYPE_GRE)) {
            // if tunnel type is some incorrect value, then
            // take VXLAN tunnel type by default
            return TUNNEL_TYPE_MAP.get(ITMConstants.TUNNEL_TYPE_VXLAN);
        }

        // return TunnelTypeBase object corresponding to tunnel-type
        return TUNNEL_TYPE_MAP.get(tunnelType);
    }

    public static List<TzMembership> removeTransportZoneMembership(TunnelEndPoints endPts, List<TzMembership> zones) {
        LOG.trace(" RemoveTransportZoneMembership TEPs {}, Membership to be removed {} ", endPts, zones);
        List<TzMembership> existingTzList = new ArrayList<>(nullToEmpty(endPts.getTzMembership())) ;
        for (TzMembership membership : zones) {
            existingTzList.remove(new TzMembershipBuilder().setZoneName(membership.getZoneName()).build());
        }
        LOG.debug("Modified Membership List {}", existingTzList);
        return existingTzList;
    }

    @Nonnull
    public static List<TzMembership> getOriginalTzMembership(TunnelEndPoints srcTep, BigInteger dpnId,
                                                             Collection<DPNTEPsInfo> meshedDpnList) {
        LOG.trace("Original Membership for source DPN {}, source TEP {}", dpnId, srcTep);
        for (DPNTEPsInfo dstDpn : meshedDpnList) {
            if (dpnId.equals(dstDpn.getDPNID())) {
                List<TunnelEndPoints> endPts = dstDpn.getTunnelEndPoints();
                for (TunnelEndPoints tep : nullToEmpty(endPts)) {
                    if (Objects.equals(tep.getIpAddress(), srcTep.getIpAddress())) {
                        List<TzMembership> tzMemberships = nullToEmpty(tep.getTzMembership());
                        LOG.debug("Original Membership size {}", tzMemberships.size()) ;
                        return tzMemberships;
                    }
                }
            }
        }
        return emptyList();
    }

    public static StateTunnelList buildStateTunnelList(StateTunnelListKey tlKey, String name, boolean state,
                                                       TunnelOperStatus tunOpStatus, IInterfaceManager  ifaceManager,
                                                       DataBroker broker) {
        StateTunnelListBuilder stlBuilder = new StateTunnelListBuilder();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface iface =
                ItmUtils.getInterface(name, ifaceManager);
        IfTunnel ifTunnel = iface.augmentation(IfTunnel.class);
        ParentRefs parentRefs = iface.augmentation(ParentRefs.class);
        if (ifTunnel == null || parentRefs == null) {
            return null;
        }
        DstInfoBuilder dstInfoBuilder = new DstInfoBuilder();
        SrcInfoBuilder srcInfoBuilder = new SrcInfoBuilder();
        dstInfoBuilder.setTepIp(ifTunnel.getTunnelDestination());
        srcInfoBuilder.setTepIp(ifTunnel.getTunnelSource());
        // TODO: Add/Improve logic for device type
        InternalTunnel internalTunnel = ItmUtils.ITM_CACHE.getInternalTunnel(name);
        ExternalTunnel externalTunnel = ItmUtils.ITM_CACHE.getExternalTunnel(name);
        if (internalTunnel == null && externalTunnel == null) {
            // both not present in cache. let us update and try again.
            ItmUtils.updateTunnelsCache(broker);
            internalTunnel = ItmUtils.ITM_CACHE.getInternalTunnel(name);
            externalTunnel = ItmUtils.ITM_CACHE.getExternalTunnel(name);
        }
        if (internalTunnel != null) {
            srcInfoBuilder.setTepDeviceId(internalTunnel.getSourceDPN().toString())
                    .setTepDeviceType(TepTypeInternal.class);
            dstInfoBuilder.setTepDeviceId(internalTunnel.getDestinationDPN().toString())
                    .setTepDeviceType(TepTypeInternal.class);
            stlBuilder.setTransportType(internalTunnel.getTransportType());
        } else if (externalTunnel != null) {
            ExternalTunnel tunnel = ItmUtils.ITM_CACHE.getExternalTunnel(name);
            srcInfoBuilder.setTepDeviceId(tunnel.getSourceDevice())
                    .setTepDeviceType(getDeviceType(tunnel.getSourceDevice()));
            dstInfoBuilder.setTepDeviceId(tunnel.getDestinationDevice())
                    .setTepDeviceType(getDeviceType(tunnel.getDestinationDevice()))
                    .setTepIp(ifTunnel.getTunnelDestination());
            stlBuilder.setTransportType(tunnel.getTransportType());
        }
        stlBuilder.withKey(tlKey).setTunnelInterfaceName(name).setOperState(tunOpStatus).setTunnelState(state)
                .setDstInfo(dstInfoBuilder.build()).setSrcInfo(srcInfoBuilder.build());
        return stlBuilder.build();
    }

    private static Class<? extends TepTypeBase> getDeviceType(String device) {
        if (device.startsWith("hwvtep")) {
            return TepTypeHwvtep.class;
        } else if (InetAddresses.isInetAddress(device)) {
            // In case of external tunnel, destination-device will be of IP address type.
            return TepTypeExternal.class;
        } else {
            return TepTypeInternal.class;
        }
    }

    public static InstanceIdentifier<StateTunnelList> buildStateTunnelListId(StateTunnelListKey tlKey) {
        return InstanceIdentifier.builder(TunnelsState.class)
                .child(StateTunnelList.class, tlKey).build();
    }

    @Nonnull
    public static  Optional<InternalTunnel> getInternalTunnelFromDS(BigInteger srcDpn, BigInteger destDpn,
                                                                    Class<? extends TunnelTypeBase> type,
                                                                    DataBroker dataBroker) {
        InstanceIdentifier<InternalTunnel> pathLogicTunnel = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class,
                        new InternalTunnelKey(destDpn, srcDpn, type));
        //TODO: need to be replaced by cached copy
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, pathLogicTunnel, dataBroker);
    }

    public static boolean isTunnelAggregationUsed(Class<? extends TunnelTypeBase> tunType) {
        return ItmTunnelAggregationHelper.isTunnelAggregationEnabled()
                && (tunType.isAssignableFrom(TunnelTypeVxlan.class)
                || tunType.isAssignableFrom(TunnelTypeLogicalGroup.class));
    }

    public static List<TunnelOptions> buildTunnelOptions(TunnelEndPoints tep, ItmConfig itmConfig) {
        List<TunnelOptions> tunOptions = new ArrayList<>();

        String tos = tep.getOptionTunnelTos();
        if (tos == null) {
            tos = itmConfig.getDefaultTunnelTos();
        }
        /* populate tos option only if its not default value of 0 */
        if (tos != null && !tos.equals("0")) {
            TunnelOptionsBuilder optionsBuilder = new TunnelOptionsBuilder();
            optionsBuilder.withKey(new TunnelOptionsKey("tos"));
            optionsBuilder.setTunnelOption("tos");
            optionsBuilder.setValue(tos);
            tunOptions.add(optionsBuilder.build());
        }

        if (tep.getTunnelType() == TunnelTypeVxlan.class && itmConfig.isGpeExtensionEnabled()) {
            TunnelOptionsBuilder optionsBuilder = new TunnelOptionsBuilder();
            optionsBuilder.withKey(new TunnelOptionsKey("exts"));
            optionsBuilder.setTunnelOption("exts");
            optionsBuilder.setValue("gpe");
            tunOptions.add(optionsBuilder.build());
        }
        return tunOptions.isEmpty() ? null : tunOptions;
    }

    public static ExternalTunnel getExternalTunnelbyExternalTunnelKey(ExternalTunnelKey externalTunnelKey,
                                                                      InstanceIdentifier<ExternalTunnel> path,
                                                                      DataBroker dataBroker) {
        ExternalTunnel exTunnel = ITM_CACHE.getExternalTunnelKeyToExternalTunnels().get(externalTunnelKey);
        if (exTunnel == null) {
            Optional<ExternalTunnel> ext = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
            if (ext.isPresent()) {
                exTunnel = ext.get();
            }
        }
        return exTunnel;
    }

    public static List<DPNTEPsInfo> getDpnTEPsInfos(DataBroker dataBroker) {
        InstanceIdentifier<DpnEndpoints> iid = InstanceIdentifier.builder(DpnEndpoints.class).build();
        Optional<DpnEndpoints> dpnEndpoints = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, iid, dataBroker);
        if (dpnEndpoints.isPresent()) {
            return dpnEndpoints.get().getDPNTEPsInfo();
        } else {
            return new ArrayList<>();
        }
    }

    public static InstanceIdentifier<TransportZone> getTZInstanceIdentifier(String tzName) {
        return InstanceIdentifier.builder(TransportZones.class).child(TransportZone.class,
                new TransportZoneKey(tzName)).build();
    }

    /**
     * Returns the transport zone from Configuration datastore.
     *
     * @param tzName transport zone name
     * @param dataBroker data broker handle to perform operations on datastore
     * @return the TransportZone object in Config DS
     */
    // FIXME: Better is to implement cache to avoid datastore read.
    public static TransportZone getTransportZoneFromConfigDS(String tzName, DataBroker dataBroker) {
        InstanceIdentifier<TransportZone> tzonePath = getTZInstanceIdentifier(tzName);
        Optional<TransportZone> transportZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath,
                dataBroker);
        if (transportZoneOptional.isPresent()) {
            return transportZoneOptional.get();
        }
        return null;
    }

    public static Class<? extends TunnelTypeBase> convertStringToTunnelType(String tunnelType) {
        Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
        if (STRING_CLASS_IMMUTABLE_BI_MAP.containsKey(tunnelType)) {
            tunType = STRING_CLASS_IMMUTABLE_BI_MAP.get(tunnelType);
        }
        return tunType ;
    }

    // TODO Replace this with mdsal's DataObjectUtils.nullToEmpty when upgrading to mdsal 3.0.2
    @Nonnull
    public static <T> List<T> nullToEmpty(final @Nullable List<T> input) {
        return input != null ? input : emptyList();
    }
}
