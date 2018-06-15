/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.ipv6util.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.infrautils.testutils.Asserts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public class Ipv6UtilTest {

    @Test
    public void testGetFormattedIpAddress() {
        // Positive cases
        assertEquals(Ipv6Util.getFormattedIpAddress(new IpAddress("1001:db8:0:2::1".toCharArray())),
                "1001:db8:0:2:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpAddress(new IpAddress("2607:f0d0:1002:51::4".toCharArray())),
                "2607:f0d0:1002:51:0:0:0:4");
        assertEquals(Ipv6Util.getFormattedIpAddress(new IpAddress("1001:db8:0:2:0:0:0:1".toCharArray())),
                "1001:db8:0:2:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpAddress(new IpAddress("2001:db8::1".toCharArray())),
                "2001:db8:0:0:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpAddress(new IpAddress("fe80::a00:27f1".toCharArray())),
                "fe80:0:0:0:0:0:a00:27f1");
        assertEquals(Ipv6Util.getFormattedIpAddress(new IpAddress("10.0.0.10".toCharArray())), "10.0.0.10");

        // Negative cases
        Asserts.assertThrows(IllegalArgumentException.class, () -> {
            Ipv6Util.getFormattedIpAddress(new IpAddress("abcd-invalid".toCharArray()));
        });
        Asserts.assertThrows(IllegalArgumentException.class, () -> {
            Ipv6Util.getFormattedIpAddress(new IpAddress("1001:db8:0:2::/64".toCharArray()));
        });
        Asserts.assertThrows(IllegalArgumentException.class, () -> {
            Ipv6Util.getFormattedIpAddress(new IpAddress("20.0.0.10/24".toCharArray()));
        });
    }

    @Test
    public void testGetFormattedIpv6Address() {
        // Positive cases
        assertEquals(Ipv6Util.getFormattedIpv6Address(Ipv6Address.getDefaultInstance("1001:db8:0:2::1")),
                "1001:db8:0:2:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpv6Address(Ipv6Address.getDefaultInstance("2607:f0d0:1002:51::4")),
                "2607:f0d0:1002:51:0:0:0:4");
        assertEquals(Ipv6Util.getFormattedIpv6Address(Ipv6Address.getDefaultInstance("1001:db8:0:2:0:0:0:1")),
                "1001:db8:0:2:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpv6Address(Ipv6Address.getDefaultInstance("2001:db8::1")),
                "2001:db8:0:0:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpv6Address(Ipv6Address.getDefaultInstance("fe80::a00:27f1")),
                "fe80:0:0:0:0:0:a00:27f1");

        // Negative cases
        Asserts.assertThrows(IllegalArgumentException.class, () -> {
            Ipv6Util.getFormattedIpv6Address(Ipv6Address.getDefaultInstance("abcd-invalid"));
        });
        Asserts.assertThrows(IllegalArgumentException.class, () -> {
            Ipv6Util.getFormattedIpv6Address(Ipv6Address.getDefaultInstance("1001:db8:0:2::/64"));
        });
    }
}
