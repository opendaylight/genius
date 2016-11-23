/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.genius.itm.impl.ItmProvider;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;

import static org.mockito.Mockito.mock;

public class ItmProviderTest {

    @Mock DataBroker dataBroker;
    @Mock IMdsalApiManager mdsalManager;

    @Test
    public void testClose() throws Exception {
        ItmProvider provider = new ItmProvider( dataBroker,mdsalManager);
        // ensure no exceptions
        // currently this method is empty
        provider.close();
    }

}
