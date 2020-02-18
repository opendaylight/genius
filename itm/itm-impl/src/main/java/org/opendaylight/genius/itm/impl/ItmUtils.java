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
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.NotHostedTransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.not.hosted.transport.zones.TepsInNotHostedTransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItmUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ItmUtils.class);
    private static final String ITM_LLDP_FLOW_ENTRY =  "ITM Flow Entry ::" + ITMConstants.LLDP_SERVICE_ID;
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
    private static final Uint64 COOKIE_ITM_LLD = Uint64.fromLongBits(
        ITMConstants.COOKIE_ITM.longValue() + ITMConstants.LLDP_SERVICE_ID).intern();

    private ItmUtils() {
    }

    public static final FutureCallback<Void> DEFAULT_WRITE_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(Void result) {
            LOG.debug("Success in Datastore write operation");
        }

        @Override
        public void onFailure(Throwable error) {
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
        Futures.addCallback(tx.commit(), callback, MoreExecutors.directExecutor());
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
        Futures.addCallback(tx.commit(), callback, MoreExecutors.directExecutor());
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
        Futures.addCallback(tx.commit(), callback, MoreExecutors.directExecutor());
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
            Futures.addCallback(tx.commit(), callback ,MoreExecutors.directExecutor());
        }
    }

    //ITM cleanup:portname and vlanId are removed, causes change in generated
    //interface name: This has upgrade impact
    public static String getInterfaceName(final Uint64 datapathid, final String portName, final Integer vlanId) {
        return datapathid + ":" + portName + ":" + vlanId;
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
        String trunkInterfaceName = trunkInterfaceName(parentInterfaceName, localHostName, remoteHostName,
            tunnelTypeStr);
        LOG.trace("trunk interface name is {}", trunkInterfaceName);
        return TUNNEL + getUniqueIdString(trunkInterfaceName);
    }

    public static void releaseIdForTrunkInterfaceName(String parentInterfaceName,
                                                      String localHostName, String remoteHostName, String tunnelType) {
        String tunnelTypeStr;
        if (tunnelType.contains("TunnelTypeGre")) {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_GRE;
        } else {
            tunnelTypeStr = ITMConstants.TUNNEL_TYPE_VXLAN;
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Releasing Id for trunkInterface - {}", trunkInterfaceName(parentInterfaceName, localHostName,
                remoteHostName, tunnelTypeStr));
        }
    }

    private static String trunkInterfaceName(String parentInterfaceName, String localHostName, String remoteHostName,
            String tunnelType) {
        return parentInterfaceName + ":" + localHostName + ":" + remoteHostName + ":" + tunnelType;
    }

    public static String getLogicalTunnelGroupName(Uint64 srcDpnId, Uint64 destDpnId) {
        String groupName = srcDpnId + ":" + destDpnId + ":" + ITMConstants.TUNNEL_TYPE_LOGICAL_GROUP_VXLAN;
        LOG.trace("logical tunnel group name is {}", groupName);
        return TUNNEL +  getUniqueIdString(groupName);
    }

    public static InetAddress getInetAddressFromIpAddress(IpAddress ip) {
        return IetfInetUtil.INSTANCE.inetAddressFor(ip);
    }

    public static InstanceIdentifier<DPNTEPsInfo> getDpnTepInstance(Uint64 dpIdKey) {
        return InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, new DPNTEPsInfoKey(dpIdKey))
                .build();
    }

    public static DPNTEPsInfo createDPNTepInfo(Uint64 dpId, List<TunnelEndPoints> endpoints) {
        return new DPNTEPsInfoBuilder().withKey(new DPNTEPsInfoKey(dpId)).setTunnelEndPoints(endpoints).build();
    }

    public static TunnelEndPoints createTunnelEndPoints(Uint64 dpnId, IpAddress ipAddress, String portName,
                                                        boolean isOfTunnel, int vlanId, List<TzMembership> zones,
                                                        Class<? extends TunnelTypeBase>  tunnelType,
                                                        String tos) {
        // when Interface Mgr provides support to take in Dpn Id
        return new TunnelEndPointsBuilder().withKey(new TunnelEndPointsKey(ipAddress, tunnelType))
                .setTzMembership(zones)
                .setOptionOfTunnel(isOfTunnel).setInterfaceName(ItmUtils.getInterfaceName(dpnId, portName, vlanId))
                .setTunnelType(tunnelType)
                .setOptionTunnelTos(tos)
                .build();
    }

    public static TunnelEndPoints createDummyTunnelEndPoints(Uint64 dpnID, IpAddress ipAddress, boolean ofTunnel,
                                                             String tos, List<TzMembership> zones,
                                                             Class<? extends TunnelTypeBase>  tunnelType,
                                                             String port, int vlanID) {

        return ItmUtils.createTunnelEndPoints(dpnID, ipAddress, port, ofTunnel,vlanID, zones,
                tunnelType, tos);
    }

    public static InstanceIdentifier<Interface> buildId(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static InstanceIdentifier<IfTunnel> buildTunnelId(String ifName) {
        return InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(ifName)).augmentation(IfTunnel.class).build();
    }

    public static Interface buildLogicalTunnelInterface(Uint64 dpn, String ifName, String desc, boolean enabled) {
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

    public static Interface buildTunnelInterface(Uint64 dpn, String ifName, String desc, boolean enabled,
                                                 Class<? extends TunnelTypeBase> tunType, IpAddress localIp,
                                                 IpAddress remoteIp, boolean internal,
                                                 Boolean monitorEnabled,
                                                 Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                                 Integer monitorInterval, boolean useOfTunnel,
                                                 List<TunnelOptions> tunOptions) {

        return buildTunnelInterface(dpn, ifName, desc, enabled, tunType, localIp, remoteIp, internal,
                monitorEnabled, monitorProtocol, monitorInterval,  useOfTunnel, null,
                tunOptions);
    }

    public static Interface buildTunnelInterface(Uint64 dpn, String ifName, String desc, boolean enabled,
                                                 Class<? extends TunnelTypeBase> tunType, IpAddress localIp,
                                                 IpAddress remoteIp, boolean internal,
                                                 Boolean monitorEnabled,
                                                 Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                                 Integer monitorInterval, boolean useOfTunnel, String parentIfaceName,
                                                 List<TunnelOptions> tunnelOptions) {
        InterfaceBuilder builder = new InterfaceBuilder().withKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs =
                new ParentRefsBuilder().setDatapathNodeIdentifier(dpn).setParentInterface(parentIfaceName).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        Long monitoringInterval = null;
        LOG.debug("buildTunnelInterface: monitorProtocol = {} and monitorInterval = {}",
                monitorProtocol.getName(),monitorInterval);

        if (monitorInterval != null) {
            monitoringInterval = monitorInterval.longValue();
        }

        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(remoteIp)
                .setTunnelSource(localIp).setTunnelInterfaceType(tunType)
                .setMonitorEnabled(monitorEnabled).setMonitorProtocol(monitorProtocol)
                .setMonitorInterval(monitoringInterval).setTunnelRemoteIpFlow(useOfTunnel)
                .setTunnelOptions(tunnelOptions).setInternal(internal)
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

    public static InternalTunnel buildInternalTunnel(Uint64 srcDpnId, Uint64 dstDpnId,
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

    public static List<DPNTEPsInfo> getDpnTepListFromDpnId(DPNTEPsInfoCache dpnTEPsInfoCache, List<Uint64> dpnIds) {
        Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        List<DPNTEPsInfo> cfgDpnList = new ArrayList<>();
        for (Uint64 dpnId : dpnIds) {
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
                                                  Uint64 dpnId, IMdsalApiManager mdsalManager) {
        LOG.trace("Installing PUNT to Controller flow in DPN {} ", dpnId);
        List<ActionInfo> listActionInfo = new ArrayList<>();
        listActionInfo.add(new ActionPuntToController());

        try {
            List<MatchInfo> mkMatches = new ArrayList<>();

            mkMatches.add(new MatchTunnelId(Uint64.valueOf(ITMConstants.LLDP_SERVICE_ID)));

            List<InstructionInfo> mkInstructions = new ArrayList<>();
            mkInstructions.add(new InstructionApplyActions(listActionInfo));

            FlowEntity terminatingServiceTableFlowEntity = MDSALUtil
                    .buildFlowEntity(dpnId, NwConstants.INTERNAL_TUNNEL_TABLE,
                            getFlowRef(NwConstants.INTERNAL_TUNNEL_TABLE, ITMConstants.LLDP_SERVICE_ID),
                            5, ITM_LLDP_FLOW_ENTRY, 0, 0,
                            COOKIE_ITM_LLD, mkMatches, mkInstructions);
            mdsalManager.addFlow(tx, terminatingServiceTableFlowEntity);
        } catch (Exception e) {
            LOG.error("Error while setting up Table 36 for {}", dpnId, e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void removeTerminatingServiceTable(TypedReadWriteTransaction<Configuration> tx,
                                                     Uint64 dpnId, IMdsalApiManager mdsalManager) {
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

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static @NonNull HwVtep createHwVtepObject(String topoId, String nodeId, IpAddress ipAddress,
                                                     Class<? extends TunnelTypeBase> tunneltype,
                                                     TransportZone transportZone) {
        HwVtep hwVtep = new HwVtep();
        hwVtep.setHwIp(ipAddress);
        hwVtep.setNodeId(nodeId);
        hwVtep.setTopoId(topoId);
        hwVtep.setTransportZone(transportZone.getZoneName());
        hwVtep.setTunnelType(tunneltype);
        return hwVtep;
    }

    public static String getHwParentIf(String topoId, String srcNodeid) {
        return topoId + ":" + srcNodeid;
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
            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("ITMUtils:SyncWrite , Error writing to datastore (path, data) : ({}, {})", path, data);
            throw new RuntimeException(e.getMessage(), e);
        }
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

    @NonNull
    public static List<String> getInternalTunnelInterfaces(DataBroker dataBroker) {
        Collection<String> internalInterfaces = ITM_CACHE.getAllInternalInterfaces();
        List<String> tunnelList = new ArrayList<>();
        if (internalInterfaces.isEmpty()) {
            tunnelList = getAllInternalTunnlInterfacesFromDS(dataBroker);
        }
        else {
            LOG.debug("Internal Interfaces from Cache size: {}", internalInterfaces.size());
            tunnelList.addAll(internalInterfaces);
        }
        LOG.trace("ItmUtils Internal TunnelList: {}", tunnelList);
        return tunnelList;
    }

    public static List<InternalTunnel> getInternalTunnelsFromCache(DataBroker dataBroker) {
        Collection<InternalTunnel> internalInterfaces = ITM_CACHE.getAllInternalTunnel();
        LOG.trace("getInternalTunnelsFromCache - List of InternalTunnels in the Cache: {} ", internalInterfaces);
        List<InternalTunnel> tunnelList = new ArrayList<>();
        if (internalInterfaces.isEmpty()) {
            LOG.trace("ItmUtils.getInternalTunnelsFromCache invoking getAllInternalTunnlInterfacesFromDS");
            tunnelList = getAllInternalTunnels(dataBroker);
        }
        else {
            LOG.debug("No. of Internal Tunnel Interfaces in cache: {} ", internalInterfaces.size());
            tunnelList.addAll(internalInterfaces);
        }
        LOG.trace("List of Internal Tunnels: {}", tunnelList);
        return tunnelList;
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

    public static List<TunnelEndPoints> getTEPsForDpn(Uint64 srcDpn, Collection<DPNTEPsInfo> dpnList) {
        for (DPNTEPsInfo dpn : dpnList) {
            if (Objects.equals(dpn.getDPNID(), srcDpn)) {
                return new ArrayList<>(dpn.nonnullTunnelEndPoints());
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
        LOG.trace("ItmUtils getInternalTunnel List of InternalTunnels in the Cache {} ", internalTunnel);
        if (internalTunnel == null) {
            internalTunnel = getInternalTunnelFromDS(interfaceName, broker);
        }
        return internalTunnel;
    }

    private static List<String> getAllInternalTunnlInterfacesFromDS(DataBroker broker) {
        List<String> tunnelList = new ArrayList<>();
        List<InternalTunnel> internalTunnels = getAllInternalTunnels(broker);
        if (internalTunnels != null) {
            for (InternalTunnel tunnel : internalTunnels) {
                List<String> tunnelInterfaceNames = tunnel.getTunnelInterfaceNames();
                if (tunnelInterfaceNames != null) {
                    for (String tunnelInterfaceName : tunnelInterfaceNames) {
                        tunnelList.add(tunnelInterfaceName);
                    }
                }
            }
        }
        LOG.debug("Internal Tunnel Interfaces list: {} ", tunnelList);
        return tunnelList;
    }

    private static ExternalTunnel getExternalTunnelFromDS(String interfaceName, DataBroker broker) {
        List<ExternalTunnel> externalTunnels = getAllExternalTunnels(broker);
        if (externalTunnels !=  null) {
            for (ExternalTunnel tunnel : externalTunnels) {
                String tunnelInterfaceName = tunnel.getTunnelInterfaceName();
                if (tunnelInterfaceName != null && tunnelInterfaceName.equalsIgnoreCase(interfaceName)) {
                    LOG.trace("getExternalTunnelFromDS tunnelInterfaceName: {} ", tunnelInterfaceName);
                    return tunnel;
                }
            }
        }
        return null;
    }

    public static ExternalTunnel getExternalTunnel(String interfaceName, DataBroker broker) {
        ExternalTunnel externalTunnel = ITM_CACHE.getExternalTunnel(interfaceName);
        if (externalTunnel == null) {
            externalTunnel = getExternalTunnelFromDS(interfaceName, broker);
        }
        LOG.trace("getExternalTunnel externalTunnel: {} ", externalTunnel);
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
        List<TzMembership> existingTzList = new ArrayList<>(endPts.nonnullTzMembership()) ;
        for (TzMembership membership : zones) {
            existingTzList.remove(new TzMembershipBuilder().setZoneName(membership.getZoneName()).build());
        }
        LOG.debug("Modified Membership List {}", existingTzList);
        return existingTzList;
    }

    @NonNull
    public static List<TzMembership> getOriginalTzMembership(TunnelEndPoints srcTep, Uint64 dpnId,
                                                             Collection<DPNTEPsInfo> meshedDpnList) {
        LOG.trace("Original Membership for source DPN {}, source TEP {}", dpnId, srcTep);
        for (DPNTEPsInfo dstDpn : meshedDpnList) {
            if (dpnId.equals(dstDpn.getDPNID())) {
                for (TunnelEndPoints tep : dstDpn.nonnullTunnelEndPoints()) {
                    if (Objects.equals(tep.getIpAddress(), srcTep.getIpAddress())) {
                        List<TzMembership> tzMemberships = tep.nonnullTzMembership();
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
            internalTunnel = getInternalTunnel(name, broker);
            externalTunnel = getExternalTunnel(name, broker);
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
        return InstanceIdentifier.builder(TunnelsState.class).child(StateTunnelList.class, tlKey).build();
    }

    @NonNull
    public static  Optional<InternalTunnel> getInternalTunnelFromDS(Uint64 srcDpn, Uint64 destDpn,
                                                                    Class<? extends TunnelTypeBase> type,
                                                                    DataBroker dataBroker) {
        InstanceIdentifier<InternalTunnel> pathLogicTunnel = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class,
                        new InternalTunnelKey(destDpn, srcDpn, type));
        //TODO: need to be replaced by cached copy
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, pathLogicTunnel, dataBroker);
    }

    private static InternalTunnel getInternalTunnelFromDS(String interfaceName, DataBroker broker) {
        List<InternalTunnel> internalTunnels = getAllInternalTunnels(broker);
        if (internalTunnels != null) {
            for (InternalTunnel tunnel : internalTunnels) {
                List<String> tunnelInterfaceNames = tunnel.getTunnelInterfaceNames();
                if (tunnelInterfaceNames != null) {
                    for (String tunnelInterfaceName : tunnelInterfaceNames) {
                        if (tunnelInterfaceName.equalsIgnoreCase(interfaceName)) {
                            LOG.trace("ItmUtils getInternalTunnelFromDS {} ", tunnelInterfaceName);
                            return tunnel;
                        }
                    }
                }
            }
        }
        return null;
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
            return new ArrayList<>(dpnEndpoints.get().getDPNTEPsInfo());
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

    public static List<Uint64> getDpIdFromTransportzone(DataBroker dataBroker, String tzone) {
        List<Uint64> listOfDpId = new ArrayList<>();
        InstanceIdentifier<TransportZone> path = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> transportZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                path, dataBroker);
        if (transportZoneOptional.isPresent()) {
            TransportZone transportZone = transportZoneOptional.get();
            if (transportZone.getVteps() != null && !transportZone.getVteps().isEmpty()) {
                List<Vteps> vtepsList = transportZone.getVteps();
                if (vtepsList != null && !vtepsList.isEmpty()) {
                    for (Vteps vtep : vtepsList) {
                        listOfDpId.add(vtep.getDpnId());
                    }
                }
            }
        }
        return listOfDpId;
    }
}
