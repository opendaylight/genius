/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

/**
 * {@link MatchIcmpv6} builder for XtendBeanGenerator.
 */
public class MatchIcmpv6Builder {
    private short code;
    private short type;

    public MatchIcmpv6Builder() {
        // Nothing to do
    }

    public MatchIcmpv6Builder setCode(short code) {
        this.code = code;
        return this;
    }

    public MatchIcmpv6Builder setType(short type) {
        this.type = type;
        return this;
    }

    public MatchIcmpv6 build() {
        return new MatchIcmpv6(type, code);
    }
}
