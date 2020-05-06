/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;

/**
 * Unit Test for {@link ActionNxConntrack}.
 *
 * @author Michael Vorburger.ch
 */
public class ActionNxConntrackTest {

    private final XtendBeanGenerator generator = new XtendBeanGenerator();

    @Test
    public void testNxCtMark() {
        assertThat(generator.getExpression(new ActionNxConntrack.NxCtMark(123))).isEqualTo("new NxCtMark(123L)");
    }

    @Test
    public void testNxNat() {
        assertThat(new ActionNxConntrack.NxNat(123, 456, 789,
                IpAddressBuilder.getDefaultInstance("1.2.3.4"),
                IpAddressBuilder.getDefaultInstance("1.2.3.4"), 987, 654).toString()
                .equals("new NxNat(123, 456, 789, new IpAddress(new Ipv4Address(\"1.2.3.4\")), "
                + "new IpAddress(new Ipv4Address(\"1.2.3.4\")), 987, 654)"));
    }

}
