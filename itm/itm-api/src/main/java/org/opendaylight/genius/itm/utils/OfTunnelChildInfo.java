/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.utils;

import java.math.BigInteger;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;
import org.opendaylight.genius.infra.OpenDaylightImmutableStyle;

@Value.Immutable
@OpenDaylightImmutableStyle
public interface OfTunnelChildInfo {

    String getOfTunnelName();

    List<String> getOutChildTunnels();

    List<Pair<BigInteger, String>> getInDpnChildTunnels();
}
