/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches.tests;

import static org.junit.Assert.assertEquals;

import org.junit.ComparisonFailure;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Source;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

public class MatchIpv4SourceTest {

    @Test(expected = ComparisonFailure.class)
    public void testMatchIpv4SourceAssertEqual() {
        MatchIpv4Source matchIpv4Source1 = new MatchIpv4Source(new Ipv4Prefix("10.0.0.1/32"));
        MatchIpv4Source matchIpv4Source2 = new MatchIpv4Source(new Ipv4Prefix("10.0.0.2/32"));
        assertEquals(matchIpv4Source1.toString(), matchIpv4Source2.toString());
    }

}
