/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.genius.itm.impl.ItmProvider;

import static org.mockito.Mockito.mock;

public class ItmProviderTest {
    @Test
    public void testOnSessionInitiated() {
        ItmProvider provider = new ItmProvider();

        // ensure no exceptions
        // currently this method is empty
        provider.onSessionInitiated(mock(BindingAwareBroker.ProviderContext.class));
    }

    @Test
    public void testClose() throws Exception {
        ItmProvider provider = new ItmProvider();

        // ensure no exceptions
        // currently this method is empty
        provider.close();
    }
}
