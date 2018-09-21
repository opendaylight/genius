/*
 * Copyright (c) 2016 - 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.shell;

import java.math.BigInteger;
import java.util.Formatter;

import org.apache.felix.service.command.CommandSession;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo.InterfaceOpState;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;

public final class IfmCLIUtil {
    private static final String VLAN_OUTPUT_FORMAT_LINE1 = "%-55s";
    private static final String VLAN_OUTPUT_FORMAT = "%-24s %-20s %-15s %-24s";
    private static final String VXLAN_OUTPUT_FORMAT = "%-24s %-24s %-18s %-5s";
    private static final String VXLAN_OUTPUT_FORMAT_LINE1 = "%-49s %-45s";
    private static final String IF_TP_OUTPUT_FORMAT = "%-24s";
    private static final String TP_OUTPUT_FORMAT = "%-24s %-20s %-8s";
    private static final String BFD_OUTPUT_FORMAT = "%-24s %-8s";
    private static final String TP_VXLAN_OUTPUT_FORMAT_LINE1 = "local_ip:%-24s remote_ip:%-24s key:%-12s";
    private static final String BRIDGE_PORTS_OUTPUT_FORMAT_HEADER = "%-8s %-20s";
    private static final String TP_OUTPUT_FORMAT_LINE2 = "%-12s";
    private static final String UNSET = "N/A";
    private static final int IFTYPE_LEN = "InterfaceType".length();

    private IfmCLIUtil() {
    }

    static void showVlanHeaderOutput(CommandSession session) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt.format(VLAN_OUTPUT_FORMAT_LINE1, "Name"));
        sb.setLength(0);
        session.getConsole().println(fmt.format(VLAN_OUTPUT_FORMAT, "", "Dpn", "PortName",
                "Vlan-Id"));
        sb.setLength(0);
        session.getConsole().println(fmt.format(VLAN_OUTPUT_FORMAT, "Tag", "PortNo",
                "AdmState", "OpState"));
        sb.setLength(0);
        session.getConsole().println(fmt.format(VLAN_OUTPUT_FORMAT, "Description", "", "", ""));
        sb.setLength(0);
        session.getConsole().println(fmt
            .format("--------------------------------------------------------------------------------"));
        sb.setLength(0);
        fmt.close();
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    static void showVlanOutput(InterfaceInfo ifaceInfo, Interface iface) {
        IfL2vlan l2vlan = iface.getAugmentation(IfL2vlan.class);
        int vlanId = l2vlan != null ? l2vlan.getVlanId() != null ? l2vlan.getVlanId().getValue() : 0 : 0;
        System.out.println(String.format("%-55s", iface.getName()));
        System.out.println(String.format("%-24s %-20s %-15s %-24s", "",
            ifaceInfo == null ? UNSET : ifaceInfo.getDpId(),
            ifaceInfo == null ? UNSET : ifaceInfo.getPortName(), vlanId));
        System.out.println(String.format("%-24s %-20s %-15s %-24s",
            ifaceInfo == null ? UNSET : ifaceInfo.getInterfaceTag(),
            ifaceInfo == null ? UNSET : ifaceInfo.getPortNo(),
            ifaceInfo == null ? UNSET : ifaceInfo.getAdminState(),
            ifaceInfo == null ? UNSET : ifaceInfo.getOpState()));
        System.out.println(String.format("%-24s %-20s %-15s %-24s", iface.getDescription(), "", "", ""));
        System.out.println();
    }

    static void showVxlanHeaderOutput(CommandSession session) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt
                .format(VXLAN_OUTPUT_FORMAT_LINE1, "Name", "Description"));
        sb.setLength(0);
        session.getConsole().println(fmt.format(VXLAN_OUTPUT_FORMAT, "Local IP",
                "Remote IP", "Gateway IP", "AdmState"));
        sb.setLength(0);
        session.getConsole().println(fmt.format(VXLAN_OUTPUT_FORMAT, "OpState", "Parent",
                "Tag", ""));
        sb.setLength(0);
        session.getConsole().println(fmt
                .format("--------------------------------------------------------------------------------"));
        fmt.close();
    }

    static void showVxlanOutput(Interface iface, InterfaceInfo interfaceInfo, CommandSession session) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt.format(VXLAN_OUTPUT_FORMAT_LINE1,
                iface.getName(),
                iface.getDescription() == null ? UNSET : iface.getDescription()));
        sb.setLength(0);
        IfTunnel ifTunnel = iface.getAugmentation(IfTunnel.class);
        session.getConsole().println(fmt.format(VXLAN_OUTPUT_FORMAT,
                new String(ifTunnel.getTunnelSource().getValue()),
                new String(ifTunnel.getTunnelDestination().getValue()),
                ifTunnel.getTunnelGateway() == null ? UNSET : new String(ifTunnel.getTunnelGateway().getValue()),
                interfaceInfo == null ? InterfaceInfo.InterfaceAdminState.DISABLED : interfaceInfo.getAdminState()));
        sb.setLength(0);
        ParentRefs parentRefs = iface.getAugmentation(ParentRefs.class);
        session.getConsole().println(fmt.format(VXLAN_OUTPUT_FORMAT + "\n",
                interfaceInfo == null ? InterfaceOpState.DOWN : interfaceInfo.getOpState(),
                String.format("%s/%s", parentRefs.getDatapathNodeIdentifier(),
                        iface.getName()),
                interfaceInfo == null ? UNSET : interfaceInfo.getInterfaceTag(), ""));
        fmt.close();
    }

    //TODO: Capture more information and cleaner display for TerminationPoint
    static void showInterfaceToTpHeader(CommandSession session) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt
                        .format(TP_OUTPUT_FORMAT, "PortName", "Type", "OFPort"));
        sb.setLength(0);
        session.getConsole().println(fmt
                .format(IF_TP_OUTPUT_FORMAT, "InterfaceName"));
        sb.setLength(0);
        session.getConsole().println(fmt
                .format("--------------------------------------------------------------------------------"));
        fmt.close();
    }

    static void showInterfaceToTpOutput(String ifName, OvsdbTerminationPointAugmentation port,
                                        CommandSession session) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt
                .format(TP_OUTPUT_FORMAT, port.getName(), getPortTypeStr(port), port.getOfport()));
        sb.setLength(0);
        session.getConsole().println(fmt.format(IF_TP_OUTPUT_FORMAT, ifName));
        sb.setLength(0);
        fmt.close();
    }

    static void printBfdCachesHeader(CommandSession session) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt.format(BFD_OUTPUT_FORMAT, "InterfaceName", "OperStatus"));
        sb.setLength(0);
        session.getConsole().println(fmt
                .format("--------------------------------------------------------------------------------"));
        fmt.close();
    }

    static void printBfdCachesOutput(String ifName, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .interfaces.rev140508.interfaces.state.Interface.OperStatus st,
                                       CommandSession session) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt.format(BFD_OUTPUT_FORMAT, ifName, st.getName()));
        fmt.close();
    }

    static void showBridgePortsHeader(CommandSession session, BigInteger dpnId) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt
                .format(BRIDGE_PORTS_OUTPUT_FORMAT_HEADER, "DPN-ID", dpnId));
        sb.setLength(0);
        session.getConsole().println(fmt
                .format("--------------------------------------------------------------------------------"));
        sb.setLength(0);
        session.getConsole().println(fmt
                        .format(TP_OUTPUT_FORMAT, "PortName", "Type", "OFPort"));
        sb.setLength(0);
        session.getConsole().println(fmt
                        .format(TP_OUTPUT_FORMAT_LINE2, "PortDetails"));
        sb.setLength(0);
        session.getConsole().println(fmt
                        .format("--------------------------------------------------------------------------------"));
        fmt.close();
    }

    static void showBridgePortsOutput(CommandSession session, OvsdbTerminationPointAugmentation port) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        session.getConsole().println(fmt
            .format(TP_OUTPUT_FORMAT, port.getName(), getPortTypeStr(port), port.getOfport()));
        sb.setLength(0);
        session.getConsole().println(getPortDetails(port));
        sb.setLength(0);
        fmt.close();
    }

    private static String getPortTypeStr(OvsdbTerminationPointAugmentation port) {
        String portType = port.getInterfaceType().getSimpleName();
        // Skip the InterfaceType part
        if (portType.startsWith("InterfaceType")) {
            return portType.substring(IFTYPE_LEN);
        } else {
            return portType;
        }
    }

    private static String getPortDetails(OvsdbTerminationPointAugmentation port) {
        if (SouthboundUtils.isInterfaceTypeTunnel(port.getInterfaceType())) {
            String remoteIp = UNSET;
            String localIp = UNSET;
            String key = UNSET;
            for (Options portOption: port.getOptions()) {
                if (portOption.getOption().equals("local_ip")) {
                    localIp = portOption.getValue();
                } else if (portOption.getOption().equals("remote_ip")) {
                    remoteIp = portOption.getValue();
                } else if (portOption.getOption().equals("key")) {
                    key = portOption.getValue();
                }
            }
            return String.format(TP_VXLAN_OUTPUT_FORMAT_LINE1, localIp, remoteIp, key);
        }
        return String.format(TP_OUTPUT_FORMAT_LINE2, UNSET);
    }
}
