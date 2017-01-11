/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework;

import org.opendaylight.genius.fcapsmanager.PMService;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.PerformanceCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info.Switch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info.SwitchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info.SwitchKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019._switch.info._switch.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.bgp.info.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pm.counter.config.rev161019.performance.counters.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CounterManagement implements PMService,AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CounterManagement.class);
    private static DataBroker dataBroker;

    public CounterManagement(final DataBroker broker) {
        dataBroker = Preconditions.checkNotNull(broker, "DataBroker can not be null!");
        LOG.debug("databroker is set in counter Management");
    }

    public void start() {
        LOG.debug("starting CounterManagement");
        CounterUtil.batchSize = CounterUtil.BATCH_SIZE;
        if (Integer.getInteger("batch.size") != null) {
            CounterUtil.batchSize = Integer.getInteger("batch.size");
        }
        CounterUtil.batchInterval = CounterUtil.PERIODICITY;
        if (Integer.getInteger("batch.wait.time") != null) {
            CounterUtil.batchInterval = Integer.getInteger("batch.wait.time");
        }
        CounterUtil.registerWithBatchManager(new CounterBatchHandler());
    }

    /*
     * Method is called by platform independent code which passes the countermap
     * from each of the counter mbeans registered by applications
     */
    @SuppressWarnings("unchecked")
    public void connectToPMFactory(Map<String,String> counterMap) {
        BigInteger switchId, portId, tableId, asId, counterValue;
        InstanceIdentifier<Switch> switchPath;
        InstanceIdentifier<SwitchPortsCounters> switchPortsPath;
        InstanceIdentifier<TableCounters> switchTablePath;
        InstanceIdentifier<BgpNeighborCounters> bgpNeighborPath;
        String tempCount[], neighborIp ,counterName,counterKey;
        try {
            for (Map.Entry<String, String> counterEntry : counterMap.entrySet()) {
                counterKey = counterEntry.getKey();
                counterName = counterKey.split(":")[0];
                counterValue = new BigInteger(counterEntry.getValue());
                LOG.debug("Retrieved counterName {}", counterName);

                switch (Enum.valueOf(Counters.class, counterName)) {
                    case NumberOfEFS:
                        String hostName = counterKey.split("_")[1];
                        LOG.debug("NumberOfEFS received hostname {}", hostName);
                        try {
                            InstanceIdentifier<Controllers> path = InstanceIdentifier.create(PerformanceCounters.class)
                                    .child(Controllers.class, new ControllersKey(hostName));
                            Controllers data = new ControllersBuilder().setKey(new ControllersKey(hostName))
                                    .setNoOfOFSwitch(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, path, data);
                            LOG.debug("Updated NumberOfEFS counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing NumberOfEFS counter {}", ex);
                        }
                        break;

                    case NumberOfOFPorts:
                        switchId = new BigInteger(counterKey.split("_")[1]);
                        LOG.debug("NumberOfOFPorts received dpnId {}", switchId);
                        try {
                            switchPath = InstanceIdentifier.create(PerformanceCounters.class)
                                    .child(SwitchCounters.class).child(Switch.class, new SwitchKey(switchId));
                            Switch data = new SwitchBuilder().setKey(new SwitchKey(switchId)).setSwitchId(switchId)
                                    .setNoOfOFPorts(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPath, data);
                            LOG.debug("Updated NumberOfOFPorts counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing NumberOfOFPorts counter {}", ex);
                        }
                        break;

                    case InjectedOFMessagesSent:
                        switchId = new BigInteger(counterKey.split("_")[1]);
                        LOG.debug("InjectedOFMessagesSent received dpnId {}", switchId);

                        switchPath = InstanceIdentifier.create(PerformanceCounters.class)
                                .child(SwitchCounters.class).child(Switch.class, new SwitchKey(switchId));
                        try {
                            Optional<Switch> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getInjectedOFMessagesSent();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for InjectedOFMessagesSent counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while reading InjectedOFMessagesSent counter {}", ex);
                        }
                        try {
                            Switch data = new SwitchBuilder().setKey(new SwitchKey(switchId)).setSwitchId(switchId).setInjectedOFMessagesSent(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPath, data);
                            LOG.debug("Updated InjectedOFMessagesSent counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing InjectedOFMessagesSent counter {}", ex);
                        }
                        break;

                    case InjectedOFMessagesReceive:
                        switchId = new BigInteger(counterKey.split("_")[1]);
                        LOG.debug("InjectedOFMessagesReceive received dpnId {}", switchId);

                        switchPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId));
                        try {
                            Optional<Switch> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getInjectedOFMessagesReceive();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for InjectedOFMessagesReceive counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading InjectedOFMessagesReceive counter {}", ex);
                        }
                        try {
                            Switch data = new SwitchBuilder().setKey(new SwitchKey(switchId)).setSwitchId(switchId).setInjectedOFMessagesReceive(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPath, data);
                            LOG.debug("Updated InjectedOFMessagesReceive counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing InjectedOFMessagesReceive counter {}", ex);
                        }
                        break;

                    case PacketsPerOFPortReceiveDrop:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("PacketsPerOFPortReceiveDrop received dpnId {} portId {}", switchId, portId);

                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getPacketsPerOFPortReceiveDrop();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for PacketsPerOFPortReceiveDrop counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading PacketsPerOFPortReceiveDrop counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setPacketsPerOFPortReceiveDrop(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated PacketsPerOFPortReceiveDrop counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing PacketsPerOFPortReceiveDrop counter {}", ex);
                        }
                        break;

                    case PacketsPerOFPortReceiveError:

                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("PacketsPerOFPortReceiveError received dpnId {} portId {}", switchId, portId);

                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getPacketsPerOFPortReceiveError();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for PacketsPerOFPortReceiveError counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading PacketsPerOFPortReceiveError counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setPacketsPerOFPortReceiveError(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated PacketsPerOFPortReceiveError counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing PacketsPerOFPortReceiveError counter {}", ex);
                        }
                        break;

                    case OFPortDuration:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("OFPortDuration received dpnId {} portId {}", switchId, portId);
                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getOFPortDuration();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for OFPortDuration counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading OFPortDuration counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setOFPortDuration(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated OFPortDuration counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing OFPortDuration counter {}", ex);
                        }
                        break;

                    case PacketsPerOFPortSent:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("PacketsPerOFPortSent received dpnId {} portId {}", switchId, portId);
                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getPacketsPerOFPortSent();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for PacketsPerOFPortSent counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading PacketsPerOFPortSent counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setPacketsPerOFPortSent(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated PacketsPerOFPortSent counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing PacketsPerOFPortSent counter {}", ex);
                        }
                        break;

                    case PacketsPerOFPortReceive:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("PacketsPerOFPortReceive received dpnId {} portId {}", switchId, portId);
                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getPacketsPerOFPortReceive();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for PacketsPerOFPortReceive counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading PacketsPerOFPortReceive counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setPacketsPerOFPortReceive(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated PacketsPerOFPortReceive counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing PacketsPerOFPortReceive counter {}", ex);
                        }
                        break;

                    case BytesPerOFPortSent:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("BytesPerOFPortSent received dpnId {} portId {}", switchId, portId);
                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getBytesPerOFPortSent();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for BytesPerOFPortSent counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading BytesPerOFPortSent counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setBytesPerOFPortSent(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated BytesPerOFPortSent counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing BytesPerOFPortSent counter {}", ex);
                        }
                        break;

                    case BytesPerOFPortReceive:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("BytesPerOFPortReceive received dpnId {} portId {}", switchId, portId);
                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getBytesPerOFPortReceive();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for BytesPerOFPortReceive counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading BytesPerOFPortReceive counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setBytesPerOFPortReceive(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated BytesPerOFPortReceive counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing BytesPerOFPortReceive counter {}", ex);
                        }
                        break;

                    case PacketsPerInternalPortSent:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("PacketsPerInternalPortSent received dpnId {} portId {}", switchId, portId);
                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getPacketsPerInternalPortSent();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for PacketsPerInternalPortSent counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading PacketsPerInternalPortSent counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setPacketsPerInternalPortSent(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated PacketsPerInternalPortSent counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing PacketsPerInternalPortSent counter {}", ex);
                        }
                        break;

                    case PacketsPerInternalPortReceive:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        portId = new BigInteger(tempCount[3]);
                        LOG.debug("PacketsPerInternalPortReceive received dpnId {} portId {}", switchId, portId);
                        switchPortsPath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(SwitchPortsCounters.class, new SwitchPortsCountersKey(portId));
                        try {
                            Optional<SwitchPortsCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getPacketsPerInternalPortReceive();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for PacketsPerInternalPortReceive counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading PacketsPerInternalPortReceive counter {}", ex);
                        }
                        try {
                            SwitchPortsCounters portData = new SwitchPortsCountersBuilder().setKey(new SwitchPortsCountersKey(portId))
                                    .setPacketsPerInternalPortReceive(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchPortsPath, portData);

                            LOG.debug("Updated PacketsPerInternalPortReceive counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing PacketsPerInternalPortReceive counter {}", ex);
                        }
                        break;

                    case EntriesPerOFTable:
                        tempCount = counterKey.split("_");
                        switchId = new BigInteger(tempCount[1]);
                        tableId = new BigInteger(tempCount[3]);
                        LOG.debug("EntriesPerOFTable received dpnId {} tableId {}", switchId, tableId);
                        switchTablePath = InstanceIdentifier.create(PerformanceCounters.class).child(SwitchCounters.class)
                                .child(Switch.class, new SwitchKey(switchId)).child(TableCounters.class, new TableCountersKey(tableId));
                        try {
                            Optional<TableCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION, switchTablePath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getEntriesPerOFTable();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for EntriesPerOFTable counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("exception occurred while reading EntriesPerOFTable counter {}", ex);
                        }
                        try {
                            TableCounters tableData = new TableCountersBuilder().setKey(new TableCountersKey(tableId))
                                    .setEntriesPerOFTable(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, switchTablePath, tableData);

                            LOG.debug("Updated EntriesPerOFTable counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing EntriesPerOFTable counter {}", ex);
                        }
                        break;

                    case BgpNeighborPacketsReceived:
                        asId = new BigInteger(counterKey.split("_")[5]);
                        neighborIp = counterKey.split("_")[3];
                        LOG.debug("BgpNeighborPacketsReceived received asId {} neighborIp {}", asId, neighborIp);

                        bgpNeighborPath = InstanceIdentifier.create(PerformanceCounters.class)
                                .child(BgpCounters.class).child(BgpNeighborCounters.class, new BgpNeighborCountersKey(asId));

                        try {
                            Optional<BgpNeighborCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                                    bgpNeighborPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getBgpNeighborPacketsReceived();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for BgpNeighborPacketsReceived counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while reading BgpNeighborPacketsReceived counter {}", ex);
                        }
                        try {
                            BgpNeighborCounters bgpNeighborData = new BgpNeighborCountersBuilder().setKey(new BgpNeighborCountersKey(asId))
                                    .setAsId(asId).setNeighborIp(neighborIp).setBgpNeighborPacketsReceived(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, bgpNeighborPath, bgpNeighborData);
                            LOG.debug("Updated BgpNeighborPacketsReceived counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing BgpNeighborPacketsReceived counter {}", ex);
                        }
                        break;

                    case BgpNeighborPacketsSent:
                        asId = new BigInteger(counterKey.split("_")[5]);
                        neighborIp = counterKey.split("_")[3];
                        LOG.debug("BgpNeighborPacketsSent received asId {} neighborIp {}", asId, neighborIp);

                        bgpNeighborPath = InstanceIdentifier.create(PerformanceCounters.class)
                                .child(BgpCounters.class).child(BgpNeighborCounters.class, new BgpNeighborCountersKey(asId));

                        try {
                            Optional<BgpNeighborCounters> counterData = CounterUtil.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                                    bgpNeighborPath);
                            BigInteger readValue;
                            if (counterData != null && counterData.isPresent()) {
                                readValue = counterData.get().getBgpNeighborPacketsSent();
                                LOG.debug("Retrieved counter value from DS {}", readValue);
                                if (readValue != null && !readValue.equals(BigInteger.ZERO)) {
                                    counterValue = doComputation(counterValue,readValue);
                                }
                            } else {
                                LOG.debug("No counter value present for BgpNeighborPacketsSent counter");
                            }
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while reading BgpNeighborPacketsSent counter {}", ex);
                        }
                        try {
                            BgpNeighborCounters bgpNeighborData = new BgpNeighborCountersBuilder().setKey(new BgpNeighborCountersKey(asId))
                                    .setAsId(asId).setNeighborIp(neighborIp).setBgpNeighborPacketsSent(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, bgpNeighborPath, bgpNeighborData);
                            LOG.debug("Updated BgpNeighborPacketsSent counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing BgpNeighborPacketsSent counter {}", ex);
                        }
                        break;

                    case BgpTotalPrefixes:
                        InstanceIdentifier<BgpCounters> bgpCountersId = InstanceIdentifier.create(PerformanceCounters.class).child(BgpCounters.class);
                        try {
                            BgpCounters bgpPrefixData = new BgpCountersBuilder().setBgpTotalPrefixes(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, bgpCountersId, bgpPrefixData);
                            LOG.debug("Updated BgpTotalPrefixes counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing BgpTotalPrefixes counter {}", ex);
                        }
                        break;

                    case BgpRdRouteCount:
                        BigInteger rdValue = new BigInteger(counterKey.split("_")[2]);
                        LOG.debug("BgpRdRouteCount received rd value {}", rdValue);

                        InstanceIdentifier<BgpRdRouteCounters> bgpRdId = InstanceIdentifier.create(PerformanceCounters.class)
                                .child(BgpCounters.class).child(BgpRdRouteCounters.class, new BgpRdRouteCountersKey(rdValue));
                        try {
                            BgpRdRouteCounters bgpRdRouteData = new BgpRdRouteCountersBuilder().setKey(new BgpRdRouteCountersKey(rdValue))
                                    .setRd(rdValue).setBgpRdRouteCount(counterValue).build();
                            CounterUtil.batchUpdate(dataBroker, LogicalDatastoreType.CONFIGURATION, bgpRdId, bgpRdRouteData);
                            LOG.debug("Updated BgpRdRouteCount counter with value {}", counterValue);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred while writing BgpRdRouteCount counter {}", ex);
                        }
                        break;

                    default:
                        LOG.debug("Counter not found in the list");
                        break;
                }
            }
        } catch (Exception e) {
            LOG.debug("failed to update pm counters error: {}", e.getMessage());
        }
    }


    private BigInteger doComputation(BigInteger currentCounter,BigInteger memoryCounter) {
        BigInteger newCounter = BigInteger.ZERO;

        if(currentCounter.compareTo(memoryCounter) == 1) {
            LOG.debug("Current received counter value {} is greater than counter {} stored in db",
                    currentCounter,memoryCounter);

            BigInteger diff = currentCounter.subtract(memoryCounter);
            newCounter = newCounter.add(diff);
            newCounter = newCounter.add(memoryCounter);

            LOG.debug("Computed counter value {}",newCounter);
        } else if(currentCounter.compareTo(memoryCounter) == -1) {
            LOG.debug("Current received counter value {} is less than than counter {} stored in db",
                    currentCounter,memoryCounter);

            newCounter = newCounter.add(memoryCounter);
            newCounter = newCounter.add(currentCounter);

            LOG.debug("Computed counter value {}",newCounter);
        } else if(currentCounter.compareTo(memoryCounter) == 0) {
            LOG.debug("Both counter values are equal ignore computation, value : {}",currentCounter);
            return currentCounter;
        }

        return newCounter;
    }

    @Override
    public void close() throws Exception {

    }
}