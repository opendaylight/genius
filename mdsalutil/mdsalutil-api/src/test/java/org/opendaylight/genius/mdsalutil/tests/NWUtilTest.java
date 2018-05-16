/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public class NWUtilTest {

    @Test
    public void testIsIpAddressInRange() {
        // Validate for IPv4
        boolean result = NWUtil.isIpAddressInRange(buildIpAddress("1.1.1.1"), "1.1.1.0/24");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(buildIpAddress("10.0.0.10"), "10.0.0.0/24");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(buildIpAddress("20.0.0.20"), "20.0.0.0/24");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(buildIpAddress("20.0.1.20"), "20.0.0.0/8");
        assertTrue(result);

        result = NWUtil.isIpAddressInRange(buildIpAddress("10.0.1.10"), "10.0.0.0/24");
        assertFalse(result);
        result = NWUtil.isIpAddressInRange(buildIpAddress("10.0.0.10"), "20.0.0.0/24");
        assertFalse(result);

        // Validate for IPv6
        result = NWUtil.isIpAddressInRange(buildIpAddress("1001:db8:0:2:f816:3eff:fe5e:7e1"), "1001:db8:0:2::/64");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(buildIpAddress("2001:db8:0:2:f816:3eff:fe5e:1111"), "2001:db8:0:2::/64");
        assertTrue(result);

        // Negative cases
        result = NWUtil.isIpAddressInRange(buildIpAddress("2001:db8:0:3:f816:3eff:fe5e:1111"), "2001:db8:0:2::/64");
        assertFalse(result);
        result = NWUtil.isIpAddressInRange(buildIpAddress("1001:db8:0:2:f816:3eff:fe5e:1111"), "2001:db8:0:2::/64");
        assertFalse(result);
    }

    @Test
    public void testIsIpv6AddressInRange() {
        // Positive cases
        boolean result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("1001:db8:0:2:f816:3eff:fe5e:7e1"),
                "1001:db8:0:2::/64");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8:0:2:f816:3eff:fe5e:1111"),
                "2001:db8:0:2::/64");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::1234"),
                "2001:db8::/32");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:0:db8::12"),
                "2001:0:db8::/24");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::1"),
                "2001:db8::/127");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::3"),
                "2001:db8::/126");
        assertTrue(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("fe80::a00:27fc"),
                "fe80::a00:27ff/126");
        assertTrue(result);

        // Negative cases
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8:0:3:f816:3eff:fe5e:1111"),
                "2001:db8:0:2::/64");
        assertFalse(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("1001:db8:0:2:f816:3eff:fe5e:1111"),
                "2001:db8:0:2::/64");
        assertFalse(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::2"),
                "2001:db8::/127");
        assertFalse(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("2001:db8::4"),
                "2001:db8::/126");
        assertFalse(result);
        result = NWUtil.isIpAddressInRange(Ipv6Address.getDefaultInstance("fe80::a00:27f1"),
                "fe80::a00:27ff/126");
        assertFalse(result);
    }

    private IpAddress buildIpAddress(String ipAddress) {
        return new IpAddress(ipAddress.toCharArray());
    }
}
