/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * An {@link ExceptionMapper} for {@link TransactionCommitFailedException}.
 *
 * <p>Like org.opendaylight.controller.md.sal.dom.broker.impl.TransactionCommitFailedExceptionMapper,
 * but that is in a bundle internal private implementation package, so not accessible for us here.
 *
 * <p>And org.opendaylight.mdsal.dom.broker.TransactionCommitFailedExceptionMapper
 * is for org.opendaylight.mdsal.common.api.TransactionCommitFailedException instead of for
 * org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException, of course.
 *
 * @author Michael Vorburger.ch
 */
// intentionally only package-local, for now
class TransactionCommitFailedExceptionMapper extends ExceptionMapper<TransactionCommitFailedException> {

    static final TransactionCommitFailedExceptionMapper SUBMIT_MAPPER = create("submit");

    static TransactionCommitFailedExceptionMapper create(String opName) {
        return new TransactionCommitFailedExceptionMapper(opName);
    }

    TransactionCommitFailedExceptionMapper(String opName) {
        super(opName, TransactionCommitFailedException.class);
    }

    @Override
    protected TransactionCommitFailedException newWithCause(String message, Throwable cause) {
        return new TransactionCommitFailedException(message, cause);
    }

}
