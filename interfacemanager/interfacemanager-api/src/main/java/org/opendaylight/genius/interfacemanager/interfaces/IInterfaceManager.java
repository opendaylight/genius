/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.interfaces;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.genius.interfacemanager.exceptions.InterfaceAlreadyExistsException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;

public interface IInterfaceManager {
    @Deprecated
    Long getPortForInterface(String ifName);

    @Deprecated
    BigInteger getDpnForInterface(String ifName);

    @Deprecated
    BigInteger getDpnForInterface(Interface intrf);

    @Deprecated
    String getEndpointIpForDpn(BigInteger dpnId);

    @Deprecated
    List<ActionInfo> getInterfaceEgressActions(String ifName);

    @Deprecated
    Long getPortForInterface(Interface intf);

    InterfaceInfo getInterfaceInfo(String intInfo);

    InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName,
            InterfaceInfo.InterfaceType interfaceType);
    InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName);

    public Interface getInterfaceInfoFromConfigDataStore(String interfaceName);

    Interface getInterfaceInfoFromConfigDataStore(String interfaceName);

    void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId,
            String description, IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException;

    void createVLANInterface(String interfaceName, String portName, BigInteger dpId, Integer vlanId,
            String description, IfL2vlan.L2vlanMode l2vlanMode, boolean isExternal) throws InterfaceAlreadyExistsException;

    void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo);
    void unbindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo);
    List<Interface> getVlanInterfaces();
    List<Interface> getVxlanInterfaces();

    public boolean isExternalInterface(String interfaceName);
}
