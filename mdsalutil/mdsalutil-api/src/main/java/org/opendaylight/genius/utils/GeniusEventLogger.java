/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeniusEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(GeniusEventLogger.class);

    private GeniusEventLogger() { }

    public static void logInfo(Class loggerClass, String event, String objects) {
        LOG.info("Class {}, Event {}, Object {}", loggerClass, event, objects);
    }
}
