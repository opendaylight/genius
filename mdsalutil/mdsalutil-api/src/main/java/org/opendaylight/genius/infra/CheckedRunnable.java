/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

/**
 * {@link Runnable} which can throw a checked exception.
 *
 * @author Michael Vorburger.ch
 */
@FunctionalInterface
public interface CheckedRunnable {

    void run() throws Exception;

}
