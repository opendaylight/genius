/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.utils.hwvtep;

import static org.opendaylight.controller.md.sal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedReadTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Utility class to related to Hardware VTEP devices.
 */
public final class HwvtepUtils {
    private HwvtepUtils() {

    }

    // TODO: (eperefr) Move this to HwvtepSouthboundUtils when in place.
    public static InstanceIdentifier<LocalUcastMacs> getWildCardPathForLocalUcastMacs() {
        return InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                .augmentation(HwvtepGlobalAugmentation.class).child(LocalUcastMacs.class);
    }

    /**
     * Adds the logical switch into config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param logicalSwitch
     *            the logical switch
     * @return the listenable future
     * @deprecated Use {@link #addLogicalSwitch(TypedWriteTransaction, NodeId, LogicalSwitches)}.
     */
    @Deprecated
    public static ListenableFuture<Void> addLogicalSwitch(DataBroker broker, NodeId nodeId,
                                                          LogicalSwitches logicalSwitch) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        putLogicalSwitch(transaction,LogicalDatastoreType.CONFIGURATION, nodeId, logicalSwitch);
        return transaction.commit();
    }

    @Deprecated
    public static ListenableFuture<Void> addLogicalSwitch(DataBroker broker, LogicalDatastoreType logicalDatastoreType,
                                                          NodeId nodeId,
                                                          LogicalSwitches logicalSwitch) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        putLogicalSwitch(transaction,logicalDatastoreType, nodeId, logicalSwitch);
        return transaction.commit();
    }

    /**
     * Adds the logical switch.
     *
     * @param tx The configuration transaction.
     * @param nodeId The node identifier.
     * @param logicalSwitch The logical switch.
     */
    public static void addLogicalSwitch(TypedWriteTransaction<Configuration> tx, NodeId nodeId,
        LogicalSwitches logicalSwitch) {
        InstanceIdentifier<LogicalSwitches> iid = HwvtepSouthboundUtils.createLogicalSwitchesInstanceIdentifier(nodeId,
            logicalSwitch.getHwvtepNodeName());
        tx.put(iid, logicalSwitch, CREATE_MISSING_PARENTS);
    }

    /**
     * Put the logical switches in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstSwitches
     *            the lst switches
     */
    public static void putLogicalSwitches(final WriteTransaction transaction, final NodeId nodeId,
                                          final List<LogicalSwitches> lstSwitches) {
        if (lstSwitches != null) {
            for (LogicalSwitches logicalSwitch : lstSwitches) {
                putLogicalSwitch(transaction,LogicalDatastoreType.CONFIGURATION, nodeId, logicalSwitch);
            }
        }
    }

    /**
     * Put logical switch in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param logicalSwitch
     *            the logical switch
     */
    public static void putLogicalSwitch(final WriteTransaction transaction,LogicalDatastoreType logicalDatastoreType,
                                        final NodeId nodeId, final LogicalSwitches logicalSwitch) {
        InstanceIdentifier<LogicalSwitches> iid = HwvtepSouthboundUtils.createLogicalSwitchesInstanceIdentifier(nodeId,
                logicalSwitch.getHwvtepNodeName());
        transaction.put(logicalDatastoreType, iid, logicalSwitch, true);
    }

    /**
     * Delete logical switch from config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param logicalSwitchName
     *            the logical switch name
     * @return the listenable future
     * @deprecated Use {@link #deleteLogicalSwitch(TypedWriteTransaction, NodeId, String)}.
     */
    @Deprecated
    public static ListenableFuture<Void> deleteLogicalSwitch(DataBroker broker, NodeId nodeId,
                                                             String logicalSwitchName) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        deleteLogicalSwitch(transaction, nodeId, logicalSwitchName);
        return transaction.commit();
    }

    /**
     * Delete logical switch from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param logicalSwitchName
     *            the logical switch name
     * @deprecated Use {@link #deleteLogicalSwitch(TypedWriteTransaction, NodeId, String)}.
     */
    @Deprecated
    public static void deleteLogicalSwitch(final WriteTransaction transaction, final NodeId nodeId,
                                           final String logicalSwitchName) {
        transaction.delete(LogicalDatastoreType.CONFIGURATION, HwvtepSouthboundUtils
                .createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(logicalSwitchName)));
    }

    /**
     * Deletes the given logical switch.
     *
     * @param tx The transaction.
     * @param nodeId The node identifier.
     * @param logicalSwitchName The logical switch name.
     */
    public static void deleteLogicalSwitch(TypedWriteTransaction<Configuration> tx, NodeId nodeId,
        String logicalSwitchName) {
        tx.delete(HwvtepSouthboundUtils
            .createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(logicalSwitchName)));
    }

    /**
     * Gets the logical switch.
     *
     * @param nodeId
     *            the node id
     * @param logicalSwitchName
     *            the logical switch name
     * @return the logical switch
     * @deprecated Use {@link #getLogicalSwitch(TypedReadTransaction, NodeId, String)}.
     */
    @Deprecated
    public static LogicalSwitches getLogicalSwitch(DataBroker broker, LogicalDatastoreType datastoreType, NodeId nodeId,
                                                   String logicalSwitchName) {
        final InstanceIdentifier<LogicalSwitches> iid = HwvtepSouthboundUtils
                .createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(logicalSwitchName));
        return MDSALUtil.read(broker, datastoreType, iid).orNull();
    }

    /**
     * Retrieves the logical switch.
     *
     * @param tx The transaction to use.
     * @param nodeId The node identifier.
     * @param logicalSwitchName The logical switch name.
     * @return The logical switch, if any.
     */
    @Nullable
    public static LogicalSwitches getLogicalSwitch(TypedReadTransaction<Configuration> tx, NodeId nodeId,
        String logicalSwitchName) {
        final InstanceIdentifier<LogicalSwitches> iid = HwvtepSouthboundUtils
            .createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(logicalSwitchName));
        try {
            return tx.read(iid).get().orNull();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error reading logical switch " + iid, e);
        }
    }

    /**
     * Gets physical port termination point.
     *
     * @param broker
     *          the broker
     * @param datastoreType
     *          the datastore type
     * @param nodeId
     *          the physical switch node id
     * @param portName
     *          port name under physical switch node id
     * @return the physical port termination point
     */
    public static TerminationPoint getPhysicalPortTerminationPoint(DataBroker broker,
            LogicalDatastoreType datastoreType, NodeId nodeId, String portName) {
        TerminationPointKey tpKey = new TerminationPointKey(new TpId(portName));
        InstanceIdentifier<TerminationPoint> iid = HwvtepSouthboundUtils.createTerminationPointId(nodeId, tpKey);
        return MDSALUtil.read(broker, datastoreType, iid).orNull();
    }

    /**
     * Get LogicalSwitches for a given hwVtepNodeId.
     *
     * @param broker
     *            the broker
     * @param hwVtepNodeId
     *            Hardware VTEP Node Id
     * @param vni
     *            virtual network id
     * @return the logical switches
     */
    public static LogicalSwitches getLogicalSwitches(DataBroker broker, String hwVtepNodeId, String vni) {
        NodeId nodeId = new NodeId(hwVtepNodeId);
        InstanceIdentifier<LogicalSwitches> logicalSwitchesIdentifier = HwvtepSouthboundUtils
                .createLogicalSwitchesInstanceIdentifier(nodeId, new HwvtepNodeName(vni));

        return MDSALUtil.read(broker, LogicalDatastoreType.CONFIGURATION, logicalSwitchesIdentifier).orNull();
    }

    /**
     * Put physical locators in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstPhysicalLocator
     *            the lst physical locator
     */
    public static void putPhysicalLocators(WriteTransaction transaction, NodeId nodeId,
                                           List<HwvtepPhysicalLocatorAugmentation> lstPhysicalLocator) {
        if (lstPhysicalLocator != null) {
            for (HwvtepPhysicalLocatorAugmentation phyLocator : lstPhysicalLocator) {
                putPhysicalLocator(transaction, nodeId, phyLocator);
            }
        }
    }

    /**
     * Put physical locator in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param phyLocator
     *            the phy locator
     */
    public static void putPhysicalLocator(final WriteTransaction transaction, final NodeId nodeId,
                                          final HwvtepPhysicalLocatorAugmentation phyLocator) {
        InstanceIdentifier<TerminationPoint> iid = HwvtepSouthboundUtils.createPhysicalLocatorInstanceIdentifier(nodeId,
                phyLocator);
        TerminationPoint terminationPoint = new TerminationPointBuilder()
                .withKey(HwvtepSouthboundUtils.getTerminationPointKey(phyLocator))
                .addAugmentation(HwvtepPhysicalLocatorAugmentation.class, phyLocator).build();

        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, terminationPoint, true);
    }

    /**
     * Gets the physical locator.
     *
     * @param broker
     *            the broker
     * @param datastoreType
     *            the datastore type
     * @param nodeId
     *            the node id
     * @param phyLocatorIp
     *            the phy locator ip
     * @return the physical locator
     */
    public static HwvtepPhysicalLocatorAugmentation getPhysicalLocator(DataBroker broker,
            LogicalDatastoreType datastoreType, NodeId nodeId, final IpAddress phyLocatorIp) {
        HwvtepPhysicalLocatorAugmentation phyLocatorAug = HwvtepSouthboundUtils
                .createHwvtepPhysicalLocatorAugmentation(phyLocatorIp);
        InstanceIdentifier<HwvtepPhysicalLocatorAugmentation> iid = HwvtepSouthboundUtils
                .createPhysicalLocatorInstanceIdentifier(nodeId, phyLocatorAug)
                .augmentation(HwvtepPhysicalLocatorAugmentation.class);
        return MDSALUtil.read(broker, datastoreType, iid).orNull();
    }

    /**
     * Adds the remote ucast macs into config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param lstRemoteUcastMacs
     *            the lst remote ucast macs
     * @return the listenable future
     * @deprecated Use {@link #addRemoteUcastMacs(TypedWriteTransaction, NodeId, Iterable)}.
     */
    @Deprecated
    public static ListenableFuture<Void> addRemoteUcastMacs(DataBroker broker, NodeId nodeId,
                                                            List<RemoteUcastMacs> lstRemoteUcastMacs) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        putRemoteUcastMacs(transaction, nodeId, lstRemoteUcastMacs);
        return transaction.commit();
    }

    /**
     * Adds the given remote unicast MACs.
     *
     * @param tx The transaction to use.
     * @param nodeId The node identifier.
     * @param remoteUcastMacs The MACs to add.
     */
    public static void addRemoteUcastMacs(TypedWriteTransaction<Configuration> tx, NodeId nodeId,
        Iterable<RemoteUcastMacs> remoteUcastMacs) {
        if (remoteUcastMacs != null) {
            remoteUcastMacs.forEach(remoteUcastMac -> addRemoteUcastMac(tx, nodeId, remoteUcastMac));
        }
    }

    /**
     * Adds the given remote unicast MAC.
     *
     * @param tx The transaction to use.
     * @param nodeId The node identifier.
     * @param remoteUcastMac The MAC to add.
     */
    public static void addRemoteUcastMac(TypedWriteTransaction<Configuration> tx, NodeId nodeId,
        RemoteUcastMacs remoteUcastMac) {
        InstanceIdentifier<RemoteUcastMacs> iid = HwvtepSouthboundUtils.createInstanceIdentifier(nodeId)
            .augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class,
                new RemoteUcastMacsKey(remoteUcastMac.getLogicalSwitchRef(), remoteUcastMac.getMacEntryKey()));
        tx.put(iid, remoteUcastMac, CREATE_MISSING_PARENTS);
    }

    /**
     * Put remote ucast macs in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstRemoteUcastMacs
     *            the lst remote ucast macs
     * @deprecated Use {@link #addRemoteUcastMacs(TypedWriteTransaction, NodeId, Iterable)}.
     */
    @Deprecated
    public static void putRemoteUcastMacs(final WriteTransaction transaction, final NodeId nodeId,
                                          final List<RemoteUcastMacs> lstRemoteUcastMacs) {
        if (lstRemoteUcastMacs != null && !lstRemoteUcastMacs.isEmpty()) {
            for (RemoteUcastMacs remoteUcastMac : lstRemoteUcastMacs) {
                putRemoteUcastMac(transaction, nodeId, remoteUcastMac);
            }
        }
    }

    /**
     * Put remote ucast mac in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param remoteUcastMac
     *            the remote ucast mac
     * @deprecated Use {@link #addRemoteUcastMac(TypedWriteTransaction, NodeId, RemoteUcastMacs)}.
     */
    @Deprecated
    public static void putRemoteUcastMac(final WriteTransaction transaction, final NodeId nodeId,
                                         RemoteUcastMacs remoteUcastMac) {
        InstanceIdentifier<RemoteUcastMacs> iid = HwvtepSouthboundUtils.createInstanceIdentifier(nodeId)
                .augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class,
                        new RemoteUcastMacsKey(remoteUcastMac.getLogicalSwitchRef(), remoteUcastMac.getMacEntryKey()));
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, remoteUcastMac, true);
    }

    /**
     * Delete remote ucast mac from the config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param mac
     *            the mac
     * @return the listenable future
     * @deprecated Use {@link #deleteRemoteUcastMac(TypedWriteTransaction, NodeId, String, MacAddress)}.
     */
    @Deprecated
    public static ListenableFuture<Void> deleteRemoteUcastMac(DataBroker broker, NodeId nodeId,
                                                              String logicalSwitchName, MacAddress mac) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        deleteRemoteUcastMac(transaction, nodeId, logicalSwitchName, mac);
        return transaction.commit();
    }

    /**
     * Delete remote ucast mac from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param mac
     *            the mac
     * @deprecated Use {@link #deleteRemoteUcastMac(TypedWriteTransaction, NodeId, String, MacAddress)}.
     */
    @Deprecated
    public static void deleteRemoteUcastMac(final WriteTransaction transaction, final NodeId nodeId,
                                            String logialSwitchName, final MacAddress mac) {
        transaction.delete(LogicalDatastoreType.CONFIGURATION,
                HwvtepSouthboundUtils.createRemoteUcastMacsInstanceIdentifier(nodeId, logialSwitchName, mac));
    }

    /**
     * Deletes the given remote unicast MAC.
     *
     * @param tx The transaction to use.
     * @param nodeId The node identifier.
     * @param logicalSwitchName The logical switch name.
     * @param macAddress The MAC.
     */
    public static void deleteRemoteUcastMac(TypedWriteTransaction<Configuration> tx, NodeId nodeId,
        String logicalSwitchName, MacAddress macAddress) {
        tx.delete(HwvtepSouthboundUtils.createRemoteUcastMacsInstanceIdentifier(nodeId, logicalSwitchName, macAddress));
    }

    /**
     * Delete remote ucast macs from the config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param lstMac
     *            the lst mac
     * @return the listenable future
     * @deprecated Use {@link #deleteRemoteUcastMacs(TypedWriteTransaction, NodeId, String, Iterable)}.
     */
    @Deprecated
    public static ListenableFuture<Void> deleteRemoteUcastMacs(DataBroker broker, NodeId nodeId,
                                                               String logicalSwitchName, List<MacAddress> lstMac) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        deleteRemoteUcastMacs(transaction, nodeId, logicalSwitchName, lstMac);
        return transaction.commit();
    }

    /**
     * Delete remote ucast macs from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstMac
     *            the lst mac
     * @deprecated Use {@link #deleteRemoteUcastMacs(TypedWriteTransaction, NodeId, String, Iterable)}.
     */
    @Deprecated
    public static void deleteRemoteUcastMacs(final WriteTransaction transaction, final NodeId nodeId,
                                             String logicalSwitchName, final List<MacAddress> lstMac) {
        if (lstMac != null && !lstMac.isEmpty()) {
            for (MacAddress mac : lstMac) {
                deleteRemoteUcastMac(transaction, nodeId, logicalSwitchName, mac);
            }
        }
    }

    /**
     * Deletes the given remote unicast MACs.
     *
     * @param tx The transaction to use.
     * @param nodeId The node identifier.
     * @param logicalSwitchName The logical switch name.
     * @param macAddresses The MAC addresses.
     */
    public static void deleteRemoteUcastMacs(TypedWriteTransaction<Configuration> tx, NodeId nodeId,
        String logicalSwitchName, Iterable<MacAddress> macAddresses) {
        if (macAddresses != null) {
            macAddresses.forEach(macAddress -> deleteRemoteUcastMac(tx, nodeId, logicalSwitchName, macAddress));
        }
    }

    /**
     * Adds the remote mcast macs into config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param lstRemoteMcastMacs
     *            the lst remote mcast macs
     * @return the listenable future
     */
    public static ListenableFuture<Void> addRemoteMcastMacs(DataBroker broker, NodeId nodeId,
                                                            List<RemoteMcastMacs> lstRemoteMcastMacs) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        putRemoteMcastMacs(transaction, nodeId, lstRemoteMcastMacs);
        return transaction.commit();
    }

    /**
     * Put remote mcast macs in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstRemoteMcastMacs
     *            the lst remote mcast macs
     */
    public static void putRemoteMcastMacs(final WriteTransaction transaction, final NodeId nodeId,
                                          final List<RemoteMcastMacs> lstRemoteMcastMacs) {
        if (lstRemoteMcastMacs != null && !lstRemoteMcastMacs.isEmpty()) {
            for (RemoteMcastMacs remoteMcastMac : lstRemoteMcastMacs) {
                putRemoteMcastMac(transaction, nodeId, remoteMcastMac);
            }
        }
    }

    /**
     * Put remote mcast mac in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param remoteMcastMac
     *            the remote mcast mac
     * @deprecated Use {@link #addRemoteMcastMac(TypedWriteTransaction, NodeId, RemoteMcastMacs)}.
     */
    @Deprecated
    public static void putRemoteMcastMac(final WriteTransaction transaction, final NodeId nodeId,
                                         RemoteMcastMacs remoteMcastMac) {
        InstanceIdentifier<RemoteMcastMacs> iid = HwvtepSouthboundUtils.createRemoteMcastMacsInstanceIdentifier(nodeId,
                remoteMcastMac.key());
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, remoteMcastMac, true);
    }

    /**
     * Adds a remote multicast MAC.
     *
     * @deprecated Use {@link #addRemoteMcastMac(TypedWriteTransaction, NodeId, RemoteMcastMacs)}.
     */
    @Deprecated
    public static void putRemoteMcastMac(final WriteTransaction transaction,LogicalDatastoreType logicalDatastoreType,
                                         final NodeId nodeId,
                                         RemoteMcastMacs remoteMcastMac) {
        InstanceIdentifier<RemoteMcastMacs> iid = HwvtepSouthboundUtils.createRemoteMcastMacsInstanceIdentifier(nodeId,
                remoteMcastMac.key());
        transaction.put(logicalDatastoreType, iid, remoteMcastMac, true);
    }

    /**
     * Store a remote multicast MAC.
     *
     * @param tx The transaction.
     * @param nodeId The node identifier.
     * @param remoteMcastMac The remote multicast MAC.
     */
    public static void addRemoteMcastMac(final TypedWriteTransaction<? extends Datastore> tx, final NodeId nodeId,
        RemoteMcastMacs remoteMcastMac) {
        InstanceIdentifier<RemoteMcastMacs> iid = HwvtepSouthboundUtils.createRemoteMcastMacsInstanceIdentifier(nodeId,
            remoteMcastMac.key());
        tx.put(iid, remoteMcastMac, CREATE_MISSING_PARENTS);
    }

    /**
     * Gets the remote mcast mac.
     *
     * @param broker
     *            the broker
     * @param datastoreType
     *            the datastore type
     * @param nodeId
     *            the node id
     * @param remoteMcastMacsKey
     *            the remote mcast macs key
     * @return the remote mcast mac
     * @deprecated Use {@link #getRemoteMcastMac(TypedReadTransaction, NodeId, RemoteMcastMacsKey)}.
     */
    @Deprecated
    public static RemoteMcastMacs getRemoteMcastMac(DataBroker broker, LogicalDatastoreType datastoreType,
                                                    NodeId nodeId, RemoteMcastMacsKey remoteMcastMacsKey) {
        final InstanceIdentifier<RemoteMcastMacs> iid = HwvtepSouthboundUtils
                .createRemoteMcastMacsInstanceIdentifier(nodeId, remoteMcastMacsKey);
        return MDSALUtil.read(broker, datastoreType, iid).orNull();
    }

    /**
     * Retrieve a remote multicast MAC.
     *
     * @param tx The transction to use.
     * @param nodeId The node identifier.
     * @param remoteMcastMacsKey The MAC key.
     * @return The MAC, if any ({@code null} if there is none).
     */
    @Nullable
    public static RemoteMcastMacs getRemoteMcastMac(TypedReadTransaction<? extends Datastore> tx, NodeId nodeId,
        RemoteMcastMacsKey remoteMcastMacsKey) {
        final InstanceIdentifier<RemoteMcastMacs> iid = HwvtepSouthboundUtils
            .createRemoteMcastMacsInstanceIdentifier(nodeId, remoteMcastMacsKey);
        try {
            return tx.read(iid).get().orNull();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error reading remote multicast MAC " + iid, e);
        }
    }

    /**
     * Delete remote mcast mac from config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param remoteMcastMacsKey
     *            the remote mcast macs key
     * @return the listenable future
     * @deprecated Use {@link #deleteRemoteMcastMac(TypedWriteTransaction, NodeId, RemoteMcastMacsKey)}.
     */
    @Deprecated
    public static ListenableFuture<Void> deleteRemoteMcastMac(DataBroker broker, NodeId nodeId,
                                                              RemoteMcastMacsKey remoteMcastMacsKey) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        deleteRemoteMcastMac(transaction, nodeId, remoteMcastMacsKey);
        return transaction.commit();
    }

    /**
     * Delete remote mcast mac from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param remoteMcastMacsKey
     *            the remote mcast macs key
     * @deprecated Use {@link #deleteRemoteMcastMac(TypedWriteTransaction, NodeId, RemoteMcastMacsKey)}.
     */
    @Deprecated
    public static void deleteRemoteMcastMac(final WriteTransaction transaction, final NodeId nodeId,
                                            final RemoteMcastMacsKey remoteMcastMacsKey) {
        transaction.delete(LogicalDatastoreType.CONFIGURATION,
                HwvtepSouthboundUtils.createRemoteMcastMacsInstanceIdentifier(nodeId, remoteMcastMacsKey));
    }

    /**
     * Deletes the given remote multicast MAC.
     *
     * @param tx The configuration transaction.
     * @param nodeId The node identifier.
     * @param remoteMcastMacsKey The remote multicast MAC key.
     */
    public static void deleteRemoteMcastMac(TypedWriteTransaction<Configuration> tx, final NodeId nodeId,
        final RemoteMcastMacsKey remoteMcastMacsKey) {
        tx.delete(HwvtepSouthboundUtils.createRemoteMcastMacsInstanceIdentifier(nodeId, remoteMcastMacsKey));
    }

    /**
     * Delete remote mcast macs from config DS.
     *
     * @param broker
     *            the broker
     * @param nodeId
     *            the node id
     * @param lstRemoteMcastMacsKey
     *            the lst remote mcast macs key
     * @return the listenable future
     */
    public static ListenableFuture<Void> deleteRemoteMcastMacs(DataBroker broker, NodeId nodeId,
                                                               List<RemoteMcastMacsKey> lstRemoteMcastMacsKey) {
        WriteTransaction transaction = broker.newWriteOnlyTransaction();
        deleteRemoteMcastMacs(transaction, nodeId, lstRemoteMcastMacsKey);
        return transaction.commit();
    }

    /**
     * Delete remote mcast macs from the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param lstRemoteMcastMacsKey
     *            the lst remote mcast macs key
     */
    public static void deleteRemoteMcastMacs(final WriteTransaction transaction, final NodeId nodeId,
                                             final List<RemoteMcastMacsKey> lstRemoteMcastMacsKey) {
        if (lstRemoteMcastMacsKey != null && !lstRemoteMcastMacsKey.isEmpty()) {
            for (RemoteMcastMacsKey mac : lstRemoteMcastMacsKey) {
                deleteRemoteMcastMac(transaction, nodeId, mac);
            }
        }
    }

    /**
     * Merge vlan bindings in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param nodeId
     *            the node id
     * @param phySwitchName
     *            the phy switch name
     * @param phyPortName
     *            the phy port name
     * @param vlanBindings
     *            the vlan bindings
     * @deprecated Use {@link #mergeVlanBindings(TypedWriteTransaction, NodeId, String, String, List)}.
     */
    @Deprecated
    public static void mergeVlanBindings(final WriteTransaction transaction, final NodeId nodeId,
            final String phySwitchName, final String phyPortName, final List<VlanBindings> vlanBindings) {
        NodeId physicalSwitchNodeId = HwvtepSouthboundUtils.createManagedNodeId(nodeId, phySwitchName);
        mergeVlanBindings(transaction, physicalSwitchNodeId, phyPortName, vlanBindings);
    }

    /**
     * Merges the given VLAN bindings.
     *
     * @param tx The transaction to use.
     * @param nodeId The node identifier.
     * @param phySwitchName The physical switch name.
     * @param phyPortName The physical port name.
     * @param vlanBindings The VLAN bindings.
     */
    public static void mergeVlanBindings(TypedWriteTransaction<Configuration> tx, NodeId nodeId,
        String phySwitchName, String phyPortName, List<VlanBindings> vlanBindings) {
        NodeId physicalSwitchNodeId = HwvtepSouthboundUtils.createManagedNodeId(nodeId, phySwitchName);
        mergeVlanBindings(tx, physicalSwitchNodeId, phyPortName, vlanBindings);
    }

    /**
     * Merge vlan bindings in the transaction.
     *
     * @param transaction
     *            the transaction
     * @param physicalSwitchNodeId
     *            the physical switch node id
     * @param phyPortName
     *            the phy port name
     * @param vlanBindings
     *            the vlan bindings
     * @deprecated Use {@link #mergeVlanBindings(TypedWriteTransaction, NodeId, String, List)}.
     */
    @Deprecated
    public static void mergeVlanBindings(final WriteTransaction transaction, final NodeId physicalSwitchNodeId,
                                         final String phyPortName, final List<VlanBindings> vlanBindings) {
        HwvtepPhysicalPortAugmentation phyPortAug = new HwvtepPhysicalPortAugmentationBuilder()
                .setHwvtepNodeName(new HwvtepNodeName(phyPortName)).setVlanBindings(vlanBindings).build();

        final InstanceIdentifier<HwvtepPhysicalPortAugmentation> iid = HwvtepSouthboundUtils
                .createPhysicalPortInstanceIdentifier(physicalSwitchNodeId, phyPortName);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, iid, phyPortAug, true);
    }

    /**
     * Merges the given VLAN bindings.
     *
     * @param tx The transaction to use.
     * @param physicalSwitchNodeId The physical switch’s node identifier.
     * @param phyPortName The physical port name.
     * @param vlanBindings The VLAN bindings.
     */
    public static void mergeVlanBindings(TypedWriteTransaction<Configuration> tx, NodeId physicalSwitchNodeId,
        String phyPortName, List<VlanBindings> vlanBindings) {
        HwvtepPhysicalPortAugmentation phyPortAug = new HwvtepPhysicalPortAugmentationBuilder()
            .setHwvtepNodeName(new HwvtepNodeName(phyPortName)).setVlanBindings(vlanBindings).build();

        final InstanceIdentifier<HwvtepPhysicalPortAugmentation> iid = HwvtepSouthboundUtils
            .createPhysicalPortInstanceIdentifier(physicalSwitchNodeId, phyPortName);
        tx.merge(iid, phyPortAug, CREATE_MISSING_PARENTS);
    }

    /**
     * Delete vlan binding from transaction.
     *
     * @param transaction
     *            the transaction
     * @param physicalSwitchNodeId
     *            the physical switch node id
     * @param phyPortName
     *            the phy port name
     * @param vlanId
     *            the vlan id
     * @deprecated Use {@link #deleteVlanBinding(TypedWriteTransaction, NodeId, String, Integer)}.
     */
    @Deprecated
    public static void deleteVlanBinding(WriteTransaction transaction, NodeId physicalSwitchNodeId, String phyPortName,
                                         Integer vlanId) {
        InstanceIdentifier<VlanBindings> iid = HwvtepSouthboundUtils
                .createVlanBindingInstanceIdentifier(physicalSwitchNodeId, phyPortName, vlanId);
        transaction.delete(LogicalDatastoreType.CONFIGURATION, iid);
    }

    /**
     * Deletes the given VLAN binding.
     *
     * @param tx The transaction to use.
     * @param physicalSwitchNodeId The physical switch’s node identifier.
     * @param phyPortName The physical port name.
     * @param vlanId The VLAN identifier.
     */
    public static void deleteVlanBinding(TypedWriteTransaction<Configuration> tx, NodeId physicalSwitchNodeId,
        String phyPortName, Integer vlanId) {
        tx.delete(HwvtepSouthboundUtils.createVlanBindingInstanceIdentifier(physicalSwitchNodeId, phyPortName, vlanId));
    }

    /**
     * Gets the hw vtep node.
     *
     * @param dataBroker
     *            the data broker
     * @param datastoreType
     *            the datastore type
     * @param nodeId
     *            the node id
     * @return the hw vtep node
     * @deprecated Use {@link #getHwVtepNode(TypedReadTransaction, NodeId)}.
     */
    @Deprecated
    public static Node getHwVtepNode(DataBroker dataBroker, LogicalDatastoreType datastoreType, NodeId nodeId) {
        return MDSALUtil.read(dataBroker, datastoreType,
                HwvtepSouthboundUtils.createInstanceIdentifier(nodeId)).orNull();
    }

    /**
     * Retrieves the hardware VTEP node.
     *
     * @param tx The transaction.
     * @param nodeId The node identifier.
     * @return The hardware VTEP node.
     */
    public static Node getHwVtepNode(TypedReadTransaction<? extends Datastore> tx, NodeId nodeId) {
        try {
            return tx.read(HwvtepSouthboundUtils.createInstanceIdentifier(nodeId)).get().orNull();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to read hwvtep node", e);
        }
    }

    /**
     * Installs a list of Mac Addresses as remote Ucast address in an external
     * device using the hwvtep-southbound.
     *
     * @param deviceNodeId
     *            NodeId if the ExternalDevice where the macs must be installed
     *            in.
     * @param macAddresses
     *            List of Mac addresses to be installed in the external device.
     * @param logicalSwitchName
     *            the logical switch name
     * @param remoteVtepIp
     *            VTEP's IP in this OVS used for the tunnel with external
     *            device.
     * @deprecated Use {@link #addUcastMacs(TypedWriteTransaction, String, Iterable, String, IpAddress)}.
     */
    @Deprecated
    public static ListenableFuture<Void> installUcastMacs(DataBroker broker,
                                                          String deviceNodeId, List<PhysAddress> macAddresses,
                                                          String logicalSwitchName, IpAddress remoteVtepIp) {
        NodeId nodeId = new NodeId(deviceNodeId);
        HwvtepPhysicalLocatorAugmentation phyLocatorAug = HwvtepSouthboundUtils
                .createHwvtepPhysicalLocatorAugmentation(remoteVtepIp);
        List<RemoteUcastMacs> macs = new ArrayList<>();
        for (PhysAddress mac : macAddresses) {
            // TODO: Query ARP cache to get IP address corresponding to
            // the MAC
            //IpAddress ipAddress = null;
            macs.add(HwvtepSouthboundUtils.createRemoteUcastMac(nodeId,
                IetfYangUtil.INSTANCE.canonizePhysAddress(mac).getValue(), /*ipAddress*/ null, logicalSwitchName,
                phyLocatorAug));
        }
        return addRemoteUcastMacs(broker, nodeId, macs);
    }

    /**
     * Adds unicast MACs.
     *
     * @param tx The transaction to use.
     * @param deviceNodeId The device’s node identifier.
     * @param macAddresses The MAC addresses.
     * @param logicalSwitchName The logical switch name.
     * @param remoteVtepIp The remote VTEP IP address.
     */
    public static void addUcastMacs(TypedWriteTransaction<Configuration> tx, String deviceNodeId,
        Iterable<PhysAddress> macAddresses, String logicalSwitchName, IpAddress remoteVtepIp) {
        NodeId nodeId = new NodeId(deviceNodeId);
        HwvtepPhysicalLocatorAugmentation phyLocatorAug = HwvtepSouthboundUtils
            .createHwvtepPhysicalLocatorAugmentation(remoteVtepIp);
        // TODO: Query ARP cache to get IP address corresponding to the MAC
        StreamSupport.stream(macAddresses.spliterator(), false)
            .map(macAddress -> HwvtepSouthboundUtils.createRemoteUcastMac(nodeId,
                IetfYangUtil.INSTANCE.canonizePhysAddress(macAddress).getValue(), /*ipAddress*/ null, logicalSwitchName,
                phyLocatorAug))
            .forEach(mac -> addRemoteUcastMac(tx, nodeId, mac));
    }

    /**
     * Retrieves the database version.
     *
     * @deprecated Use {@link #getDbVersion(TypedReadTransaction, NodeId)}.
     */
    @Deprecated
    public static String getDbVersion(DataBroker broker, NodeId nodeId) {
        Node hwvtepNode = getHwVtepNode(broker, LogicalDatastoreType.OPERATIONAL, nodeId);
        String dbVersion = "";
        if (hwvtepNode != null) {
            dbVersion = hwvtepNode.augmentation(HwvtepGlobalAugmentation.class).getDbVersion();
        }
        return dbVersion;
    }

    /**
     * Retrieves the database version, as indicated by the hardware VTEP node.
     *
     * @param tx The transaction.
     * @param nodeId The node identifier.
     * @return The database version.
     */
    public static String getDbVersion(TypedReadTransaction<? extends Datastore> tx, NodeId nodeId) {
        Node hwvtepNode = getHwVtepNode(tx, nodeId);
        return hwvtepNode == null ? "" :  hwvtepNode.augmentation(HwvtepGlobalAugmentation.class).getDbVersion();
    }

}
