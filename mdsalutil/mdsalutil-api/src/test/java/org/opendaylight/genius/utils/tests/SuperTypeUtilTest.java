/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.tests;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import org.junit.Test;
import org.opendaylight.genius.utils.SuperTypeUtil;

/**
 * Unit Test for {@link SuperTypeUtil} .
 *
 * @author Michael Vorburger
 */
public class SuperTypeUtilTest {

    @Test
    public void getTypeParameter() {
        Thing thing = new Thing();
        assertThat(thing.clazzT).isEqualTo(File.class);
    }

}
