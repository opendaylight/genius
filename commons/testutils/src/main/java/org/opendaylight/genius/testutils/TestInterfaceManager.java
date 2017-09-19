/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionPushVlan;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldVlanVid;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelDestinationIp;
import org.opendaylight.genius.mdsalutil.actions.ActionSetTunnelSourceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IInterfaceManager implementation for tests.
 *
 * @author Michael Vorburger
 */
public abstract class TestInterfaceManager implements IInterfaceManager {

    // Implementation similar to e.g. the org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager
    private static final Logger LOG = LoggerFactory.getLogger(TestInterfaceManager.class);
    public static final int REG6_START_INDEX = 0;
    public static final int REG6_END_INDEX = 31;

    public static TestInterfaceManager newInstance() {
        TestInterfaceManager testInterfaceManager = Mockito.mock(TestInterfaceManager.class, realOrException());
        testInterfaceManager.interfaceInfos = new ConcurrentHashMap<>();
        testInterfaceManager.interfaces = new ConcurrentHashMap<>();
        return testInterfaceManager;
    }

    public static TestInterfaceManager newInstance(DataBroker dataBroker) {
        TestInterfaceManager testInterfaceManager = Mockito.mock(TestInterfaceManager.class, realOrException());
        testInterfaceManager.interfaceInfos = new ConcurrentHashMap<>();
        testInterfaceManager.externalInterfaces = new ConcurrentHashMap<>();
        testInterfaceManager.dataBroker = dataBroker;
        return testInterfaceManager;
    }

    private Map<String, InterfaceInfo> interfaceInfos;
    private Map<String, Interface> interfaces;
    private Map<String, Boolean> externalInterfaces = new ConcurrentHashMap<>();
    private DataBroker dataBroker;

    public void addInterface(DataBroker dataBroker, InterfaceDetails interfaceDetails)
            throws TransactionCommitFailedException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceDetails.getIfaceIid(), interfaceDetails.getIface());
        tx.submit().checkedGet();
        tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, interfaceDetails.getIfStateId(), interfaceDetails.getIfState());
        tx.submit().checkedGet();
        String interfaceName = interfaceDetails.getName();
    }


    public void addTunnelInterface(DataBroker dataBroker, TunnelInterfaceDetails interfaceDetails)
            throws TransactionCommitFailedException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceDetails.getIfaceIid(), interfaceDetails.getIface());
        tx.submit().checkedGet();
        tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, interfaceDetails.getIfStateId(), interfaceDetails.getIfState());
        tx.submit().checkedGet();
    }

    @Override
    public InterfaceInfo getInterfaceInfo(String interfaceName) {
        InterfaceInfo interfaceInfo = interfaceInfos.get(interfaceName);
        if (interfaceInfo == null) {
            throw new IllegalStateException(
                    "must addInterfaceInfo() to TestInterfaceManager before getInterfaceInfo: " + interfaceName);
        }
        return interfaceInfo;
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName) {
        return getInterfaceInfo(interfaceName);
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(
            String interfaceName, InterfaceInfo.InterfaceType interfaceType) {
        return interfaceInfos.get(interfaceName);
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDSCache(String interfaceName) {
        return getInterfaceInfo(interfaceName);
    }

    @Override
    public Interface getInterfaceInfoFromConfigDataStore(String interfaceName) {
        Interface iface = interfaces.get(interfaceName);
        if (iface == null) {
            throw new IllegalStateException(
                    "must addInterface() to TestInterfaceManager before getInterfaceInfoFromConfigDataStore: "
                    + interfaceName);
        }
        return iface;

    }

    public static Interface getInterfaceFromConfigDS(InterfaceKey interfaceKey, DataBroker dataBroker) {
        return getInterfaceFromConfigDS(interfaceKey.getName(), dataBroker);
    }

    public static Interface getInterfaceFromConfigDS(String interfaceName, DataBroker dataBroker) {
        InstanceIdentifier<Interface> interfaceId = getInterfaceIdentifier(new InterfaceKey(interfaceName));
        Optional<Interface> interfaceOptional = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, interfaceId,
                dataBroker);
        if (interfaceOptional.isPresent()) {
            return interfaceOptional.get();
        }
        return null;
    }

    public static InstanceIdentifier<Interface> getInterfaceIdentifier(InterfaceKey interfaceKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> interfaceInstanceIdentifierBuilder = InstanceIdentifier
                .builder(Interfaces.class).child(Interface.class, interfaceKey);
        return interfaceInstanceIdentifierBuilder.build();
    }

    public static InterfaceInfo.InterfaceType getInterfaceType(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
                                                                       .yang.ietf.interfaces.rev140508
                                                                       .interfaces.Interface iface) {
        InterfaceInfo.InterfaceType interfaceType = InterfaceInfo.InterfaceType.UNKNOWN_INTERFACE;
        Class ifType = iface.getType();
        if (ifType.isAssignableFrom(L2vlan.class)) {
            interfaceType = InterfaceInfo.InterfaceType.VLAN_INTERFACE;
        }
        return interfaceType;
    }

    @Override
    public List<ActionInfo> getInterfaceEgressActions(String ifName) {
        return getEgressActionInfosForInterface(ifName, 0, dataBroker, false);
    }

    Map<Pair<String, Long>, List<Action>> egressMap = new ConcurrentHashMap<>();

    public void addEgressActions(String ifName, Long tunnelKey, List<Action> actions) {
        if (tunnelKey == null) {
            tunnelKey = new Long(0);
        }
        egressMap.put(Pair.of(ifName, tunnelKey), actions);
    }


    public static List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, int actionKeyStart,
                                                                    DataBroker dataBroker, Boolean isDefaultEgress) {
        return getEgressActionInfosForInterface(interfaceName, null, actionKeyStart, dataBroker, isDefaultEgress);
    }

    /**
     * Returns a list of Actions to be taken when sending a packet over an
     * interface.
     *
     * @param interfaceName
     *            name of the interface
     * @param tunnelKey
     *            Optional.
     * @param actionKeyStart
     *            action key
     * @param dataBroker
     *            databroker
     * @return list of actions
     */
    public static List<ActionInfo> getEgressActionInfosForInterface(String interfaceName, Long tunnelKey,
                                                                    int actionKeyStart, DataBroker dataBroker,
                                                                    Boolean isDefaultEgress) {
        Interface interfaceInfo = getInterfaceFromConfigDS(new InterfaceKey(interfaceName),
                dataBroker);
        if (interfaceInfo == null) {
            throw new NullPointerException("Interface information not present in config DS for " + interfaceName);
        }
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceStateHelper
                .getInterfaceState(interfaceName, dataBroker);
        if (ifState == null) {
            throw new NullPointerException("Interface information not present in oper DS for " + interfaceName);
        }
        String lowerLayerIf = ifState.getLowerLayerIf().get(0);
        NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
        String portNo = InterfaceIidHelper.getPortNoFromNodeConnectorId(nodeConnectorId);

        InterfaceInfo.InterfaceType ifaceType = getInterfaceType(interfaceInfo);
        return getEgressActionInfosForInterface(interfaceInfo, portNo, ifaceType, tunnelKey, actionKeyStart,
                isDefaultEgress, ifState.getIfIndex(), 0);
    }


    /**
     * Returns the list of egress actions for a given interface.
     *
     * @param interfaceInfo the interface to look up
     * @param portNo port number
     * @param ifaceType the type of the interface
     * @param tunnelKey the tunnel key
     * @param actionKeyStart the start for the first key assigned for the new actions
     * @param isDefaultEgress if it is the default egress
     * @param ifIndex interface index
     * @return list of actions for the interface
     */
    // The following suppression is for javac, not for checkstyle
    @SuppressWarnings("fallthrough")
    public static List<ActionInfo> getEgressActionInfosForInterface(Interface interfaceInfo, String portNo,
                                                                    InterfaceInfo.InterfaceType ifaceType,
                                                                    Long tunnelKey, int actionKeyStart,
                                                                    boolean isDefaultEgress,
                                                                    int ifIndex, long groupId) {
        List<ActionInfo> result = new ArrayList<>();
        switch (ifaceType) {
            case MPLS_OVER_GRE:
                // fall through
            case GRE_TRUNK_INTERFACE:
                if (!isDefaultEgress) {
                    // TODO tunnel_id to encode GRE key, once it is supported
                    // Until then, tunnel_id should be "cleaned", otherwise it
                    // stores the value coming from a VXLAN tunnel
                    if (tunnelKey == null) {
                        tunnelKey = 0L;
                    }
                }
                // fall through
            case VXLAN_TRUNK_INTERFACE:
                if (!isDefaultEgress) {
                    if (tunnelKey != null) {
                        result.add(new ActionSetFieldTunnelId(actionKeyStart++, BigInteger.valueOf(tunnelKey)));
                    }
                } else {
                    // For OF Tunnels default egress actions need to set tunnelIps
                    IfTunnel ifTunnel = interfaceInfo.getAugmentation(IfTunnel.class);
                    if (BooleanUtils.isTrue(ifTunnel.isTunnelRemoteIpFlow()
                            && ifTunnel.getTunnelDestination() != null)) {
                        result.add(new ActionSetTunnelDestinationIp(actionKeyStart++, ifTunnel.getTunnelDestination()));
                    }
                    if (BooleanUtils.isTrue(ifTunnel.isTunnelSourceIpFlow()
                            && ifTunnel.getTunnelSource() != null)) {
                        result.add(new ActionSetTunnelSourceIp(actionKeyStart++, ifTunnel.getTunnelSource()));
                    }
                }
                // fall through
            case VLAN_INTERFACE:
                if (isDefaultEgress) {
                    IfL2vlan vlanIface = interfaceInfo.getAugmentation(IfL2vlan.class);
                    LOG.trace("get egress actions for l2vlan interface: {}", vlanIface);
                    boolean isVlanTransparent = false;
                    int vlanVid = 0;
                    if (vlanIface != null) {
                        vlanVid = vlanIface.getVlanId() == null ? 0 : vlanIface.getVlanId().getValue();
                        isVlanTransparent = vlanIface.getL2vlanMode() == IfL2vlan.L2vlanMode.Transparent;
                    }
                    if (vlanVid != 0 && !isVlanTransparent) {
                        result.add(new ActionPushVlan(actionKeyStart++));
                        result.add(new ActionSetFieldVlanVid(actionKeyStart++, vlanVid));
                    }
                    result.add(new ActionOutput(actionKeyStart++, new Uri(portNo)));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                break;
            case LOGICAL_GROUP_INTERFACE:
                if (isDefaultEgress) {
                    result.add(new ActionGroup(groupId));
                } else {
                    addEgressActionInfosForInterface(ifIndex, actionKeyStart, result);
                }
                break;

            default:
                LOG.warn("Interface Type {} not handled yet", ifaceType);
                break;
        }
        return result;
    }

    public static void addEgressActionInfosForInterface(int ifIndex, int actionKeyStart, List<ActionInfo> result) {
        long regValue = MetaDataUtil.getReg6ValueForLPortDispatcher(ifIndex, NwConstants.DEFAULT_SERVICE_INDEX);
        result.add(new ActionRegLoad(actionKeyStart++, NxmNxReg6.class, REG6_START_INDEX,
                REG6_END_INDEX, regValue));
        result.add(new ActionNxResubmit(actionKeyStart++, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE));
    }


    public Future<RpcResult<GetDpidFromInterfaceOutput>> getDpidFromInterface(
            GetDpidFromInterfaceInput getDpidFromInterfaceInput) {
        BigInteger dpnId = getDpnForInterface(getDpidFromInterfaceInput.getIntfName());
        GetDpidFromInterfaceOutput output = new GetDpidFromInterfaceOutputBuilder().setDpid(dpnId).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public BigInteger getDpnForInterface(String interfaceName) {
        return interfaceInfos.get(interfaceName).getDpId();
    }

    @Override
    public BigInteger getDpnForInterface(Interface intrface) {
        return interfaceInfos.get(intrface.getName()).getDpId();
    }

    @Override
    public boolean isExternalInterface(String interfaceName) {
        return interfaceInfos.containsKey(interfaceName);
    }
}
