/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

/**
 * Utility for use in tests which cover code that use the not-yet-de-static-ified  ItmUtils ItmCache.
 *
 * @author Michael Vorburger
 */
public final class ItmTestUtils {

    private ItmTestUtils() { }

    public static void clearAllItmCaches() {
        ItmUtils.ITM_CACHE.clear();
    }
}
