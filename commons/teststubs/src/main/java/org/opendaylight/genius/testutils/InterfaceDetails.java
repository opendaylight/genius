/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import java.math.BigInteger;

import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class InterfaceDetails {

    private String elan;
    private String name;
    private BigInteger dpId;
    private int portno;
    private String mac;
    private String prefix;
    private int lportTag;
    private String parentName;

    private Interface iface;
    private InstanceIdentifier<Interface> ifaceIid;
    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState;
    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId;
    private InterfaceInfo interfaceInfo;

    public InterfaceDetails(String elan, String name, BigInteger dpId, int portno, String mac,
                            String prefix, int lportTag) {
        this.dpId = dpId;
        this.elan = elan;
        this.lportTag = lportTag;
        this.mac = mac;
        this.name = name;
        this.portno = portno;
        this.prefix = prefix;
        parentName = "tap" + name.substring(0, 12);
        iface = getInterface(name, parentName);
        ifaceIid = InterfaceIidHelper.buildId(name);
        ifState = InterfaceStateHelper.buildStateEntry(name, lportTag, new PhysAddress(mac), dpId, portno);
        ifStateId = InterfaceStateHelper.buildStateInterfaceId(name);
        interfaceInfo = InterfaceIidHelper.getInterfaceInfo(name, iface, dpId, portno, ifState);
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

    public BigInteger getDpId() {
        return dpId;
    }

    public String getElan() {
        return elan;
    }

    public int getLportTag() {
        return lportTag;
    }

    public String getMac() {
        return mac;
    }

    public String getName() {
        return name;
    }

    public int getPortno() {
        return portno;
    }

    public String getPrefix() {
        return prefix;
    }

    private Interface getInterface(String interfaceName, String parentName) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.InterfaceBuilder interfaceBuilder
                = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.InterfaceBuilder();
        interfaceBuilder.setDescription(interfaceName).setName(interfaceName);
        interfaceBuilder.setType(L2vlan.class);
        ParentRefs parentRefsBuilder = new ParentRefsBuilder().setParentInterface(parentName).build();
        return interfaceBuilder.addAugmentation(ParentRefs.class,parentRefsBuilder).build();
    }
}
