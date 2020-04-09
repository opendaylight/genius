/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Exception thrown from code which expects data to necessarily be present in the
 * data store.
 *
 * <p>While this could be considered a functional problem instead of a technical error,
 * and thus should not extend ReadFailedException (which is for technical
 * errors), it does still for convenience of being able to propagate all
 * ReadFailedException with a single "throws ReadFailedException" clause,
 * instead of requiring you to declare "throws
 * ExpectedDataObjectNotFoundException, ReadFailedException".
 *
 * <p>This is because any code which treats this as functional error and catches it
 * is abusing the
 * {@link SingleTransactionDataBroker#syncRead(DataBroker, LogicalDatastoreType, InstanceIdentifier)}
 * method (the only one throwing this Exception), and should use another method:
 * If code IS expecting data to not be found, then it should never use that
 * method, but one of the alternatives in that class. If code is NEVER expecting
 * data to not be found, then this is, effectively, a technical error (in that
 * particular usage).
 *
 * @see SingleTransactionDataBroker#syncRead(DataBroker, LogicalDatastoreType, InstanceIdentifier)
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
