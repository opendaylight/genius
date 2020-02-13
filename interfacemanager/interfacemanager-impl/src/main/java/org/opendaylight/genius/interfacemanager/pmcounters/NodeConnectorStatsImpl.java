/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

import static org.opendaylight.infrautils.utils.concurrent.Executors.newListeningScheduledThreadPool;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.listeners.InterfaceChildCache;
import org.opendaylight.genius.interfacemanager.listeners.PortNameCache;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.Labeled;
import org.opendaylight.infrautils.metrics.MetricDescriptor;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.infrautils.utils.UncheckedCloseable;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredAsyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetFlowStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetFlowStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetFlowStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.OpendaylightDirectStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.config.rev160406.IfmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
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
public class NodeConnectorStatsImpl extends AbstractClusteredAsyncDataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeConnectorStatsImpl.class);

    private static final int THREAD_POOL_SIZE = 4;
    private final Set<String> nodes = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<Counter>> metricsCountersPerNodeMap = new ConcurrentHashMap<>();
    private final OpendaylightDirectStatisticsService opendaylightDirectStatisticsService;
    private final ScheduledExecutorService portStatExecutorService;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final PortNameCache portNameCache;
    private final InterfaceChildCache interfaceChildCache;
    private final IfmConfig ifmConfig;
    private final MetricProvider metricProvider;

    private volatile int delayStatsQuery;
    private ScheduledFuture<?> scheduledResult;

    @Inject
    public NodeConnectorStatsImpl(@Reference DataBroker dataBroker,
                                  final OpendaylightDirectStatisticsService opendaylightDirectStatisticsService,
                                  final EntityOwnershipUtils entityOwnershipUtils,
                                  final PortNameCache portNameCache,
                                  final InterfaceChildCache interfaceChildCache,
                                  final IfmConfig ifmConfigObj,
                                  final @Reference  MetricProvider metricProvider) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(Nodes.class).child(Node.class),
                Executors.newSingleThreadExecutor("NodeConnectorStatsImpl", LOG));
        this.opendaylightDirectStatisticsService = opendaylightDirectStatisticsService;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.portNameCache = portNameCache;
        this.interfaceChildCache = interfaceChildCache;
        this.ifmConfig = ifmConfigObj;
        this.metricProvider = metricProvider;
        portStatExecutorService = newListeningScheduledThreadPool(THREAD_POOL_SIZE, "Port Stats Request Task", LOG);
    }

    @Override
    @PreDestroy
    public void close() {
        // close the nested counter objects for each node
        metricsCountersPerNodeMap.values().forEach(counterSet -> counterSet.forEach(UncheckedCloseable::close));
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
                ifmConfig.getIfmStatsDefPollInterval().toJava(), ifmConfig.getIfmStatsDefPollInterval().toJava(),
                TimeUnit.MINUTES);
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
                LOG.trace("Requesting port stats");
            }
            for (String node : nodes) {
                LOG.trace("Requesting AllNodeConnectorStatistics and flow table statistics for node - {}", node);
                // Call RPC to Get NodeConnector Stats for node
                ListenableFuture<RpcResult<GetNodeConnectorStatisticsOutput>> ncStatsFuture =
                        opendaylightDirectStatisticsService.getNodeConnectorStatistics(
                            buildGetNodeConnectorStatisticsInput(node));

                Futures.addCallback(ncStatsFuture, new FutureCallback<RpcResult<GetNodeConnectorStatisticsOutput>>() {

                    @Override
                    public void onFailure(Throwable error) {
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
                }, portStatExecutorService);

                // Call RPC to Get flow stats for node
                ListenableFuture<RpcResult<GetFlowStatisticsOutput>> flowStatsFuture =
                        opendaylightDirectStatisticsService.getFlowStatistics(buildGetFlowStatisticsInput(node));

                Futures.addCallback(flowStatsFuture, new FutureCallback<RpcResult<GetFlowStatisticsOutput>>() {

                    @Override
                    public void onFailure(Throwable error) {
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
                }, portStatExecutorService);

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

        /**
         * This method builds GetNodeConnectorStatisticsInput which is input for NodeConnectorStatistics RPC.
         */
        private GetNodeConnectorStatisticsInput buildGetNodeConnectorStatisticsInput(String dpId) {
            return new GetNodeConnectorStatisticsInputBuilder()
                    .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, new NodeKey(new NodeId("openflow:" + dpId))).build()))
                    .build();
        }

        /**
         * This method builds GetFlowStatisticsInput which is input for FlowStatistics RPC.
         */
        private GetFlowStatisticsInput buildGetFlowStatisticsInput(String dpId) {
            return new GetFlowStatisticsInputBuilder()
                    .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, new NodeKey(new NodeId("openflow:" + dpId))).build()))
                    .build();
        }
    }

    /**
     * This method processes NodeConnectorStatistics RPC result.
     * It performs:
     * - fetches various OF Port counters values
     * - creates/updates new OF Port counters using Infrautils metrics API
     * - set counter with values fetched from NodeConnectorStatistics
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void processNodeConnectorStatistics(GetNodeConnectorStatisticsOutput nodeConnectorStatisticsOutput,
                                                String dpid) {
        String port = "";
        String portUuid = "";
        List<NodeConnectorStatisticsAndPortNumberMap> ncStatsAndPortMapList = nodeConnectorStatisticsOutput
                        .nonnullNodeConnectorStatisticsAndPortNumberMap();
        // Parse NodeConnectorStatistics and create/update counters for them
        for (NodeConnectorStatisticsAndPortNumberMap ncStatsAndPortMap : ncStatsAndPortMapList) {
            NodeConnectorId nodeConnector = ncStatsAndPortMap.getNodeConnectorId();
            LOG.trace("Create/update metric counter for NodeConnector: {} of node: {}", nodeConnector, dpid);
            port = nodeConnector.getValue();
            // update port name as per port name maintained in portNameCache
            String portNameInCache = "openflow" + ":" + dpid + ":" + port;
            java.util.Optional<String> portName = portNameCache.get(portNameInCache);
            if (portName.isPresent()) {
                Optional<List<InterfaceChildEntry>> interfaceChildEntries = interfaceChildCache
                        .getInterfaceChildEntries(portName.get());
                if (interfaceChildEntries.isPresent()) {
                    if (!interfaceChildEntries.get().isEmpty()) {
                        portUuid = interfaceChildEntries.get().get(0).getChildInterface();
                        LOG.trace("Retrieved portUuid {} for portname {}", portUuid, portName.get());
                    } else {
                        LOG.trace("PortUuid is not found for portname {}. Skipping IFM counters publish for this port.",
                            portName.get());
                        continue;
                    }
                } else {
                    LOG.trace("PortUuid is not present for portname {}. Skipping IFM counters publish for this port.",
                        portName.get());
                    continue;
                }
            } else {
                LOG.trace("Port {} not found in PortName Cache.", portNameInCache);
                continue;
            }

            Counter counter = getCounter(CounterConstants.IFM_PORT_COUNTER_OFPORT_DURATION, dpid, port, portUuid,null);
            long ofPortDuration = ncStatsAndPortMap.getDuration().getSecond().getValue().toJava();
            updateCounter(counter, ofPortDuration);

            counter = getCounter(CounterConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECVDROP, dpid, port, portUuid, null);
            long packetsPerOFPortReceiveDrop = ncStatsAndPortMap.getReceiveDrops().longValue();
            updateCounter(counter, packetsPerOFPortReceiveDrop);

            counter = getCounter(CounterConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECVERROR, dpid, port, portUuid, null);
            long packetsPerOFPortReceiveError = ncStatsAndPortMap.getReceiveErrors().longValue();
            updateCounter(counter, packetsPerOFPortReceiveError);

            counter = getCounter(CounterConstants.IFM_PORT_COUNTER_OFPORT_PKT_SENT, dpid, port, portUuid, null);
            long packetsPerOFPortSent = ncStatsAndPortMap.getPackets().getTransmitted().longValue();
            updateCounter(counter, packetsPerOFPortSent);

            counter = getCounter(CounterConstants.IFM_PORT_COUNTER_OFPORT_PKT_RECV, dpid, port, portUuid, null);
            long packetsPerOFPortReceive = ncStatsAndPortMap.getPackets().getReceived().longValue();
            updateCounter(counter, packetsPerOFPortReceive);

            counter = getCounter(CounterConstants.IFM_PORT_COUNTER_OFPORT_BYTE_SENT, dpid, port, portUuid, null);
            long bytesPerOFPortSent = ncStatsAndPortMap.getBytes().getTransmitted().longValue();
            updateCounter(counter, bytesPerOFPortSent);

            counter = getCounter(CounterConstants.IFM_PORT_COUNTER_OFPORT_BYTE_RECV, dpid, port, portUuid, null);
            long bytesPerOFPortReceive = ncStatsAndPortMap.getBytes().getReceived().longValue();
            updateCounter(counter, bytesPerOFPortReceive);
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
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void processFlowStatistics(GetFlowStatisticsOutput flowStatsOutput, String dpid) {
        Map<Short, AtomicInteger> flowTableMap = new HashMap<>();
        // Get all flows for node from RPC result
        List<FlowAndStatisticsMapList> flowTableAndStatisticsMapList =
            flowStatsOutput.nonnullFlowAndStatisticsMapList();
        for (FlowAndStatisticsMapList flowAndStatisticsMap : flowTableAndStatisticsMapList) {
            short tableId = flowAndStatisticsMap.getTableId().toJava();
            // populate map to maintain flow count per table
            flowTableMap.computeIfAbsent(tableId, key -> new AtomicInteger(0)).incrementAndGet();
        }
        LOG.trace("FlowTableStatistics (tableId:counter): {} for node: {}", flowTableMap.entrySet(), dpid);
        for (Map.Entry<Short, AtomicInteger> flowTable : flowTableMap.entrySet()) {
            Short tableId = flowTable.getKey();
            AtomicInteger flowCount = flowTable.getValue();
            Counter counter = getCounter(CounterConstants.IFM_FLOW_TBL_COUNTER_FLOWS_PER_TBL, dpid, null, null,
                    tableId.toString());
            // update counter value
            updateCounter(counter, flowCount.longValue());
        }
    }

    /*
     * This method returns counter and also creates counter if does not exist.
     *
     * @param counterName name of the counter
     * @param switchId datapath-id value
     * @param port port-id value
     * @param aliasId alias-id value
     * @param tableId table-id value of switch
     * @return counter object
     */
    private Counter getCounter(String counterName, String switchId, String port, String aliasId, String tableId) {
        /*
         * Pattern to be followed for key generation:
         *
         * genius.interfacemanager.entitycounter{entitytype=port,switchid=value,portid=value,aliasid=value,
         * name=counterName}
         * genius.interfacemanager.entitycounter{entitytype=flowtable,switchid=value,flowtableid=value,name=counterName}
         */
        Counter counter = null;
        if (port != null) {
            Labeled<Labeled<Labeled<Labeled<Labeled<Counter>>>>> labeledCounter =
                    metricProvider.newCounter(MetricDescriptor.builder().anchor(this).project("genius")
                        .module("interfacemanager").id(CounterConstants.CNT_TYPE_ENTITY_CNT_ID).build(),
                        CounterConstants.LBL_KEY_ENTITY_TYPE, CounterConstants.LBL_KEY_SWITCHID,
                        CounterConstants.LBL_KEY_PORTID, CounterConstants.LBL_KEY_ALIASID,
                        CounterConstants.LBL_KEY_COUNTER_NAME);
            counter = labeledCounter.label(CounterConstants.LBL_VAL_ENTITY_TYPE_PORT).label(switchId)
                    .label(port).label(aliasId).label(counterName);
        }
        if (tableId != null) {
            Labeled<Labeled<Labeled<Labeled<Counter>>>> labeledCounter =
                    metricProvider.newCounter(MetricDescriptor.builder().anchor(this).project("genius")
                        .module("interfacemanager").id(CounterConstants.CNT_TYPE_ENTITY_CNT_ID).build(),
                        CounterConstants.LBL_KEY_ENTITY_TYPE, CounterConstants.LBL_KEY_SWITCHID,
                        CounterConstants.LBL_KEY_FLOWTBLID, CounterConstants.LBL_KEY_COUNTER_NAME);
            counter = labeledCounter.label(CounterConstants.LBL_VAL_ENTITY_TYPE_FLOWTBL).label(switchId)
                    .label(tableId).label(counterName);
        }

        // create counters set for node if absent.
        // and then populate counter set with counter object
        // which will be needed to close counters when node is removed.
        metricsCountersPerNodeMap.computeIfAbsent(switchId, counterSet -> ConcurrentHashMap.newKeySet()).add(counter);

        return counter;
    }

    /**
     * This method updates counter values.
     */
    private void updateCounter(Counter counter, long counterValue) {
        try {
            // reset counter to zero
            counter.decrement(counter.get());
            // set counter to specified value
            counter.increment(counterValue);
        } catch (IllegalStateException e) {
            LOG.error("Metric counter ({}) update has got exception: ", counter, e);
        }
    }

    @Override
    public void remove(InstanceIdentifier<Node> identifier, Node node) {
        NodeId nodeId = node.getId();
        String dpId = nodeId.getValue().split(":")[1];
        if (nodes.contains(dpId)) {
            nodes.remove(dpId);
            // remove counters set from node
            Set<Counter> nodeMetricCounterSet = metricsCountersPerNodeMap.remove(dpId);
            if (nodeMetricCounterSet != null) {
                // remove counters
                nodeMetricCounterSet.forEach(UncheckedCloseable::close);
            }
        }
        if (nodes.size() > 0) {
            delayStatsQuery = ifmConfig.getIfmStatsDefPollInterval().toJava() / nodes.size();
        } else {
            stopPortStatRequestTask();
            delayStatsQuery = 0;
        }
    }

    @Override
    public void update(InstanceIdentifier<Node> identifier, Node original, Node update) {
        // TODO Auto-generated method stub
    }

    @Override
    public void add(InstanceIdentifier<Node> identifier, Node node) {
        NodeId nodeId = node.getId();
        if (entityOwnershipUtils.isEntityOwner(IfmConstants.SERVICE_ENTITY_TYPE, nodeId.getValue())) {
            LOG.trace("Locally connected switch {}",nodeId.getValue());
            String dpId = nodeId.getValue().split(":")[1];
            if (nodes.contains(dpId)) {
                return;
            }
            nodes.add(dpId);
            delayStatsQuery = ifmConfig.getIfmStatsDefPollInterval().toJava() / nodes.size();
            if (nodes.size() == 1) {
                schedulePortStatRequestTask();
            }
        } else {
            LOG.trace("Not a locally connected switch {}",nodeId.getValue());
        }
    }
}
