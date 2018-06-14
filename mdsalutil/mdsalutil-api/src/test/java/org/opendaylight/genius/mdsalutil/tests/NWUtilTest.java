/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.tests;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.genius.mdsalutil.NWUtil.getEtherTypeFromIpPrefix;

import org.junit.Test;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.infrautils.testutils.Asserts;

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
}
