/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.ipv6util.api;

public enum Icmpv6Type {

    ECHO_REQUEST((short) 128),
    ECHO_REPLY((short) 129),
    MULTICAST_LISTENER_QUERY((short) 130),
    MULTICAST_LISTENER_REPORT((short) 131),
    MULTICAST_LISTENER_DONE((short) 132),
    ROUTER_SOLICITATION((short) 133),
    ROUTER_ADVETISEMENT((short) 134),
    NEIGHBOR_SOLICITATION((short) 135),
    NEIGHBOR_ADVERTISEMENT((short) 136);

    private final short icmpv6TypeValue;

    Icmpv6Type(short icmpv6TypeValue) {
        this.icmpv6TypeValue = icmpv6TypeValue;
    }

    public short getValue() {
        return this.icmpv6TypeValue;
    }
}
