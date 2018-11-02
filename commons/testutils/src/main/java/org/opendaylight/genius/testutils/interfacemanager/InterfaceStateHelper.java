/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils.interfacemanager;

import com.google.common.collect.Lists;
import java.math.BigInteger;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Other;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InterfaceStateHelper {

    private InterfaceStateHelper() {
    }

    public static InstanceIdentifier<Interface> buildStateInterfaceIid(String interfaceName) {
        return InstanceIdentifier
                .builder(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static Interface buildStateFromInterfaceInfo(InterfaceInfo interfaceInfo) {
        BigInteger dpId = interfaceInfo.getDpId();
        int portno = interfaceInfo.getPortNo();
        NodeConnectorId nodeConnectorId = new NodeConnectorId("openflow:" + dpId.toString() + ":" + portno);
        return new InterfaceBuilder()
                .setType(Other.class)
                .setIfIndex(interfaceInfo.getInterfaceTag())
                .setAdminStatus(AdminStatus.Up)
                .setOperStatus(OperStatus.Up)
                .setLowerLayerIf(Lists.newArrayList(nodeConnectorId.getValue()))
                .setPhysAddress(new PhysAddress(interfaceInfo.getMacAddress()))
                .withKey(new InterfaceKey(interfaceInfo.getInterfaceName()))
                .setStatistics(new StatisticsBuilder()
                        .setDiscontinuityTime(new DateAndTime(
                                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now())))
                        .build())
                .build();
    }
}
