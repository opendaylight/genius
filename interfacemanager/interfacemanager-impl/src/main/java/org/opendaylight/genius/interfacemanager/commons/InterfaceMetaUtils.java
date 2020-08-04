/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.commons;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.BatchingUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.util.Datastore.Configuration;
import org.opendaylight.mdsal.binding.util.Datastore.Operational;
import org.opendaylight.mdsal.binding.util.TypedReadTransaction;
import org.opendaylight.mdsal.binding.util.TypedWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeInterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.IfIndexesInterfaceMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.TunnelInstanceInterfaceMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.tunnel.instance._interface.map.TunnelInstanceInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.tunnel.instance._interface.map.TunnelInstanceInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.tunnel.instance._interface.map.TunnelInstanceInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class InterfaceMetaUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMetaUtils.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final BatchingUtils batchingUtils;
    private final ConcurrentMap<Uint64, BridgeEntry> bridgeEntryMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Uint64, BridgeRefEntry> bridgeRefEntryMap = new ConcurrentHashMap<>();

    @Inject
    public InterfaceMetaUtils(@Reference DataBroker dataBroker,
                              IdManagerService idManager,
                              BatchingUtils batchingUtils) {
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.batchingUtils = batchingUtils;
    }

    public static InstanceIdentifier<BridgeRefEntry> getBridgeRefEntryIdentifier(BridgeRefEntryKey bridgeRefEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<BridgeRefEntry> bridgeRefEntryInstanceIdentifierBuilder =
                InstanceIdentifier.builder(BridgeRefInfo.class)
                        .child(BridgeRefEntry.class, bridgeRefEntryKey);
        return bridgeRefEntryInstanceIdentifierBuilder.build();
    }

    public BridgeRefEntry getBridgeRefEntryFromOperationalDS(Uint64 dpId) {
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpId);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntry bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
                bridgeRefEntryIid, dataBroker).orElse(null);
        if (bridgeRefEntry != null) {
            addBridgeRefEntryToCache(dpId, bridgeRefEntry);
        }
        return bridgeRefEntry;
    }

    public BridgeRefEntry getBridgeRefEntryFromOperDS(Uint64 dpId) {
        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromCache(dpId);
        if (bridgeRefEntry != null) {
            return bridgeRefEntry;
        }
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpId);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker).orElse(null);
        if (bridgeRefEntry != null) {
            addBridgeRefEntryToCache(dpId, bridgeRefEntry);
        }
        return bridgeRefEntry;
    }

    public OvsdbBridgeRef getOvsdbBridgeRef(Uint64 dpId) {

        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromOperDS(dpId);

        if (bridgeRefEntry == null) {
            // bridge ref entry will be null if the bridge is disconnected from controller.
            // In that case, fetch bridge reference from bridge interface entry config DS
            BridgeEntry bridgeEntry = getBridgeEntryFromConfigDS(dpId);
            if (bridgeEntry == null) {
                return null;
            }
            return  bridgeEntry.getBridgeReference();
        }
        return bridgeRefEntry.getBridgeReference();
    }

    public BridgeRefEntry getBridgeReferenceForInterface(Interface interfaceInfo) {
        ParentRefs parentRefs = interfaceInfo.augmentation(ParentRefs.class);
        Uint64 dpn = parentRefs.getDatapathNodeIdentifier();
        return getBridgeRefEntryFromOperDS(dpn);
    }

    public boolean bridgeExists(BridgeRefEntry bridgeRefEntry) {
        if (bridgeRefEntry != null && bridgeRefEntry.getBridgeReference() != null) {
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry.getBridgeReference().getValue();
            return IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker).isPresent();
        }
        return false;
    }

    public static InstanceIdentifier<BridgeEntry> getBridgeEntryIdentifier(BridgeEntryKey bridgeEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<BridgeEntry> bridgeEntryIdBuilder =
                InstanceIdentifier.builder(BridgeInterfaceInfo.class).child(BridgeEntry.class, bridgeEntryKey);
        return bridgeEntryIdBuilder.build();
    }

    public BridgeEntry getBridgeEntryFromConfigDS(Uint64 dpnId) {
        BridgeEntry bridgeEntry = getBridgeEntryFromCache(dpnId);
        if (bridgeEntry != null) {
            return bridgeEntry;
        }
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpnId);
        InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier =
                InterfaceMetaUtils.getBridgeEntryIdentifier(bridgeEntryKey);
        LOG.debug("Trying to retrieve bridge entry from config for Id: {}", bridgeEntryInstanceIdentifier);
        bridgeEntry = readBridgeEntryFromConfigDS(bridgeEntryInstanceIdentifier);
        if (bridgeEntry != null) {
            addBridgeEntryToCache(dpnId, bridgeEntry);
        }
        return bridgeEntry;
    }

    public BridgeEntry getBridgeEntryFromConfigDS(InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier) {
        Uint64 dpnId = bridgeEntryInstanceIdentifier.firstKeyOf(BridgeEntry.class).getDpid();
        return getBridgeEntryFromConfigDS(dpnId);
    }

    private BridgeEntry readBridgeEntryFromConfigDS(
            InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier) {
        return IfmUtil.read(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, dataBroker).orElse(null);
    }

    public static InstanceIdentifier<BridgeInterfaceEntry> getBridgeInterfaceEntryIdentifier(
            BridgeEntryKey bridgeEntryKey, BridgeInterfaceEntryKey bridgeInterfaceEntryKey) {
        return InstanceIdentifier.builder(BridgeInterfaceInfo.class)
                .child(BridgeEntry.class, bridgeEntryKey)
                .child(BridgeInterfaceEntry.class, bridgeInterfaceEntryKey).build();

    }

    public void createBridgeInterfaceEntryInConfigDS(Uint64 dpId, String childInterface) {
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey = new BridgeInterfaceEntryKey(childInterface);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey, bridgeInterfaceEntryKey);
        BridgeInterfaceEntryBuilder entryBuilder = new BridgeInterfaceEntryBuilder().withKey(bridgeInterfaceEntryKey)
                .setInterfaceName(childInterface);
        batchingUtils.write(bridgeInterfaceEntryIid, entryBuilder.build(), BatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    public static InstanceIdentifier<InterfaceParentEntry> getInterfaceParentEntryIdentifier(
            InterfaceParentEntryKey interfaceParentEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<InterfaceParentEntry> intfIdBuilder =
                InstanceIdentifier.builder(InterfaceChildInfo.class)
                        .child(InterfaceParentEntry.class, interfaceParentEntryKey);
        return intfIdBuilder.build();
    }

    public static InstanceIdentifier<InterfaceChildEntry> getInterfaceChildEntryIdentifier(
            InterfaceParentEntryKey interfaceParentEntryKey, InterfaceChildEntryKey interfaceChildEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<InterfaceChildEntry> intfIdBuilder =
                InstanceIdentifier.builder(InterfaceChildInfo.class)
                        .child(InterfaceParentEntry.class, interfaceParentEntryKey)
                        .child(InterfaceChildEntry.class, interfaceChildEntryKey);
        return intfIdBuilder.build();
    }

    @Deprecated
    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(String interfaceName) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(interfaceName);
        InterfaceParentEntry interfaceParentEntry = getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey);
        return interfaceParentEntry;
    }

    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(ReadTransaction tx, String interfaceName)
            throws ExecutionException, InterruptedException {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(interfaceName);
        InterfaceParentEntry interfaceParentEntry = getInterfaceParentEntryFromConfigDS(tx, interfaceParentEntryKey);
        return interfaceParentEntry;
    }

    @Deprecated
    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(InterfaceParentEntryKey interfaceParentEntryKey) {
        InstanceIdentifier<InterfaceParentEntry> intfParentIid =
                getInterfaceParentEntryIdentifier(interfaceParentEntryKey);

        return getInterfaceParentEntryFromConfigDS(intfParentIid);
    }

    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(ReadTransaction tx,
            InterfaceParentEntryKey interfaceParentEntryKey) throws ExecutionException, InterruptedException {
        InstanceIdentifier<InterfaceParentEntry> intfParentIid =
                getInterfaceParentEntryIdentifier(interfaceParentEntryKey);

        return getInterfaceParentEntryFromConfigDS(tx, intfParentIid);
    }

    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(InstanceIdentifier<InterfaceParentEntry> intfId) {
        return IfmUtil.read(LogicalDatastoreType.CONFIGURATION, intfId, dataBroker).orElse(null);
    }

    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(ReadTransaction tx,
            InstanceIdentifier<InterfaceParentEntry> intfId) throws ExecutionException, InterruptedException {
        return tx.read(LogicalDatastoreType.CONFIGURATION, intfId).get().orElse(null);
    }

    public InterfaceChildEntry getInterfaceChildEntryFromConfigDS(
            InterfaceParentEntryKey interfaceParentEntryKey, InterfaceChildEntryKey interfaceChildEntryKey) {
        InstanceIdentifier<InterfaceChildEntry> intfChildIid =
                getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);

        return getInterfaceChildEntryFromConfigDS(intfChildIid);
    }

    public InterfaceChildEntry getInterfaceChildEntryFromConfigDS(
            InstanceIdentifier<InterfaceChildEntry> intfChildIid) {
        return IfmUtil.read(LogicalDatastoreType.CONFIGURATION, intfChildIid, dataBroker).orElse(null);
    }

    public void createLportTagInterfaceMap(String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {} ifIndex {}",infName, ifIndex);
        InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class)
                .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
        IfIndexInterface ifIndexInterface = new IfIndexInterfaceBuilder().setIfIndex(ifIndex)
                .withKey(new IfIndexInterfaceKey(ifIndex)).setInterfaceName(infName).build();
        batchingUtils.write(id, ifIndexInterface, BatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    public int removeLportTagInterfaceMap(WriteTransaction tx, String infName) {
        // workaround to get the id to remove from lport tag interface map
        Integer ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, infName);
        IfmUtil.releaseId(idManager, IfmConstants.IFM_IDPOOL_NAME, infName);
        LOG.debug("removing lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class)
                .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
        tx.delete(LogicalDatastoreType.OPERATIONAL, id);
        return ifIndex;
    }

    public int removeLportTagInterfaceMap(TypedWriteTransaction<Operational> tx, String infName) {
        // workaround to get the id to remove from lport tag interface map
        Integer ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, infName);
        IfmUtil.releaseId(idManager, IfmConstants.IFM_IDPOOL_NAME, infName);
        LOG.debug("removing lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class)
            .child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
        tx.delete(id);
        return ifIndex;
    }

    public static void addBridgeRefToBridgeInterfaceEntry(Uint64 dpId, OvsdbBridgeRef ovsdbBridgeRef,
            TypedWriteTransaction<Configuration> tx) {
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier = getBridgeEntryIdentifier(bridgeEntryKey);

        BridgeEntryBuilder bridgeEntryBuilder =
                new BridgeEntryBuilder().withKey(bridgeEntryKey).setBridgeReference(ovsdbBridgeRef);
        tx.mergeParentStructureMerge(bridgeEntryInstanceIdentifier, bridgeEntryBuilder.build());
    }

    public static void createBridgeRefEntry(Uint64 dpnId, InstanceIdentifier<?> bridgeIid,
                                            TypedWriteTransaction<Operational> tx) {
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}",
                dpnId, bridgeIid);
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpnId);
        InstanceIdentifier<BridgeRefEntry> bridgeEntryId =
                InterfaceMetaUtils.getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder =
                new BridgeRefEntryBuilder().withKey(bridgeRefEntryKey).setDpid(dpnId)
                        .setBridgeReference(new OvsdbBridgeRef(bridgeIid));
        tx.mergeParentStructurePut(bridgeEntryId, tunnelDpnBridgeEntryBuilder.build());
    }

    public static void deleteBridgeRefEntry(Uint64 dpnId, TypedWriteTransaction<Operational> tx) {
        LOG.debug("Deleting bridge ref entry for dpn: {}", dpnId);
        tx.delete(InterfaceMetaUtils.getBridgeRefEntryIdentifier(new BridgeRefEntryKey(dpnId)));
    }

    public static void createTunnelToInterfaceMap(String tunnelInstanceId,
                                                  String infName,
                                                  TypedWriteTransaction<Operational> transaction) {
        LOG.debug("creating tunnel instance identifier to interface map for {}",infName);
        InstanceIdentifier<TunnelInstanceInterface> id = InstanceIdentifier.builder(TunnelInstanceInterfaceMap.class)
                .child(TunnelInstanceInterface.class, new TunnelInstanceInterfaceKey(tunnelInstanceId)).build();
        TunnelInstanceInterface tunnelInstanceInterface = new TunnelInstanceInterfaceBuilder()
                .setTunnelInstanceIdentifier(tunnelInstanceId).withKey(new TunnelInstanceInterfaceKey(tunnelInstanceId))
                .setInterfaceName(infName).build();
        transaction.mergeParentStructurePut(id, tunnelInstanceInterface);

    }

    public static void createTunnelToInterfaceMap(String infName,InstanceIdentifier<Node> nodeId,
                                                  TypedWriteTransaction<Operational> transaction,
                                                  IfTunnel ifTunnel) {
        InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier = org.opendaylight.genius.interfacemanager.renderer
                .hwvtep.utilities.SouthboundUtils.createTunnelsInstanceIdentifier(nodeId,
                        ifTunnel.getTunnelSource(), ifTunnel.getTunnelDestination());
        createTunnelToInterfaceMap(tunnelsInstanceIdentifier.toString(), infName, transaction);
    }

    public static void removeTunnelToInterfaceMap(InstanceIdentifier<Node> nodeId,
                                                  TypedWriteTransaction<Operational> transaction,
                                                  IfTunnel ifTunnel) {
        InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier = org.opendaylight.genius.interfacemanager.renderer
                .hwvtep.utilities.SouthboundUtils.createTunnelsInstanceIdentifier(
                        nodeId, ifTunnel.getTunnelSource(), ifTunnel.getTunnelDestination());
        transaction.delete(tunnelsInstanceIdentifier);
    }

    public static String getInterfaceForTunnelInstanceIdentifier(String tunnelInstanceId,
                                                                 TypedReadTransaction<Operational> tx)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<TunnelInstanceInterface> id = InstanceIdentifier.builder(TunnelInstanceInterfaceMap.class)
                .child(TunnelInstanceInterface.class, new TunnelInstanceInterfaceKey(tunnelInstanceId)).build();
        return tx.read(id).get().map(TunnelInstanceInterface::getInterfaceName).orElse(null);
    }

    public void deleteBridgeInterfaceEntry(BridgeEntryKey bridgeEntryKey,
            Map<BridgeInterfaceEntryKey, BridgeInterfaceEntry> bridgeInterfaceEntries,
                                           InstanceIdentifier<BridgeEntry> bridgeEntryIid, String interfaceName) {
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey =
                new BridgeInterfaceEntryKey(interfaceName);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey,
                        bridgeInterfaceEntryKey);

        if (bridgeInterfaceEntries.size() <= 1) {
            LOG.debug("{} is last bridge-interface entry for DPN : {}", interfaceName, bridgeEntryKey.getDpid());
            batchingUtils.delete(bridgeEntryIid, BatchingUtils.EntityType.DEFAULT_CONFIG);
        } else {
            // No point deleting interface individually if bridge entry is being deleted
            // Note: Will this cause issue in listener code? Does it expect separate notifications for two?
            LOG.debug("deleting bridge-interface entry {} for DPN : {}", interfaceName, bridgeEntryKey.getDpid());
            batchingUtils.delete(bridgeInterfaceEntryIid, BatchingUtils.EntityType.DEFAULT_CONFIG);
        }
    }

    public Map<TerminationPointKey, TerminationPoint> getTerminationPointsOnBridge(Uint64 dpnId) {
        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromOperDS(dpnId);
        if (bridgeRefEntry == null || bridgeRefEntry.getBridgeReference() == null) {
            LOG.debug("BridgeRefEntry for DPNID {} not found", dpnId);
            return Collections.emptyMap();
        }
        InstanceIdentifier<Node> nodeIid =
                        bridgeRefEntry.getBridgeReference().getValue().firstIdentifierOf(Node.class);
        Optional<Node> optNode =
            IfmUtil.read(LogicalDatastoreType.OPERATIONAL, nodeIid,  dataBroker);
        if (optNode.isPresent()) {
            return optNode.get().getTerminationPoint();
        }
        return Collections.emptyMap();
    }

    // Cache Util methods

    // Start: BridgeEntryCache
    public void addBridgeEntryToCache(Uint64 dpnId, BridgeEntry bridgeEntry) {
        bridgeEntryMap.put(dpnId, bridgeEntry);
    }

    public void addBridgeEntryToCache(BridgeEntry bridgeEntry) {
        addBridgeEntryToCache(bridgeEntry.key().getDpid(), bridgeEntry);
    }

    public void removeFromBridgeEntryCache(Uint64 dpnId) {
        bridgeEntryMap.remove(dpnId);
    }

    public void removeFromBridgeEntryCache(BridgeEntry bridgeEntry) {
        removeFromBridgeEntryCache(bridgeEntry.key().getDpid());
    }

    public BridgeEntry getBridgeEntryFromCache(Uint64 dpnId) {
        return bridgeEntryMap.get(dpnId);
    }
    // End: Bridge Entry Cache

    //Start: BridgeRefEntry Cache
    public void addBridgeRefEntryToCache(Uint64 dpnId, BridgeRefEntry bridgeRefEntry) {
        bridgeRefEntryMap.put(dpnId, bridgeRefEntry);
    }

    public void addBridgeRefEntryToCache(BridgeRefEntry bridgeRefEntry) {
        addBridgeRefEntryToCache(bridgeRefEntry.key().getDpid(), bridgeRefEntry);
    }

    public void removeFromBridgeRefEntryCache(Uint64 dpnId) {
        bridgeRefEntryMap.remove(dpnId);
    }

    public void removeFromBridgeRefEntryCache(BridgeRefEntry bridgeRefEntry) {
        removeFromBridgeRefEntryCache(bridgeRefEntry.key().getDpid());
    }

    public BridgeRefEntry getBridgeRefEntryFromCache(Uint64 dpnId) {
        return bridgeRefEntryMap.get(dpnId);
    }

    public Map getBridgeRefEntryMap() {
        return Collections.unmodifiableMap(bridgeRefEntryMap);
    }
}
