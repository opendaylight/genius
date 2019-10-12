/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.utils;

import org.immutables.value.Value;
import org.opendaylight.genius.infra.OpenDaylightImmutableStyle;
import org.opendaylight.yangtools.yang.common.Uint64;

@Value.Immutable
@OpenDaylightImmutableStyle
public interface TunnelEndPointInfo {

    Uint64 getSrcEndPointInfo();

    Uint64 getDstEndPointInfo();

    @Value.Lazy
    default String getSrcEndPointName() {
        return getSrcEndPointInfo().toString();
    }

    @Value.Lazy
    default String getDstEndPointName() {
        return getDstEndPointInfo().toString();
    }
}
