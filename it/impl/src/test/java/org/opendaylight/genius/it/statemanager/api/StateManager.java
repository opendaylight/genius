/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.it.statemanager.api;

public interface StateManager {
    /**
     * This method is used to indicate if all services have been started
     *
     * @param ready indicates the readiness
     */
    void setReady(boolean ready);
    void setFail();
}
