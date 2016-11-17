/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Exception thrown to code which expects data to necessarily be present in the data store.
 *
 * @see DataBrokerUtils#syncRead(DataBroker, LogicalDatastoreType, InstanceIdentifier)
 *
 * @author Michael Vorburger
 */
public class ExpectedDataObjectNotFoundException extends ReadFailedException {

    private static final long serialVersionUID = 1L;

    public <T extends DataObject> ExpectedDataObjectNotFoundException(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {

        super("Expected to find data in " + datastoreType + " at " + path + ", but there was none");
    }

}
