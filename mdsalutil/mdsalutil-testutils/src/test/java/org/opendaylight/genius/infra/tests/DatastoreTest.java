/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.Datastore;

public class DatastoreTest {

    @Test
    public void testDatastore() {
        assertThat(Datastore.toType(Datastore.CONFIGURATION)).isEqualTo(LogicalDatastoreType.CONFIGURATION);
        assertThat(Datastore.toType(Datastore.OPERATIONAL)).isEqualTo(LogicalDatastoreType.OPERATIONAL);
        assertThrows(NullPointerException.class, () -> Datastore.toType(null));

        assertThat(Datastore.toClass(LogicalDatastoreType.CONFIGURATION)).isEqualTo(Datastore.CONFIGURATION);
        assertThat(Datastore.toClass(LogicalDatastoreType.OPERATIONAL)).isEqualTo(Datastore.OPERATIONAL);
        assertThrows(NullPointerException.class, () -> Datastore.toClass(null));
    }

}
