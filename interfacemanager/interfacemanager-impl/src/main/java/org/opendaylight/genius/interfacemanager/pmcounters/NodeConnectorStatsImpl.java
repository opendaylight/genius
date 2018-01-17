/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.IfmConstants;
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
    private final OpendaylightDirectStatisticsService directStatsService;
    private final ScheduledExecutorService portStatExecutorService;
    private final MetricProvider metricProvider;

    @Inject
    public NodeConnectorStatsImpl(DataBroker dataBroker,
                                  final OpendaylightDirectStatisticsService opendaylightDirectStatisticsService,
                                  final MetricProvider metricProvider) {
        super(Node.class, NodeConnectorStatsImpl.class);
        this.directStatsService = opendaylightDirectStatisticsService;
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
                LOG.trace("Requesting AllNodeConnectorStatistics for node - {}", node);
                // Call RPC to Get NodeConnector Stats for node
                Future<RpcResult<GetNodeConnectorStatisticsOutput>>  nodeConnectorStatisticsOutput = directStatsService
                        .getNodeConnectorStatistics(buildGetNodeConnectorStatisticsInput(node));
                GetNodeConnectorStatisticsOutput ncStatsRpcResult = null;
                try {
                    ncStatsRpcResult = nodeConnectorStatisticsOutput.get().getResult();
                    // process NodeConnectorStatistics RPC result
                    processNodeConnectorStatistics(ncStatsRpcResult, node);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("getNodeConnectorStatistics RPC failed for node: {} ", node, e);
                }

                // Call RPC to Get flow stats for node
                Future<RpcResult<GetFlowStatisticsOutput>>  flowStatisticsOutput = directStatsService
                        .getFlowStatistics(buildGetFlowStatisticsInput(node));
                GetFlowStatisticsOutput flowStatsRpcResult = null;
                try {
                    flowStatsRpcResult = flowStatisticsOutput.get().getResult();
                    // process FlowStatistics RPC result
                    processFlowStatistics(flowStatsRpcResult, node);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("getFlowStatistics RPC failed for node: {} ", node, e);
                }
            }
        }

        /*
         * This method builds GetNodeConnectorStatisticsInput which is input for
         * NodeConnectorStatistics RPC
         */
        private GetNodeConnectorStatisticsInput buildGetNodeConnectorStatisticsInput(BigInteger dpId) {
            return new GetNodeConnectorStatisticsInputBuilder()
                    .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, new NodeKey(new NodeId("openflow:" + dpId.toString()))).build()))
                    .build();
        }

        /*
         * This method builds GetFlowStatisticsInput which is input for
         * FlowStatistics RPC
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

    /*
     * This method processes NodeConnectorStatistics RPC result and performs:
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

            String counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_PORT_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_PORTID_KEYWORD + port + IfmConstants.ENTITY_ID_ALIASID_KEYWORD + portUuid
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_PORT_COUNTER_OFPORT_DURATION;
            long ofPortDuration = ncStatsAndPortMap.getDuration().getSecond().getValue();
            updateCounter(dpid, counterKey, ofPortDuration);

            counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_PORT_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_PORTID_KEYWORD + port + IfmConstants.ENTITY_ID_ALIASID_KEYWORD + portUuid
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECVDROP;
            long packetsPerOFPortReceiveDrop = ncStatsAndPortMap.getReceiveDrops().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortReceiveDrop);

            counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_PORT_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_PORTID_KEYWORD + port + IfmConstants.ENTITY_ID_ALIASID_KEYWORD + portUuid
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECVERROR;
            long packetsPerOFPortReceiveError = ncStatsAndPortMap.getReceiveErrors().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortReceiveError);

            counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_PORT_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_PORTID_KEYWORD + port + IfmConstants.ENTITY_ID_ALIASID_KEYWORD + portUuid
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_SENT;
            long packetsPerOFPortSent = ncStatsAndPortMap.getPackets().getTransmitted().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortSent);

            counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_PORT_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_PORTID_KEYWORD + port + IfmConstants.ENTITY_ID_ALIASID_KEYWORD + portUuid
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECV;
            long packetsPerOFPortReceive = ncStatsAndPortMap.getPackets().getReceived().longValue();
            updateCounter(dpid, counterKey, packetsPerOFPortReceive);

            counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_PORT_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_PORTID_KEYWORD + port + IfmConstants.ENTITY_ID_ALIASID_KEYWORD + portUuid
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_PORT_COUNTER_OFPORT_BYTE_SENT;
            long bytesPerOFPortSent = ncStatsAndPortMap.getBytes().getTransmitted().longValue();
            updateCounter(dpid, counterKey, bytesPerOFPortSent);

            counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_PORT_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_PORTID_KEYWORD + port + IfmConstants.ENTITY_ID_ALIASID_KEYWORD + portUuid
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_PORT_COUNTER_OFPORT_BYTE_RECV;
            long bytesPerOFPortReceive = ncStatsAndPortMap.getBytes().getReceived().longValue();
            updateCounter(dpid, counterKey, bytesPerOFPortReceive);
        }
        LOG.trace("Port Stats {}", ncStatsAndPortMapList);
    }

    /*
     * This method processes FlowStatistics RPC result and performs:
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
            String counterKey = IfmConstants.ENTITY_CNT_KEYWORD + IfmConstants.ENTITY_TYPE_FLOWTBL_KEYWORD
                    + IfmConstants.ENTITY_ID_KEYWORD + IfmConstants.ENTITY_ID_SWITCHID_KEYWORD + node
                    + IfmConstants.ENTITY_ID_FLOWTBLID_KEYWORD + tableId.toString()
                    + IfmConstants.ENTITY_ID_BLOCK_CLOSURE_KEYWORD + IfmConstants.IFM_FLOW_TBL_COUNTER_FLOWS_PER_TBL;
            // create or update counter
            updateCounter(dpid, counterKey, flowCount.longValue());
        }
    }

    /*
     * This method creates or updates counters and performs:
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
