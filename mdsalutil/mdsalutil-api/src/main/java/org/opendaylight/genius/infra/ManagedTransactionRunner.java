/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import javax.annotation.CheckReturnValue;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;

/**
 * TODO Doc.
 */
public interface ManagedTransactionRunner {

    /**
     * The passed in WriteTransaction should not (cannot) be
     * {@link WriteTransaction#cancel()}, {@link WriteTransaction#commit()} or
     * {@link WriteTransaction#submit()}.
     */
    @CheckReturnValue
    void callWithNewWriteOnlyTransactionAndSubmit(CheckedConsumer<WriteTransaction> txRunner);

}
