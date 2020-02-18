/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.cloudscaler.rpcservice;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.infrautils.utils.concurrent.ListenableFutures;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredAsyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.CloudscalerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesEndInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesEndOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesEndOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesRecoverInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesRecoverOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesStartInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesStartOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesTepDeleteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.ScaleinComputesTepDeleteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.cloudscaler.rpcs.rev171220.compute.nodes.ComputeNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CloudscalerRpcServiceImpl implements CloudscalerRpcService {

    private static final Logger LOG = LoggerFactory.getLogger("GeniusEventLogger");

    private static final Integer DELETE_DELAY = Integer.getInteger(
            "scale.in.end.delay.inventory.delete.in.secs", 120);
    private static ScaleinComputesEndOutput IN_PROGRESS = new ScaleinComputesEndOutputBuilder()
            .setStatus("INPROGRESS")
            .build();

    private static ScaleinComputesEndOutput DONE = new ScaleinComputesEndOutputBuilder()
            .setStatus("DONE")
            .build();

    private static  RpcResult<ScaleinComputesEndOutput> IN_PROGRESS_RPC_RESPONSE = RpcResultBuilder
            .<ScaleinComputesEndOutput>success().withResult(IN_PROGRESS).build();

    private static RpcResult<ScaleinComputesEndOutput> DONE_RPC_RESPONSE = RpcResultBuilder
            .<ScaleinComputesEndOutput>success().withResult(DONE).build();

    private final DataBroker dataBroker;
    private final ComputeNodeManager computeNodeManager;
    private final ManagedNewTransactionRunner txRunner;
    //private final ItmTepClusteredListener itmTepClusteredListener;

    //The following timestamp is not persisted across reboots
    //upon reboot the timestamp will have a default value of that system timestamp
    //this way scalein end that is triggered after cluster reboot will still honour the 2 min delay
    private final LoadingCache<Uint64, Long> tepDeleteTimeStamp = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build(new CacheLoader<Uint64, Long>() {
                @Override
                public Long load(Uint64 dpnId) {
                    return System.currentTimeMillis();
                }
            });

    public static final FutureCallback<Void> DEFAULT_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(Void result) {
            LOG.debug("Success in Datastore operation");
        }

        @Override
        public void onFailure(Throwable error) {
            LOG.error("Error in Datastore operation", error);
        }
    };

    @Inject
    public CloudscalerRpcServiceImpl(DataBroker dataBroker,
                                     ComputeNodeManager computeNodeManager) {
        this.dataBroker = dataBroker;
        this.computeNodeManager = computeNodeManager;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        //this.itmTepClusteredListener = new ItmTepClusteredListener(dataBroker);
    }

    @Override
    public ListenableFuture<RpcResult<ScaleinComputesStartOutput>> scaleinComputesStart(
            ScaleinComputesStartInput input) {
        ReadWriteTransaction tx = this.dataBroker.newReadWriteTransaction();
        SettableFuture<RpcResult<ScaleinComputesStartOutput>> ft = SettableFuture.create();
        input.getScaleinComputeNames().forEach(s -> tombstoneTheNode(s, tx, true));
        input.getScaleinComputeNames().forEach(s -> LOG.info("Cloudscaler scalein-start {}", s));
        try {
            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to tombstone all the nodes ", e);
            ft.set(RpcResultBuilder.<ScaleinComputesStartOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                            "Failed to tombstone all the nodes " + e.getMessage()).build());
            return ft;
        }
        ft.set(RpcResultBuilder.<ScaleinComputesStartOutput>success().build());
        return ft;
    }

    @Override
    public ListenableFuture<RpcResult<ScaleinComputesRecoverOutput>> scaleinComputesRecover(
            ScaleinComputesRecoverInput input) {
        ReadWriteTransaction tx = this.dataBroker.newReadWriteTransaction();
        SettableFuture<RpcResult<ScaleinComputesRecoverOutput>> ft = SettableFuture.create();
        input.getRecoverComputeNames().forEach(s -> tombstoneTheNode(s, tx, false));
        input.getRecoverComputeNames().forEach(s -> LOG.info("Cloudscaler scalein-recover {}", s));
        try {
            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to recover all the nodes ", e);
            ft.set(RpcResultBuilder.<ScaleinComputesRecoverOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                            "Failed to recover all the nodes " + e.getMessage()).build());
            return ft;
        }
        //LOG.info("Recovered the nodes {}", input);
        ft.set(RpcResultBuilder.<ScaleinComputesRecoverOutput>success().build());
        return ft;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<ScaleinComputesEndOutput>> scaleinComputesEnd(ScaleinComputesEndInput input) {
        LOG.error("Cloudscaler scalein-end {}", input);
        try {
            for (String computeName : input.getScaleinComputeNames()) {
                ComputeNode computeNode = computeNodeManager.getComputeNodeFromName(computeName);
                if (computeNode == null) {
                    LOG.warn("Cloudscaler Failed to find the compute {} for scale in end ", computeName);
                    return Futures.immediateFuture(DONE_RPC_RESPONSE);
                }
                Long tepDeletedTimeStamp = tepDeleteTimeStamp.get(computeNode.getDpnid());
                Long currentTime = System.currentTimeMillis();
                if (currentTime - tepDeletedTimeStamp > DELETE_DELAY * 1000L) {
                    scaleinComputesEnd2(input);
                } else {
                    return Futures.immediateFuture(IN_PROGRESS_RPC_RESPONSE);
                }
            }
        } catch (Exception e) {
            LOG.error("Cloudscaler Failed scalein-end ", e);
            return Futures.immediateFuture(
                    RpcResultBuilder.<ScaleinComputesEndOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                            "Failed to read the compute node " + e.getMessage()).build());
        }
        return Futures.immediateFuture(DONE_RPC_RESPONSE);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<ScaleinComputesEndOutput>> scaleinComputesEnd2(ScaleinComputesEndInput input) {
        try {
            for (String computeName : input.getScaleinComputeNames()) {
                ComputeNode computeNode;
                try {
                    computeNode = computeNodeManager.getComputeNodeFromName(computeName);
                    if (computeNode == null) {
                        LOG.error("Cloudscaler Failed to find the compute {} for scale in end ",
                                computeName);
                        return Futures.immediateFuture(IN_PROGRESS_RPC_RESPONSE);
                    }
                } catch (ReadFailedException e) {
                    LOG.error("Cloudscaler Failed to read the compute node {}", e.getMessage());
                    return Futures.immediateFuture(
                            RpcResultBuilder.<ScaleinComputesEndOutput>failed().withError(
                                    RpcError.ErrorType.APPLICATION,
                                    "Failed to read the compute node " + e.getMessage()).build());
                }
                LOG.info("Cloudscaler Deleting compute node details {}", computeNode);
                LOG.info("Cloudscaler Deleting compute node details {}",
                        buildOpenflowNodeIid(computeNode));
                LOG.info("Cloudscaler Deleting compute node details {}", buildOvsdbNodeId(computeNode));
                ListenableFutures.addErrorLogging(txRunner.callWithNewReadWriteTransactionAndSubmit(tx -> {
                    computeNodeManager.deleteComputeNode(tx, computeNode);
                }), LOG, "Cloudscaler Failed to delete the compute node");
                ListenableFutures.addErrorLogging(txRunner.callWithNewReadWriteTransactionAndSubmit(tx -> {
                    tx.delete(LogicalDatastoreType.CONFIGURATION, buildOpenflowNodeIid(computeNode));
                }), LOG, "Cloudscaler Failed to delete the config inventory");
                ListenableFutures.addErrorLogging(txRunner.callWithNewReadWriteTransactionAndSubmit(tx -> {
                    tx.delete(LogicalDatastoreType.CONFIGURATION, buildOvsdbNodeId(computeNode));
                }), LOG, "Cloudscaler Failed to delete the config topology");
            }
        } catch (Throwable e) {
            LOG.error("Cloudscaler Failed to do scale in end {} ", input, e);
            return Futures.immediateFuture(
                    RpcResultBuilder.<ScaleinComputesEndOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                            "Failed to read the transport zone " + e.getMessage()).build());
        }
        return Futures.immediateFuture(DONE_RPC_RESPONSE);
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network
            .topology.rev131021.network.topology.topology.Node> buildOvsdbNodeId(ComputeNode computeNode) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network
                                .topology.topology.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                                .network.topology.topology.NodeKey(new NodeId(computeNode.getNodeid())));
    }

    private InstanceIdentifier<Node> buildOpenflowNodeIid(ComputeNode computeNode) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(
                                "openflow:" + computeNode.getDpnid().toString()))).build();
    }

    private void tombstoneTheNode(String computeName, ReadWriteTransaction tx, Boolean tombstone) {
        ComputeNode computeNode = null;
        try {
            computeNode = computeNodeManager.getComputeNodeFromName(computeName);
            if (computeNode == null) { //TODO throw error to rpc
                LOG.error("Cloudscaler Node not present to {} {}",
                        computeName, tombstone ? "tombstone" : "recover");
                return;
            }
        } catch (ReadFailedException e) {
            LOG.error("Cloudscaler Failed to {} the compute {} read failed",
                    tombstone ? "tombstone" : "recover", computeName);
            return;
        }
        ComputeNodeBuilder builder = new ComputeNodeBuilder(computeNode);
        builder.setTombstoned(tombstone);
        tx.put(LogicalDatastoreType.CONFIGURATION,
                computeNodeManager.buildComputeNodeIid(computeName), builder.build(), true);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<ScaleinComputesTepDeleteOutput>> scaleinComputesTepDelete(
            ScaleinComputesTepDeleteInput input) {
        ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction();
        SettableFuture<RpcResult<ScaleinComputesTepDeleteOutput>> ft = SettableFuture.create();
        Optional<TransportZones> tz;
        try {
            tz = readTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(TransportZones.class))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Cloudscaler Failed to read the transport zone {}", e.getMessage());
            ft.set(RpcResultBuilder.<ScaleinComputesTepDeleteOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                    "Failed to read the transport zone " + e.getMessage()).build());
            return ft;
        } finally {
            readTx.close();
        }
        try {
            for (String computeName : input.getScaleinComputeNames()) {
                ComputeNode computeNode = null;
                try {
                    computeNode = computeNodeManager.getComputeNodeFromName(computeName);
                    if (computeNode == null) {
                        LOG.warn("Cloudscaler Could not find the compute for tep delete {}", computeName);
                        ft.set(RpcResultBuilder.<ScaleinComputesTepDeleteOutput>success().build());
                        return ft;
                    }
                } catch (ReadFailedException e) {
                    LOG.error("Cloudscaler Failed to read the compute node {}", e.getMessage());
                    ft.set(RpcResultBuilder.<ScaleinComputesTepDeleteOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "Failed to read the compute node "
                                    + e.getMessage()).build());
                    return ft;
                }
                if (tz.isPresent() && tz.get().getTransportZone() != null) {
                    for (TransportZone zone : tz.get().getTransportZone()) {
                        if (zone.getVteps() == null) {
                            continue;
                        }
                        for (Vteps vteps : zone.getVteps()) {
                            if (vteps.getDpnId().equals(computeNode.getDpnid())) {
                                InstanceIdentifier<Vteps> dpnVtepIid = InstanceIdentifier
                                        .create(TransportZones.class)
                                        .child(TransportZone.class, zone.key())
                                        .child(Vteps.class, vteps.key());
                                LOG.error("Cloudscaler deleting dpn {}", vteps);
                                ListenableFutures.addErrorLogging(
                                        txRunner.callWithNewReadWriteTransactionAndSubmit(tx -> {
                                            tx.delete(LogicalDatastoreType.CONFIGURATION, dpnVtepIid);
                                        }), LOG, "Cloudscaler Failed to delete the itm tep");
                            }
                        }
                    }
                }
            }
            InstanceIdentifier.create(TransportZones.class)
                    .child(TransportZone.class)
                    .child(Vteps.class);
        } catch (Throwable e) {
            LOG.error("Failed to read the transport zone ", e);
            ft.set(RpcResultBuilder.<ScaleinComputesTepDeleteOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                    "Failed to read the transport zone " + e.getMessage()).build());
            return ft;
        }
        ft.set(RpcResultBuilder.<ScaleinComputesTepDeleteOutput>success().build());
        return ft;
    }

    class ItmTepClusteredListener extends AbstractClusteredAsyncDataTreeChangeListener<Vteps> {

        @Inject
        ItmTepClusteredListener(DataBroker dataBroker) {
            super(dataBroker, LogicalDatastoreType.OPERATIONAL,InstanceIdentifier.create(TransportZones.class)
                    .child(TransportZone.class).child(Vteps.class),
                    Executors.newSingleThreadExecutor("ItmTepClusteredListener", LOG));
        }

        @Override
        public void remove(InstanceIdentifier<Vteps> instanceIdentifier, Vteps tep) {
            tepDeleteTimeStamp.put(tep.getDpnId(), System.currentTimeMillis());
        }

        @Override
        public void update(InstanceIdentifier<Vteps> instanceIdentifier, Vteps vteps, Vteps t1) {
        }

        @Override
        public void add(InstanceIdentifier<Vteps> instanceIdentifier, Vteps vteps) {
        }
    }
}
