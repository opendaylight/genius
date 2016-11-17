/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods around DataBroker.
 *
 * @author Michael Vorburger
 */
public class DataBrokerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerUtils.class);

    // TODO JavaDoc ...

    public static <T extends DataObject> Optional<T> syncReadOptional(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws ReadFailedException {

        return broker.newReadOnlyTransaction().read(datastoreType, path).checkedGet();
    }

    public static <T extends DataObject> Optional<T> syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {

        try {
            return broker.newReadOnlyTransaction().read(datastoreType, path).checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("ReadFailedException while reading data from {} store path {}; returning Optional.absent()",
                    datastoreType, path, e);
            return Optional.absent();
        }
    }

    public static <T extends DataObject> T syncRead(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws ExpectedDataObjectNotFoundException, ReadFailedException {

        Optional<T> optionalDataObject = broker.newReadOnlyTransaction().read(datastoreType, path).checkedGet();
        if (optionalDataObject.isPresent()) {
            return optionalDataObject.get();
        } else {
            throw new ExpectedDataObjectNotFoundException(datastoreType, path);
        }
    }

    // TODO syncWrite/syncUpdate/syncDelete, from org.opendaylight.genius.mdsalutil.MDSALUtil

    // TODO Move asyncWrite/asyncUpdate/asyncRemove from org.opendaylight.genius.mdsalutil.MDSALDataStoreUtils to here

}
