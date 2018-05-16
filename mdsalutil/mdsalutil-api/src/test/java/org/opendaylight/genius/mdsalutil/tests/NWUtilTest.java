/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.genius.mdsalutil.NWUtil.getEtherTypeFromIpPrefix;
import static org.opendaylight.genius.mdsalutil.NWUtil.isIpAddressInRange;

import org.junit.Test;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.infrautils.testutils.Asserts;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

public class NWUtilTest {

    @Test
    public void testPositiveEtherTypeFromIpPrefix() {
        // IPv4 Positive cases
        assertEquals(getEtherTypeFromIpPrefix("10.0.1.10"), NwConstants.ETHTYPE_IPV4);
        assertEquals(getEtherTypeFromIpPrefix("172.168.10.20"), NwConstants.ETHTYPE_IPV4);
        assertEquals(getEtherTypeFromIpPrefix("225.200.100.10/32"), NwConstants.ETHTYPE_IPV4);
        assertEquals(getEtherTypeFromIpPrefix("254.200.100.10/8"), NwConstants.ETHTYPE_IPV4);

        //IPv6 Positive cases
        assertEquals(getEtherTypeFromIpPrefix("1001:db8:0:2:f816:3eff:fe5e:7e11"), NwConstants.ETHTYPE_IPV6);
        assertEquals(getEtherTypeFromIpPrefix("2001:db8:0:6:f816:3eff:fe5e:aabb"), NwConstants.ETHTYPE_IPV6);
        assertEquals(getEtherTypeFromIpPrefix("2001:db8:0:3:f816:3eff:fe5e:1111/128"), NwConstants.ETHTYPE_IPV6);
        assertEquals(getEtherTypeFromIpPrefix("2001:db8:0:3:f816:3eff:fe5e:ff/64"), NwConstants.ETHTYPE_IPV6);
    }

    @Test
    public void testNegativeEtherTypeFromIpPrefix() {
        //IPv4 Negative cases
        IllegalArgumentException thrown = null;
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("10.0.1.256");
        });
        assertEquals("Cannot create IpAddress from 10.0.1.256", thrown.getMessage());
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("172.168.290.2");
        });
        assertEquals("Cannot create IpAddress from 172.168.290.2", thrown.getMessage());
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("225.0.1.256/32");
        });
        assertEquals("Cannot create IpAddress from 225.0.1.256", thrown.getMessage());
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("254.200.100.256/28");
        });
        assertEquals("Cannot create IpAddress from 254.200.100.256", thrown.getMessage());

        //IPv6 Negative cases
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("1001:db8:0:2:f816:3eff:fe5e:7e1fg");
        });
        assertEquals("Cannot create IpAddress from 1001:db8:0:2:f816:3eff:fe5e:7e1fg", thrown.getMessage());
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("2001:db8:0:6:f81k::");
        });
        assertEquals("Cannot create IpAddress from 2001:db8:0:6:f81k::", thrown.getMessage());
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("2001:db8:0:ffw::/64");
        });
        assertEquals("Cannot create IpAddress from 2001:db8:0:ffw::", thrown.getMessage());
        thrown = Asserts.assertThrows(IllegalArgumentException.class, () -> {
            getEtherTypeFromIpPrefix("aaaa:bbbb:cccc:dddd:eeee:ffff:gggg:hhhh/128");
        });
        assertEquals("Cannot create IpAddress from aaaa:bbbb:cccc:dddd:eeee:ffff:gggg:hhhh",
                thrown.getMessage());
    }

    public void testIsIpAddressInRange() {
        // Validate for IPv4
        assertTrue(isIpAddressInRange(buildIpAddress("1.1.1.1"), buildIpPrefix("1.1.1.0/24")));
        assertTrue(isIpAddressInRange(buildIpAddress("10.0.0.10"), buildIpPrefix("10.0.0.0/24")));
        assertTrue(isIpAddressInRange(buildIpAddress("20.0.0.20"), buildIpPrefix("20.0.0.0/24")));
        assertTrue(isIpAddressInRange(buildIpAddress("20.0.1.20"), buildIpPrefix("20.0.0.0/8")));

        // Negative cases
        assertFalse(isIpAddressInRange(buildIpAddress("10.0.1.10"), buildIpPrefix("10.0.0.0/24")));
        assertFalse(isIpAddressInRange(buildIpAddress("10.0.0.10"), buildIpPrefix("20.0.0.0/24")));
        assertFalse(isIpAddressInRange(buildIpAddress("10.0.0.10"), buildIpPrefix("1001:db8:0:2::/64")));

        // Validate for IPv6
        assertTrue(isIpAddressInRange(buildIpAddress("1001:db8:0:2:f816:3eff:fe5e:7e1"),
                buildIpPrefix("1001:db8:0:2::/64")));
        assertTrue(isIpAddressInRange(buildIpAddress("2001:db8:0:2:f816:3eff:fe5e:1111"),
                buildIpPrefix("2001:db8:0:2::/64")));

        // Negative cases
        assertFalse(isIpAddressInRange(buildIpAddress("2001:db8:0:3:f816:3eff:fe5e:1111"),
                buildIpPrefix("2001:db8:0:2::/64")));
        assertFalse(isIpAddressInRange(buildIpAddress("1001:db8:0:2:f816:3eff:fe5e:1111"),
                buildIpPrefix("2001:db8:0:2::/64")));
        assertFalse(isIpAddressInRange(buildIpAddress("1001:db8:0:2:f816:3eff:fe5e:1111"),
                buildIpPrefix("10.0.0.0/24")));
    }

    @Test
    public void testIsIpv6AddressInRange() {
        // Positive cases
        assertTrue(isIpAddressInRange(Ipv6Address.getDefaultInstance("1001:db8:0:2:f816:3eff:fe5e:7e1"),
                Ipv6Prefix.getDefaultInstance("1001:db8:0:2::/64")));
        assertTrue(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8:0:2:f816:3eff:fe5e:1111"),
                Ipv6Prefix.getDefaultInstance("2001:db8:0:2::/64")));
        assertTrue(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::1234"),
                Ipv6Prefix.getDefaultInstance("2001:db8::/32")));
        assertTrue(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:0:db8::12"),
                Ipv6Prefix.getDefaultInstance("2001:0:db8::/24")));
        assertTrue(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::1"),
                Ipv6Prefix.getDefaultInstance("2001:db8::/127")));
        assertTrue(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::3"),
                Ipv6Prefix.getDefaultInstance("2001:db8::/126")));
        assertTrue(isIpAddressInRange(Ipv6Address.getDefaultInstance("fe80::a00:27fc"),
                Ipv6Prefix.getDefaultInstance("fe80::a00:27ff/126")));

        // Negative cases
        assertFalse(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8:0:3:f816:3eff:fe5e:1111"),
                Ipv6Prefix.getDefaultInstance("2001:db8:0:2::/64")));
        assertFalse(isIpAddressInRange(Ipv6Address.getDefaultInstance("1001:db8:0:2:f816:3eff:fe5e:1111"),
                Ipv6Prefix.getDefaultInstance("2001:db8:0:2::/64")));
        assertFalse(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::2"),
                Ipv6Prefix.getDefaultInstance("2001:db8::/127")));
        assertFalse(isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::4"),
                Ipv6Prefix.getDefaultInstance("2001:db8::/126")));
        assertFalse(isIpAddressInRange(Ipv6Address.getDefaultInstance("fe80::a00:27f1"),
                Ipv6Prefix.getDefaultInstance("fe80::a00:27ff/126")));
    }

    private IpAddress buildIpAddress(String ipAddress) {
        return new IpAddress(ipAddress.toCharArray());
    }

    private IpPrefix buildIpPrefix(String cidr) {
        return new IpPrefix(cidr.toCharArray());
    }
}
