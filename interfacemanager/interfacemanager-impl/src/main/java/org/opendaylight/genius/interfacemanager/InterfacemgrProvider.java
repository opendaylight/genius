/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.exceptions.InterfaceAlreadyExistsException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceAdminState;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.BatchingUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.IfmClusterUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.interfacemanager.rpcservice.InterfaceManagerRpcService;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.infrautils.caches.Cache;
import org.opendaylight.infrautils.caches.CacheConfigBuilder;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfExternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfacemgrProvider implements AutoCloseable, IInterfaceManager {
    private static final Logger LOG = LoggerFactory.getLogger(InterfacemgrProvider.class);
    private static final InterfaceStatusMonitor INTERFACE_STATUS_MONITOR = InterfaceStatusMonitor.getInstance();
    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final InterfaceManagerRpcService interfaceManagerRpcService;
    private final EntityOwnershipService entityOwnershipService;
    private final CacheProvider cacheProvider;
    //private Map<String, OvsdbTerminationPointAugmentation> ifaceToTpMap;
    private Map<String, InstanceIdentifier<Node>> ifaceToNodeIidMap;
    private Map<InstanceIdentifier<Node>, OvsdbBridgeAugmentation> nodeIidToBridgeMap;
    private Cache<String, OvsdbTerminationPointAugmentation> ifaceTpCache;

    @Inject
    public InterfacemgrProvider(final DataBroker dataBroker, final EntityOwnershipService entityOwnershipService,
            final IdManagerService idManager, final InterfaceManagerRpcService interfaceManagerRpcService,
            final CacheProvider cacheProvider) {
        this.dataBroker = dataBroker;
        this.entityOwnershipService = entityOwnershipService;
        this.idManager = idManager;
        this.interfaceManagerRpcService = interfaceManagerRpcService;
        this.cacheProvider = cacheProvider;
    }

    @PostConstruct
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void start() {
        try {
            INTERFACE_STATUS_MONITOR.registerMbean();
            createIdPool();
            IfmClusterUtils.registerEntityForOwnership(this, this.entityOwnershipService);
            BatchingUtils.registerWithBatchManager(this.dataBroker);
            //this.ifaceToTpMap = new ConcurrentHashMap<>();
            this.ifaceToNodeIidMap = new ConcurrentHashMap<>();
            this.nodeIidToBridgeMap = new ConcurrentHashMap<>();
            ifaceTpCache = cacheProvider.newCache(
                    new CacheConfigBuilder<String, OvsdbTerminationPointAugmentation>()
                    .anchor(this)
                    .id("ifaceToTp")
                    .cacheFunction(key -> ifaceTpCacheLookup(key))
                    .description("cache for interfaceName to TerminationPoint")
                    .build());

            INTERFACE_STATUS_MONITOR.reportStatus("OPERATIONAL");
        } catch (Exception e) {
            INTERFACE_STATUS_MONITOR.reportStatus("ERROR");
            throw e;
        }
        LOG.info("InterfacemgrProvider Started");
    }

    private OvsdbTerminationPointAugmentation ifaceTpCacheLookup(String key) {
        /* TODO:
         * We don't have enough information to generate Iid
         * and read off datastore. Probably need information from
         * other caches, but if we don't have it with us, they won't
         * either.
         */
        return null;
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        INTERFACE_STATUS_MONITOR.unregisterMbean();
        LOG.info("InterfacemgrProvider Closed");
        ifaceTpCache.close();
    }

    public EntityOwnershipService getEntityOwnershipService() {
        return entityOwnershipService;
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    private void createIdPool() {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder().setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setLow(IfmConstants.IFM_ID_POOL_START).setHigh(IfmConstants.IFM_ID_POOL_END).build();
        // TODO: Error handling
        Future<RpcResult<Void>> result = idManager.createIdPool(createPool);
        try {
            if (result != null && result.get().isSuccessful()) {
                LOG.debug("Created IdPool for InterfaceMgr");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create idPool for InterfaceMgr", e);
        }
    }

    @Override
    public Long getPortForInterface(String ifName) {
        GetPortFromInterfaceInput input = new GetPortFromInterfaceInputBuilder().setIntfName(ifName).build();
        Future<RpcResult<GetPortFromInterfaceOutput>> output = interfaceManagerRpcService.getPortFromInterface(input);
        try {
            RpcResult<GetPortFromInterfaceOutput> port = output.get();
            if (port.isSuccessful()) {
                return port.getResult().getPortno();
            }
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting port for interface", e);
        }
        return null;
    }

    @Override
    public Long getPortForInterface(Interface intf) {
        GetPortFromInterfaceInput input = new GetPortFromInterfaceInputBuilder().setIntfName(intf.getName()).build();
        Future<RpcResult<GetPortFromInterfaceOutput>> output = interfaceManagerRpcService.getPortFromInterface(input);
        try {
            RpcResult<GetPortFromInterfaceOutput> port = output.get();
            if (port.isSuccessful()) {
                return port.getResult().getPortno();
            }
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting port for interface", e);
        }
        return null;
    }

    @Override
    public InterfaceInfo getInterfaceInfo(String interfaceName) {

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceState(interfaceName, dataBroker);

        if (ifState == null) {
            LOG.debug("Interface {} is not present", interfaceName);
            return null;
        }

        Interface intf = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(interfaceName),
                dataBroker);
        if (intf == null) {
            LOG.error("Interface {} doesn't exist in config datastore", interfaceName);
            return null;
        }

        NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(intf.getName(), dataBroker);
        InterfaceInfo.InterfaceType interfaceType = IfmUtil.getInterfaceType(intf);
        InterfaceInfo interfaceInfo = new InterfaceInfo(interfaceName);
        BigInteger dpId = org.opendaylight.genius.interfacemanager.globals.IfmConstants.INVALID_DPID;
        Integer portNo = org.opendaylight.genius.interfacemanager.globals.IfmConstants.INVALID_PORT_NO;
        if (ncId != null) {
            dpId = IfmUtil.getDpnFromNodeConnectorId(ncId);
            portNo = Integer.parseInt(IfmUtil.getPortNoFromNodeConnectorId(ncId));
        }
        if (interfaceType == InterfaceInfo.InterfaceType.VLAN_INTERFACE) {
            interfaceInfo = IfmUtil.getVlanInterfaceInfo(intf, dpId);
        } else if (interfaceType == InterfaceInfo.InterfaceType.UNKNOWN_INTERFACE) {
            LOG.error("Type of Interface {} is unknown", interfaceName);
            return null;
        }
        InterfaceInfo.InterfaceOpState opState;
        if (ifState.getOperStatus() == OperStatus.Up) {
            opState = InterfaceInfo.InterfaceOpState.UP;
        } else if (ifState.getOperStatus() == OperStatus.Down) {
            opState = InterfaceInfo.InterfaceOpState.DOWN;
        } else {
            opState = InterfaceInfo.InterfaceOpState.UNKNOWN;
        }
        interfaceInfo.setDpId(dpId);
        interfaceInfo.setPortNo(portNo);
        interfaceInfo.setAdminState(intf.isEnabled() ? InterfaceAdminState.ENABLED : InterfaceAdminState.DISABLED);
        interfaceInfo.setInterfaceName(interfaceName);
        Integer lportTag = ifState.getIfIndex();
        interfaceInfo.setInterfaceTag(lportTag);
        interfaceInfo.setInterfaceType(interfaceType);
        interfaceInfo.setGroupId(IfmUtil.getGroupId(lportTag, interfaceType));
        interfaceInfo.setOpState(opState);
        PhysAddress phyAddress = ifState.getPhysAddress();
        if (phyAddress != null) {
            interfaceInfo.setMacAddress(ifState.getPhysAddress().getValue());
        }

        return interfaceInfo;

    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName,
            InterfaceInfo.InterfaceType interfaceType) {
        InterfaceInfo interfaceInfo = new InterfaceInfo(interfaceName);
        org.opendaylight.yang.gen.v1.urn
            .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState =
                InterfaceManagerCommonUtils
                .getInterfaceState(interfaceName, dataBroker);
        if (ifState == null) {
            LOG.debug("Interface {} is not present", interfaceName);
            return null;
        }
        NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
        if (ncId != null) {
            interfaceInfo.setDpId(IfmUtil.getDpnFromNodeConnectorId(ncId));
            interfaceInfo.setPortNo(Integer.parseInt(IfmUtil.getPortNoFromNodeConnectorId(ncId)));
        }
        InterfaceInfo.InterfaceOpState opState;
        if (ifState.getOperStatus() == OperStatus.Up) {
            opState = InterfaceInfo.InterfaceOpState.UP;
        } else if (ifState.getOperStatus() == OperStatus.Down) {
            opState = InterfaceInfo.InterfaceOpState.DOWN;
        } else {
            opState = InterfaceInfo.InterfaceOpState.UNKNOWN;
        }
        interfaceInfo.setAdminState(ifState.getAdminStatus() == AdminStatus.Up ? InterfaceAdminState.ENABLED
                : InterfaceAdminState.DISABLED);
        interfaceInfo.setInterfaceName(interfaceName);
        Integer lportTag = ifState.getIfIndex();
        interfaceInfo.setInterfaceTag(lportTag);
        interfaceInfo.setInterfaceType(interfaceType);
        interfaceInfo.setGroupId(IfmUtil.getGroupId(lportTag, interfaceType));
        interfaceInfo.setOpState(opState);
        PhysAddress phyAddress = ifState.getPhysAddress();
        if (phyAddress != null) {
            interfaceInfo.setMacAddress(ifState.getPhysAddress().getValue());
        }
        return interfaceInfo;
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceState(interfaceName, dataBroker);
        if (ifState == null) {
            LOG.debug("Interface {} is not present", interfaceName);
            return null;
        }

        return populateInterfaceInfo(interfaceName, ifState);
    }

    public InterfaceInfo populateInterfaceInfo(String interfaceName,
            org.opendaylight.yang.gen.v1.urn
                .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        InterfaceInfo interfaceInfo = new InterfaceInfo(interfaceName);
        NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
        if (ncId != null) {
            if (Tunnel.class.equals(ifState.getType())) {
                interfaceInfo.setPortName(interfaceName);
            } else {
                Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName, dataBroker);
                if (iface != null) {
                    ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);
                    interfaceInfo.setPortName(parentRefs.getParentInterface());
                }
            }
            interfaceInfo.setDpId(IfmUtil.getDpnFromNodeConnectorId(ncId));
            interfaceInfo.setPortNo(Integer.parseInt(IfmUtil.getPortNoFromNodeConnectorId(ncId)));
        }
        InterfaceInfo.InterfaceOpState opState;
        if (ifState.getOperStatus() == OperStatus.Up) {
            opState = InterfaceInfo.InterfaceOpState.UP;
        } else if (ifState.getOperStatus() == OperStatus.Down) {
            opState = InterfaceInfo.InterfaceOpState.DOWN;
        } else {
            opState = InterfaceInfo.InterfaceOpState.UNKNOWN;
        }
        interfaceInfo.setAdminState(ifState.getAdminStatus() == AdminStatus.Up ? InterfaceAdminState.ENABLED
                : InterfaceAdminState.DISABLED);
        interfaceInfo.setInterfaceName(interfaceName);
        Integer lportTag = ifState.getIfIndex();
        if (lportTag != null) {
            interfaceInfo.setInterfaceTag(lportTag);
        }
        interfaceInfo.setOpState(opState);
        PhysAddress phyAddress = ifState.getPhysAddress();
        if (phyAddress != null) {
            interfaceInfo.setMacAddress(ifState.getPhysAddress().getValue());
        }
        return interfaceInfo;
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDSCache(String interfaceName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceStateFromCache(interfaceName);
        if (ifState == null) {
            LOG.warn("Interface {} is not present", interfaceName);
            return null;
        }
        return populateInterfaceInfo(interfaceName, ifState);
    }

    @Override
    public Interface getInterfaceInfoFromConfigDataStore(String interfaceName) {
        return InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(interfaceName), dataBroker);
    }

    @Override
    public void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId,
            String description, IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException {
        createVLANInterface(interfaceName, portName, vlanId, description, l2vlanMode);
    }

    @Override
    public void createVLANInterface(String interfaceName, String portName, Integer vlanId,
            String description, IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException {
        createVLANInterface(interfaceName, portName, vlanId, description, l2vlanMode, false);
    }

    @Override
    public void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId,
            String description, IfL2vlan.L2vlanMode l2vlanMode, boolean isExternal)
            throws InterfaceAlreadyExistsException {
        createVLANInterface(interfaceName, portName, vlanId, description, l2vlanMode, isExternal);
    }

    @Override
    public void createVLANInterface(String interfaceName, String portName, Integer vlanId,
            String description, IfL2vlan.L2vlanMode l2vlanMode, boolean isExternal)
            throws InterfaceAlreadyExistsException {

        LOG.info("Create VLAN interface : {}", interfaceName);
        Interface interfaceOptional = InterfaceManagerCommonUtils
                .getInterfaceFromConfigDS(new InterfaceKey(interfaceName), dataBroker);
        if (interfaceOptional != null) {
            LOG.debug("VLAN interface is already exist", interfaceOptional.getDescription());
            throw new InterfaceAlreadyExistsException(interfaceOptional.getName());
        }
        IfL2vlanBuilder l2vlanBuilder = new IfL2vlanBuilder().setL2vlanMode(l2vlanMode);
        if (vlanId != null && vlanId > 0) {
            l2vlanBuilder.setVlanId(new VlanId(vlanId));
        }
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(portName).build();
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder().setEnabled(true).setName(interfaceName)
                .setType(L2vlan.class).addAugmentation(IfL2vlan.class, l2vlanBuilder.build())
                .addAugmentation(ParentRefs.class, parentRefs).setDescription(description);
        if (isExternal) {
            interfaceBuilder.addAugmentation(IfExternal.class, new IfExternalBuilder().setExternal(true).build());
        }
        InstanceIdentifier<Interface> interfaceInstanceIdentifier = InterfaceManagerCommonUtils
                .getInterfaceIdentifier(new InterfaceKey(interfaceName));
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier, interfaceBuilder.build(),
                true);
        writeTransaction.submit();
    }

    private boolean isServiceBoundOnInterface(short servicePriority, String interfaceName,
            Class<? extends ServiceModeBase> serviceMode) {
        InstanceIdentifier<BoundServices> boundServicesIId = IfmUtil.buildBoundServicesIId(servicePriority,
                interfaceName, serviceMode);
        try {
            return SingleTransactionDataBroker
                    .syncReadOptional(dataBroker, LogicalDatastoreType.CONFIGURATION, boundServicesIId).isPresent();
        } catch (ReadFailedException e) {
            LOG.warn("Error while reading [{}]", boundServicesIId, e);
            return false;
        }
    }

    @Override
    public boolean isServiceBoundOnInterfaceForIngress(short servicePriority, String interfaceName) {
        return isServiceBoundOnInterface(servicePriority, interfaceName, ServiceModeIngress.class);
    }

    @Override
    public boolean isServiceBoundOnInterfaceForEgress(short servicePriority, String interfaceName) {
        return isServiceBoundOnInterface(servicePriority, interfaceName, ServiceModeEgress.class);
    }

    @Override
    public void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode,
            BoundServices serviceInfo) {
        bindService(interfaceName, serviceMode, serviceInfo, /* WriteTransaction */ null);
    }

    @Override
    public void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode,
            BoundServices serviceInfo, WriteTransaction tx) {
        WriteTransaction writeTransaction = tx != null ? tx : dataBroker.newWriteOnlyTransaction();
        IfmUtil.bindService(writeTransaction, interfaceName, serviceInfo, serviceMode);
        if (tx == null) {
            writeTransaction.submit();
        }
    }

    @Override
    public void unbindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode,
            BoundServices serviceInfo) {
        IfmUtil.unbindService(dataBroker, interfaceName,
                FlowBasedServicesUtils.buildServiceId(interfaceName, serviceInfo.getServicePriority(), serviceMode));
    }

    @Override
    public BigInteger getDpnForInterface(Interface intrf) {
        return getDpnForInterface(intrf.getName());
    }

    @Override
    public BigInteger getDpnForInterface(String ifName) {
        GetDpidFromInterfaceInput input = new GetDpidFromInterfaceInputBuilder().setIntfName(ifName).build();
        Future<RpcResult<GetDpidFromInterfaceOutput>> output = interfaceManagerRpcService.getDpidFromInterface(input);
        try {
            RpcResult<GetDpidFromInterfaceOutput> dpn = output.get();
            if (dpn.isSuccessful()) {
                return dpn.getResult().getDpid();
            }
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting port for interface", e);
        }
        return null;
    }

    @Override
    public String getEndpointIpForDpn(BigInteger dpnId) {
        GetEndpointIpForDpnInput input = new GetEndpointIpForDpnInputBuilder().setDpid(dpnId).build();
        Future<RpcResult<GetEndpointIpForDpnOutput>> output = interfaceManagerRpcService.getEndpointIpForDpn(input);
        try {
            RpcResult<GetEndpointIpForDpnOutput> ipForDpnOutputRpcResult = output.get();
            if (ipForDpnOutputRpcResult.isSuccessful()) {
                List<IpAddress> localIps = ipForDpnOutputRpcResult.getResult().getLocalIps();
                if (!localIps.isEmpty()) {
                    return localIps.get(0).getIpv4Address().getValue();
                }
            }
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting port for interface", e);
        }
        return null;
    }

    @Override
    public List<ActionInfo> getInterfaceEgressActions(String ifName) {
        return IfmUtil.getEgressActionInfosForInterface(ifName, 0, dataBroker, false);
    }

    @Override
    public List<Interface> getVlanInterfaces() {
        return InterfaceManagerCommonUtils.getAllVlanInterfacesFromCache();
    }

    @Override
    public List<Interface> getVxlanInterfaces() {
        return InterfaceManagerCommonUtils.getAllTunnelInterfacesFromCache();
    }

    @Override
    public List<Interface> getChildInterfaces(String parentInterface) {
        InterfaceParentEntry parentEntry = InterfaceMetaUtils.getInterfaceParentEntryFromConfigDS(parentInterface,
                dataBroker);
        if (parentEntry == null) {
            LOG.debug("No parent entry found for {}", parentInterface);
            return Collections.emptyList();
        }

        List<InterfaceChildEntry> childEntries = parentEntry.getInterfaceChildEntry();
        if (childEntries == null || childEntries.isEmpty()) {
            LOG.debug("No child entries found for parent {}", parentInterface);
            return Collections.emptyList();
        }

        List<Interface> childInterfaces = new ArrayList<>();
        for (InterfaceChildEntry childEntry : childEntries) {
            String interfaceName = childEntry.getChildInterface();
            Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName, dataBroker);
            if (iface != null) {
                childInterfaces.add(iface);
            } else {
                LOG.debug("Child interface {} not found in config DS for parent interface {}", interfaceName,
                        parentInterface);
            }
        }

        LOG.trace("Found child interfaces {} for parent {}", childInterfaces, parentInterface);
        return childInterfaces;
    }

    @Override
    public boolean isExternalInterface(String interfaceName) {
        return isExternalInterface(getInterfaceInfoFromConfigDataStore(interfaceName));
    }

    private boolean isExternalInterface(Interface iface) {
        if (iface == null) {
            return false;
        }

        IfExternal ifExternal = iface.getAugmentation(IfExternal.class);
        return ifExternal != null && Boolean.TRUE.equals(ifExternal.isExternal());
    }

    @Override
    public String getPortNameForInterface(NodeConnectorId nodeConnectorId, String interfaceName) {
        return InterfaceManagerCommonUtils.getPortNameForInterface(nodeConnectorId, interfaceName);
    }

    @Override
    public String getPortNameForInterface(String dpnId, String interfaceName) {
        return InterfaceManagerCommonUtils.getPortNameForInterface(dpnId, interfaceName);
    }

    @Deprecated
    @Override
    public Map<String, OvsdbTerminationPointAugmentation> getTerminationPointCache() {
        //return new ConcurrentHashMap<>(this.ifaceToTpMap);
        return Collections.emptyMap();
    }

    public void addTerminationPointForInterface(String interfaceName,
            OvsdbTerminationPointAugmentation terminationPoint) {
        LOG.debug("Adding TerminationPoint {} to cache for Interface {}", terminationPoint.getName(), interfaceName);
        if (interfaceName != null && terminationPoint != null) {
            ifaceTpCache.put(interfaceName, terminationPoint);
        }
    }

    public OvsdbTerminationPointAugmentation getTerminationPoint(String interfaceName) {
        return ifaceTpCache.get(interfaceName);
    }

    public void removeTerminationPointForInterface(String interfaceName) {
        LOG.debug("Removing TerminationPoint from cache for Interface {}", interfaceName);
        if (interfaceName != null) {
            ifaceTpCache.evict(interfaceName);
        }
    }

    public void addNodeIidForInterface(String interfaceName, InstanceIdentifier<Node> nodeIid) {
        if (interfaceName != null && nodeIid != null) {
            ifaceToNodeIidMap.put(interfaceName, nodeIid);
        }
    }

    public void removeNodeIidForInterface(String interfaceName) {
        if (interfaceName != null) {
            ifaceToNodeIidMap.remove(interfaceName);
        }
    }

    public InstanceIdentifier<Node> getNodeIidForInterface(String interfaceName) {
        if (interfaceName != null) {
            return ifaceToNodeIidMap.get(interfaceName);
        }
        return null;
    }

    private OvsdbBridgeAugmentation getBridgeForInterface(String interfaceName,
            InstanceIdentifier<Node> nodeInstanceId) {
        InstanceIdentifier<Node> nodeIid = nodeInstanceId;
        if (nodeIid == null) {
            nodeIid = getNodeIidForInterface(interfaceName);
        }
        return getBridgeForNodeIid(nodeIid);
    }

    public String getDpidForInterface(String interfaceName) {
        return getDpidForInterface(interfaceName, null);
    }

    public String getDpidForInterface(String interfaceName, InstanceIdentifier<Node> nodeInstanceId) {
        OvsdbBridgeAugmentation bridge = getBridgeForInterface(interfaceName, nodeInstanceId);
        if (bridge != null) {
            BigInteger dpid = IfmUtil.getDpnId(bridge.getDatapathId());
            if (dpid != null && dpid.longValue() != 0) {
                return String.valueOf(dpid);
            }
        }
        return null;
    }

    public void addBridgeForNodeIid(InstanceIdentifier<Node> nodeIid, OvsdbBridgeAugmentation bridge) {
        if (nodeIid != null && bridge != null) {
            nodeIidToBridgeMap.put(nodeIid, bridge);
        }
    }

    public void removeBridgeForNodeIid(InstanceIdentifier<Node> nodeIid) {
        if (nodeIid != null) {
            nodeIidToBridgeMap.remove(nodeIid);
        }
    }

    public OvsdbBridgeAugmentation getBridgeForNodeIid(InstanceIdentifier<Node> nodeIid) {
        if (nodeIid != null) {
            return nodeIidToBridgeMap.get(nodeIid);
        }
        return null;
    }

    @Override
    public String getParentRefNameForInterface(String interfaceName) {
        String parentRefName = null;

        String dpnId = getDpidForInterface(interfaceName, null);
        OvsdbTerminationPointAugmentation ovsdbTp = getTerminationPoint(interfaceName);
        if (ovsdbTp != null) {
            if (dpnId == null) {
                LOG.error("Got NULL dpnId when looking for TP with external ID {}", interfaceName);
                return null;
            }
            parentRefName = getPortNameForInterface(dpnId, ovsdbTp.getName());
            LOG.debug("Building parent ref for interface {}, using parentRefName {} acquired by external ID",
                    interfaceName, parentRefName);
        } else {
            LOG.debug("Skipping parent ref for interface {}, as there is no termination point that references "
                    + "this interface yet.", interfaceName);
        }

        return parentRefName;
    }

    @Override
    public void updateInterfaceParentRef(String interfaceName, String parentInterface) {
        // This should generally be called by EOS Owner for
        // INTERFACE_CONFIG_ENTITY - runOnlyInLeaderNode()
        updateInterfaceParentRef(interfaceName, parentInterface, true);
    }

    @Override
    public void updateInterfaceParentRef(String interfaceName, String parentInterface,
            boolean readInterfaceBeforeWrite) {
        // This should generally be called by EOS Owner for
        // INTERFACE_CONFIG_ENTITY - runOnlyInLeaderNode()
        if (interfaceName == null) {
            return;
        }

        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        ParentRefUpdateWorker parentRefUpdateWorker = new ParentRefUpdateWorker(interfaceName, parentInterface,
                readInterfaceBeforeWrite);
        jobCoordinator.enqueueJob(interfaceName, parentRefUpdateWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    public class ParentRefUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        String interfaceName;
        String parentInterfaceName;
        Boolean readInterfaceBeforeWrite;

        public ParentRefUpdateWorker(String interfaceName, String parentInterfaceName,
                boolean readInterfaceBeforeWrite) {
            this.interfaceName = interfaceName;
            this.parentInterfaceName = parentInterfaceName;
            this.readInterfaceBeforeWrite = readInterfaceBeforeWrite;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            if (readInterfaceBeforeWrite) {
                Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName, dataBroker);
                if (iface == null) {
                    LOG.debug("Interface doesn't exist in config DS - no need to update parentRef, skipping");
                    return null;
                }
            }
            WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            IfmUtil.updateInterfaceParentRef(writeTransaction, interfaceName, parentInterfaceName);
            return Collections.singletonList(writeTransaction.submit());
        }
    }

    @Override
    public OvsdbTerminationPointAugmentation getTerminationPointForInterface(String interfaceName) {
        return getTerminationPoint(interfaceName);
    }

    @Override
    public OvsdbBridgeAugmentation getOvsdbBridgeForInterface(String interfaceName) {
        return getBridgeForInterface(interfaceName, null);
    }

    @Override
    public OvsdbBridgeAugmentation getOvsdbBridgeForNodeIid(InstanceIdentifier<Node> nodeIid) {
        return getBridgeForNodeIid(nodeIid);
    }

    @Override
    /**
     * Get all termination points on a given DPN.
     * This API uses read on Operational DS. If there are perf issues in cluster
     * setup, we can consider caching later.
     *
     * @param dpnId
     *            Datapath Node Identifier
     *
     * @return If the data at the supplied path exists, returns a list of all termination point
     *         Augmentations
     */
    public List<OvsdbTerminationPointAugmentation> getPortsOnBridge(BigInteger dpnId) {
        List<OvsdbTerminationPointAugmentation> ports = new ArrayList<>();
        List<TerminationPoint> portList = IfmUtil.getTerminationPointsOnBridge(dataBroker, dpnId);
        for (TerminationPoint ovsPort : portList) {
            if (ovsPort.getAugmentation(OvsdbTerminationPointAugmentation.class) != null) {
                ports.add(ovsPort.getAugmentation(OvsdbTerminationPointAugmentation.class));
            }
        }
        LOG.debug("Found {} ports on bridge {}", ports.size(), dpnId);
        return ports;
    }

    /**
     * Get all termination points of type tunnel on a given DPN.
     *
     * @param dpnId
     *            Datapath Node Identifier
     *
     * @return If the data at the supplied path exists, returns a list of all termination point
     *         Augmentations of type tunnel
     */
    @Override
    public List<OvsdbTerminationPointAugmentation> getTunnelPortsOnBridge(BigInteger dpnId) {
        List<OvsdbTerminationPointAugmentation> tunnelPorts = new ArrayList<>();
        List<TerminationPoint> portList = IfmUtil.getTerminationPointsOnBridge(dataBroker, dpnId);
        for (TerminationPoint ovsPort : portList) {
            OvsdbTerminationPointAugmentation portAug =
                    ovsPort.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (portAug != null && SouthboundUtils.isInterfaceTypeTunnel(portAug.getInterfaceType())) {
                tunnelPorts.add(portAug);
            }
        }

        LOG.debug("Found {} tunnel ports on bridge {}", tunnelPorts.size(), dpnId);
        return tunnelPorts;
    }

    /**
     * Get all termination points by type on a given DPN.
     *
     * @param dpnId
     *            Datapath Node Identifier
     *
     * @return If the data at the supplied path exists, returns a Map where key is interfaceType
     *         and value is list of termination points of given type
     */
    @Override
    public Map<Class<? extends InterfaceTypeBase>, List<OvsdbTerminationPointAugmentation>>
        getPortsOnBridgeByType(BigInteger dpnId) {

        Map<Class<? extends InterfaceTypeBase>, List<OvsdbTerminationPointAugmentation>> portMap;
        portMap = new ConcurrentHashMap<>();
        List<TerminationPoint> ovsPorts = IfmUtil.getTerminationPointsOnBridge(dataBroker, dpnId);
        if (ovsPorts != null) {
            for (TerminationPoint ovsPort : ovsPorts) {
                OvsdbTerminationPointAugmentation portAug =
                        ovsPort.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (portAug != null && portAug.getInterfaceType() != null) {
                    portMap.computeIfAbsent(portAug.getInterfaceType(), k -> new ArrayList<>()).add(portAug);
                }
            }
        }
        return portMap;
    }

    @Override
    public long getLogicalTunnelSelectGroupId(int lportTag) {
        return IfmUtil.getLogicalTunnelSelectGroupId(lportTag);
    }
}
