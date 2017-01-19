/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.cache;

/**
 * Utility for use in tests which cover code that still ;-( use the deprecated CacheUtil.
 *
 * @author Michael Vorburger
 */
public final class CacheTestUtil {

    private CacheTestUtil() {
    }

    @SuppressWarnings("deprecation")
    public static void clearAllCaches() {
        CacheUtil.MAP_OF_MAP.clear();
    }
}
