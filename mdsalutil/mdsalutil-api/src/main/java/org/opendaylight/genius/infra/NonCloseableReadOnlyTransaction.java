/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import org.opendaylight.controller.md.sal.binding.api.ForwardingReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;

/**
 * ReadOnlyTransaction which cannot be {@link ReadOnlyTransaction#close()}.
 *
 * @author Michael Vorburger.ch
 */
// intentionally package local, for now
class NonCloseableReadOnlyTransaction extends ForwardingReadOnlyTransaction {

    NonCloseableReadOnlyTransaction(ReadOnlyTransaction delegate) {
        super(delegate);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("close() cannot be used inside a Managed[New]TransactionRunner");
    }

}
