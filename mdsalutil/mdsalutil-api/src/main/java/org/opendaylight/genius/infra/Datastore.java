/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

public interface Datastore {
    Class<Configuration> CONFIGURATION = Configuration.class;
    Class<Operational> OPERATIONAL = Operational.class;

    interface Configuration extends Datastore {}

    interface Operational extends Datastore {}

    static LogicalDatastoreType toType(Class<? extends Datastore> datastoreClass) {
        if (datastoreClass.equals(Configuration.class)) {
            return LogicalDatastoreType.CONFIGURATION;
        } else if (datastoreClass.equals(Operational.class)) {
            return LogicalDatastoreType.OPERATIONAL;
        } else {
            throw new IllegalArgumentException("Unknown datastore class " + datastoreClass);
        }
    }

    static Class<? extends Datastore> toClass(LogicalDatastoreType datastoreType) {
        switch (datastoreType) {
            case CONFIGURATION:
                return CONFIGURATION;
            case OPERATIONAL:
                return OPERATIONAL;
            default:
                throw new IllegalArgumentException("Unknown datastore type " + datastoreType);
        }
    }
}
