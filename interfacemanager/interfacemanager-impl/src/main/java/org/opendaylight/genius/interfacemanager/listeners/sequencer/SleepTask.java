/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners.sequencer;

import java.util.concurrent.Callable;

public class SleepTask implements Callable {

    @Override
    public Object call() throws Exception {
        return null;
    }

    @Override
    public String toString() {
        return "Singleton SleepTask for Interfaces";
    }
}