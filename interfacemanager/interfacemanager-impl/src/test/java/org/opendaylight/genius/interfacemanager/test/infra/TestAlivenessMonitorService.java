
package org.opendaylight.genius.interfacemanager.test.infra;
/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

/**
 * To facilitate Listener bindings in InterfaceMangerTestModule
 *
 * @author Edwin Anthony
 */
public abstract class TestAlivenessMonitorService implements AlivenessMonitorService {

    public static TestAlivenessMonitorService newInstance() {
        return Mockito.mock(TestAlivenessMonitorService.class, realOrException());
    }
}
