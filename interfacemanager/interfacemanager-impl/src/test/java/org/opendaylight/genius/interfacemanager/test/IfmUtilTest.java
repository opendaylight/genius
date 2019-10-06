/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.common.Uint64;

public class IfmUtilTest {

    @Mock
    NodeConnectorId ncId;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDpnConversions() {
        String nodeId = IfmUtil.buildDpnNodeId(Uint64.valueOf(101)).getValue();
        assertEquals("openflow:101", nodeId);
        when(ncId.getValue()).thenReturn("openflow:101:11");
        assertEquals(Uint64.valueOf(101), IfmUtil.getDpnFromNodeConnectorId(ncId));
    }
}
