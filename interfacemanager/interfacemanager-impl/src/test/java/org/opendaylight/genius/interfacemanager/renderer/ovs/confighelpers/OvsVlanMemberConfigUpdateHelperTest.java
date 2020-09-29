/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * Unit tests for {@link OvsVlanMemberConfigUpdateHelper}.
 */
public class OvsVlanMemberConfigUpdateHelperTest {
    @Test
    public void testVlanIdModified() {
        VlanId vlanId1 = new VlanId(Uint16.ONE);
        VlanId vlanId2 = new VlanId(Uint16.TWO);
        assertFalse(OvsVlanMemberConfigUpdateHelper.vlanIdModified(null, null));
        assertTrue(OvsVlanMemberConfigUpdateHelper.vlanIdModified(null, vlanId2));
        assertTrue(OvsVlanMemberConfigUpdateHelper.vlanIdModified(vlanId1, null));
        assertFalse(OvsVlanMemberConfigUpdateHelper.vlanIdModified(vlanId1, vlanId1));
        assertTrue(OvsVlanMemberConfigUpdateHelper.vlanIdModified(vlanId1, vlanId2));
    }
}
