/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils;

import java.util.Optional;
import java.util.function.Consumer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Open DataBroker transaction with asynchronous utility methods.
 *
 * @author Michael Vorburger.ch
 */
public interface AsyncReadTransaction extends AutoCloseable {
/*
    <T extends DataObject>
        CompletionStage2<T> readAnd(
            LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> reader);

    <T extends DataObject>
        CompletionStage2<T> readAndElse(
            LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> reader, Runnable elser);
*/
    <T extends DataObject>
        Elser<Optional<T>> read(
            LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> reader);

    // perhaps generalize and rename this.. it really more of ifAbsent kind of thing
    interface Elser<T> extends CompletionStage2<T> {
        CompletionStage2<T> orElse(Runnable elser);
    }

    @Override
    void close(); // NO throws Exception
}
