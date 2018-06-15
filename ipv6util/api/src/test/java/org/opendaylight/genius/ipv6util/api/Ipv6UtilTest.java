/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.ipv6util.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class Ipv6UtilTest {

    @Test
    public void testGetFormattedIpv6Address() {
        // Positive cases
        assertEquals(Ipv6Util.getFormattedIpAddress("1001:db8:0:2::1"), "1001:db8:0:2:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpAddress("2607:f0d0:1002:51::4"), "2607:f0d0:1002:51:0:0:0:4");
        assertEquals(Ipv6Util.getFormattedIpAddress("1001:db8:0:2:0:0:0:1"), "1001:db8:0:2:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpAddress("2001:db8::1"), "2001:db8:0:0:0:0:0:1");
        assertEquals(Ipv6Util.getFormattedIpAddress("fe80::a00:27f1"), "fe80:0:0:0:0:0:a00:27f1");
        assertEquals(Ipv6Util.getFormattedIpAddress("10.0.0.10"), "10.0.0.10");

        // Negative cases
        assertNull(Ipv6Util.getFormattedIpAddress("abcd-invalid"));
        assertNull(Ipv6Util.getFormattedIpAddress("1001:db8:0:2::/64"));
        assertNull(Ipv6Util.getFormattedIpAddress("20.0.0.10/24"));
    }
}
