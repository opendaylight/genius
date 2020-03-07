/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.rpcservice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.interfaces.InterfaceManagerService;
import org.opendaylight.genius.interfacemanager.listeners.IfIndexInterfaceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.DpnToInterfaceList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._if.indexes._interface.map.IfIndexInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.BridgeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge._interface.info.bridge.entry.BridgeInterfaceEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.DpnToInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.dpn.to._interface.list.dpn.to._interface.InterfaceNameEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpnInterfaceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEndpointIpForDpnOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetNodeconnectorIdFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetTunnelTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.get.dpn._interface.list.output.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.get.dpn._interface.list.output.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceManagerServiceImpl implements InterfaceManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManagerServiceImpl.class);

    private final DataBroker dataBroker;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final InterfaceMetaUtils interfaceMetaUtils;
    private final IfIndexInterfaceCache ifIndexInterfaceCache;

    @Inject
    public InterfaceManagerServiceImpl(@Reference final DataBroker dataBroker,
            final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            final InterfaceMetaUtils interfaceMetaUtils, final IfIndexInterfaceCache ifIndexInterfaceCache) {
        this.dataBroker = dataBroker;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.interfaceMetaUtils = interfaceMetaUtils;
        this.ifIndexInterfaceCache = ifIndexInterfaceCache;
    }

    @Override
    public ListenableFuture<GetDpidFromInterfaceOutput> getDpidFromInterface(GetDpidFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        BigInteger dpId;
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        Interface interfaceInfo = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey);
        if (interfaceInfo == null) {
            throw new IllegalArgumentException(
                    getDpidFromInterfaceErrorMessage(interfaceName, "missing Interface in Config DataStore"));
        }
        if (Tunnel.class.equals(interfaceInfo.getType())) {
            ParentRefs parentRefs = interfaceInfo.augmentation(ParentRefs.class);
            dpId = parentRefs.getDatapathNodeIdentifier();
        } else {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                    .getInterfaceState(interfaceName);
            if (ifState != null) {
                String lowerLayerIf = ifState.getLowerLayerIf().get(0);
                NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
                dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            } else {
                throw new IllegalArgumentException(
                        getDpidFromInterfaceErrorMessage(interfaceName, "missing Interface-state"));
            }
        }
        return Futures.immediateFuture(new GetDpidFromInterfaceOutputBuilder().setDpid(dpId).build());
    }

    private String getDpidFromInterfaceErrorMessage(final String interfaceName, final String dueTo) {
        return String.format("Retrieval of datapath id for the key {%s} failed due to %s",
                interfaceName, dueTo);
    }

    @Override
    public ListenableFuture<GetEndpointIpForDpnOutput> getEndpointIpForDpn(GetEndpointIpForDpnInput input) {
        BridgeEntryKey bridgeEntryKey = new BridgeEntryKey(input.getDpid());
        InstanceIdentifier<BridgeEntry> bridgeEntryInstanceIdentifier = InterfaceMetaUtils
                .getBridgeEntryIdentifier(bridgeEntryKey);
        BridgeEntry bridgeEntry = interfaceMetaUtils.getBridgeEntryFromConfigDS(bridgeEntryInstanceIdentifier);
        // local ip of any of the bridge interface entry will be the dpn end
        // point ip
        BridgeInterfaceEntry bridgeInterfaceEntry = bridgeEntry.getBridgeInterfaceEntry().get(0);
        InterfaceKey interfaceKey = new InterfaceKey(bridgeInterfaceEntry.getInterfaceName());
        Interface interfaceInfo = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey);
        IfTunnel tunnel = interfaceInfo.augmentation(IfTunnel.class);
        return Futures.immediateFuture(new GetEndpointIpForDpnOutputBuilder()
                .setLocalIps(Collections.singletonList(tunnel.getTunnelSource())).build());
    }

    @Override
    public ListenableFuture<GetEgressInstructionsForInterfaceOutput> getEgressInstructionsForInterface(
            GetEgressInstructionsForInterfaceInput input) {
        List<Instruction> instructions = IfmUtil.getEgressInstructionsForInterface(input.getIntfName(),
                input.getTunnelKey(), interfaceManagerCommonUtils, false);
        return Futures.immediateFuture(
                new GetEgressInstructionsForInterfaceOutputBuilder().setInstruction(instructions).build());
    }

    @Override
    public ListenableFuture<GetInterfaceTypeOutput> getInterfaceType(GetInterfaceTypeInput input) {
        String interfaceName = input.getIntfName();
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        Interface interfaceInfo = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey);
        if (interfaceInfo == null) {
            throw new IllegalStateException(String.format("getInterfaceType() Retrieval of Interface Type for "
                    + "the key {%s} failed due to missing Interface in Config DataStore", interfaceName));
        }
        return Futures.immediateFuture(
                new GetInterfaceTypeOutputBuilder().setInterfaceType(interfaceInfo.getType()).build());
    }

    @Override
    public ListenableFuture<GetTunnelTypeOutput> getTunnelType(GetTunnelTypeInput input) {
        String interfaceName = input.getIntfName();
        InterfaceKey interfaceKey = new InterfaceKey(interfaceName);
        Interface interfaceInfo = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey);
        if (interfaceInfo == null) {
            throw new IllegalArgumentException(String.format(
                    "Retrieval of Tunnel Type for the key {%s} failed due to missing Interface in Config DataStore",
                    interfaceName));
        }
        if (Tunnel.class.equals(interfaceInfo.getType())) {
            IfTunnel tnl = interfaceInfo.augmentation(IfTunnel.class);
            Class<? extends TunnelTypeBase> tunType = tnl.getTunnelInterfaceType();
            return Futures.immediateFuture(new GetTunnelTypeOutputBuilder().setTunnelType(tunType).build());
        } else {
            throw new IllegalArgumentException("Retrieval of interface type failed for key: " + interfaceName);
        }
    }

    @Override
    public ListenableFuture<GetEgressActionsForInterfaceOutput> getEgressActionsForInterface(
            GetEgressActionsForInterfaceInput input) {
        List<Action> actionsList = IfmUtil.getEgressActionsForInterface(input.getIntfName(), input.getTunnelKey(),
                input.getActionKey(), interfaceManagerCommonUtils, false);
        // TODO as above, simplify the success case later, as we have the failure case below
        return Futures
                .immediateFuture(new GetEgressActionsForInterfaceOutputBuilder().setAction(actionsList).build());
    }

    @Override
    public ListenableFuture<GetPortFromInterfaceOutput> getPortFromInterface(GetPortFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        BigInteger dpId = null;
        long portNo = 0;
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(interfaceName);
        if (ifState != null) {
            String lowerLayerIf = ifState.getLowerLayerIf().get(0);
            NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
            dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
            portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
            String phyAddress = ifState.getPhysAddress().getValue();
            // FIXME Assuming portName and interfaceName are same
            // TODO as above, simplify the success case later, as we have the failure case below
            return Futures.immediateFuture(new GetPortFromInterfaceOutputBuilder().setDpid(dpId)
                    .setPortname(interfaceName).setPortno(portNo).setPhyAddress(phyAddress).build());
        } else {
            throw new IllegalArgumentException(
                    "Retrieval of Port for the key " + interfaceName + " failed due to missing Interface state");
        }
    }

    @Override
    public ListenableFuture<GetNodeconnectorIdFromInterfaceOutput> getNodeconnectorIdFromInterface(
            GetNodeconnectorIdFromInterfaceInput input) {
        String interfaceName = input.getIntfName();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(interfaceName);
        String lowerLayerIf = ifState.getLowerLayerIf().get(0);
        NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
        // TODO as above, simplify the success case later, as we have the failure case below
        return Futures.immediateFuture(
                new GetNodeconnectorIdFromInterfaceOutputBuilder().setNodeconnectorId(nodeConnectorId).build());
    }

    @Override
    public ListenableFuture<GetInterfaceFromIfIndexOutput> getInterfaceFromIfIndex(
            GetInterfaceFromIfIndexInput input) {
        Integer ifIndex = input.getIfIndex();
        Optional<IfIndexInterface> ifIndexesInterface;
        try {
            ifIndexesInterface = ifIndexInterfaceCache.get(ifIndex);
        } catch (ReadFailedException e) {
            return Futures.immediateFailedFuture(e);
        }

        if (!ifIndexesInterface.isPresent()) {
            return Futures.immediateFailedFuture(new IllegalArgumentException(
                    "Could not find " + ifIndex + " in OperationalDS"));
        }
        String interfaceName = ifIndexesInterface.get().getInterfaceName();
        // TODO as above, simplify the success case later, as we have the failure case below
        return Futures.immediateFuture(
                new GetInterfaceFromIfIndexOutputBuilder().setInterfaceName(interfaceName).build());
    }

    @Override
    public ListenableFuture<GetDpnInterfaceListOutput> getDpnInterfaceList(GetDpnInterfaceListInput input) {
        BigInteger dpnid = input.getDpid();
        InstanceIdentifier<DpnToInterface> id = InstanceIdentifier.builder(DpnToInterfaceList.class)
                .child(DpnToInterface.class, new DpnToInterfaceKey(dpnid)).build();
        Optional<DpnToInterface> entry = IfmUtil.read(LogicalDatastoreType.OPERATIONAL, id, dataBroker);
        if (!entry.isPresent()) {
            LOG.warn("Could not find Operational DpnToInterface info for DPN {}. Returning empty list", dpnid);
            return buildEmptyInterfaceListResult();
        }

        List<InterfaceNameEntry> interfaceNameEntries = entry.get().getInterfaceNameEntry();
        if (interfaceNameEntries == null || interfaceNameEntries.isEmpty()) {
            LOG.debug("No Interface list found in Operational for DPN {}", dpnid);
            return buildEmptyInterfaceListResult();
        }
        List<Interfaces> interfaceList = new ArrayList<>();
        interfaceNameEntries.forEach(
            (interfaceNameEntry) -> {
                InterfacesBuilder intf = new InterfacesBuilder()
                    .setInterfaceName(interfaceNameEntry.getInterfaceName())
                    .setInterfaceType(interfaceNameEntry.getInterfaceType());
                interfaceList.add(intf.build());
            });
        // TODO as above, simplify the success case later, as we have the failure case below
        return Futures
                .immediateFuture(new GetDpnInterfaceListOutputBuilder().setInterfaces(interfaceList).build());
    }

    private ListenableFuture<GetDpnInterfaceListOutput> buildEmptyInterfaceListResult() {
        GetDpnInterfaceListOutput emptyListResult =
            new GetDpnInterfaceListOutputBuilder().setInterfaces(Collections.emptyList()).build();
        return Futures.immediateFuture(emptyListResult);
    }
}
