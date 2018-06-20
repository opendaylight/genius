/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

abstract class DatastoreTransaction<D extends Datastore> {
    private final LogicalDatastoreType datastoreType;

    DatastoreTransaction(Class<D> datastoreType) {
        this.datastoreType = Datastore.toType(datastoreType);
    }

    LogicalDatastoreType getDatastoreType() {
        return this.datastoreType;
    }
}
