/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils;

import java.util.function.Function;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.genius.databrokerutils.infra.CompletionStage2;

/**
 * Utility to make working with the DataBroker in async style easier.
 *
 * @author Michael Vorburger.ch
 */
public interface AsyncDataBroker {

    AsyncReadOnlyTransaction newAsyncReadOnlyTransaction();

    AsyncReadWriteTransaction newAsyncReadWriteTransaction();

    AsyncReadTransaction asAsyncReadTransaction(ReadTransaction tx);

    CompletionStage2<?> withNewSingleAsyncReadOnlyTransaction(
            Function<AsyncReadTransaction, CompletionStage2<?>> lambda);

    CompletionStage2<?> withNewSingleAsyncReadWriteTransaction(
            Function<AsyncReadWriteTransaction, CompletionStage2<?>> lambda);

}
