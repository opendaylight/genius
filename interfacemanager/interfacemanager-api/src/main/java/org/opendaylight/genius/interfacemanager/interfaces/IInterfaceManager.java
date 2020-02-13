/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.interfaces;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Map;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.exceptions.InterfaceAlreadyExistsException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

public interface IInterfaceManager {

    Long getPortForInterface(String ifName);

    Long getPortForInterface(Interface intf);

    Uint64 getDpnForInterface(String ifName);

    Uint64 getDpnForInterface(Interface intrf);

    String getEndpointIpForDpn(Uint64 dpnId);

    List<ActionInfo> getInterfaceEgressActions(String ifName);

    InterfaceInfo getInterfaceInfo(String intInfo);

    InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName,
            InterfaceInfo.InterfaceType interfaceType);

    InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName);

    /**
     * This API is currently used only for CLI usage. Please be careful that this API
     * can return stale entries since it is just a cache read.
     */
    InterfaceInfo getInterfaceInfoFromOperationalDSCache(String interfaceName);

    /**
     * Retrieve the interface information from the configuration datastore.
     *
     * @param interfaceName The interface name.
     * @return The interface information.
     * @deprecated Use {@link #getInterfaceInfoFromConfigDataStore(ReadTransaction, String)}.
     */
    @Deprecated
    Interface getInterfaceInfoFromConfigDataStore(String interfaceName);

    /**
     * Retrieve the interface information from the configuration datastore.
     *
     * @param tx The transaction to use.
     * @param interfaceName The interface name.
     * @return The interface information.
     * @throws ReadFailedException if an exception occurs while reading from the datastore.
     */
    Interface getInterfaceInfoFromConfigDataStore(ReadTransaction tx, String interfaceName) throws ReadFailedException;

    /**
     * Create a VLAN interface.
     *
     * @deprecated Use {@link #createVLANInterface(String, String, Integer, String, IfL2vlan.L2vlanMode)}.
     */
    @Deprecated
    void createVLANInterface(String interfaceName, String portName, Uint64 dpId, Integer vlanId, String description,
            IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException;

    ListenableFuture<Void> createVLANInterface(String interfaceName, String portName, Integer vlanId,
                                               String description,
                                               IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException;

    /**
     * Create a VLAN interface.
     *
     * @deprecated Use {@link #createVLANInterface(String, String, Integer, String, IfL2vlan.L2vlanMode, boolean)}.
     */
    @Deprecated
    void createVLANInterface(String interfaceName, String portName, Uint64 dpId, Integer vlanId, String description,
            IfL2vlan.L2vlanMode l2vlanMode, boolean isExternal) throws InterfaceAlreadyExistsException;

    ListenableFuture<Void> createVLANInterface(String interfaceName, String portName, Integer vlanId,
                                               String description, IfL2vlan.L2vlanMode l2vlanMode,
                                               boolean isExternal) throws InterfaceAlreadyExistsException;

    boolean isServiceBoundOnInterfaceForIngress(short servicePriority, String interfaceName);

    boolean isServiceBoundOnInterfaceForEgress(short servicePriority, String interfaceName);

    void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo);

    void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo,
                     TypedWriteTransaction<Configuration> tx);

    void unbindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo);

    List<Interface> getVlanInterfaces();

    List<Interface> getVxlanInterfaces();

    /**
     * Retrieve an interface's children.
     *
     * @param parentInterface The parent interface.
     * @return The child interfaces.
     * @deprecated Use {@link #getChildInterfaces(ReadTransaction, String)} instead.
     */
    @Deprecated
    List<Interface> getChildInterfaces(String parentInterface);

    /**
     * Retrieve an interface's children.
     *
     * @param tx The transaction to use.
     * @param parentInterface The parent interface.
     * @return The child interfaces.
     * @throws ReadFailedException if an exception occurs while reading from the datastore.
     */
    List<Interface> getChildInterfaces(ReadTransaction tx, String parentInterface) throws ReadFailedException;

    /**
     * Determine whether an interface is external.
     *
     * @param interfaceName The interface name.
     * @return {@code true} if the interface is external, {@code false} if it isn't.
     * @deprecated Use {@link #isExternalInterface(ReadTransaction, String)} instead.
     */
    @Deprecated
    boolean isExternalInterface(String interfaceName);

    /**
     * Determine whether an interface is external.
     *
     * @param tx The transaction to use.
     * @param interfaceName The interface name.
     * @return {@code true} if the interface is external, {@code false} if it isn't.
     * @throws ReadFailedException if an exception occurs while reading from the datastore.
     */
    boolean isExternalInterface(ReadTransaction tx, String interfaceName) throws ReadFailedException;

    String getPortNameForInterface(NodeConnectorId nodeConnectorId, String interfaceName);

    String getPortNameForInterface(String dpnId, String interfaceName);

    String getParentRefNameForInterface(String interfaceName);

    Map<String, OvsdbTerminationPointAugmentation> getTerminationPointCache();

    Map<String, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
            .Interface.OperStatus> getBfdStateCache();

    OvsdbTerminationPointAugmentation getTerminationPointForInterface(String interfaceName);

    OvsdbBridgeAugmentation getOvsdbBridgeForInterface(String interfaceName);

    OvsdbBridgeAugmentation getOvsdbBridgeForNodeIid(InstanceIdentifier<Node> nodeIid);

    List<OvsdbTerminationPointAugmentation> getPortsOnBridge(Uint64 dpnId);

    List<OvsdbTerminationPointAugmentation> getTunnelPortsOnBridge(Uint64 dpnId);

    Map<Class<? extends InterfaceTypeBase>, List<OvsdbTerminationPointAugmentation>>
        getPortsOnBridgeByType(Uint64 dpnId);

    void updateInterfaceParentRef(String interfaceName, String parentInterface);

    void updateInterfaceParentRef(String interfaceName, String parentInterface, boolean readInterfaceBeforeWrite);

    long getLogicalTunnelSelectGroupId(int lportTag);

    boolean isItmDirectTunnelsEnabled();

    Map getBridgeRefEntryMap();
}
