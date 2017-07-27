/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;

/*
 * Created by Anuradha Raju on 02-Feb-16.
 */
public class HwVtep {

    private String transportZone;
    private Class<? extends TunnelTypeBase> tunnelType;
    private IpPrefix ipPrefix;
    private IpAddress gatewayIP;
    private int vlanID;
    private String topoId;
    private String nodeId;
    private IpAddress hwIp;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        HwVtep hwVtep = (HwVtep) obj;

        if (vlanID != hwVtep.vlanID) {
            return false;
        }
        if (!transportZone.equals(hwVtep.transportZone)) {
            return false;
        }
        if (tunnelType != null ? !tunnelType.equals(hwVtep.tunnelType) : hwVtep.tunnelType != null) {
            return false;
        }
        if (!ipPrefix.equals(hwVtep.ipPrefix)) {
            return false;
        }
        if (gatewayIP != null ? !gatewayIP.equals(hwVtep.gatewayIP) : hwVtep.gatewayIP != null) {
            return false;
        }
        if (!topoId.equals(hwVtep.topoId)) {
            return false;
        }
        if (!nodeId.equals(hwVtep.nodeId)) {
            return false;
        }
        return hwIp.equals(hwVtep.hwIp);
    }

    @Override
    public String toString() {
        return "HwVtep{"
                + "transportZone='" + transportZone + '\''
                + ", tunnelType=" + tunnelType
                + ", ipPrefix=" + ipPrefix
                + ", gatewayIP=" + gatewayIP
                + ", vlanID=" + vlanID
                + ", topoId='" + topoId + '\''
                + ", nodeId='" + nodeId + '\''
                + ", hwIp=" + hwIp + '}';
    }

    @Override
    public int hashCode() {
        int result = transportZone.hashCode();
        result = 31 * result + (tunnelType != null ? tunnelType.hashCode() : 0);
        result = 31 * result + ipPrefix.hashCode();
        result = 31 * result + (gatewayIP != null ? gatewayIP.hashCode() : 0);
        result = 31 * result + vlanID;
        result = 31 * result + topoId.hashCode();
        result = 31 * result + nodeId.hashCode();
        result = 31 * result + hwIp.hashCode();
        return result;
    }

    public String getTransportZone() {
        return transportZone;
    }

    public void setTransportZone(String transportZone) {
        this.transportZone = transportZone;
    }

    public Class<? extends TunnelTypeBase> getTunnelType() {
        return tunnelType;
    }

    public void setTunnelType(Class<? extends TunnelTypeBase> tunnelType) {
        this.tunnelType = tunnelType;
    }

    public IpPrefix getIpPrefix() {
        return ipPrefix;
    }

    public void setIpPrefix(IpPrefix ipPrefix) {
        this.ipPrefix = ipPrefix;
    }

    public IpAddress getGatewayIP() {
        return gatewayIP;
    }

    public void setGatewayIP(IpAddress gatewayIP) {
        this.gatewayIP = gatewayIP;
    }

    public int getVlanID() {
        return vlanID;
    }

    public void setVlanID(int vlanID) {
        this.vlanID = vlanID;
    }

    public String getTopoId() {
        return topoId;
    }

    public void setTopoId(String topoId) {
        this.topoId = topoId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public IpAddress getHwIp() {
        return hwIp;
    }

    public void setHwIp(IpAddress hwIp) {
        this.hwIp = hwIp;
    }
}
