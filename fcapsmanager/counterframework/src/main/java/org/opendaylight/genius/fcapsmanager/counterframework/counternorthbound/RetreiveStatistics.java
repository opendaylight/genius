/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.counternorthbound;

import org.opendaylight.genius.fcapsmanager.counterframework.CounterUtil;
import org.opendaylight.genius.fcapsmanager.counterframework.model.*;
import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.PerformanceCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info.Switch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info._switch.SwitchPortsCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info._switch.TableCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.bgp.info.BgpNeighborCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.bgp.info.BgpRdRouteCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.performance.counters.BgpCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.performance.counters.Controllers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.performance.counters.SwitchCounters;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RetreiveStatistics {
    private static final Logger LOG = LoggerFactory.getLogger(RetreiveStatistics.class);
    private static DataBroker dataBroker;
    private List<NBBgpNeighbor> neighborList;
    private List<NBBgpRoute> nbBgpRoutesList;
    private NorthBoundBgpStatistics northBoundBgpStatistics;
    private NBBgpCounters nbBgpCounters = new NBBgpCounters();

    private List<NBTableCounters> tableCountersList;
    private List<NBSwitchPortCounters> switchPortCountersList;
    private List<NBSwitchCounters> switchCountersList;
    private NorthBoundSwitchStatistics northBoundSwitchStatistics;

    private List<NBNode> nodeList;
    private NorthBoundControllersCounters northBoundControllersCounters;

    public RetreiveStatistics() {
        northBoundBgpStatistics = new NorthBoundBgpStatistics();
        northBoundSwitchStatistics = new NorthBoundSwitchStatistics();
        northBoundControllersCounters = new NorthBoundControllersCounters();
    }

    public RetreiveStatistics(final DataBroker db) {
        LOG.info("RetreiveStatistics databroker is set");
        dataBroker = db;
        northBoundBgpStatistics = new NorthBoundBgpStatistics();
        northBoundSwitchStatistics = new NorthBoundSwitchStatistics();
        northBoundControllersCounters = new NorthBoundControllersCounters();
    }
    /*public static void setDataBroker(DataBroker dataBroker) {
        RetreiveStatistics.dataBroker = dataBroker;
    }*/

    public NorthBoundBgpStatistics getBgpNeighborsStatistics() {

        List<BgpNeighborCounters> bgpNeigh = null;
        List<BgpRdRouteCounters> bgpRdRouteCountersList = null;

        InstanceIdentifier<BgpCounters> bgpNeighborCountersId = InstanceIdentifier.create(PerformanceCounters.class)
                .child(BgpCounters.class);
        Optional<BgpCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                bgpNeighborCountersId);

        if (counterData != null && counterData.isPresent()) {
            LOG.debug("Bgp statistics found");
            bgpNeigh = counterData.get().getBgpNeighborCounters();
            bgpRdRouteCountersList = counterData.get().getBgpRdRouteCounters();

            if (bgpNeigh != null) {
                neighborList = new ArrayList<>();
                for (BgpNeighborCounters counter : bgpNeigh) {
                    NBBgpNeighbor nbBgpNeighborObj = new NBBgpNeighbor();
                    nbBgpNeighborObj.setAutonomous_system_number(counter.getAsId());
                    nbBgpNeighborObj.setNeighbor_ip(counter.getNeighborIp());
                    nbBgpNeighborObj.setPackets_received(counter.getBgpNeighborPacketsReceived());
                    nbBgpNeighborObj.setPackets_sent(counter.getBgpNeighborPacketsSent());
                    neighborList.add(nbBgpNeighborObj);
                }
            }

            if (bgpRdRouteCountersList != null) {
                nbBgpRoutesList = new ArrayList<>();
                for (BgpRdRouteCounters counter : bgpRdRouteCountersList) {
                    NBBgpRoute nbBgpRouteObj = new NBBgpRoute();
                    nbBgpRouteObj.setRoute_distinguisher(counter.getRd());
                    nbBgpRouteObj.setRoutes(counter.getBgpRdRouteCount());
                    nbBgpRoutesList.add(nbBgpRouteObj);
                }
            }

            nbBgpCounters.setTotal_routes(counterData.get().getBgpTotalPrefixes());
            nbBgpCounters.setBgp_neighbor_counters(neighborList);
            nbBgpCounters.setBgp_route_counters(nbBgpRoutesList);
        } else {
            LOG.debug("No Bgp statistics found");
        }
        northBoundBgpStatistics.setBgp(nbBgpCounters);
        return northBoundBgpStatistics;

    }


    public NorthBoundSwitchStatistics getSwitchStatistics() {
        List<Switch> switchList = null;
        List<TableCounters> tableCounters = null;
        List<SwitchPortsCounters> switchPortsCounters = null;

        InstanceIdentifier<SwitchCounters> switchCountersId = InstanceIdentifier.create(PerformanceCounters.class)
                .child(SwitchCounters.class);

        Optional<SwitchCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                switchCountersId);

        switchCountersList = new ArrayList<>();
        if (counterData != null && counterData.isPresent()) {
            LOG.debug("Switch statistics found");
            switchList = counterData.get().getSwitch();

            if (switchList != null) {
                for (Switch switchEntry : switchList) {

                    //TableCounters
                    tableCounters = switchEntry.getTableCounters();
                    if (tableCounters != null) {
                        tableCountersList = new ArrayList<>();
                        for (TableCounters tableEntry : tableCounters) {
                            NBTableCounters nbTableCounters = new NBTableCounters();
                            nbTableCounters.setTable_id(tableEntry.getTableId());
                            nbTableCounters.setFlow_count(tableEntry.getEntriesPerOFTable());
                            tableCountersList.add(nbTableCounters);
                        }
                    }

                    //SwitchPortCounters
                    switchPortsCounters = switchEntry.getSwitchPortsCounters();
                    if (switchPortsCounters != null) {
                        switchPortCountersList = new ArrayList<>();
                        for (SwitchPortsCounters portEntry : switchPortsCounters) {
                            NBSwitchPortCounters nbSwitchPortCounters = new NBSwitchPortCounters();
                            nbSwitchPortCounters.setPort_id(portEntry.getPortId());
                            if (portEntry.getOFPortDuration() != null)
                                nbSwitchPortCounters.setDuration(portEntry.getOFPortDuration());
                            if (portEntry.getBytesPerOFPortReceive() != null)
                                nbSwitchPortCounters.setBytes_received(portEntry.getBytesPerOFPortReceive());
                            if (portEntry.getBytesPerOFPortSent() != null)
                                nbSwitchPortCounters.setBytes_sent(portEntry.getBytesPerOFPortSent());
                            if (portEntry.getPacketsPerInternalPortReceive() != null)
                                nbSwitchPortCounters.setPackets_received_on_tunnel(portEntry.getPacketsPerInternalPortReceive());
                            if (portEntry.getPacketsPerInternalPortSent() != null)
                                nbSwitchPortCounters.setPackets_sent_on_tunnel(portEntry.getPacketsPerInternalPortSent());
                            if (portEntry.getPacketsPerOFPortReceive() != null)
                                nbSwitchPortCounters.setPackets_received(portEntry.getPacketsPerOFPortReceive());
                            if (portEntry.getPacketsPerOFPortSent() != null)
                                nbSwitchPortCounters.setPackets_sent(portEntry.getPacketsPerOFPortSent());
                            if (portEntry.getPacketsPerOFPortReceiveDrop() != null)
                                nbSwitchPortCounters.setPackets_received_drop(portEntry.getPacketsPerOFPortReceiveDrop());
                            if (portEntry.getPacketsPerOFPortReceiveError() != null)
                                nbSwitchPortCounters.setPackets_received_error(portEntry.getPacketsPerOFPortReceiveError());
                            switchPortCountersList.add(nbSwitchPortCounters);
                        }
                    }

                    //SwitchCounters
                    NBSwitchCounters nbSwitchCounters = new NBSwitchCounters();
                    nbSwitchCounters.setFlow_datapath_id(switchEntry.getSwitchId());
                    if (switchEntry.getNoOfOFPorts() != null)
                        nbSwitchCounters.setPorts(switchEntry.getNoOfOFPorts());
                    if (switchEntry.getInjectedOFMessagesReceive() != null)
                        nbSwitchCounters.setPacket_in_messages_received(switchEntry.getInjectedOFMessagesReceive());
                    if (switchEntry.getInjectedOFMessagesSent() != null)
                        nbSwitchCounters.setPacket_out_messages_sent(switchEntry.getInjectedOFMessagesSent());
                    nbSwitchCounters.setSwitch_port_counters(switchPortCountersList);
                    nbSwitchCounters.setTable_counters(tableCountersList);
                    switchCountersList.add(nbSwitchCounters);
                }
            }
        } else {
            LOG.debug("No switch statistics found");
        }
        northBoundSwitchStatistics.setFlow_capable_switches(switchCountersList);
        return northBoundSwitchStatistics;
    }

    public NorthBoundControllersCounters getControllerNodeCounters() {

        List<Controllers> controllersList = null;
        northBoundControllersCounters = new NorthBoundControllersCounters();
        nodeList = new ArrayList<>();

        InstanceIdentifier<PerformanceCounters> countersId = InstanceIdentifier.create(PerformanceCounters.class);
        Optional<PerformanceCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                countersId);
        if (counterData != null && counterData.isPresent()) {
            LOG.debug("Controller statistics found");
            controllersList = counterData.get().getControllers();
            if (controllersList != null) {
                for (Controllers controllers : controllersList) {
                    NBNode node = new NBNode();
                    node.setController_host_name(controllers.getHostNodeName());
                    node.setConnected_flow_capable_switches(controllers.getNoOfOFSwitch());
                    nodeList.add(node);
                }
            }
        } else {
            LOG.debug("No controller statistics found");
        }
        northBoundControllersCounters.setController_switch_mappings(nodeList);
        return northBoundControllersCounters;
    }
}