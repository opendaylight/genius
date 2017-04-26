/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * DataBroker write transaction with asynchronous utility methods, and
 * {@link #commit()} or {@link #cancel()}, one of which MUST be invoked.
 *
 * <p>
 * Modeled, of course, after {@link WriteTransaction}.
 *
 * @author Michael Vorburger.ch
 */
public interface AsyncWriteTransaction {

    <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data);

    <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents);

    <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data);

    <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents);

    void delete(LogicalDatastoreType store, InstanceIdentifier<?> path);

    CompletionStage2<Void> submit();

    boolean cancel();
}
