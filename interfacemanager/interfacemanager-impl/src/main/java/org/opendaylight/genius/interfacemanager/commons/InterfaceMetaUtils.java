/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.commons;

import static org.opendaylight.controller.md.sal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.Datastore.Operational;
import org.opendaylight.genius.infra.TypedReadTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.BatchingUtils;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class InterfaceMetaUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMetaUtils.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final BatchingUtils batchingUtils;
    private final ConcurrentMap<BigInteger, BridgeEntry> bridgeEntryMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<BigInteger, BridgeRefEntry> bridgeRefEntryMap = new ConcurrentHashMap<>();

    @Inject
    public InterfaceMetaUtils(DataBroker dataBroker, IdManagerService idManager, BatchingUtils batchingUtils) {
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

    public BridgeRefEntry getBridgeRefEntryFromOperationalDS(BigInteger dpId) {
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpId);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntry bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL,
                bridgeRefEntryIid, dataBroker).orNull();
        if (bridgeRefEntry != null) {
            addBridgeRefEntryToCache(dpId, bridgeRefEntry);
        }
        return bridgeRefEntry;
    }

    public BridgeRefEntry getBridgeRefEntryFromOperDS(BigInteger dpId) {
        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromCache(dpId);
        if (bridgeRefEntry != null) {
            return bridgeRefEntry;
        }
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpId);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
                .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker).orNull();
        if (bridgeRefEntry != null) {
            addBridgeRefEntryToCache(dpId, bridgeRefEntry);
        }
        return bridgeRefEntry;
    }

    public OvsdbBridgeRef getOvsdbBridgeRef(BigInteger dpId) {

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
        BigInteger dpn = parentRefs.getDatapathNodeIdentifier();
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

    public BridgeEntry getBridgeEntryFromConfigDS(BigInteger dpnId) {
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
        BigInteger dpnId = bridgeEntryInstanceIdentifier.firstKeyOf(BridgeEntry.class).getDpid();
        return getBridgeEntryFromConfigDS(dpnId);
    }

    private BridgeEntry readBridgeEntryFromConfigDS(
            InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier) {
        return IfmUtil.read(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, dataBroker).orNull();
    }

    public static InstanceIdentifier<BridgeInterfaceEntry> getBridgeInterfaceEntryIdentifier(
            BridgeEntryKey bridgeEntryKey, BridgeInterfaceEntryKey bridgeInterfaceEntryKey) {
        return InstanceIdentifier.builder(BridgeInterfaceInfo.class)
                .child(BridgeEntry.class, bridgeEntryKey)
                .child(BridgeInterfaceEntry.class, bridgeInterfaceEntryKey).build();

    }

    public void createBridgeInterfaceEntryInConfigDS(BigInteger dpId, String childInterface) {
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
            throws ReadFailedException {
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
            InterfaceParentEntryKey interfaceParentEntryKey) throws ReadFailedException {
        InstanceIdentifier<InterfaceParentEntry> intfParentIid =
                getInterfaceParentEntryIdentifier(interfaceParentEntryKey);

        return getInterfaceParentEntryFromConfigDS(tx, intfParentIid);
    }

    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(InstanceIdentifier<InterfaceParentEntry> intfId) {
        return IfmUtil.read(LogicalDatastoreType.CONFIGURATION, intfId, dataBroker).orNull();
    }

    public InterfaceParentEntry getInterfaceParentEntryFromConfigDS(ReadTransaction tx,
            InstanceIdentifier<InterfaceParentEntry> intfId) throws ReadFailedException {
        return tx.read(LogicalDatastoreType.CONFIGURATION, intfId).checkedGet().orNull();
    }

    public InterfaceChildEntry getInterfaceChildEntryFromConfigDS(
            InterfaceParentEntryKey interfaceParentEntryKey, InterfaceChildEntryKey interfaceChildEntryKey) {
        InstanceIdentifier<InterfaceChildEntry> intfChildIid =
                getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);

        return getInterfaceChildEntryFromConfigDS(intfChildIid);
    }

    public InterfaceChildEntry getInterfaceChildEntryFromConfigDS(
            InstanceIdentifier<InterfaceChildEntry> intfChildIid) {
        return IfmUtil.read(LogicalDatastoreType.CONFIGURATION, intfChildIid, dataBroker).orNull();
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

    public static void addBridgeRefToBridgeInterfaceEntry(BigInteger dpId, OvsdbBridgeRef ovsdbBridgeRef,
            TypedWriteTransaction<Configuration> tx) {
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier = getBridgeEntryIdentifier(bridgeEntryKey);

        BridgeEntryBuilder bridgeEntryBuilder =
                new BridgeEntryBuilder().withKey(bridgeEntryKey).setBridgeReference(ovsdbBridgeRef);
        tx.merge(bridgeEntryInstanceIdentifier, bridgeEntryBuilder.build(), CREATE_MISSING_PARENTS);
    }

    public static void createBridgeRefEntry(BigInteger dpnId, InstanceIdentifier<?> bridgeIid,
                                            TypedWriteTransaction<Operational> tx) {
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}",
                dpnId, bridgeIid);
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpnId);
        InstanceIdentifier<BridgeRefEntry> bridgeEntryId =
                InterfaceMetaUtils.getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder =
                new BridgeRefEntryBuilder().withKey(bridgeRefEntryKey).setDpid(dpnId)
                        .setBridgeReference(new OvsdbBridgeRef(bridgeIid));
        tx.put(bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(), CREATE_MISSING_PARENTS);
    }

    public static void deleteBridgeRefEntry(BigInteger dpnId, TypedWriteTransaction<Operational> tx) {
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
        transaction.put(id, tunnelInstanceInterface, CREATE_MISSING_PARENTS);

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
        return tx.read(id).get().toJavaUtil().map(TunnelInstanceInterface::getInterfaceName).orElse(null);
    }

    public void deleteBridgeInterfaceEntry(BridgeEntryKey bridgeEntryKey, String interfaceName) {
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey =
                new BridgeInterfaceEntryKey(interfaceName);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey,
                        bridgeInterfaceEntryKey);
        batchingUtils.delete(bridgeInterfaceEntryIid, BatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    public List<TerminationPoint> getTerminationPointsOnBridge(BigInteger dpnId) {
        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromOperDS(dpnId);
        if (bridgeRefEntry == null || bridgeRefEntry.getBridgeReference() == null) {
            LOG.debug("BridgeRefEntry for DPNID {} not found", dpnId);
            return Collections.emptyList();
        }
        InstanceIdentifier<Node> nodeIid =
                        bridgeRefEntry.getBridgeReference().getValue().firstIdentifierOf(Node.class);
        com.google.common.base.Optional<Node> optNode =
            IfmUtil.read(LogicalDatastoreType.OPERATIONAL, nodeIid,  dataBroker);
        if (optNode.isPresent()) {
            return optNode.get().getTerminationPoint();
        }
        return Collections.emptyList();
    }

    // Cache Util methods

    // Start: BridgeEntryCache
    public void addBridgeEntryToCache(BigInteger dpnId, BridgeEntry bridgeEntry) {
        bridgeEntryMap.put(dpnId, bridgeEntry);
    }

    public void addBridgeEntryToCache(BridgeEntry bridgeEntry) {
        addBridgeEntryToCache(bridgeEntry.key().getDpid(), bridgeEntry);
    }

    public void removeFromBridgeEntryCache(BigInteger dpnId) {
        bridgeEntryMap.remove(dpnId);
    }

    public void removeFromBridgeEntryCache(BridgeEntry bridgeEntry) {
        removeFromBridgeEntryCache(bridgeEntry.key().getDpid());
    }

    public BridgeEntry getBridgeEntryFromCache(BigInteger dpnId) {
        return bridgeEntryMap.get(dpnId);
    }
    // End: Bridge Entry Cache

    //Start: BridgeRefEntry Cache
    public void addBridgeRefEntryToCache(BigInteger dpnId, BridgeRefEntry bridgeRefEntry) {
        bridgeRefEntryMap.put(dpnId, bridgeRefEntry);
    }

    public void addBridgeRefEntryToCache(BridgeRefEntry bridgeRefEntry) {
        addBridgeRefEntryToCache(bridgeRefEntry.key().getDpid(), bridgeRefEntry);
    }

    public void removeFromBridgeRefEntryCache(BigInteger dpnId) {
        bridgeRefEntryMap.remove(dpnId);
    }

    public void removeFromBridgeRefEntryCache(BridgeRefEntry bridgeRefEntry) {
        removeFromBridgeRefEntryCache(bridgeRefEntry.key().getDpid());
    }

    public BridgeRefEntry getBridgeRefEntryFromCache(BigInteger dpnId) {
        return bridgeRefEntryMap.get(dpnId);
    }
}
