/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import org.immutables.value.Value;

@Value.Immutable
@ImmutableStyle
public abstract class TunnelEndPointInfo {

    @Value.Parameter
    public abstract String getSrcEndPointInfo();

    @Value.Parameter
    public abstract String getDstEndPointInfo();
}
