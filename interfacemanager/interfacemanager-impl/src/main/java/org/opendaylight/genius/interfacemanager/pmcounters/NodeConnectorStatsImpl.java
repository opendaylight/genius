/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetFlowStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetFlowStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetFlowStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.OpendaylightDirectStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NodeConnectorStatsImpl extends AsyncDataTreeChangeListenerBase<Node, NodeConnectorStatsImpl> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeConnectorStatsImpl.class);
    private static final String STATS_POLL_FLAG = "interfacemgr.pmcounters.poll";
    private static final int THREAD_POOL_SIZE = 4;
    private static final int NO_DELAY = 0;
    private static final String POLLING_INTERVAL_PATH = "interfacemanager-statistics-polling-interval";
    private static final int DEFAULT_POLLING_INTERVAL = 15;
    private final List<BigInteger> nodes = new ArrayList<>();
    Map<BigInteger, Map<String, Counter>> metricsCountersPerNodeMap = new ConcurrentHashMap<>();

    private ScheduledFuture<?> scheduledResult;
    private final OpendaylightDirectStatisticsService opendaylightDirectStatisticsService;
    private final ScheduledExecutorService portStatExecutorService;
    private final MetricProvider metricProvider;

    @Inject
    public NodeConnectorStatsImpl(DataBroker dataBroker,
                                  final OpendaylightDirectStatisticsService opendaylightDirectStatisticsService,
                                  final MetricProvider metricProvider) {
        super(Node.class, NodeConnectorStatsImpl.class);
        this.opendaylightDirectStatisticsService = opendaylightDirectStatisticsService;
        this.metricProvider = metricProvider;

        registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
        portStatExecutorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE,
            getThreadFactory("Port Stats " + "Request Task"));
    }

    @Override
    public InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class);
    }

    @Override
    protected NodeConnectorStatsImpl getDataTreeChangeListener() {
        return NodeConnectorStatsImpl.this;
    }

    @Override
    @PreDestroy
    public void close() {
        // close the nested counter objects for each node
        for (Map.Entry<BigInteger, Map<String, Counter>> metricsCountersPerNodeMapEntry : metricsCountersPerNodeMap
                .entrySet()) {
            Map<String, Counter> nodeMetricCounterMap = metricsCountersPerNodeMapEntry.getValue();
            for (Map.Entry<String, Counter> nodeMetricCounterMapEntry : nodeMetricCounterMap.entrySet()) {
                nodeMetricCounterMapEntry.getValue().close();
            }
        }
    }

    /*
     * PortStat request task is started when first DPN gets connected
     */
    private void schedulePortStatRequestTask() {
        if (!Boolean.getBoolean(STATS_POLL_FLAG)) {
            LOG.info("Port statistics is turned off");
            return;
        }
        LOG.info("Scheduling port statistics request");
        PortStatRequestTask portStatRequestTask = new PortStatRequestTask();
        scheduledResult = portStatExecutorService.scheduleAtFixedRate(portStatRequestTask, NO_DELAY,
                Integer.getInteger(POLLING_INTERVAL_PATH, DEFAULT_POLLING_INTERVAL), TimeUnit.MINUTES);
    }

    /*
     * PortStat request task is stopped when last DPN is removed.
     */
    private void stopPortStatRequestTask() {
        if (scheduledResult != null) {
            LOG.info("Stopping port statistics request");
            scheduledResult.cancel(true);
        }
    }

    /*
     * This task queries for node connector statistics as well as flowtables
     * statistics every 10 secs. Minimum period which can be configured for
     * PMJob is 10 secs.
     */
    private class PortStatRequestTask implements Runnable {

        @Override
        public void run() {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Requesting port stats - {}");
            }
            for (BigInteger node : nodes) {
                LOG.trace("Requesting AllNodeConnectorStatistics and flow table statistics for node - {}", node);
                // Call RPC to Get NodeConnector Stats for node
                Future<RpcResult<GetNodeConnectorStatisticsOutput>> ncStatsFuture = opendaylightDirectStatisticsService
                        .getNodeConnectorStatistics(buildGetNodeConnectorStatisticsInput(node));
                //Create ListenableFuture to get RPC result asynchronously
                ListenableFuture<RpcResult<GetNodeConnectorStatisticsOutput>> ncStatsListenableFuture =
                        JdkFutureAdapters.listenInPoolThread(ncStatsFuture);

                Futures.addCallback(ncStatsListenableFuture, new
                        FutureCallback<RpcResult<GetNodeConnectorStatisticsOutput>>() {

                    @Override
                    public void onFailure(@Nonnull Throwable error) {
                        LOG.error("getNodeConnectorStatistics RPC failed for node: {} ", node, error);
                    }

                    @Override
                    public void onSuccess(RpcResult<GetNodeConnectorStatisticsOutput> result) {
                        if (result != null) {
                            if (result.isSuccessful()) {
                                GetNodeConnectorStatisticsOutput ncStatsRpcResult = result.getResult();
                                // process NodeConnectorStatistics RPC result
                                processNodeConnectorStatistics(ncStatsRpcResult, node);
                            } else {
                                LOG.error("getNodeConnectorStatistics RPC failed for node: {} with error: {}",
                                        node, result.getErrors());
                            }
                        }
                    }
                }, MoreExecutors.directExecutor());

                // Call RPC to Get flow stats for node
                Future<RpcResult<GetFlowStatisticsOutput>>  flowStatsFuture = opendaylightDirectStatisticsService
                        .getFlowStatistics(buildGetFlowStatisticsInput(node));
                //Create ListenableFuture to get RPC result asynchronously
                ListenableFuture<RpcResult<GetFlowStatisticsOutput>> flowStatsListenableFuture =
                        JdkFutureAdapters.listenInPoolThread(flowStatsFuture);

                Futures.addCallback(flowStatsListenableFuture, new
                        FutureCallback<RpcResult<GetFlowStatisticsOutput>>() {

                    @Override
                    public void onFailure(@Nonnull Throwable error) {
                        LOG.error("getFlowStatistics RPC failed for node: {} ", node, error);
                    }

                    @Override
                    public void onSuccess(RpcResult<GetFlowStatisticsOutput> result) {
                        if (result != null) {
                            if (result.isSuccessful()) {
                                GetFlowStatisticsOutput flowStatsRpcResult = result.getResult();
                                // process FlowStatistics RPC result
                                processFlowStatistics(flowStatsRpcResult, node);
                            } else {
                                LOG.error("getFlowStatistics RPC failed for node: {} with error: {}",
                                        node, result.getErrors());
                            }
                        }
                    }
                }, MoreExecutors.directExecutor());
            }
        }

        /**
         * This method builds GetNodeConnectorStatisticsInput which is input for NodeConnectorStatistics RPC.
         */
        private GetNodeConnectorStatisticsInput buildGetNodeConnectorStatisticsInput(BigInteger dpId) {
            return new GetNodeConnectorStatisticsInputBuilder()
                    .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, new NodeKey(new NodeId("openflow:" + dpId.toString()))).build()))
                    .build();
        }

        /**
         * This method builds GetFlowStatisticsInput which is input for FlowStatistics RPC.
         */
        private GetFlowStatisticsInput buildGetFlowStatisticsInput(BigInteger dpId) {
            return new GetFlowStatisticsInputBuilder()
                    .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, new NodeKey(new NodeId("openflow:" + dpId.toString()))).build()))
                    .build();
        }
    }

    private ThreadFactory getThreadFactory(String threadNameFormat) {
        ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat(threadNameFormat);
        builder.setUncaughtExceptionHandler((thread, exception) -> LOG
                .error("Received Uncaught Exception event in Thread: {}", thread.getName(), exception));
        return builder.build();
    }

    /**
     * This method processes NodeConnectorStatistics RPC result.
     * It performs:
     * - fetches various OF Port counters values
     * - creates/updates new OF Port counters using Infrautils metrics API
     * - set counter with values fetched from NodeConnectorStatistics
     */
    private void processNodeConnectorStatistics(GetNodeConnectorStatisticsOutput nodeConnectorStatisticsOutput,
                                                BigInteger dpid) {
        String node = dpid.toString();
        String portUuid = "";

        List<NodeConnectorStatisticsAndPortNumberMap> ncStatsAndPortMapList = nodeConnectorStatisticsOutput
                        .getNodeConnectorStatisticsAndPortNumberMap();

        // Parse NodeConnectorStatistics and create/update counters for them
        for (NodeConnectorStatisticsAndPortNumberMap ncStatsAndPortMap : ncStatsAndPortMapList) {
            NodeConnectorId nodeConnector = ncStatsAndPortMap.getNodeConnectorId();
            String port = nodeConnector.getValue().split(":")[2];

            String counterKey = IfmUtil.getMetricKey(node, port, portUuid,null)
                    + IfmConstants.IFM_PORT_COUNTER_OFPORT_DURATION;
            long ofPortDuration = ncStatsAndPortMap.getDuration().getSecond().getValue();
            updateCounter(dpid, counterKey, ofPortDuration);

            counterKey = IfmUtil.getMetricKey(node, port, portUuid,null)
                    + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECVDROP;
            long packetsPerOFPortReceiveDrop = ncStatsAndPortMap.getReceiveDrops().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortReceiveDrop);

            counterKey = IfmUtil.getMetricKey(node, port, portUuid,null)
                    + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECVERROR;
            long packetsPerOFPortReceiveError = ncStatsAndPortMap.getReceiveErrors().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortReceiveError);

            counterKey = IfmUtil.getMetricKey(node, port, portUuid,null)
                    + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_SENT;
            long packetsPerOFPortSent = ncStatsAndPortMap.getPackets().getTransmitted().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortSent);

            counterKey = IfmUtil.getMetricKey(node, port, portUuid,null)
                    + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECV;
            long packetsPerOFPortReceive = ncStatsAndPortMap.getPackets().getReceived().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortReceive);

            counterKey = IfmUtil.getMetricKey(node, port, portUuid,null)
                    + IfmConstants.IFM_PORT_COUNTER_OFPORT_BYTE_SENT;
            long bytesPerOFPortSent = ncStatsAndPortMap.getBytes().getTransmitted().longValue();
            updateCounter(dpid, counterKey, bytesPerOFPortSent);

            counterKey = IfmUtil.getMetricKey(node, port, portUuid,null)
                    + IfmConstants.IFM_PORT_COUNTER_OFPORT_BYTE_RECV;
            long bytesPerOFPortReceive = ncStatsAndPortMap.getBytes().getReceived().longValue();
            updateCounter(dpid, counterKey, bytesPerOFPortReceive);
        }
    }

    /**
     * This method processes FlowStatistics RPC result.
     * It performs:
     * - fetches all flows of node
     * - stores flows count per table in local map
     * - creates/updates Flow table counters using Infrautils metrics API
     * - set counter with values fetched from FlowStatistics
     */
    private void processFlowStatistics(GetFlowStatisticsOutput flowStatsOutput, BigInteger dpid) {
        String node = dpid.toString();
        Map<Short, Integer> flowTableMap = new HashMap<>();
        // Get all flows for node from RPC result
        List<FlowAndStatisticsMapList> flowTableAndStatisticsMapList = flowStatsOutput.getFlowAndStatisticsMapList();
        for (FlowAndStatisticsMapList flowAndStatisticsMap : flowTableAndStatisticsMapList) {
            short tableId = flowAndStatisticsMap.getTableId().shortValue();
            // populate map to maintain flow count per table
            if (flowTableMap.containsKey(tableId)) {
                Integer flowCount = flowTableMap.get(tableId);
                flowCount++;
                flowTableMap.put(tableId, flowCount);
            } else {
                flowTableMap.put(tableId, 1);
            }
        }
        for (Map.Entry<Short, Integer> flowTable : flowTableMap.entrySet()) {
            Short tableId = flowTable.getKey();
            Integer flowCount = flowTable.getValue();
            String counterKey = IfmUtil.getMetricKey(node, null, null, tableId.toString())
                    + IfmConstants.IFM_FLOW_TBL_COUNTER_FLOWS_PER_TBL;
            // create or update counter
            updateCounter(dpid, counterKey, flowCount.longValue());
        }
    }

    /**
     * This method creates or updates counters.
     * It performs:
     * - creates new counters using Infrautils metrics API
     * - store counter object into local map which will help to get counter object while updation
     * - updates counter value
     */
    private void updateCounter(BigInteger dpid, String counterKey, long counterValue) {
        if (metricsCountersPerNodeMap.containsKey(dpid)) {
            Counter counter = null;
            Map<String, Counter> nodeMetricCounterMap = metricsCountersPerNodeMap.get(dpid);
            if (nodeMetricCounterMap.containsKey(counterKey)) {
                counter = nodeMetricCounterMap.get(counterKey);
                if (counter == null) {
                    LOG.error("Metric counter (key: {}) is null.", counterKey);
                    return;
                }
                // reset counter to zero
                counter.decrement(counter.get());
            } else {
                counter = metricProvider.newCounter(this, counterKey);
                if (counter == null) {
                    LOG.error("Metric counter (key: {}) creation failed.", counterKey);
                    return;
                }
                nodeMetricCounterMap.put(counterKey, counter);
            }
            counter.increment(counterValue);
        } else {
            LOG.error("MetricCounter map was not added on Node Add event.");
        }
    }

    @Override
    protected void remove(InstanceIdentifier<Node> identifier, Node node) {
        NodeId nodeId = node.getId();
        String nodeVal = nodeId.getValue().split(":")[1];
        BigInteger dpId = new BigInteger(nodeVal);
        if (nodes.contains(dpId)) {
            nodes.remove(dpId);
            // close the nested counter objects for each node
            if (metricsCountersPerNodeMap.containsKey(dpId)) {
                Map<String, Counter> nodeMetricCounterMap = metricsCountersPerNodeMap.get(dpId);
                for (Map.Entry<String, Counter> nodeMetricCounterMapEntry : nodeMetricCounterMap.entrySet()) {
                    nodeMetricCounterMapEntry.getValue().close();
                }
            }
            // remove counters map for node
            metricsCountersPerNodeMap.remove(dpId);
        }
        if (nodes.isEmpty()) {
            stopPortStatRequestTask();
        }
    }

    @Override
    protected void update(InstanceIdentifier<Node> identifier, Node original, Node update) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void add(InstanceIdentifier<Node> identifier, Node node) {
        NodeId nodeId = node.getId();
        BigInteger dpId = new BigInteger(nodeId.getValue().split(":")[1]);
        if (nodes.contains(dpId)) {
            return;
        }
        nodes.add(dpId);
        // create counters map for node
        Map<String, Counter> nodeMetricCounterMap = new ConcurrentHashMap<>();
        metricsCountersPerNodeMap.put(dpId, nodeMetricCounterMap);
        if (nodes.size() == 1) {
            schedulePortStatRequestTask();
        }
    }
}
