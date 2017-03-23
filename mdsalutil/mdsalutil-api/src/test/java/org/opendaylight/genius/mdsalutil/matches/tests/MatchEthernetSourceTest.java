/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches.tests;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetDestination;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetSource;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

/**
 * Unit test for {@link MatchEthernetSource}.
 */
public class MatchEthernetSourceTest {

    @Test
    public void testCompareDifferentClasses() {
        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetDestination(new MacAddress("0D:AA:D8:42:30:F4"))))
            .isGreaterThan(0);
    }

    @Test
    public void testCompareBothOnlyAddress() {
        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"))))
            .isEqualTo(0);

        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F5"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"))))
            .isGreaterThan(0);

        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F5"))))
            .isLessThan(0);
    }

    @Test
    public void testCompareOneOnlyAddressOtherAddressAndMask() {
        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F5"), new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"))))
            .isGreaterThan(0);

        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F5"), new MacAddress("0D:AA:D8:42:30:F4"))))
            .isLessThan(0);
    }

    @Test
    public void testCompareBothAddressAndMask() {
        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F4"))))
            .isEqualTo(0);

        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F5"), new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F4"))))
            .isGreaterThan(0);

        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F5"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F4"))))
            .isGreaterThan(0);

        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F5"), new MacAddress("0D:AA:D8:42:30:F4"))))
            .isLessThan(0);

        assertThat(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F4"))
        .compareTo(new MatchEthernetSource(new MacAddress("0D:AA:D8:42:30:F4"), new MacAddress("0D:AA:D8:42:30:F5"))))
            .isLessThan(0);
    }

}
