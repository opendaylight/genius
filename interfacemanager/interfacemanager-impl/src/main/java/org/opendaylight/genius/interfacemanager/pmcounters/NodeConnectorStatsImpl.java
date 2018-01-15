/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.utils.concurrent.ListenableFutures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.config.rev160406.IfmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NodeConnectorStatsImpl extends AsyncClusteredDataTreeChangeListenerBase<Node, NodeConnectorStatsImpl> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeConnectorStatsImpl.class);
    private static final int THREAD_POOL_SIZE = 4;
    private static final PMAgentForNodeConnectorCounters PMAGENT = new PMAgentForNodeConnectorCounters();
    private volatile int delayStatsQuery;
    private final PortRpcStatisticsListener portStatsListener = new PortRpcStatisticsListener();
    private final FlowRpcStatisticsListener flowTableStatsListener = new FlowRpcStatisticsListener();
    private final Set<BigInteger> nodes = ConcurrentHashMap.newKeySet();
    private Map<String, Map<String, String>> nodeAndNcIdOFPortDurationMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> nodeAndNcIdOFPortReceiveDropMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> nodeAndNcIdOFPortReceiveError = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> nodeAndNcIdPacketSentMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> nodeAndNcIdPacketReceiveMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> nodeAndNcIdBytesSentMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> nodeAndNcIdBytesReceiveMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> nodeAndEntriesPerOFTableMap = new ConcurrentHashMap<>();
    private ScheduledFuture<?> scheduledResult;
    private final OpendaylightPortStatisticsService statPortService;
    private final ScheduledExecutorService portStatExecutorService;
    private final OpendaylightFlowTableStatisticsService opendaylightFlowTableStatisticsService;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final IfmConfig ifmConfig;

    @Inject
    public NodeConnectorStatsImpl(DataBroker dataBroker, NotificationService notificationService,
                                  final OpendaylightPortStatisticsService opendaylightPortStatisticsService,
                                  final OpendaylightFlowTableStatisticsService opendaylightFlowTableStatisticsService,
                                  final EntityOwnershipUtils entityOwnershipUtils,
                                  final IfmConfig ifmConfigObj) {
        super(Node.class, NodeConnectorStatsImpl.class);
        this.statPortService = opendaylightPortStatisticsService;
        this.opendaylightFlowTableStatisticsService = opendaylightFlowTableStatisticsService;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.ifmConfig = ifmConfigObj;
        registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
        portStatExecutorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE,
            getThreadFactory("Port Stats " + "Request Task"));
        notificationService.registerNotificationListener(portStatsListener);
        notificationService.registerNotificationListener(flowTableStatsListener);
        PMAGENT.registerMbean();
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
        if (!ifmConfig.isIfmStatsPollEnabled()) {
            LOG.info("Port statistics is turned off");
            return;
        }
        LOG.info("Scheduling port statistics request");
        PortStatRequestTask portStatRequestTask = new PortStatRequestTask();
        scheduledResult = portStatExecutorService.scheduleWithFixedDelay(portStatRequestTask,
                ifmConfig.getIfmStatsInitialDelay(), ifmConfig.getIfmStatsDefPollInterval(), TimeUnit.MINUTES);
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

                ListenableFutures.addErrorLogging(JdkFutureAdapters.listenInPoolThread(statPortService
                        .getAllNodeConnectorsStatistics(buildGetAllNodeConnectorStatistics(node))),
                        LOG, "Get node connector stats");

                ListenableFutures.addErrorLogging(JdkFutureAdapters.listenInPoolThread(
                        opendaylightFlowTableStatisticsService.getFlowTablesStatistics(
                                buildGetFlowTablesStatistics(node))), LOG, "Get flow table stats");
                delay();
            }
        }

        /**
         * The delay is added to spread the RPC call of the switches to query statistics
         * across the polling interval.
         * delay factor is calculated by dividing pollinginterval by no.of.switches.
         */

        private void delay() {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(delayStatsQuery));
            } catch (InterruptedException ex) {
                LOG.error("InterruptedException");
            }
        }

        private GetAllNodeConnectorsStatisticsInput buildGetAllNodeConnectorStatistics(BigInteger dpId) {
            return new GetAllNodeConnectorsStatisticsInputBuilder()
                    .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, new NodeKey(new NodeId("openflow:" + dpId.toString()))).build()))
                    .build();
        }

        private GetFlowTablesStatisticsInput buildGetFlowTablesStatistics(BigInteger dpId) {
            return new GetFlowTablesStatisticsInputBuilder()
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
     * PortRpcStatisticsListener listens for the NodeConnectorStatisticsUpdate
     * and then update the corresponding counter map
     */
    class PortRpcStatisticsListener implements OpendaylightPortStatisticsListener {

        @Override
        public void onNodeConnectorStatisticsUpdate(NodeConnectorStatisticsUpdate ncStats) {
            Map<String, String> ncIdOFPortDurationMap = new HashMap<>();
            Map<String, String> ncIdOFPortReceiveDropMap = new HashMap<>();
            Map<String, String> ncIdOFPortReceiveError = new HashMap<>();
            Map<String, String> ncIdPacketSentMap = new HashMap<>();
            Map<String, String> ncIdPacketReceiveMap = new HashMap<>();
            Map<String, String> ncIdBytesSentMap = new HashMap<>();
            Map<String, String> ncIdBytesReceiveMap = new HashMap<>();
            List<NodeConnectorStatisticsAndPortNumberMap> ncStatsAndPortMapList = ncStats
                    .getNodeConnectorStatisticsAndPortNumberMap();
            NodeId nodeId = ncStats.getId();
            String node = nodeId.getValue().split(":")[1];
            for (NodeConnectorStatisticsAndPortNumberMap ncStatsAndPortMap : ncStatsAndPortMapList) {
                NodeConnectorId nodeConnector = ncStatsAndPortMap.getNodeConnectorId();
                String port = nodeConnector.getValue().split(":")[2];
                String nodePortStr = "dpnId_" + node + "_portNum_" + port;
                ncIdOFPortDurationMap.put("OFPortDuration:" + nodePortStr + "_OFPortDuration",
                        ncStatsAndPortMap.getDuration().getSecond().getValue().toString());
                ncIdOFPortReceiveDropMap.put(
                        "PacketsPerOFPortReceiveDrop:" + nodePortStr + "_PacketsPerOFPortReceiveDrop",
                        ncStatsAndPortMap.getReceiveDrops().toString());
                ncIdOFPortReceiveError.put(
                        "PacketsPerOFPortReceiveError:" + nodePortStr + "_PacketsPerOFPortReceiveError",
                        ncStatsAndPortMap.getReceiveErrors().toString());
                ncIdPacketSentMap.put("PacketsPerOFPortSent:" + nodePortStr + "_PacketsPerOFPortSent",
                        ncStatsAndPortMap.getPackets().getTransmitted().toString());
                ncIdPacketReceiveMap.put("PacketsPerOFPortReceive:" + nodePortStr + "_PacketsPerOFPortReceive",
                        ncStatsAndPortMap.getPackets().getReceived().toString());
                ncIdBytesSentMap.put("BytesPerOFPortSent:" + nodePortStr + "_BytesPerOFPortSent",
                        ncStatsAndPortMap.getBytes().getTransmitted().toString());
                ncIdBytesReceiveMap.put("BytesPerOFPortReceive:" + nodePortStr + "_BytesPerOFPortReceive",
                        ncStatsAndPortMap.getBytes().getReceived().toString());
            }
            // Storing allNodeConnectorStats(like ncIdOFPortDurationMap) in a
            // map with key as node for easy removal and addition of
            // allNodeConnectorStats.
            nodeAndNcIdOFPortDurationMap.put(node, ncIdOFPortDurationMap);
            nodeAndNcIdOFPortReceiveDropMap.put(node, ncIdOFPortReceiveDropMap);
            nodeAndNcIdOFPortReceiveError.put(node, ncIdOFPortReceiveError);
            nodeAndNcIdPacketSentMap.put(node, ncIdPacketSentMap);
            nodeAndNcIdPacketReceiveMap.put(node, ncIdPacketReceiveMap);
            nodeAndNcIdBytesSentMap.put(node, ncIdBytesSentMap);
            nodeAndNcIdBytesReceiveMap.put(node, ncIdBytesReceiveMap);
            // Combining the stats of all nodeconnectors in all nodes. This Map
            // will be stored under MBean which will be queried as regular
            // intervals.
            ncIdOFPortDurationMap = combineAllNodesStats(nodeAndNcIdOFPortDurationMap);
            ncIdOFPortReceiveDropMap = combineAllNodesStats(nodeAndNcIdOFPortReceiveDropMap);
            ncIdOFPortReceiveError = combineAllNodesStats(nodeAndNcIdOFPortReceiveError);
            ncIdPacketSentMap = combineAllNodesStats(nodeAndNcIdPacketSentMap);
            ncIdPacketReceiveMap = combineAllNodesStats(nodeAndNcIdPacketReceiveMap);
            ncIdBytesSentMap = combineAllNodesStats(nodeAndNcIdBytesSentMap);
            ncIdBytesReceiveMap = combineAllNodesStats(nodeAndNcIdBytesReceiveMap);
            PMAGENT.connectToPMAgent(ncIdOFPortDurationMap, ncIdOFPortReceiveDropMap, ncIdOFPortReceiveError,
                    ncIdPacketSentMap, ncIdPacketReceiveMap, ncIdBytesSentMap, ncIdBytesReceiveMap);
        }

        /*
         * Input allNodesStats contains statistics of all nodeConnectors of all
         * nodes. Key is the node and values contains another map with key as
         * node connector and value as statresult. Output will be a map with key
         * as nodeconnector and value as the statresult. The key contains
         * nodeconnectors of all the nodes.
         */
    }

    private Map<String, String> combineAllNodesStats(Map<String, Map<String, String>> allNodesStats) {
        Map<String, String> allNcsStatsMap = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : allNodesStats.entrySet()) {
            Map<String, String> ncStatsMap = entry.getValue();
            for (Map.Entry<String, String> statResult : ncStatsMap.entrySet()) {
                allNcsStatsMap.put(statResult.getKey(), statResult.getValue());
            }
        }
        return allNcsStatsMap;
    }

    /*
     * FlowRpcStatisticsListener listens for the FlowTableStatisticsUpdate and
     * then update the corresponding counter map
     */
    class FlowRpcStatisticsListener implements OpendaylightFlowTableStatisticsListener {

        @Override
        public void onFlowTableStatisticsUpdate(FlowTableStatisticsUpdate flowTableStats) {
            String node = flowTableStats.getId().getValue().split(":")[1];
            Map<String, String> entriesPerOFTableMap = new HashMap<>();
            List<FlowTableAndStatisticsMap> flowTableAndStatisticsMapList = flowTableStats
                    .getFlowTableAndStatisticsMap();
            for (FlowTableAndStatisticsMap flowTableAndStatisticsMap : flowTableAndStatisticsMapList) {
                String nodeTableStr = "dpnId_" + node + "_table_"
                        + flowTableAndStatisticsMap.getTableId().getValue().toString();
                entriesPerOFTableMap.put("EntriesPerOFTable:" + nodeTableStr + "_EntriesPerOFTable",
                        flowTableAndStatisticsMap.getActiveFlows().getValue().toString());
            }
            nodeAndEntriesPerOFTableMap.put(node, entriesPerOFTableMap);
            entriesPerOFTableMap = combineAllNodesStats(nodeAndEntriesPerOFTableMap);
            PMAGENT.connectToPMAgentAndInvokeEntriesPerOFTable(entriesPerOFTableMap);
        }
    }

    @Override
    protected void remove(InstanceIdentifier<Node> identifier, Node node) {
        NodeId nodeId = node.getId();
        String nodeVal = nodeId.getValue().split(":")[1];
        BigInteger dpId = new BigInteger(nodeVal);
        if (nodes.contains(dpId)) {
            nodes.remove(dpId);
            nodeAndNcIdOFPortDurationMap.remove(nodeVal);
            nodeAndNcIdOFPortReceiveDropMap.remove(nodeVal);
            nodeAndNcIdOFPortReceiveError.remove(nodeVal);
            nodeAndNcIdPacketSentMap.remove(nodeVal);
            nodeAndNcIdPacketReceiveMap.remove(nodeVal);
            nodeAndNcIdBytesSentMap.remove(nodeVal);
            nodeAndNcIdBytesReceiveMap.remove(nodeVal);
            nodeAndEntriesPerOFTableMap.remove(nodeVal);
        }
        if (nodes.size() > 0) {
            delayStatsQuery = ifmConfig.getIfmStatsDefPollInterval() / nodes.size();
        } else {
            stopPortStatRequestTask();
            delayStatsQuery = 0;
        }
    }

    @Override
    protected void update(InstanceIdentifier<Node> identifier, Node original, Node update) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void add(InstanceIdentifier<Node> identifier, Node node) {
        NodeId nodeId = node.getId();
        if (entityOwnershipUtils.isEntityOwner(IfmConstants.SERVICE_ENTITY_TYPE, nodeId.getValue())) {
            LOG.trace("Locally connected switch {}",nodeId.getValue());
            BigInteger dpId = new BigInteger(nodeId.getValue().split(":")[1]);
            if (nodes.contains(dpId)) {
                return;
            }
            nodes.add(dpId);
            delayStatsQuery = ifmConfig.getIfmStatsDefPollInterval() / nodes.size();
            if (nodes.size() == 1) {
                schedulePortStatRequestTask();
            }
        } else {
            LOG.trace("Not a locally connected switch {}",nodeId.getValue());
        }
    }
}
