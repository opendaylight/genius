/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static java.util.Collections.singletonList;
import static org.opendaylight.controller.md.sal.binding.api.WriteTransaction.CREATE_MISSING_PARENTS;
import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;
import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.Datastore.Configuration;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.tunnel.optional.params.TunnelOptions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItmInternalTunnelAddWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ItmInternalTunnelAddWorker.class) ;

    private final ItmConfig itmCfg;
    private final Integer monitorInterval;
    private final boolean isTunnelMonitoringEnabled;
    private final Class<? extends TunnelMonitoringTypeBase> monitorProtocol;

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final JobCoordinator jobCoordinator;
    private final DirectTunnelUtils directTunnelUtils;
    private final IInterfaceManager interfaceManager;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;

    public ItmInternalTunnelAddWorker(DataBroker dataBroker, JobCoordinator jobCoordinator,
                                      TunnelMonitoringConfig tunnelMonitoringConfig, ItmConfig itmCfg,
                                      DirectTunnelUtils directTunnelUtil,
                                      IInterfaceManager interfaceManager,
                                      OvsBridgeRefEntryCache ovsBridgeRefEntryCache) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.jobCoordinator = jobCoordinator;
        this.itmCfg = itmCfg;
        this.directTunnelUtils = directTunnelUtil;
        this.interfaceManager = interfaceManager;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;

        isTunnelMonitoringEnabled = tunnelMonitoringConfig.isTunnelMonitoringEnabled();
        monitorProtocol = tunnelMonitoringConfig.getMonitorProtocol();
        monitorInterval = tunnelMonitoringConfig.getMonitorInterval();
    }

    public List<ListenableFuture<Void>> buildAllTunnels(IMdsalApiManager mdsalManager, List<DPNTEPsInfo> cfgdDpnList,
                                                        Collection<DPNTEPsInfo> meshedDpnList) {
        LOG.trace("Building tunnels with DPN List {} " , cfgdDpnList);
        if (null == cfgdDpnList || cfgdDpnList.isEmpty()) {
            LOG.error(" Build Tunnels was invoked with empty list");
            return Collections.emptyList();
        }

        return Collections.singletonList(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION, tx -> {
            for (DPNTEPsInfo dpn : cfgdDpnList) {
                //#####if dpn is not in meshedDpnList
                buildTunnelFrom(tx, dpn, meshedDpnList, mdsalManager);
                if (meshedDpnList != null) {
                    meshedDpnList.add(dpn);
                }
                // Update the config datastore -- FIXME -- Error Handling
                updateDpnTepInfoToConfig(tx, dpn);
            }
        }));
    }

    private static void updateDpnTepInfoToConfig(TypedWriteTransaction<Configuration> tx, DPNTEPsInfo dpn) {
        LOG.debug("Updating CONFIGURATION datastore with DPN {} ", dpn);
        InstanceIdentifier<DpnEndpoints> dep = InstanceIdentifier.builder(DpnEndpoints.class).build() ;
        List<DPNTEPsInfo> dpnList = new ArrayList<>() ;
        dpnList.add(dpn) ;
        DpnEndpoints tnlBuilder = new DpnEndpointsBuilder().setDPNTEPsInfo(dpnList).build() ;
        tx.merge(dep, tnlBuilder);
    }

    private void buildTunnelFrom(TypedReadWriteTransaction<Configuration> tx, DPNTEPsInfo srcDpn,
        Collection<DPNTEPsInfo> meshedDpnList, IMdsalApiManager mdsalManager)
        throws ExecutionException, InterruptedException, OperationFailedException {
        LOG.trace("Building tunnels from DPN {} " , srcDpn);
        if (null == meshedDpnList || meshedDpnList.isEmpty()) {
            LOG.debug("No DPN in the mesh ");
            return ;
        }
        for (DPNTEPsInfo dstDpn: meshedDpnList) {
            if (!srcDpn.equals(dstDpn)) {
                wireUpWithinTransportZone(tx, srcDpn, dstDpn, mdsalManager);
            }
        }

    }

    private void wireUpWithinTransportZone(TypedReadWriteTransaction<Configuration> tx, DPNTEPsInfo srcDpn,
        DPNTEPsInfo dstDpn, IMdsalApiManager mdsalManager)
        throws ExecutionException, InterruptedException, OperationFailedException {
        LOG.trace("Wiring up within Transport Zone for Dpns {}, {} " , srcDpn, dstDpn);
        List<TunnelEndPoints> srcEndPts = srcDpn.getTunnelEndPoints();
        List<TunnelEndPoints> dstEndPts = dstDpn.getTunnelEndPoints();

        for (TunnelEndPoints srcte : nullToEmpty(srcEndPts)) {
            for (TunnelEndPoints dstte : nullToEmpty(dstEndPts)) {
                // Compare the Transport zones
                if (!Objects.equals(srcDpn.getDPNID(), dstDpn.getDPNID())) {
                    if (!ItmUtils.getIntersection(nullToEmpty(srcte.getTzMembership()),
                            nullToEmpty(dstte.getTzMembership())).isEmpty()) {
                        // wire them up
                        wireUpBidirectionalTunnel(tx, srcte, dstte, srcDpn.getDPNID(), dstDpn.getDPNID(),
                                mdsalManager);
                        if (!ItmTunnelAggregationHelper.isTunnelAggregationEnabled()) {
                            // CHECK THIS -- Assumption -- One end point per Dpn per transport zone
                            break;
                        }
                    }
                }
            }
        }
    }

    private void wireUpBidirectionalTunnel(TypedReadWriteTransaction<Configuration> tx, TunnelEndPoints srcte,
        TunnelEndPoints dstte, BigInteger srcDpnId, BigInteger dstDpnId, IMdsalApiManager mdsalManager)
        throws ExecutionException, InterruptedException, OperationFailedException {
        // Setup the flow for LLDP monitoring -- PUNT TO CONTROLLER

        if (monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
            ItmUtils.addTerminatingServiceTable(tx, srcDpnId, mdsalManager);
            ItmUtils.addTerminatingServiceTable(tx, dstDpnId, mdsalManager);
        }
        // Create the forward direction tunnel
        if (!wireUp(tx, srcte, dstte, srcDpnId, dstDpnId)) {
            LOG.error("Could not build tunnel between end points {}, {} " , srcte, dstte);
        }

        // CHECK IF FORWARD IS NOT BUILT , REVERSE CAN BE BUILT
        // Create the tunnel for the reverse direction
        if (!wireUp(tx, dstte, srcte, dstDpnId, srcDpnId)) {
            LOG.error("Could not build tunnel between end points {}, {} " , dstte, srcte);
        }
    }

    private boolean wireUp(TypedWriteTransaction<Configuration> tx, TunnelEndPoints srcte, TunnelEndPoints dstte,
        BigInteger srcDpnId, BigInteger dstDpnId)
        throws ExecutionException, InterruptedException, OperationFailedException {
        // Wire Up logic
        LOG.trace("Wiring between source tunnel end points {}, destination tunnel end points {}", srcte, dstte);
        String interfaceName = srcte.getInterfaceName();
        Class<? extends TunnelTypeBase> tunType = srcte.getTunnelType();
        String tunTypeStr = srcte.getTunnelType().getName();
        // Form the trunk Interface Name

        String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(interfaceName,
                srcte.getIpAddress().stringValue(), dstte.getIpAddress().stringValue(), tunTypeStr);
        String parentInterfaceName = null;
        if (tunType.isAssignableFrom(TunnelTypeVxlan.class)) {
            parentInterfaceName = createLogicalGroupTunnel(srcDpnId, dstDpnId);
        }
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            createInternalDirectTunnels(srcte, dstte, srcDpnId, dstDpnId, tunType, trunkInterfaceName,
                    parentInterfaceName);
        } else {
            createTunnelInterface(srcte, dstte, srcDpnId, tunType, trunkInterfaceName, parentInterfaceName);
            // also update itm-state ds?
            createInternalTunnel(tx, srcDpnId, dstDpnId, tunType, trunkInterfaceName);
        }
        return true;
    }

    private void createTunnelInterface(TunnelEndPoints srcte, TunnelEndPoints dstte, BigInteger srcDpnId,
            Class<? extends TunnelTypeBase> tunType, String trunkInterfaceName, String parentInterfaceName) {
        String gateway = srcte.getIpAddress().getIpv4Address() != null ? "0.0.0.0" : "::";
        IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance(gateway);
        IpAddress gwyIpAddress =
                Objects.equals(srcte.getSubnetMask(), dstte.getSubnetMask()) ? gatewayIpObj : srcte.getGwIpAddress();
        LOG.debug(" Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {}, "
                + "source IP - {}, destination IP - {} gateway IP - {}",
                trunkInterfaceName, srcte.getInterfaceName(), srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress);
        boolean useOfTunnel = ItmUtils.falseIfNull(srcte.isOptionOfTunnel());

        List<TunnelOptions> tunOptions = ItmUtils.buildTunnelOptions(srcte, itmCfg);
        Boolean isMonitorEnabled = !tunType.isAssignableFrom(TunnelTypeLogicalGroup.class) && isTunnelMonitoringEnabled;
        Interface iface = ItmUtils.buildTunnelInterface(srcDpnId, trunkInterfaceName,
                String.format("%s %s",ItmUtils.convertTunnelTypetoString(tunType), "Trunk Interface"),
                true, tunType, srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress, srcte.getVLANID(), true,
                isMonitorEnabled, monitorProtocol, monitorInterval, useOfTunnel, parentInterfaceName, tunOptions);
        LOG.debug(" Trunk Interface builder - {} ", iface);
        InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
        LOG.debug(" Trunk Interface Identifier - {} ", trunkIdentifier);
        LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", trunkIdentifier, iface);
        ITMBatchingUtils.update(trunkIdentifier, iface, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        ItmUtils.ITM_CACHE.addInterface(iface);
    }

    private static void createLogicalTunnelInterface(BigInteger srcDpnId,
            Class<? extends TunnelTypeBase> tunType, String interfaceName) {
        Interface iface = ItmUtils.buildLogicalTunnelInterface(srcDpnId, interfaceName,
                String.format("%s %s",ItmUtils.convertTunnelTypetoString(tunType), "Interface"), true);
        InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(interfaceName);
        ITMBatchingUtils.update(trunkIdentifier, iface, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        ItmUtils.ITM_CACHE.addInterface(iface);
    }

    private static void createInternalTunnel(TypedWriteTransaction<Configuration> tx, BigInteger srcDpnId,
        BigInteger dstDpnId, Class<? extends TunnelTypeBase> tunType, String trunkInterfaceName) {
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(dstDpnId, srcDpnId, tunType));
        InternalTunnel tnl = ItmUtils.buildInternalTunnel(srcDpnId, dstDpnId, tunType, trunkInterfaceName);
        // Switching to individual transaction submit as batching latencies is causing ELAN failures.
        // Will revert when ELAN can handle this.
        // ITMBatchingUtils.update(path, tnl, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        tx.merge(path, tnl, CREATE_MISSING_PARENTS);
        ItmUtils.ITM_CACHE.addInternalTunnel(tnl);
    }

    private String createLogicalGroupTunnel(BigInteger srcDpnId, BigInteger dstDpnId) {
        boolean tunnelAggregationEnabled = ItmTunnelAggregationHelper.isTunnelAggregationEnabled();
        if (!tunnelAggregationEnabled) {
            return null;
        }
        String logicTunnelGroupName = ItmUtils.getLogicalTunnelGroupName(srcDpnId, dstDpnId);
        ItmTunnelAggregationWorker addWorker =
                new ItmTunnelAggregationWorker(logicTunnelGroupName, srcDpnId, dstDpnId, dataBroker);
        jobCoordinator.enqueueJob(logicTunnelGroupName, addWorker);
        return logicTunnelGroupName;
    }

    private static class ItmTunnelAggregationWorker implements Callable<List<ListenableFuture<Void>>> {

        private final String logicTunnelGroupName;
        private final BigInteger srcDpnId;
        private final BigInteger dstDpnId;
        private final ManagedNewTransactionRunner txRunner;

        ItmTunnelAggregationWorker(String logicGroupName, BigInteger srcDpnId, BigInteger dstDpnId, DataBroker broker) {
            this.logicTunnelGroupName = logicGroupName;
            this.srcDpnId = srcDpnId;
            this.dstDpnId = dstDpnId;
            this.txRunner = new ManagedNewTransactionRunnerImpl(broker);
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
                //The logical tunnel interface be created only when the first tunnel interface on each OVS is created
                InternalTunnel tunnel = ItmUtils.ITM_CACHE.getInternalTunnel(logicTunnelGroupName);
                if (tunnel == null) {
                    LOG.info("MULTIPLE_VxLAN_TUNNELS: add the logical tunnel group {} because a first tunnel"
                        + " interface on srcDpnId {} dstDpnId {} is created", logicTunnelGroupName, srcDpnId, dstDpnId);
                    createLogicalTunnelInterface(srcDpnId, TunnelTypeLogicalGroup.class, logicTunnelGroupName);
                    createInternalTunnel(tx, srcDpnId, dstDpnId, TunnelTypeLogicalGroup.class, logicTunnelGroupName);
                } else {
                    LOG.debug("MULTIPLE_VxLAN_TUNNELS: not first tunnel on srcDpnId {} dstDpnId {}", srcDpnId,
                            dstDpnId);
                }
            }));
        }
    }

    private void createInternalDirectTunnels(TunnelEndPoints srcte, TunnelEndPoints dstte,
            BigInteger srcDpnId, BigInteger dstDpnId, Class<? extends TunnelTypeBase> tunType,
            String trunkInterfaceName, String parentInterfaceName) throws ExecutionException, InterruptedException,
            OperationFailedException {
        IpAddress gatewayIpObj = IpAddressBuilder.getDefaultInstance("0.0.0.0");
        IpAddress gwyIpAddress = Objects.equals(srcte.getSubnetMask(), dstte.getSubnetMask())
                ? gatewayIpObj : srcte.getGwIpAddress() ;
        LOG.debug("Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {}, source IP - {},"
                        + " destination IP - {} gateway IP - {}", trunkInterfaceName, parentInterfaceName,
                srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress) ;

        boolean useOfTunnel = ItmUtils.falseIfNull(srcte.isOptionOfTunnel());

        List<TunnelOptions> tunOptions = ItmUtils.buildTunnelOptions(srcte, itmCfg);
        Boolean isMonitorEnabled = !tunType.isAssignableFrom(TunnelTypeLogicalGroup.class) && isTunnelMonitoringEnabled;
        Interface iface = ItmUtils.buildTunnelInterface(srcDpnId, trunkInterfaceName,
                String.format("%s %s",ItmUtils.convertTunnelTypetoString(srcte.getTunnelType()), "Trunk Interface"),
                true, tunType, srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress, srcte.getVLANID(),
                true, isMonitorEnabled, monitorProtocol, monitorInterval, useOfTunnel, parentInterfaceName, tunOptions);
        LOG.debug("Trunk Interface builder - {} ", iface);

        final DpnTepsStateBuilder dpnTepsStateBuilder = new DpnTepsStateBuilder();
        final DpnsTepsBuilder dpnsTepsBuilder = new DpnsTepsBuilder();
        final List<DpnsTeps> dpnTeps = new ArrayList<>();
        final List<RemoteDpns> remoteDpns = new ArrayList<>();
        dpnsTepsBuilder.withKey(new DpnsTepsKey(srcDpnId));
        dpnsTepsBuilder.setTunnelType(srcte.getTunnelType());
        dpnsTepsBuilder.setSourceDpnId(srcDpnId);
        Integer groupId = directTunnelUtils.allocateId(ITMConstants.ITM_IDPOOL_NAME, srcDpnId.toString());
        dpnsTepsBuilder.setGroupId(groupId);

        RemoteDpnsBuilder remoteDpn = new RemoteDpnsBuilder();
        remoteDpn.withKey(new RemoteDpnsKey(dstDpnId));
        remoteDpn.setDestinationDpnId(dstDpnId);
        remoteDpn.setTunnelName(trunkInterfaceName);
        remoteDpn.setMonitoringEnabled(isTunnelMonitoringEnabled);
        remoteDpn.setMonitoringInterval(monitorInterval);
        remoteDpn.setInternal(true);
        remoteDpns.add(remoteDpn.build());
        dpnsTepsBuilder.setRemoteDpns(remoteDpns);
        dpnTeps.add(dpnsTepsBuilder.build());
        dpnTepsStateBuilder.setDpnsTeps(dpnTeps);
        updateDpnTepInterfaceInfoToConfig(dpnTepsStateBuilder.build());
        addTunnelConfiguration(iface);
    }

    private static void updateDpnTepInterfaceInfoToConfig(DpnTepsState dpnTeps) {
        LOG.debug("Updating CONFIGURATION datastore with DPN-Teps {} ", dpnTeps);
        InstanceIdentifier<DpnTepsState> dpnTepsII = InstanceIdentifier.builder(DpnTepsState.class).build() ;
        ITMBatchingUtils.update(dpnTepsII, dpnTeps, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    private List<ListenableFuture<Void>> addTunnelConfiguration(Interface iface) throws ReadFailedException {
        // ITM Direct Tunnels This transaction is not being used -- CHECK
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        ParentRefs parentRefs = iface.augmentation(ParentRefs.class);
        if (parentRefs == null) {
            LOG.warn("ParentRefs for interface: {} Not Found. Creation of Tunnel OF-Port not supported"
                    + " when dpid not provided.", iface.getName());
            return Collections.emptyList();
        }

        BigInteger dpId = parentRefs.getDatapathNodeIdentifier();
        if (dpId == null) {
            LOG.warn("dpid for interface: {} Not Found. No DPID provided. Creation of OF-Port not supported.",
                    iface.getName());
            return Collections.emptyList();
        }

        LOG.info("adding tunnel configuration for {}", iface.getName());
        LOG.debug("creating bridge interfaceEntry in ConfigDS {}", dpId);
        DirectTunnelUtils.createBridgeTunnelEntryInConfigDS(dpId, iface.getName());

        // create bridge on switch, if switch is connected
        Optional<OvsBridgeRefEntry> ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(dpId);

        if (ovsBridgeRefEntry.isPresent()) {
            LOG.debug("creating bridge interface on dpn {}", dpId);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) ovsBridgeRefEntry.get()
                            .getOvsBridgeReference().getValue();
            addPortToBridge(bridgeIid, iface, iface.getName());
        }
        return Collections.singletonList(transaction.submit());
    }

    private void addPortToBridge(InstanceIdentifier<?> bridgeIid, Interface iface, String portName) {
        IfTunnel ifTunnel = iface.augmentation(IfTunnel.class);
        if (ifTunnel != null) {
            directTunnelUtils.addTunnelPortToBridge(ifTunnel, bridgeIid, iface, portName);
        }
    }
}
