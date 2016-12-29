/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;

import java.math.BigInteger;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedTransportZoneObjects {

    static def newTransportZone() {
        new TransportZoneBuilder >> [
            zoneName = "TZA"
            tunnelType = TunnelTypeVxlan
            subnets = #[
                new SubnetsBuilder >> [
                gatewayIp = new IpAddress("0.0.0.0".toCharArray())
                prefix = new IpPrefix("255.255.255.255/32".toCharArray())
                vlanId = 0

                vteps = #[
                        new VtepsBuilder >> [
                            dpnId = BigInteger.valueOf(2)
                            ipAddress = new IpAddress("192.168.56.40".toCharArray())
                            portname = ""
                        ]
                    ]
                ]
            ]
        ]
    }

}
