/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.commons;

import static org.opendaylight.mdsal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;
import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import java.util.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedReadTransaction;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.BatchingUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.instructions.InstructionWriteMetadata;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelDestinationIp;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelSourceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Other;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.DpnToInterfaceList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class InterfaceManagerCommonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerCommonUtils.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private static final String NOVA_PORT_REGEX = "(tap|vhu)[0-9a-f]{8}-[0-9a-f]{2}";
    private static final String TUNNEL_PORT_REGEX = "tun[0-9a-f]{11}";
    private static final String K8S_CNI_PORT_REGEX = "veth[0-9a-f]{8}";
    private static final String NOVA_OR_TUNNEL_PORT_REGEX = NOVA_PORT_REGEX + "|" + TUNNEL_PORT_REGEX;

    private static final Pattern NOVA_OR_TUNNEL_PORT_PATTERN = Pattern.compile(NOVA_OR_TUNNEL_PORT_REGEX);
    private static final Pattern TUNNEL_PORT_PATTERN = Pattern.compile(TUNNEL_PORT_REGEX);
    private static final Pattern NOVA_PORT_PATTERN = Pattern.compile(NOVA_PORT_REGEX);
    private static final Pattern K8S_CNI_PORT_PATTERN = Pattern.compile(K8S_CNI_PORT_REGEX);

    private final ConcurrentHashMap<String, Interface> interfaceConfigMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, org.opendaylight.yang.gen.v1.urn.ietf
        .params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceStateMap =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus> bfdStateMap =
            new ConcurrentHashMap<>();
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final IMdsalApiManager mdsalApiManager;
    private final IdManagerService idManager;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final BatchingUtils batchingUtils;

    public ConcurrentHashMap<String, OperStatus> getBfdStateMap() {
        return bfdStateMap;
    }

    @Inject
    public InterfaceManagerCommonUtils(@Reference DataBroker dataBroker,
                                       @Reference IMdsalApiManager mdsalApiManager,
                                       IdManagerService idManager,
                                       InterfaceMetaUtils interfaceMetaUtils,
                                       BatchingUtils batchingUtils) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.mdsalApiManager = mdsalApiManager;
        this.idManager = idManager;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.batchingUtils = batchingUtils;
    }

    public NodeConnector getNodeConnectorFromInventoryOperDS(NodeConnectorId nodeConnectorId) {
        NodeId nodeId = IfmUtil.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId)).build();

        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, ncIdentifier, dataBroker).orNull();
    }

    public boolean isNodePresent(ReadTransaction tx, NodeConnectorId nodeConnectorId) throws ReadFailedException {
        NodeId nodeID = IfmUtil.getNodeIdFromNodeConnectorId(nodeConnectorId);
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeID)).build();
        return tx.exists(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier).checkedGet();
    }

    public static InstanceIdentifier<Interface> getInterfaceIdentifier(InterfaceKey interfaceKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> interfaceInstanceIdentifierBuilder = InstanceIdentifier
                .builder(Interfaces.class).child(Interface.class, interfaceKey);
        return interfaceInstanceIdentifierBuilder.build();
    }

    public List<Interface> getAllTunnelInterfacesFromCache() {
        return interfaceConfigMap.values().stream()
                .filter(iface -> IfmUtil.getInterfaceType(iface) == InterfaceInfo.InterfaceType.VXLAN_TRUNK_INTERFACE
                        && iface.augmentation(IfTunnel.class).isInternal())
                .collect(Collectors.toList());
    }

    public List<Interface> getAllVlanInterfacesFromCache() {
        return interfaceConfigMap.values().stream()
                .filter(iface -> IfmUtil.getInterfaceType(iface) == InterfaceInfo.InterfaceType.VLAN_INTERFACE)
                .collect(Collectors.toList());
    }

    /**
     * Searches for an interface by its name.
     *
     * @param interfaceName
     *            name of the interface to search for
     * @return the Interface object
     */
    @Deprecated
    @Nullable
    public Interface getInterfaceFromConfigDS(String interfaceName) {
        try (ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
            return getInterfaceFromConfigDS(tx, interfaceName);
        } catch (ReadFailedException e) {
            LOG.error("Error retrieving interface {} from config", interfaceName, e);
            throw new RuntimeException("Error retrieving interface " + interfaceName + " from config", e);
        }
    }

    @Nullable
    public Interface getInterfaceFromConfigDS(ReadTransaction tx, String interfaceName) throws ReadFailedException {
        Interface iface = interfaceConfigMap.get(interfaceName);
        if (iface != null) {
            return iface;
        }
        InstanceIdentifier<Interface> interfaceId = getInterfaceIdentifier(new InterfaceKey(interfaceName));
        Optional<Interface> interfaceOptional = tx.read(LogicalDatastoreType.CONFIGURATION, interfaceId).checkedGet();
        if (interfaceOptional.isPresent()) {
            iface = interfaceOptional.get();
        }
        return iface;
    }

    @Deprecated
    public Interface getInterfaceFromConfigDS(InterfaceKey interfaceKey) {
        return getInterfaceFromConfigDS(interfaceKey.getName());
    }

    public Interface getInterfaceFromConfigDS(ReadTransaction tx, InterfaceKey interfaceKey)
            throws ReadFailedException {
        return getInterfaceFromConfigDS(tx, interfaceKey.getName());
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
        .Interface getInterfaceStateFromCache(
            String interfaceName) {
        return interfaceStateMap.get(interfaceName);
    }

    /**
     * This utility tries to fetch interface-state from cache first,
     * and if not present tries to read it from operational DS.
     *
     *
     * @param interfaceName
     *            name of the logical interface.
     * @return If the data at the supplied path exists, returns interfaces-state object;
     *         if the data at the supplied path does not exist, returns null;
     *
     */
    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
            .Interface getInterfaceState(String interfaceName) {
        return interfaceStateMap.computeIfAbsent(interfaceName, this::getInterfaceStateFromOperDS);
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
        .Interface getInterfaceState(TypedReadTransaction<Operational> tx, String interfaceName)
            throws ExecutionException, InterruptedException {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface
            ifState = getInterfaceStateFromCache(interfaceName);
        if (ifState != null) {
            return ifState;
        }
        ifState = getInterfaceStateFromOperDS(tx, interfaceName);
        return ifState;
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface getInterfaceStateFromOperDS(String interfaceName) {
        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
            IfmUtil.buildStateInterfaceId(interfaceName), dataBroker).orNull();
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface getInterfaceStateFromOperDS(
            TypedReadTransaction<Operational> tx, String interfaceName)
            throws ExecutionException, InterruptedException {
        return tx.read(IfmUtil.buildStateInterfaceId(interfaceName)).get().orNull();
    }

    @Deprecated
    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface getInterfaceStateFromOperDS(
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId) {
        return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, ifStateId, dataBroker).orNull();
    }

    public void addTunnelIngressFlow(TypedWriteTransaction<Configuration> tx, IfTunnel tunnel, Uint64 dpnId,
        long portNo, String interfaceName, int ifIndex) {
        if (isTunnelWithoutIngressFlow(tunnel)) {
            return;
        }
        LOG.debug("add tunnel ingress flow for {}", interfaceName);

        List<MatchInfoBase> matches = new ArrayList<>();
        matches.add(new MatchInPort(dpnId, portNo));
        if (BooleanUtils.isTrue(tunnel.isTunnelRemoteIpFlow())) {
            matches.add(new NxMatchTunnelSourceIp(tunnel.getTunnelDestination().getIpv4Address()));
        }
        if (BooleanUtils.isTrue(tunnel.isTunnelSourceIpFlow())) {
            matches.add(new NxMatchTunnelDestinationIp(tunnel.getTunnelSource().getIpv4Address()));
        }

        List<InstructionInfo> mkInstructions = new ArrayList<>();
        mkInstructions.add(
                new InstructionWriteMetadata(
                    Uint64.fromLongBits(MetaDataUtil.getLportTagMetaData(ifIndex).longValue() | 1L),
                        MetaDataUtil.METADATA_MASK_LPORT_TAG_SH_FLAG));
        short tableId = tunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeMplsOverGre.class)
                ? NwConstants.L3_LFIB_TABLE
                : tunnel.isInternal() ? NwConstants.INTERNAL_TUNNEL_TABLE : NwConstants.DHCP_TABLE_EXTERNAL_TUNNEL;
        mkInstructions.add(new InstructionGotoTable(tableId));

        mdsalApiManager.addFlow(tx, buildTunnelIngressFlowEntity(dpnId, interfaceName, matches, mkInstructions));
    }

    public void removeTunnelIngressFlow(TypedReadWriteTransaction<Configuration> tx, IfTunnel tunnel, Uint64 dpnId,
        String interfaceName) throws ExecutionException, InterruptedException {
        if (isTunnelWithoutIngressFlow(tunnel)) {
            return;
        }
        LOG.debug("remove tunnel ingress flow for {}", interfaceName);
        mdsalApiManager.removeFlow(tx, dpnId,
            InterfaceManagerCommonUtils.getTunnelInterfaceFlowRef(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE,
                interfaceName), NwConstants.VLAN_INTERFACE_INGRESS_TABLE);
    }

    private static boolean isTunnelWithoutIngressFlow(IfTunnel tunnel) {
        return tunnel != null && tunnel.getTunnelInterfaceType().isAssignableFrom(TunnelTypeLogicalGroup.class);
    }

    @NonNull
    private static FlowEntity buildTunnelIngressFlowEntity(Uint64 dpnId, String interfaceName,
            List<MatchInfoBase> matches, List<InstructionInfo> mkInstructions) {
        String flowRef = InterfaceManagerCommonUtils.getTunnelInterfaceFlowRef(dpnId,
                NwConstants.VLAN_INTERFACE_INGRESS_TABLE, interfaceName);
        return MDSALUtil.buildFlowEntity(dpnId, NwConstants.VLAN_INTERFACE_INGRESS_TABLE, flowRef,
                IfmConstants.DEFAULT_FLOW_PRIORITY, interfaceName, 0, 0, NwConstants.COOKIE_VM_INGRESS_TABLE, matches,
                mkInstructions);
    }

    public static String getTunnelInterfaceFlowRef(Uint64 dpnId, short tableId, String ifName) {
        return String.valueOf(dpnId) + tableId + ifName;
    }

    public static void setOpStateForInterface(TypedWriteTransaction<Operational> tx, String interfaceName,
            OperStatus opStatus) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
            .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> interfaceId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder()
                .withKey(new org.opendaylight.yang.gen.v1.urn
                            .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                        interfaceName));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface interfaceData = ifaceBuilder
                .setOperStatus(opStatus).build();
        tx.merge(interfaceId, interfaceData, CREATE_MISSING_PARENTS);
    }

    public void createInterfaceChildEntry(@NonNull TypedWriteTransaction<Configuration> tx, String parentInterface,
        String childInterface) {
        createInterfaceChildEntry(parentInterface, childInterface,
            pair -> tx.put(pair.getKey(), pair.getValue(), CREATE_MISSING_PARENTS));
    }

    private void createInterfaceChildEntry(String parentInterface, String childInterface,
            Consumer<Pair<InstanceIdentifier<InterfaceChildEntry>, InterfaceChildEntry>> writer) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(childInterface);
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryIdentifier = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        InterfaceChildEntry interfaceChildEntry = new InterfaceChildEntryBuilder()
                .withKey(interfaceChildEntryKey)
                .setChildInterface(childInterface)
                .build();
        writer.accept(Pair.of(interfaceChildEntryIdentifier, interfaceChildEntry));
    }

    public void deleteInterfaceChildEntry(String parentInterface, String childInterface) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(childInterface);
        InstanceIdentifier<InterfaceChildEntry> intfId = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        batchingUtils.delete(intfId, BatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    public OperStatus updateStateEntry(Interface interfaceNew, TypedWriteTransaction<Operational> transaction,
           org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
                                                  .rev140508.interfaces.state.Interface ifState) {
        final OperStatus operStatus;
        if (!interfaceNew.isEnabled()) {
            operStatus = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        } else {
            String ncStr = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(ncStr);
            NodeConnector nodeConnector = getNodeConnectorFromInventoryOperDS(nodeConnectorId);
            FlowCapableNodeConnector flowCapableNodeConnector = nodeConnector
                    .augmentation(FlowCapableNodeConnector.class);
            operStatus = getOpState(flowCapableNodeConnector);
        }

        updateOperStatus(interfaceNew.getName(), operStatus,transaction);
        return operStatus;
    }


    public static OperStatus getOpState(FlowCapableNodeConnector flowCapableNodeConnector) {
        OperStatus operStatus = flowCapableNodeConnector.getState().isLive()
            && !flowCapableNodeConnector.getConfiguration().isPORTDOWN() ? OperStatus.Up : OperStatus.Down;
        return operStatus;
    }

    public static void updateOperStatus(String interfaceName,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus,
            TypedWriteTransaction<Operational> transaction) {
        LOG.info("updating operational status {} for interface {}", interfaceName, operStatus);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifChildStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        InterfaceBuilder ifaceBuilderChild = new InterfaceBuilder();
        ifaceBuilderChild.setOperStatus(operStatus);
        ifaceBuilderChild.withKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        transaction.merge(ifChildStateId, ifaceBuilderChild.build());
    }

    public void addStateEntry(String interfaceName, List<ListenableFuture<Void>> futures,
                                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                     .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL,
            tx -> addStateEntry(tx, interfaceName, futures, ifState)));
    }

    public void addStateEntry(TypedWriteTransaction<Operational> tx, String interfaceName,
        List<ListenableFuture<Void>> futures, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        // allocate lport tag and create interface-if-index map.
        // This is done even if interface-state is not present, so that there is
        // no throttling
        // on id allocation even when multiple southbound port_up events come in
        // one shot
        Integer ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, interfaceName);
        interfaceMetaUtils.createLportTagInterfaceMap(interfaceName, ifIndex);
        if (ifState == null) {
            LOG.debug("could not retrieve interface state corresponding to {}, processing will be resumed when "
                    + "interface-state is available", interfaceName);
            return;
        }
        EVENT_LOGGER.debug("IFM-OvsInterfaceConfig,Interface-State,ADD {}", interfaceName);
        LOG.debug("adding interface state for {}", interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus = ifState
                .getOperStatus();
        PhysAddress physAddress = ifState.getPhysAddress();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus adminStatus = ifState
                .getAdminStatus();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ifState.getLowerLayerIf().get(0));
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.Interface interfaceInfo = getInterfaceFromConfigDS(interfaceKey);

        if (interfaceInfo != null && !interfaceInfo.isEnabled()) {
            operStatus = org.opendaylight.yang.gen.v1.urn
                    .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Down;
        }

        List<String> childLowerLayerIfList = new ArrayList<>();
        childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setAdminStatus(adminStatus).setOperStatus(operStatus)
                .setPhysAddress(physAddress).setLowerLayerIf(childLowerLayerIfList);
        ifaceBuilder.setIfIndex(ifIndex).setType(Other.class);
        Class<? extends InterfaceType> interfaceType = null;
        if (interfaceInfo != null) {
            interfaceType = interfaceInfo.getType();
            ifaceBuilder.setType(interfaceType);
        }
        ifaceBuilder.withKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        ifaceBuilder.setStatistics(new StatisticsBuilder().setDiscontinuityTime(DateAndTime
                .getDefaultInstance(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))).build());
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
            .buildStateInterfaceId(interfaceName);
        tx.put(ifStateId, ifaceBuilder.build(), CREATE_MISSING_PARENTS);

        // install ingress flow
        Uint64 dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        long portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        if (interfaceInfo != null && interfaceInfo.isEnabled() && ifState
                .getOperStatus() == org.opendaylight.yang.gen.v1.urn
                .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
            FlowBasedServicesUtils.installLportIngressFlow(dpId, portNo, interfaceInfo, futures, txRunner, ifIndex);
            futures.add(FlowBasedServicesUtils.bindDefaultEgressDispatcherService(txRunner,
                    interfaceInfo, Long.toString(portNo), interfaceName, ifIndex));
        }

        // Update the DpnToInterfaceList OpDS
        createOrUpdateDpnToInterface(dpId, interfaceName, interfaceType);
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface addStateEntry(
        Interface interfaceInfo, String interfaceName, TypedWriteTransaction<Operational> tx, PhysAddress physAddress,
        OperStatus operStatus, AdminStatus adminStatus, NodeConnectorId nodeConnectorId) {
        LOG.debug("adding interface state for {}", interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setType(Other.class)
                .setIfIndex(IfmConstants.DEFAULT_IFINDEX);
        Integer ifIndex;
        Class<? extends InterfaceType> interfaceType = null;
        if (interfaceInfo != null) {
            if (!interfaceInfo.isEnabled()) {
                operStatus = OperStatus.Down;
            }
            interfaceType = interfaceInfo.getType();
            ifaceBuilder.setType(interfaceType);
            // retrieve if-index only for northbound configured interfaces
            ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, interfaceName);
            ifaceBuilder.setIfIndex(ifIndex);
            interfaceMetaUtils.createLportTagInterfaceMap(interfaceName, ifIndex);
        }
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        List<String> childLowerLayerIfList = new ArrayList<>();
        if (nodeConnectorId != null) {
            childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        } else {
            //logical tunnel group doesn't have OF port
            ParentRefs parentRefs = interfaceInfo.augmentation(ParentRefs.class);
            if (parentRefs != null) {
                Uint64 dpId = parentRefs.getDatapathNodeIdentifier();
                String lowref = MDSALUtil.NODE_PREFIX + MDSALUtil.SEPARATOR + dpId + MDSALUtil.SEPARATOR + 0;
                childLowerLayerIfList.add(0, lowref);
            }
        }
        ifaceBuilder.setAdminStatus(adminStatus).setOperStatus(operStatus).setLowerLayerIf(childLowerLayerIfList);
        if (physAddress != null) {
            ifaceBuilder.setPhysAddress(physAddress);
        }
        ifaceBuilder.withKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        ifaceBuilder.setStatistics(new StatisticsBuilder().setDiscontinuityTime(DateAndTime
                .getDefaultInstance(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))).build());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = ifaceBuilder
                .build();
        boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceInfo);
        boolean isOfTunnelInterface = InterfaceManagerCommonUtils.isOfTunnelInterface(interfaceInfo);
        if (isTunnelInterface && !isOfTunnelInterface) {
            batchingUtils.write(ifStateId, ifState, BatchingUtils.EntityType.DEFAULT_OPERATIONAL);
        } else {
            tx.put(ifStateId, ifState, CREATE_MISSING_PARENTS);
        }
        if (nodeConnectorId != null) {
            Uint64 dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            // Update the DpnToInterfaceList OpDS
            createOrUpdateDpnToInterface(dpId, interfaceName, interfaceType);
        }
        return ifState;
    }


    public static void deleteStateEntry(TypedWriteTransaction<Operational> tx, String interfaceName) {
        LOG.debug("removing interface state entry for {}", interfaceName);
        tx.delete(IfmUtil.buildStateInterfaceId(interfaceName));
    }

    public void deleteInterfaceStateInformation(String interfaceName, TypedWriteTransaction<Operational> transaction) {
        LOG.debug("removing interface state information for {}", interfaceName);
        transaction.delete(IfmUtil.buildStateInterfaceId(interfaceName));
        interfaceMetaUtils.removeLportTagInterfaceMap(transaction, interfaceName);
    }

    // For trunk interfaces, binding to a parent interface which is already
    // bound to another trunk interface should not
    // be allowed
    public boolean createInterfaceChildEntryIfNotPresent(TypedWriteTransaction<Configuration> tx,
        String parentInterface, String childInterface, IfL2vlan.L2vlanMode l2vlanMode) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier = InterfaceMetaUtils
                .getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        InterfaceParentEntry interfaceParentEntry = interfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceParentEntryIdentifier);

        if (interfaceParentEntry != null) {
            List<InterfaceChildEntry> interfaceChildEntries = interfaceParentEntry.getInterfaceChildEntry();
            if (interfaceChildEntries != null) {
                for (InterfaceChildEntry interfaceChildEntry : interfaceChildEntries) {
                    String curChildInterface = interfaceChildEntry.getChildInterface();
                    if (childInterface.equals(curChildInterface)) {
                        LOG.trace("Child entry for interface {} already exists", childInterface);
                        return false;
                    }

                    Interface iface = getInterfaceFromConfigDS(curChildInterface);
                    if (l2vlanMode == IfL2vlan.L2vlanMode.Trunk && isTrunkInterface(iface)) {
                        LOG.error(
                                "Trying to bind child interface {} of type Trunk to parent interface {},"
                                        + "but it is already bound to a trunk interface {}",
                                childInterface, parentInterface, curChildInterface);
                        return false;
                    }
                }
            }
        }

        LOG.info("Creating child interface {} of type {} bound on parent-interface {}",
                childInterface, l2vlanMode, parentInterface);
        createInterfaceChildEntry(tx, parentInterface, childInterface);
        return true;
    }

    public static boolean isTrunkInterface(Interface iface) {
        if (iface != null) {
            IfL2vlan ifL2vlan = iface.augmentation(IfL2vlan.class);
            return ifL2vlan != null && IfL2vlan.L2vlanMode.Trunk.equals(ifL2vlan.getL2vlanMode());
        }

        return false;
    }

    public boolean deleteParentInterfaceEntry(String parentInterface) {
        if (parentInterface == null) {
            return false;
        }
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentInterface);
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIdentifier = InterfaceMetaUtils
                .getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        batchingUtils.delete(interfaceParentEntryIdentifier, BatchingUtils.EntityType.DEFAULT_CONFIG);
        return true;
    }

    /*
     * update operational state of interface based on events like tunnel
     * monitoring
     */
    public static void updateOpState(TypedWriteTransaction<Operational> tx, String interfaceName,
            OperStatus operStatus) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = IfmUtil
                .buildStateInterfaceId(interfaceName);
        LOG.debug("updating tep interface state as {} for {}", operStatus.name(), interfaceName);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setOperStatus(operStatus);
        ifaceBuilder.withKey(IfmUtil.getStateInterfaceKeyFromName(interfaceName));
        tx.merge(ifStateId, ifaceBuilder.build());
    }

    public static boolean isTunnelInterface(Interface interfaceInfo) {
        return interfaceInfo != null && interfaceInfo.augmentation(IfTunnel.class) != null;
    }

    public static boolean isOfTunnelInterface(Interface interfaceInfo) {
        return isTunnelInterface(interfaceInfo)
                && SouthboundUtils.isOfTunnel(interfaceInfo.augmentation(IfTunnel.class));
    }

    public static boolean isVlanInterface(Interface interfaceInfo) {
        return interfaceInfo != null && interfaceInfo.augmentation(IfL2vlan.class) != null;
    }

    // Cache Util methods
    public void addInterfaceToCache(Interface iface) {
        interfaceConfigMap.put(iface.getName(), iface);
    }

    public void removeFromInterfaceCache(Interface iface) {
        interfaceConfigMap.remove(iface.getName());
    }

    public void addInterfaceStateToCache(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface iface) {
        interfaceStateMap.put(iface.getName(), iface);
    }

    public void removeFromInterfaceStateCache(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface iface) {
        interfaceStateMap.remove(iface.getName());
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus getBfdStateFromCache(String interfaceName) {
        return bfdStateMap.get(interfaceName);
    }

    public void addBfdStateToCache(String interfaceName,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus) {
        bfdStateMap.put(interfaceName, operStatus);
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus removeBfdStateFromCache(
            String interfaceName) {
        return bfdStateMap.remove(interfaceName);
    }

    public static boolean isNovaOrTunnelPort(String portName) {
        Matcher matcher = NOVA_OR_TUNNEL_PORT_PATTERN.matcher(portName);
        return matcher.matches();
    }

    public static boolean isNovaPort(String portName) {
        Matcher matcher = NOVA_PORT_PATTERN.matcher(portName);
        return matcher.matches();
    }

    public static boolean isTunnelPort(String portName) {
        Matcher matcher = TUNNEL_PORT_PATTERN.matcher(portName);
        return matcher.matches();
    }

    public static boolean isK8SPort(String portName) {
        Matcher matcher = K8S_CNI_PORT_PATTERN.matcher(portName);
        return matcher.matches();
    }

    public void createOrUpdateDpnToInterface(Uint64 dpId, String infName,
                                             Class<? extends InterfaceType> interfaceType) {
        DpnToInterfaceKey dpnToInterfaceKey = new DpnToInterfaceKey(dpId);
        InterfaceNameEntryKey interfaceNameEntryKey = new InterfaceNameEntryKey(infName);
        InstanceIdentifier<InterfaceNameEntry> intfid = InstanceIdentifier.builder(DpnToInterfaceList.class)
                                                        .child(DpnToInterface.class, dpnToInterfaceKey)
                                                        .child(InterfaceNameEntry.class, interfaceNameEntryKey)
                                                        .build();
        InterfaceNameEntryBuilder entryBuilder =
                new InterfaceNameEntryBuilder().withKey(interfaceNameEntryKey).setInterfaceName(infName);
        if (interfaceType != null) {
            entryBuilder.setInterfaceType(interfaceType);
        }
        batchingUtils.write(intfid, entryBuilder.build(), BatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    public List<InterfaceNameEntry> getAllInterfaces(Uint64 dpnId) {
        DpnToInterfaceKey dpnToInterfaceKey = new DpnToInterfaceKey(dpnId);
        InstanceIdentifier<DpnToInterface> dpninterfaceListId =
            InstanceIdentifier.builder(DpnToInterfaceList.class).child(DpnToInterface.class, dpnToInterfaceKey).build();
        Optional<DpnToInterface> interfaceList = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, dpninterfaceListId,
            dataBroker);
        if (interfaceList.isPresent()) {
            return interfaceList.get().getInterfaceNameEntry();
        }
        return null;
    }

    public static void deleteDpnToInterface(Uint64 dpId, String infName, TypedReadWriteTransaction<Operational> tx)
            throws ExecutionException, InterruptedException {
        DpnToInterfaceKey dpnToInterfaceKey = new DpnToInterfaceKey(dpId);
        InstanceIdentifier<DpnToInterface> dpnToInterfaceId = InstanceIdentifier.builder(DpnToInterfaceList.class)
                .child(DpnToInterface.class, dpnToInterfaceKey).build();
        Optional<DpnToInterface> dpnToInterfaceOptional = tx.read(dpnToInterfaceId).get();
        if (!dpnToInterfaceOptional.isPresent()) {
            LOG.debug("DPN {} is already removed from the Operational DS", dpId);
            return;
        }

        List<InterfaceNameEntry> interfaceNameEntries = dpnToInterfaceOptional.get().getInterfaceNameEntry();
        InterfaceNameEntryKey interfaceNameEntryKey = new InterfaceNameEntryKey(infName);
        InstanceIdentifier<InterfaceNameEntry> intfid = InstanceIdentifier.builder(DpnToInterfaceList.class)
                .child(DpnToInterface.class, dpnToInterfaceKey)
                .child(InterfaceNameEntry.class, interfaceNameEntryKey)
                .build();
        tx.delete(intfid);

        if (interfaceNameEntries != null && interfaceNameEntries.size() <= 1) {
            tx.delete(dpnToInterfaceId);
        }
    }

    public static String getPortNameForInterface(NodeConnectorId nodeConnectorId, String portName) {
        if (isNovaOrTunnelPort(portName) || isK8SPort(portName)) {
            return portName;
        } else {
            return getDpnPrefixedPortName(nodeConnectorId, portName);
        }
    }

    public static String getPortNameForInterface(String dpnId, String portName) {
        if (isNovaOrTunnelPort(portName) || isK8SPort(portName)) {
            return portName;
        } else {
            return getDpnPrefixedPortName(dpnId, portName);
        }
    }

    private static String getDpnPrefixedPortName(NodeConnectorId nodeConnectorId, String portName) {
        String dpnId = IfmUtil.getDpnStringFromNodeConnectorId(nodeConnectorId);
        return getDpnPrefixedPortName(dpnId, portName);
    }

    private static String getDpnPrefixedPortName(String dpnId, String portName) {
        return dpnId + IfmConstants.OF_URI_SEPARATOR + portName;
    }

    public boolean isTunnelInternal(String interfaceName) {
        return isTunnelPort(interfaceName) && getInterfaceFromConfigDS(interfaceName) == null;
    }
}
