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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;

@Deprecated
public interface IInterfaceManager {
    @Deprecated
    public Long getPortForInterface(String ifName);

    @Deprecated
    public BigInteger getDpnForInterface(String ifName);

    @Deprecated
    public BigInteger getDpnForInterface(Interface intrf);

    @Deprecated
    public String getEndpointIpForDpn(BigInteger dpnId);

    @Deprecated
    public List<ActionInfo> getInterfaceEgressActions(String ifName);

    @Deprecated
    public Long getPortForInterface(Interface intf);

    public InterfaceInfo getInterfaceInfo(String intInfo);

    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName, InterfaceInfo.InterfaceType interfaceType);
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName);

    public void createVLANInterface(String interfaceName, String portName, BigInteger dpId,  Integer vlanId,
                             String description, IfL2vlan.L2vlanMode l2vlanMode) throws InterfaceAlreadyExistsException;

    public void createVLANInterface(String interfaceName, String portName, BigInteger dpId,  Integer vlanId,
            String description, IfL2vlan.L2vlanMode l2vlanMode, boolean isExternal) throws InterfaceAlreadyExistsException;

    public void bindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo);
    public void unbindService(String interfaceName, Class<? extends ServiceModeBase> serviceMode, BoundServices serviceInfo, String parentInterface);

    List<Interface> getVlanInterfaces();
    List<Interface> getVxlanInterfaces();
    public Interface getInterfaceInfoFromConfigDataStore(String interfaceName);
}
