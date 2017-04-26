/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils;

/**
 * DataBroker read/write transaction, combining of {@link AsyncReadTransaction}, {@link AsyncWriteTransaction}.
 *
 * @author Michael Vorburger.ch
 */
public interface AsyncReadWriteTransaction extends AsyncReadTransaction, AsyncWriteTransaction {
}
