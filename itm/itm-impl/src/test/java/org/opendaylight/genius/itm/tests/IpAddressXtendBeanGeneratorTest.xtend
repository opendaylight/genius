/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests

import ch.vorburger.xtendbeans.AssertBeans
import org.junit.Test
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

/**
 * Unit test for YANG gen. IpAddress with XtendBeanGenerator (AssertBeans).
 *
 * This didn't work with ch.vorburger.xtendbeans v1.2.0, but was fixed in v1.2.1.
 *
 * @author Michael Vorburger
 */
class IpAddressXtendBeanGeneratorTest {

    static def newTransportZone() {
        new TransportZoneBuilder >> [
            zoneName = "TZA"
            // ?? key = new TransportZoneKey("TZA")
            tunnelType = TunnelTypeVxlan
            subnets = #[
                new SubnetsBuilder >> [
                    gatewayIp = new IpAddress("0.0.0.0".toCharArray())
                    prefix = new IpPrefix("255.255.255.255/32".toCharArray())
                    vlanId = 0

                    vteps = #[
                        new VtepsBuilder >> [
                            dpnId = 2bi
                            ipAddress = new IpAddress(new Ipv4Address("192.168.56.40"))
                            portname = ""
                        ]
                    ]
                ]
            ]
        ]
    }

    @Test
    def void testXtendBeanGenerator() {
        // org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder
        // IpAddressBuilder has single private constructor
        AssertBeans.assertEqualByText('''
            (new TransportZoneBuilder => [
                key = new TransportZoneKey("TZA")
                subnets = #[
                    (new SubnetsBuilder => [
                        gatewayIp = new IpAddress(new Ipv4Address("0.0.0.0"))
                        key = new SubnetsKey(new IpPrefix(new Ipv4Prefix("255.255.255.255/32")))
                        prefix = new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))
                        vlanId = 0
                        vteps = #[
                            (new VtepsBuilder => [
                                dpnId = 2bi
                                ipAddress = new IpAddress(new Ipv4Address("192.168.56.40"))
                                key = new VtepsKey(2bi, "")
                                portname = ""
                            ]).build()
                        ]
                    ]).build()
                ]
                tunnelType = org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan
                zoneName = "TZA"
            ]).build()''', newTransportZone())
    }

}
