/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.commons;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceMetaUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMetaUtils.class);
    private static ConcurrentHashMap<BigInteger, BridgeEntry> bridgeEntryMap = new ConcurrentHashMap();
    private static ConcurrentHashMap<BigInteger, BridgeRefEntry> bridgeRefEntryMap = new ConcurrentHashMap();

    public static InstanceIdentifier<BridgeRefEntry> getBridgeRefEntryIdentifier(BridgeRefEntryKey bridgeRefEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<BridgeRefEntry> bridgeRefEntryInstanceIdentifierBuilder =
                InstanceIdentifier.builder(BridgeRefInfo.class)
                        .child(BridgeRefEntry.class, bridgeRefEntryKey);
        return bridgeRefEntryInstanceIdentifierBuilder.build();
    }


    public static BridgeRefEntry getBridgeRefEntryFromOperDS(BigInteger dpId, DataBroker dataBroker) {
        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromCache(dpId);
        if(bridgeRefEntry != null) {
            return bridgeRefEntry;
        }
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpId);
        InstanceIdentifier<BridgeRefEntry> bridgeRefEntryIid = InterfaceMetaUtils
            .getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        bridgeRefEntry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker).orNull();
        if(bridgeRefEntry != null) {
            addBridgeRefEntryToCache(dpId, bridgeRefEntry);
        }
        return bridgeRefEntry;
    }

    public static OvsdbBridgeRef getOvsdbBridgeRef(BigInteger dpId, DataBroker dataBroker) {

        BridgeRefEntry bridgeRefEntry = getBridgeRefEntryFromOperDS(dpId, dataBroker);

        if(bridgeRefEntry == null){
            // bridge ref entry will be null if the bridge is disconnected from controller.
            // In that case, fetch bridge reference from bridge interface entry config DS
            BridgeEntry bridgeEntry = getBridgeEntryFromConfigDS(dpId, dataBroker);
            if(bridgeEntry == null){
                return null;
            }
            return  bridgeEntry.getBridgeReference();
        }
        return bridgeRefEntry.getBridgeReference();
    }

    public static BridgeRefEntry getBridgeReferenceForInterface(Interface interfaceInfo,
                                                                DataBroker dataBroker) {
        ParentRefs parentRefs = interfaceInfo.getAugmentation(ParentRefs.class);
        BigInteger dpn = parentRefs.getDatapathNodeIdentifier();
        return getBridgeRefEntryFromOperDS(dpn, dataBroker);
    }

    public static boolean bridgeExists(BridgeRefEntry bridgeRefEntry,
                                       DataBroker dataBroker) {
        if (bridgeRefEntry != null && bridgeRefEntry.getBridgeReference() != null) {
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry.getBridgeReference().getValue();
            Optional<OvsdbBridgeAugmentation> bridgeNodeOptional =
                    IfmUtil.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);
            if (bridgeNodeOptional.isPresent()) {
                return true;
            }
        }
        return false;
    }
    public static InstanceIdentifier<BridgeEntry> getBridgeEntryIdentifier(BridgeEntryKey bridgeEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<BridgeEntry> bridgeEntryIdBuilder =
                InstanceIdentifier.builder(BridgeInterfaceInfo.class).child(BridgeEntry.class, bridgeEntryKey);
        return bridgeEntryIdBuilder.build();
    }

    public static InstanceIdentifier<BridgeInterfaceEntry> getBridgeInterfaceEntryIdentifier(BridgeEntryKey bridgeEntryKey,
                                                                                             BridgeInterfaceEntryKey bridgeInterfaceEntryKey) {
        return InstanceIdentifier.builder(BridgeInterfaceInfo.class)
                .child(BridgeEntry.class, bridgeEntryKey)
                .child(BridgeInterfaceEntry.class, bridgeInterfaceEntryKey).build();

    }

    public static BridgeEntry getBridgeEntryFromConfigDS(BigInteger dpnId,
                                                         DataBroker dataBroker) {
        BridgeEntry bridgeEntry = getBridgeEntryFromCache(dpnId);
        if(bridgeEntry != null) {
            return bridgeEntry;
        }
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpnId);
        InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier =
            InterfaceMetaUtils.getBridgeEntryIdentifier(bridgeEntryKey);
        LOG.debug("Trying to retrieve bridge entry from config for Id: {}", bridgeEntryInstanceIdentifier);
        bridgeEntry = readBridgeEntryFromConfigDS(bridgeEntryInstanceIdentifier,
            dataBroker);
        if(bridgeEntry != null) {
            addBridgeEntryToCache(dpnId, bridgeEntry);
        }
        return bridgeEntry;
    }

    private static BridgeEntry readBridgeEntryFromConfigDS(InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier,
                                                           DataBroker dataBroker) {
        return IfmUtil.read(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, dataBroker).orNull();
    }

    public static BridgeEntry getBridgeEntryFromConfigDS(InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier,
                                                         DataBroker dataBroker) {
        BigInteger dpnId = bridgeEntryInstanceIdentifier.firstKeyOf(BridgeEntry.class).getDpid();
        return getBridgeEntryFromConfigDS(dpnId, dataBroker);
    }

    public static void createBridgeInterfaceEntryInConfigDS(BigInteger dpId,
                                                            String childInterface) {
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey = new BridgeInterfaceEntryKey(childInterface);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey, bridgeInterfaceEntryKey);
        BridgeInterfaceEntryBuilder entryBuilder = new BridgeInterfaceEntryBuilder().setKey(bridgeInterfaceEntryKey)
                .setInterfaceName(childInterface);
        BatchingUtils.write(bridgeInterfaceEntryIid, entryBuilder.build(), BatchingUtils.EntityType.DEFAULT_CONFIG);
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

    public static InterfaceParentEntry getInterfaceParentEntryFromConfigDS(
            String interfaceName, DataBroker dataBroker) {
        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(interfaceName);
        InterfaceParentEntry interfaceParentEntry =
                InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(interfaceParentEntryKey, dataBroker);
        return interfaceParentEntry;
    }

    public static InterfaceParentEntry getInterfaceParentEntryFromConfigDS(
            InterfaceParentEntryKey interfaceParentEntryKey, DataBroker dataBroker) {
        InstanceIdentifier<InterfaceParentEntry> intfParentIid =
                getInterfaceParentEntryIdentifier(interfaceParentEntryKey);

        return getInterfaceParentEntryFromConfigDS(intfParentIid, dataBroker);
    }

    public static InterfaceParentEntry getInterfaceParentEntryFromConfigDS(
            InstanceIdentifier<InterfaceParentEntry> intfId, DataBroker dataBroker) {
        Optional<InterfaceParentEntry> interfaceParentEntryOptional =
                IfmUtil.read(LogicalDatastoreType.CONFIGURATION, intfId, dataBroker);
        if (!interfaceParentEntryOptional.isPresent()) {
            return null;
        }
        return interfaceParentEntryOptional.get();
    }

    public static InterfaceChildEntry getInterfaceChildEntryFromConfigDS(InterfaceParentEntryKey interfaceParentEntryKey,
                                                                         InterfaceChildEntryKey interfaceChildEntryKey,
                                                                         DataBroker dataBroker) {
        InstanceIdentifier<InterfaceChildEntry> intfChildIid =
                getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);

        return getInterfaceChildEntryFromConfigDS(intfChildIid, dataBroker);
    }

    public static InterfaceChildEntry getInterfaceChildEntryFromConfigDS(
            InstanceIdentifier<InterfaceChildEntry> intfChildIid, DataBroker dataBroker) {
        Optional<InterfaceChildEntry> interfaceChildEntryOptional =
                IfmUtil.read(LogicalDatastoreType.CONFIGURATION, intfChildIid, dataBroker);
        if (!interfaceChildEntryOptional.isPresent()) {
            return null;
        }
        return interfaceChildEntryOptional.get();
    }

    public static void createLportTagInterfaceMap(WriteTransaction t, String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class).child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
        IfIndexInterface ifIndexInterface = new IfIndexInterfaceBuilder().setIfIndex(ifIndex).setKey(new IfIndexInterfaceKey(ifIndex)).setInterfaceName(infName).build();
        t.put(LogicalDatastoreType.OPERATIONAL, id, ifIndexInterface, true);
    }

    public static void removeLportTagInterfaceMap(IdManagerService idManager, WriteTransaction t, String infName) {
        // workaround to get the id to remove from lport tag interface map
        Integer ifIndex = IfmUtil.allocateId(idManager, IfmConstants.IFM_IDPOOL_NAME, infName);
        IfmUtil.releaseId(idManager, IfmConstants.IFM_IDPOOL_NAME, infName);
        LOG.debug("removing lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexInterface> id = InstanceIdentifier.builder(IfIndexesInterfaceMap.class).child(IfIndexInterface.class, new IfIndexInterfaceKey(ifIndex)).build();
        t.delete(LogicalDatastoreType.OPERATIONAL, id);
    }

    public static void addBridgeRefToBridgeInterfaceEntry(BigInteger dpId, OvsdbBridgeRef ovsdbBridgeRef, WriteTransaction t){
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(dpId);
        InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier = getBridgeEntryIdentifier(bridgeEntryKey);

        BridgeEntryBuilder bridgeEntryBuilder = new BridgeEntryBuilder().setKey(bridgeEntryKey).setBridgeReference(ovsdbBridgeRef);
        t.merge(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, bridgeEntryBuilder.build(), true);
    }

    public static void createBridgeRefEntry(BigInteger dpnId, InstanceIdentifier<?> bridgeIid,
                                            WriteTransaction tx){
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}",
                dpnId, bridgeIid);
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpnId);
        InstanceIdentifier<BridgeRefEntry> bridgeEntryId =
                InterfaceMetaUtils.getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        BridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder =
                new BridgeRefEntryBuilder().setKey(bridgeRefEntryKey).setDpid(dpnId)
                        .setBridgeReference(new OvsdbBridgeRef(bridgeIid));
        tx.put(LogicalDatastoreType.OPERATIONAL, bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(), true);
    }
    public static void deleteBridgeRefEntry(BigInteger dpnId,
                                            WriteTransaction tx) {
        LOG.debug("Deleting bridge ref entry for dpn: {}",
                dpnId);
        BridgeRefEntryKey bridgeRefEntryKey = new BridgeRefEntryKey(dpnId);
        InstanceIdentifier<BridgeRefEntry> bridgeEntryId =
                InterfaceMetaUtils.getBridgeRefEntryIdentifier(bridgeRefEntryKey);
        tx.delete(LogicalDatastoreType.OPERATIONAL, bridgeEntryId);
    }

    public static void createTunnelToInterfaceMap(String tunnelInstanceId,
                                                  String infName,
                                                  WriteTransaction transaction) {
        LOG.debug("creating tunnel instance identifier to interface map for {}",infName);
        InstanceIdentifier<TunnelInstanceInterface> id = InstanceIdentifier.builder(TunnelInstanceInterfaceMap.class).
                child(TunnelInstanceInterface.class, new TunnelInstanceInterfaceKey(tunnelInstanceId)).build();
        TunnelInstanceInterface tunnelInstanceInterface = new TunnelInstanceInterfaceBuilder().
                setTunnelInstanceIdentifier(tunnelInstanceId).setKey(new TunnelInstanceInterfaceKey(tunnelInstanceId)).setInterfaceName(infName).build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, id, tunnelInstanceInterface, true);

    }

    public static void createTunnelToInterfaceMap(String infName,InstanceIdentifier<Node> nodeId,
                                                  WriteTransaction transaction,
                                                  IfTunnel ifTunnel){
        InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier = org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils.
                createTunnelsInstanceIdentifier(nodeId,
                        ifTunnel.getTunnelSource(), ifTunnel.getTunnelDestination());
        createTunnelToInterfaceMap(tunnelsInstanceIdentifier.toString(), infName, transaction);
    }

    public static void removeTunnelToInterfaceMap(InstanceIdentifier<Node> nodeId,
                                                  WriteTransaction transaction,
                                                  IfTunnel ifTunnel){
        InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier = org.opendaylight.genius.interfacemanager.renderer.hwvtep.utilities.SouthboundUtils.
                createTunnelsInstanceIdentifier(nodeId,
                        ifTunnel.getTunnelSource(), ifTunnel.getTunnelDestination());
        transaction.delete(LogicalDatastoreType.OPERATIONAL, tunnelsInstanceIdentifier);
    }

    public static String getInterfaceForTunnelInstanceIdentifier(String tunnelInstanceId,
                                                                 DataBroker dataBroker) {
        InstanceIdentifier<TunnelInstanceInterface> id = InstanceIdentifier.builder(TunnelInstanceInterfaceMap.class).
                child(TunnelInstanceInterface.class, new TunnelInstanceInterfaceKey(tunnelInstanceId)).build();
        Optional<TunnelInstanceInterface> tunnelInstanceInterfaceOptional = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker);
        if(tunnelInstanceInterfaceOptional.isPresent()){
            return tunnelInstanceInterfaceOptional.get().getInterfaceName();
        }
        return null;
    }

    public static void deleteBridgeInterfaceEntry(BridgeEntryKey bridgeEntryKey, List<BridgeInterfaceEntry> bridgeInterfaceEntries,
                                                  InstanceIdentifier<BridgeEntry> bridgeEntryIid,
                                                  WriteTransaction transaction, String interfaceName){
        BridgeInterfaceEntryKey bridgeInterfaceEntryKey =
                new BridgeInterfaceEntryKey(interfaceName);
        InstanceIdentifier<BridgeInterfaceEntry> bridgeInterfaceEntryIid =
                InterfaceMetaUtils.getBridgeInterfaceEntryIdentifier(bridgeEntryKey,
                        bridgeInterfaceEntryKey);
        BatchingUtils.delete(bridgeInterfaceEntryIid, BatchingUtils.EntityType.DEFAULT_CONFIG);

        if (bridgeInterfaceEntries.size() <= 1) {
            BatchingUtils.delete(bridgeEntryIid, BatchingUtils.EntityType.DEFAULT_CONFIG);
        }
    }

    // Cache Util methods

    // Start: BridgeEntryCache
    public static void addBridgeEntryToCache(BigInteger dpnId, BridgeEntry bridgeEntry) {
        bridgeEntryMap.put(dpnId, bridgeEntry);
    }

    public static void addBridgeEntryToCache(BridgeEntry bridgeEntry) {
        addBridgeEntryToCache(bridgeEntry.getKey().getDpid(), bridgeEntry);
    }

    public static void removeFromBridgeEntryCache(BigInteger dpnId) {
        bridgeEntryMap.remove(dpnId);
    }

    public static void removeFromBridgeEntryCache(BridgeEntry bridgeEntry) {
        removeFromBridgeEntryCache(bridgeEntry.getKey().getDpid());
    }

    public static BridgeEntry getBridgeEntryFromCache(BigInteger dpnId) {
        return bridgeEntryMap.get(dpnId);
    }
    // End: Bridge Entry Cache

    //Start: BridgeRefEntry Cache
    public static void addBridgeRefEntryToCache(BigInteger dpnId, BridgeRefEntry bridgeRefEntry) {
        bridgeRefEntryMap.put(dpnId, bridgeRefEntry);
    }

    public static void addBridgeRefEntryToCache(BridgeRefEntry bridgeRefEntry) {
        addBridgeRefEntryToCache(bridgeRefEntry.getKey().getDpid(), bridgeRefEntry);
    }

    public static void removeFromBridgeRefEntryCache(BigInteger dpnId) {
        bridgeRefEntryMap.remove(dpnId);
    }

    public static void removeFromBridgeRefEntryCache(BridgeRefEntry bridgeRefEntry) {
        removeFromBridgeRefEntryCache(bridgeRefEntry.getKey().getDpid());
    }

    public static BridgeRefEntry getBridgeRefEntryFromCache(BigInteger dpnId) {
        return bridgeRefEntryMap.get(dpnId);
    }

}
