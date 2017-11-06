/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager;

import com.google.common.base.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.globals.IfmOvsdbNodeInfo;
import org.opendaylight.infrautils.caches.Cache;
import org.opendaylight.infrautils.caches.CacheConfigBuilder;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IfmCacheProvider {
    private static final Logger LOG = LoggerFactory.getLogger(IfmCacheProvider.class);

    private DataBroker dataBroker;
    private final CacheProvider cacheProvider;
    private Cache<String, IfmOvsdbNodeInfo> ovsNodesCache;

    @Inject
    public IfmCacheProvider(final DataBroker dataBroker, final CacheProvider cacheProvider) {
        this.dataBroker = dataBroker;
        this.cacheProvider = cacheProvider;
    }

    @PostConstruct
    public void start() {
        ovsNodesCache = cacheProvider.newCache(
            new CacheConfigBuilder<String, IfmOvsdbNodeInfo>()
                .anchor(this)
                .id("ifm-ovsNodes")
                .cacheFunction(key -> ovsNodesCacheLookup(key))
                .description("Ifm Cache for OvsdbNodes")
                .build());
    }

    @PreDestroy
    public void close() throws Exception {
        if (ovsNodesCache != null) {
            ovsNodesCache.close();
        }
    }

    private IfmOvsdbNodeInfo ovsNodesCacheLookup(String key) {
        InstanceIdentifier<OvsdbNodeAugmentation> ovsNodeIid = createOvsdbNodeIid(key);
        Optional<OvsdbNodeAugmentation> optOvsdbNode =
            IfmUtil.read(LogicalDatastoreType.OPERATIONAL, ovsNodeIid, dataBroker);
        if (optOvsdbNode.isPresent()) {
            return new IfmOvsdbNodeInfo(key, optOvsdbNode.get());
        }
        return null;
    }

    private InstanceIdentifier<OvsdbNodeAugmentation> createOvsdbNodeIid(String nodeIdStr) {
        return createTopologyInstanceIdentifier()
            .child(Node.class, createNodeKey(nodeIdStr))
            .augmentation(OvsdbNodeAugmentation.class);
    }

    private InstanceIdentifier<Topology> createTopologyInstanceIdentifier() {
        return InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class,
                new TopologyKey(org.opendaylight.genius.interfacemanager.globals.IfmConstants.OVSDB_TOPOLOGY_ID));
    }

    private NodeKey createNodeKey(String nodeIdStr) {
        return new NodeKey(new NodeId(new Uri(nodeIdStr)));
    }

    public void addOvsNode(String ovsNodeId, OvsdbNodeAugmentation ovsNode) {
        IfmOvsdbNodeInfo ovsNodeInfo = new IfmOvsdbNodeInfo(ovsNodeId, ovsNode);
        ovsNodesCache.put(ovsNodeId, ovsNodeInfo);
    }

    public void updateOvsNode(String ovsNodeId, OvsdbNodeAugmentation oldNode, OvsdbNodeAugmentation newNode) {
        addOvsNode(ovsNodeId, newNode);
    }

    public void removeOvsNode(String ovsNodeId) {
        ovsNodesCache.evict(ovsNodeId);
    }
}