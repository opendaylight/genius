/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.BridgeTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.IfIndexesTunnelMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113._if.indexes.tunnel.map.IfIndexTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113._if.indexes.tunnel.map.IfIndexTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113._if.indexes.tunnel.map.IfIndexTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.bridge.tunnel.info.OvsBridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.bridge.tunnel.info.OvsBridgeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.bridge.tunnel.info.OvsBridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.bridge.tunnel.info.ovs.bridge.entry.OvsBridgeTunnelEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.ovs.bridge.ref.info.OvsBridgeRefEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelMetaUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelMetaUtils.class);
    private static ConcurrentHashMap<BigInteger, OvsBridgeEntry> ovsBridgeEntryMap = new ConcurrentHashMap();
    private static ConcurrentHashMap<BigInteger, OvsBridgeRefEntry> ovsBridgeRefEntryMap = new ConcurrentHashMap();

    public static InstanceIdentifier<OvsBridgeRefEntry> getOvsBridgeRefEntryIdentifier(OvsBridgeRefEntryKey bridgeRefEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<OvsBridgeRefEntry> bridgeRefEntryInstanceIdentifierBuilder =
                InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                        .child(OvsBridgeRefEntry.class, bridgeRefEntryKey);
        return bridgeRefEntryInstanceIdentifierBuilder.build();
    }


    public static OvsBridgeRefEntry getOvsBridgeRefEntryFromOperDS(BigInteger dpId, DataBroker dataBroker) {
        OvsBridgeRefEntry bridgeRefEntry = getOvsBridgeRefEntryFromCache(dpId);
        if(bridgeRefEntry != null) {
            return bridgeRefEntry;
        }
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeRefEntryIid = TunnelMetaUtils
            .getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        bridgeRefEntry = ItmUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryIid, dataBroker).orNull();
        if(bridgeRefEntry != null) {
            addBridgeRefEntryToCache(dpId, bridgeRefEntry);
        }
        return bridgeRefEntry;
    }

    public static OvsdbBridgeRef getOvsdbBridgeRef(BigInteger dpId, DataBroker dataBroker) {

        OvsBridgeRefEntry bridgeRefEntry = getOvsBridgeRefEntryFromOperDS(dpId, dataBroker);

        if(bridgeRefEntry == null){
            // bridge ref entry will be null if the bridge is disconnected from controller.
            // In that case, fetch bridge reference from bridge interface entry config DS
            OvsBridgeEntry bridgeEntry = getOvsBridgeEntryFromConfigDS(dpId, dataBroker);
            if(bridgeEntry == null){
                return null;
            }
            return  bridgeEntry.getOvsBridgeReference();
        }
        return bridgeRefEntry.getOvsBridgeReference();
    }

    // SF419 Is this Required ??

    public static OvsBridgeRefEntry getBridgeReferenceForInterface(Interface interfaceInfo,
                                                                DataBroker dataBroker) {
        ParentRefs parentRefs = interfaceInfo.getAugmentation(ParentRefs.class);
        BigInteger dpn = parentRefs.getDatapathNodeIdentifier();
        return getOvsBridgeRefEntryFromOperDS(dpn, dataBroker);
    }

    public static boolean bridgeExists(OvsBridgeRefEntry bridgeRefEntry,
                                       DataBroker dataBroker) {
        if (bridgeRefEntry != null && bridgeRefEntry.getOvsBridgeReference() != null) {
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry.getOvsBridgeReference().getValue();
            Optional<OvsdbBridgeAugmentation> bridgeNodeOptional =
                    ItmUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid, dataBroker);
            if (bridgeNodeOptional.isPresent()) {
                return true;
            }
        }
        return false;
    }
    public static InstanceIdentifier<OvsBridgeEntry> getOvsBridgeEntryIdentifier(OvsBridgeEntryKey bridgeEntryKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<OvsBridgeEntry> bridgeEntryIdBuilder =
                InstanceIdentifier.builder(BridgeTunnelInfo.class).child(OvsBridgeEntry.class, bridgeEntryKey);
        return bridgeEntryIdBuilder.build();
    }

    public static InstanceIdentifier<OvsBridgeTunnelEntry> getBridgeTunnelEntryIdentifier(OvsBridgeEntryKey bridgeEntryKey,
                                                                                             OvsBridgeTunnelEntryKey bridgeInterfaceEntryKey) {
        return InstanceIdentifier.builder(BridgeTunnelInfo.class)
                .child(OvsBridgeEntry.class, bridgeEntryKey)
                .child(OvsBridgeTunnelEntry.class, bridgeInterfaceEntryKey).build();

    }

    public static OvsBridgeEntry getOvsBridgeEntryFromConfigDS(BigInteger dpnId,
                                                         DataBroker dataBroker) {
        OvsBridgeEntry bridgeEntry = getOvsBridgeEntryFromCache(dpnId);
        if(bridgeEntry != null) {
            return bridgeEntry;
        }
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryInstanceIdentifier =
            TunnelMetaUtils.getOvsBridgeEntryIdentifier(bridgeEntryKey);
        LOG.debug("Trying to retrieve bridge entry from config for Id: {}", bridgeEntryInstanceIdentifier);
        bridgeEntry = readBridgeEntryFromConfigDS(bridgeEntryInstanceIdentifier,
            dataBroker);
        if(bridgeEntry != null) {
            addBridgeEntryToCache(dpnId, bridgeEntry);
        }
        return bridgeEntry;
    }

    private static OvsBridgeEntry readBridgeEntryFromConfigDS(InstanceIdentifier<OvsBridgeEntry> bridgeEntryInstanceIdentifier,
                                                           DataBroker dataBroker) {
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, dataBroker).orNull();
    }

    public static OvsBridgeEntry getOvsBridgeEntryFromConfigDS(InstanceIdentifier<OvsBridgeEntry> bridgeEntryInstanceIdentifier,
                                                         DataBroker dataBroker) {
        BigInteger dpnId = bridgeEntryInstanceIdentifier.firstKeyOf(OvsBridgeEntry.class).getDpid();
        return getOvsBridgeEntryFromConfigDS(dpnId, dataBroker);
    }
    // SF 419 Is this required

    public static void createBridgeTunnelEntryInConfigDS(BigInteger dpId,
                                                            String childInterface) {
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        OvsBridgeTunnelEntryKey bridgeTunnelEntryKey = new OvsBridgeTunnelEntryKey(childInterface);
        InstanceIdentifier<OvsBridgeTunnelEntry> bridgeTunnelEntryIid =
                TunnelMetaUtils.getBridgeTunnelEntryIdentifier(bridgeEntryKey, bridgeTunnelEntryKey);
        OvsBridgeTunnelEntryBuilder entryBuilder = new OvsBridgeTunnelEntryBuilder().setKey(bridgeTunnelEntryKey)
                .setTunnelName(childInterface);
        ITMBatchingUtils.write(bridgeTunnelEntryIid, entryBuilder.build(), ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    // SF419 CHECK -- THIS MAY BE REQUIRED
    public static void createLportTagInterfaceMap(WriteTransaction t, String infName, Integer ifIndex) {
        LOG.debug("creating lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class).child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        IfIndexTunnel ifIndexInterface = new IfIndexTunnelBuilder().setIfIndex(ifIndex).setKey(new IfIndexTunnelKey(ifIndex)).setInterfaceName(infName).build();
        ITMBatchingUtils.write(id, ifIndexInterface, ITMBatchingUtils.EntityType.DEFAULT_OPERATIONAL);
    }

    public static void removeLportTagInterfaceMap(IdManagerService idManager, WriteTransaction t, String infName) {
        // workaround to get the id to remove from lport tag interface map
        Integer ifIndex = ItmUtils.allocateId(idManager, ITMConstants.ITM_IDPOOL_NAME, infName);
        ItmUtils.releaseId(idManager, ITMConstants.ITM_IDPOOL_NAME, infName);
        LOG.debug("removing lport tag to interface map for {}",infName);
        InstanceIdentifier<IfIndexTunnel> id = InstanceIdentifier.builder(IfIndexesTunnelMap.class).child(IfIndexTunnel.class, new IfIndexTunnelKey(ifIndex)).build();
        t.delete(LogicalDatastoreType.OPERATIONAL, id);
    }


    public static void addBridgeRefToBridgeTunnelEntry(BigInteger dpId, OvsdbBridgeRef ovsdbBridgeRef, WriteTransaction t){
        OvsBridgeEntryKey bridgeEntryKey = new OvsBridgeEntryKey(dpId);
        InstanceIdentifier<OvsBridgeEntry> bridgeEntryInstanceIdentifier = getOvsBridgeEntryIdentifier(bridgeEntryKey);

        OvsBridgeEntryBuilder bridgeEntryBuilder = new OvsBridgeEntryBuilder().setKey(bridgeEntryKey).setOvsBridgeReference(ovsdbBridgeRef);
        t.merge(LogicalDatastoreType.CONFIGURATION, bridgeEntryInstanceIdentifier, bridgeEntryBuilder.build(), true);
    }

    public static void createOvsBridgeRefEntry(BigInteger dpnId, InstanceIdentifier<?> bridgeIid,
                                            WriteTransaction tx){
        LOG.debug("Creating bridge ref entry for dpn: {} bridge: {}",
                dpnId, bridgeIid);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId =
                TunnelMetaUtils.getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        OvsBridgeRefEntryBuilder tunnelDpnBridgeEntryBuilder =
                new OvsBridgeRefEntryBuilder().setKey(bridgeRefEntryKey).setDpid(dpnId)
                        .setOvsBridgeReference(new OvsdbBridgeRef(bridgeIid));
        tx.put(LogicalDatastoreType.OPERATIONAL, bridgeEntryId, tunnelDpnBridgeEntryBuilder.build(), true);
    }
    public static void deleteOvsBridgeRefEntry(BigInteger dpnId,
                                            WriteTransaction tx) {
        LOG.debug("Deleting bridge ref entry for dpn: {}",
                dpnId);
        OvsBridgeRefEntryKey bridgeRefEntryKey = new OvsBridgeRefEntryKey(dpnId);
        InstanceIdentifier<OvsBridgeRefEntry> bridgeEntryId =
                TunnelMetaUtils.getOvsBridgeRefEntryIdentifier(bridgeRefEntryKey);
        tx.delete(LogicalDatastoreType.OPERATIONAL, bridgeEntryId);
    }

    public static void deleteBridgeInterfaceEntry(OvsBridgeEntryKey bridgeEntryKey, List<OvsBridgeTunnelEntry> bridgeTunnelEntries,
                                                  InstanceIdentifier<OvsBridgeEntry> bridgeEntryIid,
                                                  WriteTransaction transaction, String interfaceName){
        OvsBridgeTunnelEntryKey bridgeTunnelEntryKey =
                new OvsBridgeTunnelEntryKey(interfaceName);
        InstanceIdentifier<OvsBridgeTunnelEntry> bridgeTunnelEntryIid =
                TunnelMetaUtils.getBridgeTunnelEntryIdentifier(bridgeEntryKey,
                        bridgeTunnelEntryKey);
        ITMBatchingUtils.delete(bridgeTunnelEntryIid, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);

        if (bridgeTunnelEntries.size() <= 1) {
            ITMBatchingUtils.delete(bridgeEntryIid, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        }
    }

    // Cache Util methods

    // Start: BridgeEntryCache
    public static void addBridgeEntryToCache(BigInteger dpnId, OvsBridgeEntry bridgeEntry) {
        ovsBridgeEntryMap.put(dpnId, bridgeEntry);
    }

    public static void addBridgeEntryToCache(OvsBridgeEntry bridgeEntry) {
        addBridgeEntryToCache(bridgeEntry.getKey().getDpid(), bridgeEntry);
    }

    public static void removeFromBridgeEntryCache(BigInteger dpnId) {
        ovsBridgeEntryMap.remove(dpnId);
    }

    public static void removeFromBridgeEntryCache(OvsBridgeEntry bridgeEntry) {
        removeFromBridgeEntryCache(bridgeEntry.getKey().getDpid());
    }

    public static OvsBridgeEntry getOvsBridgeEntryFromCache(BigInteger dpnId) {
        return ovsBridgeEntryMap.get(dpnId);
    }
    // End: Bridge Entry Cache

    //Start: BridgeRefEntry Cache
    public static void addBridgeRefEntryToCache(BigInteger dpnId, OvsBridgeRefEntry bridgeRefEntry) {
        ovsBridgeRefEntryMap.put(dpnId, bridgeRefEntry);
    }

    public static void addBridgeRefEntryToCache(OvsBridgeRefEntry bridgeRefEntry) {
        addBridgeRefEntryToCache(bridgeRefEntry.getKey().getDpid(), bridgeRefEntry);
    }

    public static void removeFromBridgeRefEntryCache(BigInteger dpnId) {
        ovsBridgeRefEntryMap.remove(dpnId);
    }

    public static void removeFromBridgeRefEntryCache(OvsBridgeRefEntry bridgeRefEntry) {
        removeFromBridgeRefEntryCache(bridgeRefEntry.getKey().getDpid());
    }

    public static OvsBridgeRefEntry getOvsBridgeRefEntryFromCache(BigInteger dpnId) {
        return ovsBridgeRefEntryMap.get(dpnId);
    }

}
