/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.globals;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;

public class InterfaceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum InterfaceType {
        VLAN_INTERFACE,
        VXLAN_TRUNK_INTERFACE,
        GRE_TRUNK_INTERFACE,
        VXLAN_VNI_INTERFACE,
        MPLS_OVER_GRE,
        MPLS_OVER_UDP,
        LOGICAL_GROUP_INTERFACE,
        UNKNOWN_INTERFACE
    }

    public enum InterfaceAdminState {
        ENABLED, DISABLED
    }

    public enum InterfaceOpState {
        UP, DOWN, UNKNOWN;

        public static InterfaceOpState fromModel(Interface.OperStatus modelStatus) {
            switch (modelStatus) {
                case Up:
                    return UP;
                case Down:
                    return DOWN;
                default:
                    return UNKNOWN;
            }
        }
    }

    protected InterfaceType interfaceType;
    protected int interfaceTag;
    protected BigInteger dpId = IfmConstants.INVALID_DPID;
    protected InterfaceAdminState adminState = InterfaceAdminState.ENABLED;
    protected InterfaceOpState opState;
    protected long groupId;
    protected long l2domainGroupId;
    protected int portNo = IfmConstants.INVALID_PORT_NO;
    protected String portName;
    protected String interfaceName;
    protected boolean isUntaggedVlan;
    protected String macAddress;

    public InterfaceInfo(BigInteger dpId, String portName) {
        this.dpId = dpId;
        this.portName = portName;
    }

    public InterfaceInfo(String portName) {
        this.portName = portName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public boolean isOperational() {
        return adminState == InterfaceAdminState.ENABLED && opState == InterfaceOpState.UP;
    }

    public InterfaceType getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(InterfaceType lportType) {
        this.interfaceType = lportType;
    }

    public int getInterfaceTag() {
        return interfaceTag;
    }

    public void setInterfaceTag(int interfaceTag) {
        this.interfaceTag = interfaceTag;
    }

    public void setUntaggedVlan(boolean untaggedVlan) {
        this.isUntaggedVlan = untaggedVlan;
    }

    public boolean isUntaggedVlan() {
        return isUntaggedVlan;
    }

    // "Confusing to have methods getDpId() and BridgeEntryBuilder.getDpid" - BridgeEntryBuilder is generated so can't
    // change it.
    @SuppressFBWarnings("NM_CONFUSING")
    public BigInteger getDpId() {
        return dpId;
    }

    public void setDpId(BigInteger dpId) {
        this.dpId = dpId;
    }

    public InterfaceAdminState getAdminState() {
        return adminState;
    }

    public void setAdminState(InterfaceAdminState adminState) {
        this.adminState = adminState;
    }

    public InterfaceOpState getOpState() {
        return opState;
    }

    public void setOpState(InterfaceOpState opState) {
        this.opState = opState;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getL2domainGroupId() {
        return l2domainGroupId;
    }

    public void setL2domainGroupId(long l2domainGroupId) {
        this.l2domainGroupId = l2domainGroupId;
    }

    public int getPortNo() {
        return portNo;
    }

    public void setPortNo(int portNo) {
        this.portNo = portNo;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    // Confusing to have methods getPortName() and GetPortFromInterfaceOutputBuilder.getPortname()" -
    // GetPortFromInterfaceOutputBuilder is generated so can't change it.
    @SuppressFBWarnings("NM_CONFUSING")
    public String getPortName() {
        return this.portName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
