/*
 * Copyright © 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

/**
 * Strongly-typed representation of a datastore (configuration or operational).
 */
public interface Datastore {
    /** Class representing the configuration datastore. */
    Class<Configuration> CONFIGURATION = Configuration.class;

    /** Class representing the operational datastore. */
    Class<Operational> OPERATIONAL = Operational.class;

    interface Configuration extends Datastore {}

    interface Operational extends Datastore {}

    /**
     * Returns the logical datastore type corresponding to the given datastore class.
     *
     * @param datastoreClass The datastore class to convert.
     * @return The corresponding logical datastore type.
     * @throws IllegalArgumentException if the provided datastore class isn’t handled.
     */
    static LogicalDatastoreType toType(Class<? extends Datastore> datastoreClass) {
        if (Configuration.class.equals(datastoreClass)) {
            return LogicalDatastoreType.CONFIGURATION;
        } else if (Operational.class.equals(datastoreClass)) {
            return LogicalDatastoreType.OPERATIONAL;
        } else {
            throw new IllegalArgumentException("Unknown datastore class " + datastoreClass);
        }
    }

    /**
     * Returns the datastore class corresponding to the given logical datastore type.
     * @param datastoreType The logical datastore type to convert.
     * @return The corresponding datastore class.
     * @throws IllegalArgumentException if the provided logical datastore type isn’t handled.
     */
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
