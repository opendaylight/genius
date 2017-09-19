/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.testutils;

import java.math.BigInteger;
import java.util.Random;

import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TunnelInterfaceDetails {
    String src;
    String dst;
    String dstIp;
    int lportTag;
    int portno;
    String trunkInterfaceName;
    String mac;
    String portName;
    Random random = new Random();
    Interface iface;
    InstanceIdentifier<Interface> ifaceIid;
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState;
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId;

    public TunnelInterfaceDetails(String src, int portno, String dst, String srcIp, String dstIp, int lportTag) {
        this.dst = dst;
        this.dstIp = dstIp;
        this.lportTag = lportTag;
        this.portno = portno;
        this.src = src;
        this.mac = getDummyMac();
        this.portName = "port" + random.nextInt(9) + "" + random.nextInt(9) + "" + random.nextInt(9);

        BigInteger srcDpnId = new BigInteger(src);
        boolean isDpn = false;
        if (isDpnDestination()) {
            isDpn = true;
        }
        Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
        final Class<? extends TunnelMonitoringTypeBase> defaultMonitorProtocol = TunnelMonitoringTypeBfd.class;

        //String interfaceName = ItmUtils.getInterfaceName(new BigInteger(src), portName, 0);
        String interfaceName = String.format("%s:%s:%s", new Object[]{new BigInteger(src), portName, 0});
        trunkInterfaceName = getTrunkInterfaceName(interfaceName);
        IpAddress srcIpAddr = new IpAddress(new Ipv4Address(srcIp));
        IpAddress dstIpAddr = new IpAddress(new Ipv4Address(dstIp));
        IpAddress gwIpAddr = new IpAddress(new Ipv4Address("0.0.0.0"));
        iface = InterfaceIidHelper.buildTunnelInterface(srcDpnId, trunkInterfaceName,
                String.format("%s %s", convertTunnelTypetoString(tunType), "Trunk Interface"),
                true, tunType, srcIpAddr, dstIpAddr, gwIpAddr, 0, isDpn,
                false, defaultMonitorProtocol, null, false, null, null);
        ifaceIid = InterfaceIidHelper.buildId(trunkInterfaceName);
        ifState = InterfaceStateHelper.buildStateEntry(trunkInterfaceName, lportTag, new PhysAddress(mac),
                new BigInteger(src), portno);
        ifStateId = InterfaceStateHelper.buildStateInterfaceId(trunkInterfaceName);
    }

    public String getTrunkInterfaceName() {
        return trunkInterfaceName;
    }

    private String getTrunkInterfaceName(String parentRef) {
        return parentRef + IfmConstants.OF_URI_SEPARATOR + "trunk";
    }

    public static String convertTunnelTypetoString(Class<? extends TunnelTypeBase> tunType) {
        String tunnelType = "VXLAN";
        if (tunType.equals(TunnelTypeVxlan.class)) {
            tunnelType = "VXLAN";
        } else if (tunType.equals(TunnelTypeGre.class)) {
            tunnelType = "GRE";
        } else if (tunType.equals(TunnelTypeMplsOverGre.class)) {
            tunnelType = "MPLS_OVER_GRE";
        } else if (tunType.equals(TunnelTypeLogicalGroup.class)) {
            tunnelType = "LOGICAL_VXLAN_GROUP";
        }

        return tunnelType;
    }

    public String getMac() {
        return mac;
    }

    String getDummyMac() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(9));
            sb.append(random.nextInt(9));
            if (i < 5) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    private boolean isDpnDestination() {
        //BigInteger dpnId = new BigInteger(dst);
        return true;
    }

    public String getDst() {
        return dst;
    }

    public String getDstIp() {
        return dstIp;
    }

    public int getLportTag() {
        return lportTag;
    }

    public int getPortno() {
        return portno;
    }

    public String getSrc() {
        return src;
    }

    public Interface getIface() {
        return iface;
    }

    public InstanceIdentifier<Interface> getIfaceIid() {
        return ifaceIid;
    }

    public org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface getIfState() {
        return ifState;
    }

    public InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> getIfStateId() {
        return ifStateId;
    }
}
