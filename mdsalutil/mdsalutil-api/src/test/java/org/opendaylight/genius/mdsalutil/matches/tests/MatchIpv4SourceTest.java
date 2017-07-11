/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches.tests;

import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import org.junit.ComparisonFailure;
import org.junit.Test;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Source;

public class MatchIpv4SourceTest {

    @Test(expected = ComparisonFailure.class)
    public void testMatchIpv4SourceAssertEqualBeans() {
        assertEqualBeans(new MatchIpv4Source("10.0.0.1", "32"), new MatchIpv4Source("10.0.0.2", "32"));
    }

}
