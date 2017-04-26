/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.internal;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.genius.databrokerutils.AsyncReadOnlyTransaction;

/**
 * {@link AsyncReadTransactionImpl} with {@link #close()}.
 *
 * @author Michael Vorburger.ch
 */
public class AsyncReadOnlyTransactionImpl extends AsyncReadTransactionImpl implements AsyncReadOnlyTransaction {

    public AsyncReadOnlyTransactionImpl(ReadOnlyTransaction tx) {
        super(tx);
    }

    @Override
    public void close() {
        ((ReadOnlyTransaction)tx).close();
    }

}
