/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.cloudscaler.rpcservice;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ComputeNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ComputeNodeManager {

    private static final Logger LOG = LoggerFactory.getLogger("GeniusEventLogger");

    private final DataBroker dataBroker;

    private final InstanceIdDataObjectCache<ComputeNode> computeNodeCache;
    private final InstanceIdDataObjectCache<Node> ovsdbTopologyNodeCache;
    private final Map<Uint64, ComputeNode> dpnIdVsComputeNode;
    // FIXME: this service is never shut down
    private final ExecutorService executorService = Executors.newSingleThreadExecutor("compute-node-manager", LOG);

    @Inject
    @SuppressFBWarnings({"URF_UNREAD_FIELD", "NP_LOAD_OF_KNOWN_NULL_VALUE"})
    public ComputeNodeManager(DataBroker dataBroker,
                              CacheProvider cacheProvider) {
        this.dataBroker = dataBroker;
        this.dpnIdVsComputeNode = new ConcurrentHashMap<>();
        this.computeNodeCache = new InstanceIdDataObjectCache<>(ComputeNode.class, dataBroker,
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(ComputeNodes.class).child(ComputeNode.class).build(),
                cacheProvider) {
            @Override
            protected void added(InstanceIdentifier<ComputeNode> path, ComputeNode computeNode) {
                LOG.info("ComputeNodeManager add compute {}", computeNode);
                dpnIdVsComputeNode.put(computeNode.getDpnid(), computeNode);
            }

            @Override
            protected void removed(InstanceIdentifier<ComputeNode> path, ComputeNode computeNode) {
                LOG.info("ComputeNodeManager remove compute {}", computeNode);
                dpnIdVsComputeNode.remove(computeNode.getDpnid());
            }
        };
        this.ovsdbTopologyNodeCache = new InstanceIdDataObjectCache<>(Node.class, dataBroker,
                LogicalDatastoreType.OPERATIONAL,
                getWildcardPath(),
                cacheProvider) {
            @Override
            @SuppressWarnings("checkstyle:IllegalCatch")
            protected void added(InstanceIdentifier<Node> path, Node dataObject) {
                executorService.execute(() -> {
                    try {
                        add(dataObject);
                    } catch (Exception e) {
                        LOG.error("ComputeNodeManager Failed to handle ovsdb node add", e);
                    }
                });
            }
        };
        //LOG.info("Compute node manager is initialized ");
    }

    public ComputeNode getComputeNodeFromName(String computeName) throws ReadFailedException {
        InstanceIdentifier<ComputeNode> computeIid = buildComputeNodeIid(computeName);
        return computeNodeCache.get(computeIid).orElse(null);
    }

    public void deleteComputeNode(ReadWriteTransaction tx, ComputeNode computeNode) {
        tx.delete(LogicalDatastoreType.CONFIGURATION, buildComputeNodeIid(computeNode.getComputeName()));
    }

    public void add(@NonNull Node node) throws TransactionCommitFailedException {
        OvsdbBridgeAugmentation bridgeAugmentation = node.augmentation(OvsdbBridgeAugmentation.class);
        if (bridgeAugmentation != null && bridgeAugmentation.getBridgeOtherConfigs() != null) {
            Uint64 datapathid = getDpnIdFromBridge(bridgeAugmentation);
            Optional<BridgeOtherConfigs> otherConfigOptional = bridgeAugmentation.getBridgeOtherConfigs()
                    .stream()
                    .filter(otherConfig -> otherConfig.getBridgeOtherConfigKey().equals("dp-desc"))
                    .findFirst();
            if (!otherConfigOptional.isPresent()) {
                LOG.debug("ComputeNodeManager Compute node name is not present in bridge {}", node.getNodeId());
                return;
            }
            String computeName = otherConfigOptional.get().getBridgeOtherConfigValue();
            String nodeId = node.getNodeId().getValue();
            InstanceIdentifier<ComputeNode> computeIid = buildComputeNodeIid(computeName);
            ComputeNode computeNode = new ComputeNodeBuilder()
                    .setComputeName(computeName)
                    .setDpnid(datapathid)
                    .setNodeid(nodeId)
                    .build();
            Optional<ComputeNode> computeNodeOptional = Optional.empty();
            try {
                computeNodeOptional = computeNodeCache.get(computeIid);
            } catch (ReadFailedException e) {
                LOG.error("ComputeNodeManager Failed to read {}", computeIid);
            }
            if (computeNodeOptional.isPresent()) {
                logErrorIfComputeNodeIsAlreadyTaken(datapathid, nodeId, computeNodeOptional);
            } else {
                LOG.info("ComputeNodeManager add ovsdb node {}", node.getNodeId());
                putComputeDetailsInConfigDatastore(computeIid, computeNode);
            }
        }
    }

    public InstanceIdentifier<ComputeNode> buildComputeNodeIid(String computeName) {
        return InstanceIdentifier.builder(ComputeNodes.class)
                .child(ComputeNode.class, new ComputeNodeKey(computeName))
                .build();
    }

    private Uint64 getDpnIdFromBridge(OvsdbBridgeAugmentation bridgeAugmentation) {
        if (bridgeAugmentation.getDatapathId() == null) {
            return Uint64.ZERO;
        }
        String datapathIdStr = bridgeAugmentation.getDatapathId().getValue() != null
                ? bridgeAugmentation.getDatapathId().getValue().replace(":", "") : null;
        return datapathIdStr != null ? Uint64.valueOf(datapathIdStr, 16) : Uint64.ZERO;
    }

    public void putComputeDetailsInConfigDatastore(InstanceIdentifier<ComputeNode> computeIid,
                                                    ComputeNode computeNode) throws TransactionCommitFailedException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, computeIid, computeNode);
        tx.submit().checkedGet();
        dpnIdVsComputeNode.put(computeNode.getDpnid(), computeNode);
        //LOG.info("Write comute node details {}", computeNode);
    }


    private void logErrorIfComputeNodeIsAlreadyTaken(Uint64 datapathid, String nodeId,
                                                     com.google.common.base.Optional<ComputeNode> optional) {
        ComputeNode existingNode = optional.get();
        if (!Objects.equals(existingNode.getNodeid(), nodeId)) {
            LOG.error("ComputeNodeManager Compute is already connected by compute {}", existingNode);
            return;
        }
        if (!Objects.equals(existingNode.getDpnid(), datapathid)) {
            LOG.error("ComputeNodeManager Compute is already connected by compute {}", existingNode);
        }
    }

    private InstanceIdentifier<Node> getWildcardPath() {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
                .child(Node.class);
    }

    public ComputeNode getComputeNode(Uint64 dpnId) {
        return dpnIdVsComputeNode.get(dpnId);
    }
}
