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
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Data Broker ready-only transaction with asynchronous utility methods.
 *
 * <p>No close() method here, as this created from an external
 * {@link ReadTransaction}, which must be commited or canceled instead.
 *
 * @author Michael Vorburger.ch
 */
public interface AsyncReadTransaction {

    <T extends DataObject>
        Elser<Optional<T>> read(
            LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> reader);

    // perhaps generalize and rename this.. it really more of ifAbsent kind of thing
    interface Elser<T> extends CompletionStage2<T> {
        CompletionStage2<T> orElse(Runnable elser);
    }

}
