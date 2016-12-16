/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.opendaylight.genius.itm.impl.ItmManagerRpcServiceTest;

@RunWith(Suite.class)
@SuiteClasses({ ItmTest.class, ItmManagerRpcServiceTest.class })
public class TemporarySuiteOfSeveralTestsToIllustrateTheIsolationIssueDueToStatic {
}
