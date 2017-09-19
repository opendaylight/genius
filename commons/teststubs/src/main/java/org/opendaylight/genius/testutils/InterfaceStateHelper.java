/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;



public class InterfaceStateHelper {

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
            .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> buildStateInterfaceId(
            String interfaceName) {
        InstanceIdentifier.InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn
                .ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.state.Interface> idBuilder = InstanceIdentifier
                .builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn
                                .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                .ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                                interfaceName));
        return idBuilder.build();
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.InterfaceKey getStateInterfaceKeyFromName(
            final String name) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.InterfaceKey(name);
    }


    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface buildStateEntry(
            final String interfaceName,
            final int lportTag,
            final PhysAddress physAddress,
            BigInteger dpId,
            int portno) {
        final NodeConnectorId nodeConnectorId = new NodeConnectorId("openflow:" + dpId.toString() + ":" + portno);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setType(Other.class)
                .setIfIndex(IfmConstants.DEFAULT_IFINDEX);
        ifaceBuilder.setIfIndex(lportTag);
        List<String> childLowerLayerIfList = new ArrayList<>();
        if (nodeConnectorId != null) {
            childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        }
        ifaceBuilder
                .setAdminStatus(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                        .interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up)
                .setOperStatus(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                        .interfaces.rev140508.interfaces.state.Interface.OperStatus.Up)
                .setLowerLayerIf(childLowerLayerIfList);
        if (physAddress != null) {
            ifaceBuilder.setPhysAddress(physAddress);
        }
        ifaceBuilder.setKey(getStateInterfaceKeyFromName(interfaceName));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = ifaceBuilder
                .build();
        return ifState;
    }
}
