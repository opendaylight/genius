/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager;


import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.exceptions.InterfaceAlreadyExistsException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceAdminState;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.BatchingUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.IfmClusterUtils;
import org.opendaylight.genius.interfacemanager.rpcservice.InterfaceManagerRpcService;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
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
    private static final InterfaceStatusMonitor interfaceStatusMonitor = InterfaceStatusMonitor.getInstance();
    private DataBroker dataBroker;
    private IdManagerService idManager;
    private InterfaceManagerRpcService interfaceManagerRpcService;
    private EntityOwnershipService entityOwnershipService;
    private MdsalUtils mdsalUtils;
    private SouthboundUtils southboundUtils;

    @Inject
    public InterfacemgrProvider(final DataBroker dataBroker, final EntityOwnershipService entityOwnershipService,
                                final IdManagerService idManager,
                                final InterfaceManagerRpcService interfaceManagerRpcService){
        this.dataBroker = dataBroker;
        this.entityOwnershipService = entityOwnershipService;
        this.idManager = idManager;
        this.interfaceManagerRpcService = interfaceManagerRpcService;
        this.mdsalUtils = new MdsalUtils(dataBroker);
        this.southboundUtils = new SouthboundUtils(mdsalUtils);
    }

    @PostConstruct
    public void start() throws Exception {
        try {
            createIdPool();
            IfmClusterUtils.registerEntityForOwnership(this, this.entityOwnershipService);
            BatchingUtils.registerWithBatchManager( this.dataBroker);
            interfaceStatusMonitor.reportStatus("OPERATIONAL");
        } catch (Exception e) {
            interfaceStatusMonitor.reportStatus("ERROR");
            throw e;
        }
        LOG.info("InterfacemgrProvider Started");
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        LOG.info("InterfacemgrProvider Closed");
    }

    public EntityOwnershipService getEntityOwnershipService() {
        return entityOwnershipService;
    }

    public DataBroker getDataBroker(){
        return this.dataBroker;
    }

    private void createIdPool() {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
                .setPoolName(IfmConstants.IFM_IDPOOL_NAME)
                .setLow(IfmConstants.IFM_ID_POOL_START)
                .setHigh(IfmConstants.IFM_ID_POOL_END)
                .build();
        //TODO: Error handling
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

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface
                ifState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);

        if (ifState == null) {
            LOG.error("Interface {} is not present", interfaceName);
            return null;
        }

        Integer lportTag = ifState.getIfIndex();
        Interface intf = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(interfaceName), dataBroker);
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
            interfaceInfo = IfmUtil.getVlanInterfaceInfo(interfaceName, intf, dpId);
        } else if (interfaceType == InterfaceInfo.InterfaceType.VXLAN_TRUNK_INTERFACE ||
                interfaceType == InterfaceInfo.InterfaceType.GRE_TRUNK_INTERFACE) {
            // TODO : since there is no logical grouping for tunnel interfaces, there is no need
            // for this code as of now. will be revisited once the support comes

        } else {
            LOG.error("Type of Interface {} is unknown", interfaceName);
            return null;
        }
        InterfaceInfo.InterfaceOpState opState ;
        if(ifState.getOperStatus() == OperStatus.Up)
        {
            opState = InterfaceInfo.InterfaceOpState.UP;
        }
        else if (ifState.getOperStatus() == OperStatus.Down)
        {
            opState = InterfaceInfo.InterfaceOpState.DOWN;
        }
        else
        {
            opState = InterfaceInfo.InterfaceOpState.UNKNOWN;
        }
        interfaceInfo.setDpId(dpId);
        interfaceInfo.setPortNo(portNo);
        interfaceInfo.setAdminState(intf.isEnabled() ? InterfaceAdminState.ENABLED : InterfaceAdminState.DISABLED);
        interfaceInfo.setInterfaceName(interfaceName);
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
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName, InterfaceInfo.InterfaceType interfaceType) {
        InterfaceInfo interfaceInfo = new InterfaceInfo(interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceStateFromOperDS(interfaceName, dataBroker);
        if (ifState == null) {
            LOG.error("Interface {} is not present", interfaceName);
            return null;
        }
        Integer lportTag = ifState.getIfIndex();
        NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
        if (ncId != null) {
            interfaceInfo.setDpId(IfmUtil.getDpnFromNodeConnectorId(ncId));
            interfaceInfo.setPortNo(Integer.parseInt(IfmUtil.getPortNoFromNodeConnectorId(ncId)));
        }
        InterfaceInfo.InterfaceOpState opState ;
        if(ifState.getOperStatus() == OperStatus.Up)
        {
            opState = InterfaceInfo.InterfaceOpState.UP;
        }
        else if (ifState.getOperStatus() == OperStatus.Down)
        {
            opState = InterfaceInfo.InterfaceOpState.DOWN;
        }
        else
        {
            opState = InterfaceInfo.InterfaceOpState.UNKNOWN;
        }
        interfaceInfo.setAdminState(ifState.getAdminStatus() == AdminStatus.Up ? InterfaceAdminState.ENABLED : InterfaceAdminState.DISABLED);
        interfaceInfo.setInterfaceName(interfaceName);
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
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface
            ifState = InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(interfaceName, dataBroker);
        if (ifState == null) {
            LOG.error("Interface {} is not present", interfaceName);
            return null;
        }

        return populateInterfaceInfo(interfaceName, ifState);
    }


    public InterfaceInfo populateInterfaceInfo(String interfaceName, org.opendaylight.yang.gen.v1.urn.ietf.params.xml
        .ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface ifState) {
        Integer lportTag = ifState.getIfIndex();
        InterfaceInfo interfaceInfo = new InterfaceInfo(interfaceName);
        NodeConnectorId ncId = IfmUtil.getNodeConnectorIdFromInterface(ifState);
        if (ncId != null) {
            if (Tunnel.class.equals(ifState.getType())) {
                interfaceInfo.setPortName(interfaceName);
            } else {
                Interface iface = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceName, dataBroker);
                ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);
                interfaceInfo.setPortName(parentRefs.getParentInterface());
            }
            interfaceInfo.setDpId(IfmUtil.getDpnFromNodeConnectorId(ncId));
            interfaceInfo.setPortNo(Integer.parseInt(IfmUtil.getPortNoFromNodeConnectorId(ncId)));
        }
        InterfaceInfo.InterfaceOpState opState ;
        if (ifState.getOperStatus() == OperStatus.Up) {
            opState = InterfaceInfo.InterfaceOpState.UP;
        } else if (ifState.getOperStatus() == OperStatus.Down) {
            opState = InterfaceInfo.InterfaceOpState.DOWN;
        } else {
            opState = InterfaceInfo.InterfaceOpState.UNKNOWN;
        }
        interfaceInfo.setAdminState((ifState.getAdminStatus() == AdminStatus.Up) ? InterfaceAdminState.ENABLED
            : InterfaceAdminState.DISABLED);
        interfaceInfo.setInterfaceName(interfaceName);
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
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface
            ifState = InterfaceManagerCommonUtils.getInterfaceStateFromCache(interfaceName);
        if (ifState == null) {
            LOG.warn("Interface {} is not present", interfaceName);
            return null;
        }
        return populateInterfaceInfo(interfaceName, ifState);
    }

    @Override
    public Interface getInterfaceInfoFromConfigDataStore(String interfaceName) {
        Interface intf = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(interfaceName), dataBroker);
        return intf;
    }

    @Override
    public void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId,
                                    String description, IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException {
        createVLANInterface(interfaceName, portName, dpId, vlanId, description, l2vlanMode, false);
    }
    @Override
    public void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId,
                                    String description, IfL2vlan.L2vlanMode l2vlanMode, boolean isExternal) throws InterfaceAlreadyExistsException {

        LOG.info("Create VLAN interface : {}", interfaceName);
        InstanceIdentifier<Interface> interfaceInstanceIdentifier = InterfaceManagerCommonUtils.getInterfaceIdentifier(new InterfaceKey(interfaceName));
        Interface interfaceOptional = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(new InterfaceKey(interfaceName), dataBroker);
        if (interfaceOptional != null) {
            LOG.debug("VLAN interface is already exist", interfaceOptional.getDescription());
            throw new InterfaceAlreadyExistsException(interfaceOptional.getName());
        }
        IfL2vlanBuilder l2vlanBuilder = new IfL2vlanBuilder().setL2vlanMode(l2vlanMode);
        if (vlanId != null && vlanId > 0) {
            l2vlanBuilder.setVlanId(new VlanId(vlanId));
        }
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(portName).build();
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder().setEnabled(true).setName(interfaceName).setType(L2vlan.class).
                addAugmentation(IfL2vlan.class, l2vlanBuilder.build()).addAugmentation(ParentRefs.class, parentRefs).
                setDescription(description);
        if (isExternal) {
            interfaceBuilder.addAugmentation(IfExternal.class, new IfExternalBuilder().setExternal(true).build());
        }
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        t.put(LogicalDatastoreType.CONFIGURATION, interfaceInstanceIdentifier, interfaceBuilder.build(), true);
        t.submit();
    }

    @Override
    public void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo) {
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        IfmUtil.bindService(t, interfaceName, serviceInfo, serviceMode);
        t.submit();
    }

    @Override
    public void unbindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo) {
        IfmUtil.unbindService(dataBroker, interfaceName,
                FlowBasedServicesUtils.buildServiceId(interfaceName, serviceInfo.getServicePriority(), serviceMode));
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
    public BigInteger getDpnForInterface(Interface intrf) {
        return getDpnForInterface(intrf.getName());
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

    @Override
    public String getParentRefNameForInterface(String interfaceName) {
        String parentRefName = null;

        // FIXME Note this utility isn't very good for scale/performance as it traverses all nodes,
        // probably need to use a cache instead of these (iface_name->dpnId+tpName).
        Node node = southboundUtils.getNodeByTerminationPointExternalId(interfaceName);
        if (node != null) {
            String dpnId = southboundUtils.getDataPathIdStr(node);
            if (dpnId == null) {
                LOG.error("Got node {} when looking for TP with external ID {}, "
                        + "but unexpectedly got NULL dpnId for this node", node, interfaceName);
                return null;
            }
            TerminationPoint tp = SouthboundUtils.getTerminationPointByExternalId(node, interfaceName);
            if (tp == null) {
                LOG.error("Got node {} when looking for TP with external ID {}, "
                        + "but unexpectedly got a NULL TP from this node", node, interfaceName);
                return null;
            }
            OvsdbTerminationPointAugmentation ovsdbTp = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
            parentRefName = getPortNameForInterface(dpnId, ovsdbTp.getName());
            LOG.debug("Building parent ref for neutron port {}, using parentRefName {} acquired by external ID",
                    interfaceName, parentRefName);
        } else {
            LOG.debug("Skipping parent ref for neutron port {}, as there is no termination point that references "
                    + "this neutron port yet.", interfaceName);
        }

        return parentRefName;
    }

    @Override
    public void updateInterfaceParentRef(String interfaceName, String parentInterface) {
        // This should generally be called by EOS Owner for IFACE_ENTITY - runOnlyInLeaderNode()
        updateInterfaceParentRef(interfaceName, parentInterface, true);
    }

    @Override
    public void updateInterfaceParentRef(String interfaceName, String parentInterface, boolean readInterfaceBeforeWrite) {
        // This should generally be called by EOS Owner for IFACE_ENTITY - runOnlyInLeaderNode()
        if (interfaceName == null) {
            return;
        }

        DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
        ParentRefUpdateWorker parentRefUpdateWorker =
                new ParentRefUpdateWorker(interfaceName, parentInterface, readInterfaceBeforeWrite);
        jobCoordinator.enqueueJob(interfaceName, parentRefUpdateWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    public class ParentRefUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        String interfaceName;
        String parentInterfaceName;
        Boolean readInterfaceBeforeWrite;

        public ParentRefUpdateWorker(String interfaceName, String parentInterfaceName, boolean readInterfaceBeforeWrite) {
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
            WriteTransaction t = dataBroker.newWriteOnlyTransaction();
            IfmUtil.updateInterfaceParentRef(t, interfaceName, parentInterfaceName);
            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = t.submit();
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            futures.add(submitFuture);
            return futures;
        }
    }

}
