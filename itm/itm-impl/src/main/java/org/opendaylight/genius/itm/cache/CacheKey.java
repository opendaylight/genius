/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Objects;
import org.opendaylight.yangtools.yang.common.Uint64;

final class CacheKey {
    private final Uint64 src;
    private final Uint64 dst;

    CacheKey(Uint64 src, Uint64 dst) {
        // FIXME: are nulls allowed here?
        this.src = src;
        this.dst = dst;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(src) * 31 + Objects.hashCode(dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CacheKey)) {
            return false;
        }
        final CacheKey other = (CacheKey) obj;
        return Objects.equals(src, other.src) && Objects.equals(dst, other.dst);
    }

    @Override
    public String toString() {
        return src + ":" + dst;
    }
}
