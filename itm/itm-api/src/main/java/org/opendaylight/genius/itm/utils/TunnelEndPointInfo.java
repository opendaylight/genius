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

@Value.Immutable
@OpenDaylightImmutableStyle
public interface TunnelEndPointInfo {
    // FIXME: we are converting these Strings to Uint64. It seems it would be better to keep them as Uint64 and/or
    //        have a lazy String cache.
    String getSrcEndPointInfo();

    String getDstEndPointInfo();
}
