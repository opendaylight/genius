/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;
import java.math.BigInteger;
import java.util.List;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.VlanInterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.tunnel.optional.params.TunnelOptions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InterfaceIidHelper {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMgrTestHelper.class);

    public static InstanceIdentifier<Interface> buildId(String interfaceName) {
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> idBuilder =
                InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName));
        InstanceIdentifier<Interface> id = idBuilder.build();
        return id;
    }


    public static Interface buildTunnelInterface(BigInteger dpn, String ifName, String desc, boolean enabled,
                                                 Class<? extends TunnelTypeBase> tunType, IpAddress localIp,
                                                 IpAddress remoteIp, IpAddress gatewayIp, Integer vlanId,
                                                 boolean internal, Boolean monitorEnabled,
                                                 Class<? extends TunnelMonitoringTypeBase> monitorProtocol,
                                                 Integer monitorInterval, boolean useOfTunnel, String parentIfaceName,
                                                 List<TunnelOptions> tunnelOptions) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.InterfaceBuilder builder = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                .interfaces.rev140508.interfaces.InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType(Tunnel.class);
        ParentRefs parentRefs =
                new ParentRefsBuilder().setDatapathNodeIdentifier(dpn).setParentInterface(parentIfaceName).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        Long monitoringInterval = null;
        if (vlanId > 0) {
            IfL2vlan l2vlan = new IfL2vlanBuilder().setVlanId(new VlanId(vlanId)).build();
            builder.addAugmentation(IfL2vlan.class, l2vlan);
        }
        if (monitorInterval != null) {
            monitoringInterval = monitorInterval.longValue();
        }

        IfTunnel tunnel = new IfTunnelBuilder().setTunnelDestination(remoteIp).setTunnelGateway(gatewayIp)
                .setTunnelSource(localIp).setTunnelInterfaceType(tunType).setInternal(internal)
                .setMonitorEnabled(monitorEnabled).setMonitorProtocol(monitorProtocol)
                .setMonitorInterval(monitoringInterval).setTunnelRemoteIpFlow(useOfTunnel)
                .setTunnelOptions(tunnelOptions)
                .build();
        builder.addAugmentation(IfTunnel.class, tunnel);
        return builder.build();
    }

    public static NodeConnectorId getNodeConnectorIdFromInterface(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
                                                                          .yang.ietf.interfaces.rev140508.interfaces
                                                                          .state.Interface ifState) {
        if (ifState != null) {
            List<String> ofportIds = ifState.getLowerLayerIf();
            return new NodeConnectorId(ofportIds.get(0));
        }
        return null;
    }

    public static InterfaceInfo getInterfaceInfo(String interfaceName, Interface intf,
                                                 BigInteger dpId,
                                                 Integer portNo,
                                                 org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                         .interfaces.rev140508.interfaces.state.Interface ifState) {

        InterfaceInfo interfaceInfo = new InterfaceInfo(interfaceName);
        InterfaceInfo.InterfaceType interfaceType = InterfaceInfo.InterfaceType.VLAN_INTERFACE;
        NodeConnectorId ncId = getNodeConnectorIdFromInterface(ifState);
        if (ncId != null) {
            dpId = getDpnFromNodeConnectorId(ncId);
            portNo = Integer.parseInt(getPortNoFromNodeConnectorId(ncId));
        }
        if (interfaceType == InterfaceInfo.InterfaceType.VLAN_INTERFACE) {
            interfaceInfo = getVlanInterfaceInfo(intf, dpId);
        } else if (interfaceType == InterfaceInfo.InterfaceType.UNKNOWN_INTERFACE) {
            return null;
        }
        InterfaceInfo.InterfaceOpState opState;
        if (ifState.getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                .interfaces.rev140508.interfaces.state.Interface.OperStatus.Up) {
            opState = InterfaceInfo.InterfaceOpState.UP;
        } else if (ifState.getOperStatus() == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                .interfaces.rev140508.interfaces.state.Interface.OperStatus.Down) {
            opState = InterfaceInfo.InterfaceOpState.DOWN;
        } else {
            opState = InterfaceInfo.InterfaceOpState.UNKNOWN;
        }
        interfaceInfo.setDpId(dpId);
        interfaceInfo.setPortNo(portNo);
        interfaceInfo.setAdminState(intf.isEnabled()
                ? InterfaceInfo.InterfaceAdminState.ENABLED : InterfaceInfo.InterfaceAdminState.DISABLED);
        interfaceInfo.setInterfaceName(interfaceName);
        Integer lportTag = ifState.getIfIndex();
        interfaceInfo.setInterfaceTag(lportTag);
        interfaceInfo.setInterfaceType(interfaceType);
        interfaceInfo.setGroupId(getGroupId(lportTag, interfaceType));
        interfaceInfo.setOpState(opState);
        PhysAddress phyAddress = ifState.getPhysAddress();
        if (phyAddress != null) {
            interfaceInfo.setMacAddress(ifState.getPhysAddress().getValue());
        }
        return interfaceInfo;
    }

    public static Interface getInterface(String interfaceName, String parentName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.InterfaceBuilder interfaceBuilder
                = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.InterfaceBuilder();
        interfaceBuilder.setDescription(interfaceName).setName(interfaceName);
        interfaceBuilder.setType(L2vlan.class);
        ParentRefs parentRefsBuilder = new ParentRefsBuilder().setParentInterface(parentName).build();
        return interfaceBuilder.addAugmentation(ParentRefs.class,parentRefsBuilder).build();
    }

    public static BigInteger getDpnFromNodeConnectorId(NodeConnectorId portId) {
        String[] split = portId.getValue().split(":");
        return new BigInteger(split[1]);
    }

    public static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        String[] split = portId.getValue().split(":");
        return split[2];
    }

    public static VlanInterfaceInfo getVlanInterfaceInfo(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                                                            .interfaces.rev140508.interfaces.Interface iface,
                                                            BigInteger dpId) {
        byte vlanId = 0;
        String portName = null;
        IfL2vlan vlanIface = (IfL2vlan)iface.getAugmentation(IfL2vlan.class);
        ParentRefs parentRefs = (ParentRefs)iface.getAugmentation(ParentRefs.class);
        if (parentRefs != null && parentRefs.getParentInterface() != null) {
            portName = parentRefs.getParentInterface();
        } else {
            LOG.warn("Portname set to null since parentRef is Null");
        }

        VlanInterfaceInfo vlanInterfaceInfo = new VlanInterfaceInfo(dpId, portName, vlanId);
        if (vlanIface != null) {
            short vlanId1 = vlanIface.getVlanId() == null ? 0 : vlanIface.getVlanId().getValue().shortValue();
            IfL2vlan.L2vlanMode l2VlanMode = vlanIface.getL2vlanMode();
            if (l2VlanMode == IfL2vlan.L2vlanMode.Transparent) {
                vlanInterfaceInfo.setVlanTransparent(true);
            }

            if (l2VlanMode == IfL2vlan.L2vlanMode.NativeUntagged) {
                vlanInterfaceInfo.setUntaggedVlan(true);
            }

            vlanInterfaceInfo.setVlanId(vlanId1);
        }

        return vlanInterfaceInfo;
    }


    public static long getGroupId(int ifIndex, InterfaceInfo.InterfaceType infType) {
        return infType == InterfaceInfo.InterfaceType.LOGICAL_GROUP_INTERFACE ? getLogicalTunnelSelectGroupId(ifIndex)
                : 0L;
    }

    public static long getLogicalTunnelSelectGroupId(int lportTag) {
        return 300000L + (long)lportTag;
    }

}
