/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import org.mockito.Mockito;
import org.opendaylight.genius.itm.api.IITMProvider;

public abstract class TestItmProvider implements IITMProvider {

    public static TestItmProvider newInstance() {
        return Mockito.mock(TestItmProvider.class, realOrException());
    }
}
