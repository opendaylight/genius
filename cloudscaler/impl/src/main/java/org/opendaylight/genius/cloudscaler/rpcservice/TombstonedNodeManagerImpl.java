/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.cloudscaler.rpcservice;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.cloudscaler.api.TombstonedNodeManager;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ComputeNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TombstonedNodeManagerImpl implements TombstonedNodeManager {

    private static final Logger LOG = LoggerFactory.getLogger(TombstonedNodeManagerImpl.class);

    private final DataBroker dataBroker;
    private final CacheProvider cacheProvider;
    private final Set<Function<BigInteger, Void>> callbacks = ConcurrentHashMap.newKeySet();
    private final ComputeNodeManager computeNodeManager;

    private InstanceIdDataObjectCache<ComputeNode> computeNodeCache;
    private ExecutorService executorService = Executors.newSingleThreadExecutor("tombstone-node-manager", LOG);

    @Inject
    public TombstonedNodeManagerImpl(DataBroker dataBroker,
                                     CacheProvider cacheProvider,
                                     ComputeNodeManager computeNodeManager) {
        this.dataBroker = dataBroker;
        this.cacheProvider = cacheProvider;
        this.computeNodeManager = computeNodeManager;
        init();
    }

    void init() {
        this.computeNodeCache = new InstanceIdDataObjectCache<ComputeNode>(ComputeNode.class, dataBroker,
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(ComputeNodes.class).child(ComputeNode.class).build(),
                cacheProvider) {
            @Override
            protected void added(InstanceIdentifier<ComputeNode> path, ComputeNode computeNode) {
                if (computeNode.isTombstoned() != null && !computeNode.isTombstoned()) {
                    executorService.execute(() -> {
                        callbacks.forEach(callback -> {
                            callback.apply(computeNode.getDpnid().toJava());
                        });
                    });
                }
            }
        };
    }

    @PreDestroy
    void close() {
        computeNodeCache.close();
    }

    @Override
    public boolean isDpnTombstoned(BigInteger dpnId) throws ReadFailedException {
        if (dpnId == null) {
            return false;
        }
        ComputeNode computeNode = computeNodeManager.getComputeNode(dpnId);
        if (computeNode != null && computeNode.isTombstoned() != null) {
            return computeNode.isTombstoned();
        }
        return false;
    }

    @Override
    public void addOnRecoveryCallback(Function<BigInteger, Void> callback) {
        callbacks.add(callback);
    }

    @Override
    public List<BigInteger> filterTombStoned(List<BigInteger> dpns) throws ReadFailedException {
        return dpns.stream().filter((dpn) -> {
            try {
                return !isDpnTombstoned(dpn);
            } catch (ReadFailedException e) {
                LOG.error("Failed to read {}", dpn);
                return true;
            }
        }).collect(Collectors.toList());
    }
}
