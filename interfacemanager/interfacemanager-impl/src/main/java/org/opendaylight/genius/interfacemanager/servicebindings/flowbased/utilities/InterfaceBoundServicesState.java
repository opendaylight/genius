/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities;


import java.io.Serializable;
import java.math.BigInteger;

import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;


public class InterfaceBoundServicesState implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Class<? extends InterfaceType> interfaceType;

    private String portName;
    private String interfaceName;
    private int ifIndex;
    private BigInteger dpId = IfmConstants.INVALID_DPID;
    private long groupId;
    private long portNo = IfmConstants.INVALID_PORT_NO;

    public InterfaceBoundServicesState(Class<? extends InterfaceType> interfaceType, String portName,
                                       String interfaceName, int ifIndex, BigInteger dpId, long groupId, long portNo) {
        this.interfaceType = interfaceType;
        this.portName = portName;
        this.interfaceName = interfaceName;
        this.ifIndex = ifIndex;
        this.dpId = dpId;
        this.groupId = groupId;
        this.portNo = portNo;
    }

    public InterfaceBoundServicesState(Interface interfaceState) {
        this.portName = interfaceState.getName();
        this.interfaceName = interfaceState.getName();
        this.ifIndex = interfaceState.getIfIndex();
        NodeConnectorId nodeConnectorId = IfmUtil.getNodeConnectorIdFromInterface(interfaceState);
        this.dpId = IfmUtil.getDpnFromNodeConnectorId(nodeConnectorId);
        this.portNo = IfmUtil.getPortNumberFromNodeConnectorId(nodeConnectorId);
        this.interfaceType = interfaceState.getType();
    }

    public String getPortName() {
        return portName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public int getIfIndex() {
        return ifIndex;
    }

    public BigInteger getDpId() {
        return dpId;
    }

    public long getGroupId() {
        return groupId;
    }

    public long getPortNo() {
        return portNo;
    }

    public Class<? extends InterfaceType> getInterfaceType() {
        return interfaceType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof InterfaceBoundServicesState)) {
            return false;
        }

        InterfaceBoundServicesState that = (InterfaceBoundServicesState) object;

        if (getIfIndex() != that.getIfIndex()) {
            return false;
        }
        if (getGroupId() != that.getGroupId()) {
            return false;
        }
        if (getPortNo() != that.getPortNo()) {
            return false;
        }
        if (!interfaceType.equals(that.interfaceType)) {
            return false;
        }
        if (getPortName() != null ? !getPortName().equals(that.getPortName()) : that.getPortName() != null) {
            return false;
        }
        if (!getInterfaceName().equals(that.getInterfaceName())) {
            return false;
        }
        return getDpId().equals(that.getDpId());

    }

    @Override
    public int hashCode() {
        int result = interfaceType.hashCode();
        result = 31 * result + (getPortName() != null ? getPortName().hashCode() : 0);
        result = 31 * result + getInterfaceName().hashCode();
        result = 31 * result + getIfIndex();
        result = 31 * result + getDpId().hashCode();
        result = 31 * result + (int) (getGroupId() ^ (getGroupId() >>> 32));
        result = 31 * result + (int) (getPortNo() ^ (getPortNo() >>> 32));
        return result;
    }
}
